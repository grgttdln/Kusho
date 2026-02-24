# Activity Suggested Prompts + Empty Word Bank State Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add AI-generated suggested prompts to the Activity Creation Modal and show an empty-state message when the word bank has no words.

**Architecture:** Mirror the existing suggested-prompts pattern used by `WordBankGenerationModal`. A new `generateActivitySuggestedPrompts()` method in `GeminiRepository` generates 2 contextual activity-description prompts via Gemini. `LessonViewModel` caches these independently from word-generation prompts. The `ActivityCreationModal` composable gains a suggested-prompts section and an empty word bank state.

**Tech Stack:** Kotlin, Jetpack Compose, Google Generative AI SDK (Gemini), MVVM

---

### Task 1: Add `generateActivitySuggestedPrompts()` to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add the new method after `generateSuggestedPrompts()` (after line 410)**

Insert the following method right after the closing `}` of `generateSuggestedPrompts()` at line 410, before the `isValidPromptSuggestion()` method at line 412:

```kotlin
    /**
     * Generate 2 contextual prompt suggestions for the activity creation modal.
     * Only called when the word bank is non-empty.
     *
     * @param words Current word bank contents (must be non-empty)
     * @return List of exactly 2 activity prompt suggestion strings, or empty list on failure
     */
    suspend fun generateActivitySuggestedPrompts(words: List<Word>): List<String> = withContext(Dispatchers.IO) {
        try {
            val validWords = words.filter { it.word.isNotBlank() }
            if (validWords.isEmpty()) return@withContext emptyList()

            val analyses = analyzeCVCWords(validWords)
            val patterns = detectCVCPatterns(analyses)
            val wordList = if (validWords.size > 30) {
                "${validWords.take(30).joinToString(", ") { it.word }} (and ${validWords.size - 30} more)"
            } else {
                validWords.joinToString(", ") { it.word }
            }
            val patternsSummary = buildPatternsSummary(patterns)

            val systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). The teacher wants to create an air-writing activity set using words from their word bank.

An activity set groups words into themed activities for air-writing practice. Each activity contains words that share a learning objective (e.g., same word family, same vowel sound, same theme).

WORD BANK: $wordList

DETECTED PATTERNS:
$patternsSummary

Suggest exactly 2 concise prompts the teacher could use to describe what kind of activity set to create. Each prompt should be a natural sentence (10-20 words) describing a learning objective or grouping strategy.

Good examples: "Practice the -at word family with rhyming words", "Group words by short vowel sounds for comparison"
Bad examples: "Create an activity" (too vague), "Generate words" (wrong purpose — words already exist)

RULES:
1. Prompts must reference grouping or practicing EXISTING words, not generating new ones
2. Suggest strategies that leverage the detected patterns (word families, vowel sounds, themes)
3. Keep suggestions practical and age-appropriate (ages 4-8)
4. Each prompt should suggest a different grouping strategy

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
            """.trimIndent()

            val userPrompt = "Based on the word bank, suggest 2 prompts for creating air-writing activity sets."

            val model = createModel(systemInstruction)
            val response = model.generateContent(userPrompt)
            val json = response.text ?: throw Exception("Empty response from activity suggested prompts")

            Log.d(TAG, "Activity suggested prompts response: $json")
            val parsed = gson.fromJson(json, SuggestedPromptsResponse::class.java)

            val prompts = parsed.prompts
                .filter { it.isNotBlank() }
                .filter { isValidPromptSuggestion(it) }
                .take(2)
            if (prompts.size < 2) {
                Log.w(TAG, "Fewer than 2 valid activity prompts returned: ${prompts.size}")
                return@withContext emptyList()
            }

            prompts
        } catch (e: Exception) {
            Log.e(TAG, "Activity suggested prompts generation failed: ${e.message}", e)
            emptyList()
        }
    }
```

**Step 2: Verify the build compiles**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: add generateActivitySuggestedPrompts to GeminiRepository"
```

---

### Task 2: Add activity suggested prompts state and loader to LessonViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt`

**Step 1: Add cache fields after the existing suggested prompts cache (after line 59)**

After this existing block:
```kotlin
    // Cached suggested prompts for generation modal
    private var cachedSuggestedPrompts: List<String> = emptyList()
    private var cachedWordCountForPrompts: Int = -1
```

Add:
```kotlin
    // Cached suggested prompts for activity creation modal
    private var cachedActivitySuggestedPrompts: List<String> = emptyList()
    private var cachedActivityWordCountForPrompts: Int = -1
```

**Step 2: Add `loadActivitySuggestedPrompts()` method after `loadSuggestedPrompts()` (after line 793)**

After the closing `}` of `loadSuggestedPrompts()` at line 793, add:

```kotlin
    /**
     * Load suggested prompts for the activity creation modal.
     * Uses cached prompts if word bank size hasn't changed.
     */
    fun loadActivitySuggestedPrompts() {
        val currentWords = _uiState.value.words
        val currentWordCount = currentWords.size

        // Don't load suggestions for empty word bank
        if (currentWordCount == 0) return

        // Use cache if word bank size hasn't changed
        if (currentWordCount == cachedActivityWordCountForPrompts && cachedActivitySuggestedPrompts.isNotEmpty()) {
            _uiState.update { it.copy(activitySuggestedPrompts = cachedActivitySuggestedPrompts) }
            return
        }

        // Generate new suggestions
        _uiState.update { it.copy(isActivitySuggestionsLoading = true, activitySuggestedPrompts = emptyList()) }

        viewModelScope.launch {
            try {
                val prompts = geminiRepository.generateActivitySuggestedPrompts(currentWords)

                if (prompts.isNotEmpty()) {
                    cachedActivitySuggestedPrompts = prompts
                    cachedActivityWordCountForPrompts = currentWordCount
                }

                _uiState.update {
                    it.copy(
                        activitySuggestedPrompts = prompts,
                        isActivitySuggestionsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        activitySuggestedPrompts = emptyList(),
                        isActivitySuggestionsLoading = false
                    )
                }
            }
        }
    }
```

**Step 3: Add new state fields to `LessonUiState` (after the `activityError` field, around line 951)**

After:
```kotlin
    val activityError: String? = null,
```

Add:
```kotlin
    val activitySuggestedPrompts: List<String> = emptyList(),
    val isActivitySuggestionsLoading: Boolean = false,
```

**Step 4: Verify the build compiles**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt
git commit -m "feat: add activity suggested prompts state and loader to LessonViewModel"
```

---

### Task 3: Add empty state and suggested prompts UI to ActivityCreationModal

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/wordbank/ActivityCreationModal.kt`

**Step 1: Add required imports**

Add these imports to the existing import block:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import com.example.app.data.entity.Word
```

**Step 2: Update the `ActivityCreationModal` function signature and body**

Replace the entire `ActivityCreationModal` composable (lines 43-160) with the version that includes:
- New parameters: `words`, `suggestedPrompts`, `isSuggestionsLoading`, `onSuggestionClick`
- Empty word bank state check
- Suggested prompts section between input and button

```kotlin
/**
 * Activity Creation Modal component for creating activities using the entire word bank.
 * Displays a dialog with activity input, suggested prompts, and magic creation button.
 * Shows an empty state when the word bank has no words.
 */
@Composable
fun ActivityCreationModal(
    isVisible: Boolean,
    activityInput: String,
    words: List<Word>,
    isLoading: Boolean,
    generationPhase: GenerationPhase = GenerationPhase.Idle,
    error: String? = null,
    suggestedPrompts: List<String> = emptyList(),
    isSuggestionsLoading: Boolean = false,
    onActivityInputChanged: (String) -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onCreateActivity: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val focusManager = LocalFocusManager.current
    val isSubmitEnabled = activityInput.isNotBlank()
    val isWordBankEmpty = words.isEmpty()

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Main card content (positioned below the mascot)
            Column(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                // Blue header section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF49A9FF))
                )

                // White content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Activity Creation with Kuu",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isWordBankEmpty) {
                        // Empty word bank state
                        EmptyWordBankMessage()
                    } else {
                        // Activity input section
                        ActivityInputSection(
                            activityInput = activityInput,
                            isLoading = isLoading,
                            onActivityInputChanged = onActivityInputChanged,
                            onSubmit = {
                                focusManager.clearFocus()
                                if (isSubmitEnabled) {
                                    onCreateActivity()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Suggested prompts section
                        SuggestedPromptsSection(
                            suggestedPrompts = suggestedPrompts,
                            isSuggestionsLoading = isSuggestionsLoading,
                            isLoading = isLoading,
                            onSuggestionClick = onSuggestionClick
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Magic button
                        MagicButton(
                            isLoading = isLoading,
                            generationPhase = generationPhase,
                            isEnabled = isSubmitEnabled,
                            onClick = {
                                focusManager.clearFocus()
                                onCreateActivity()
                            }
                        )

                        // Error message
                        if (error != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error,
                                color = Color.Red,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Mascot image overlapping the top
            Image(
                painter = painterResource(id = R.drawable.dis_wand_sit),
                contentDescription = "Kuu with wand",
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
```

**Step 3: Add the `EmptyWordBankMessage` composable (after `ActivityInputSection`, before `MagicButton`)**

```kotlin
/**
 * Message shown when the word bank is empty.
 */
@Composable
private fun EmptyWordBankMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Word Bank is empty!",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0B0B0B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add words to your Word Bank first to create activities.",
            fontSize = 14.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

**Step 4: Add the `SuggestedPromptsSection` composable (after `EmptyWordBankMessage`)**

This mirrors the suggested prompts section from `WordBankGenerationModal.kt` lines 378-452:

```kotlin
/**
 * Suggested prompts section with loading state and clickable chips.
 */
@Composable
private fun SuggestedPromptsSection(
    suggestedPrompts: List<String>,
    isSuggestionsLoading: Boolean,
    isLoading: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    if (!isSuggestionsLoading && suggestedPrompts.isEmpty()) return

    Text(
        text = "Need ideas? Start with these!",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF0B0B0B),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (isSuggestionsLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF49A9FF),
                strokeWidth = 2.dp
            )
        }
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedPrompts.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .background(
                            color = Color(0xFFF0F7FF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF49A9FF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .then(
                            if (!isLoading) Modifier.clickable {
                                onSuggestionClick(prompt)
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wand),
                            contentDescription = null,
                            tint = Color(0xFF49A9FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = prompt,
                            fontSize = 14.sp,
                            color = Color(0xFF333333),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
```

**Step 5: Update the preview composables to include new parameters**

Replace all three preview composables at the bottom of the file:

```kotlin
@Preview(showBackground = true)
@Composable
fun ActivityCreationModalPreview() {
    val sampleWords = listOf(
        Word(id = 1L, userId = 1L, word = "cat"),
        Word(id = 2L, userId = 1L, word = "dog"),
        Word(id = 3L, userId = 1L, word = "bat")
    )

    ActivityCreationModal(
        isVisible = true,
        activityInput = "",
        words = sampleWords,
        isLoading = false,
        suggestedPrompts = listOf(
            "Practice the -at word family with rhyming words",
            "Group words by short vowel sounds"
        ),
        onActivityInputChanged = {},
        onCreateActivity = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ActivityCreationModalEmptyWordBankPreview() {
    ActivityCreationModal(
        isVisible = true,
        activityInput = "",
        words = emptyList(),
        isLoading = false,
        onActivityInputChanged = {},
        onCreateActivity = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ActivityCreationModalLoadingPreview() {
    val sampleWords = listOf(
        Word(id = 1L, userId = 1L, word = "cat"),
        Word(id = 2L, userId = 1L, word = "dog")
    )

    ActivityCreationModal(
        isVisible = true,
        activityInput = "Create a story",
        words = sampleWords,
        isLoading = true,
        onActivityInputChanged = {},
        onCreateActivity = {},
        onDismiss = {}
    )
}
```

**Step 6: Verify the build compiles**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/wordbank/ActivityCreationModal.kt
git commit -m "feat: add suggested prompts section and empty word bank state to ActivityCreationModal"
```

---

### Task 4: Wire everything together in YourSetsScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/set/YourSetsScreen.kt`

**Step 1: Add LaunchedEffect import if not already present**

Ensure this import exists (it should already be there via the `*` import on `androidx.compose.runtime.*` at line 21):
```kotlin
import androidx.compose.runtime.*
```

**Step 2: Add LaunchedEffect to trigger loading activity suggested prompts**

After line 60 (`var setToDelete by remember { mutableStateOf<Set?>(null) }`), add:

```kotlin
    // Load activity suggested prompts when creation modal becomes visible
    LaunchedEffect(lessonUiState.isActivityCreationModalVisible) {
        if (lessonUiState.isActivityCreationModalVisible) {
            lessonViewModel.loadActivitySuggestedPrompts()
        }
    }
```

**Step 3: Update the `ActivityCreationModal` call to pass new parameters**

Replace the existing `ActivityCreationModal` call (lines 327-340) with:

```kotlin
        // Activity Creation Modal (AI wand)
        ActivityCreationModal(
            isVisible = lessonUiState.isActivityCreationModalVisible,
            activityInput = lessonUiState.activityInput,
            words = lessonUiState.words,
            isLoading = lessonUiState.isActivityCreationLoading,
            generationPhase = generationPhase,
            error = lessonUiState.activityError,
            suggestedPrompts = lessonUiState.activitySuggestedPrompts,
            isSuggestionsLoading = lessonUiState.isActivitySuggestionsLoading,
            onActivityInputChanged = { lessonViewModel.onActivityInputChanged(it) },
            onSuggestionClick = { suggestion ->
                lessonViewModel.onActivityInputChanged(suggestion)
            },
            onCreateActivity = {
                lessonViewModel.createActivity { jsonResult ->
                    onNavigateToAIGenerate(jsonResult)
                }
            },
            onDismiss = { lessonViewModel.hideActivityCreationModal() }
        )
```

**Step 4: Verify the build compiles**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/set/YourSetsScreen.kt
git commit -m "feat: wire activity suggested prompts and empty state in YourSetsScreen"
```

---

### Task 5: Final build verification

**Step 1: Full debug build**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 2: Verify no remaining compilation issues**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | grep -i "error\|warning" | head -20`
Expected: No errors (warnings are acceptable)
