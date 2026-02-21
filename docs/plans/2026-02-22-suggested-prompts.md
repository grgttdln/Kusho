# Suggested Prompts Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add AI-generated contextual prompt suggestions to WordBankGenerationModal, with caching to avoid redundant API calls.

**Architecture:** New `generateSuggestedPrompts()` in GeminiRepository (Gemini API call returning 2 prompts), new state/caching in LessonViewModel, new suggestion chip UI in the modal composable, wired through WordBankScreen.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Gemini 2.5 Flash Lite, Gson

**Design doc:** `docs/plans/2026-02-22-suggested-prompts-design.md`

---

### Task 1: Add `generateSuggestedPrompts()` to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add response data class for parsing**

After the `WordInfo` data class block (line 1076), before the `// ========== CVC Word Analysis ==========` comment (line 1078), add:

```kotlin
    // ========== Suggested Prompts ==========

    private data class SuggestedPromptsResponse(
        val prompts: List<String> = emptyList()
    )
```

**Step 2: Add the `generateSuggestedPrompts` public method**

After the `generateAnnotationSummary` method's closing brace (line 318), before the `// ========== Step Functions ==========` comment (line 320), add:

```kotlin
    // ========== Suggested Prompts ==========

    /**
     * Generate 2 contextual prompt suggestions for the CVC word generation modal.
     *
     * @param words Current word bank contents (may be empty)
     * @return List of exactly 2 prompt suggestion strings, or empty list on failure
     */
    suspend fun generateSuggestedPrompts(words: List<Word>): List<String> = withContext(Dispatchers.IO) {
        try {
            val systemInstruction: String
            val userPrompt: String

            if (words.isEmpty()) {
                systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). The teacher has an empty word bank and wants to generate CVC (Consonant-Vowel-Consonant) words using AI.

Suggest exactly 2 concise, helpful prompts that the teacher could use to generate their first batch of CVC words. Each prompt should be a natural sentence describing what kind of CVC words to create.

Good examples: "Common short vowel 'a' words like cat, bat, hat", "Animal-themed CVC words for beginners"
Bad examples: "Generate words" (too vague), "Create a comprehensive phonics curriculum" (too broad)

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
                """.trimIndent()

                userPrompt = "The teacher's word bank is empty. Suggest 2 helpful starter prompts for generating CVC words."
            } else {
                val analyses = analyzeCVCWords(words)
                val patterns = detectCVCPatterns(analyses)
                val wordList = words.joinToString(", ") { it.word }
                val patternsSummary = buildPatternsSummary(patterns)

                systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). The teacher already has CVC words in their word bank. Suggest exactly 2 concise prompts for generating NEW CVC words that would complement their existing collection.

EXISTING WORDS: $wordList

DETECTED PATTERNS IN EXISTING WORDS:
$patternsSummary

RULES:
1. Suggest prompts that fill GAPS in the existing collection (missing vowel sounds, missing word families, missing themes)
2. Each prompt should be a natural sentence (10-20 words) describing what CVC words to create
3. Do NOT suggest words that overlap with existing patterns — focus on what's MISSING
4. Keep suggestions practical for ages 4-8

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
                """.trimIndent()

                userPrompt = "Based on the existing word bank, suggest 2 prompts for generating complementary CVC words."
            }

            val model = createModel(systemInstruction)
            val response = model.generateContent(userPrompt)
            val json = response.text ?: throw Exception("Empty response from suggested prompts")

            Log.d(TAG, "Suggested prompts response: $json")
            val parsed = gson.fromJson(json, SuggestedPromptsResponse::class.java)

            val prompts = parsed.prompts.filter { it.isNotBlank() }.take(2)
            if (prompts.size < 2) {
                Log.w(TAG, "Fewer than 2 prompts returned: ${prompts.size}")
                return@withContext emptyList()
            }

            prompts
        } catch (e: Exception) {
            Log.e(TAG, "Suggested prompts generation failed: ${e.message}", e)
            emptyList()
        }
    }
```

**Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: add generateSuggestedPrompts method to GeminiRepository"
```

---

### Task 2: Add suggestion state and caching to LessonViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt`

**Step 1: Add state fields to LessonUiState**

In the `LessonUiState` data class (line 783), after the `editDictionarySuggestions` field (line 811), add:

```kotlin
    // Suggested prompts state (generation modal)
    val suggestedPrompts: List<String> = emptyList(),
    val isSuggestionsLoading: Boolean = false
```

**Step 2: Add private cache fields to LessonViewModel**

After the `lastSelectedWords` field (line 55), add:

```kotlin
    // Cached suggested prompts for generation modal
    private var cachedSuggestedPrompts: List<String> = emptyList()
    private var cachedWordCountForPrompts: Int = -1
```

**Step 3: Add `loadSuggestedPrompts()` method**

After the `addMoreSet` method's closing brace (line 777), before the closing brace of the class (line 778), add:

```kotlin
    /**
     * Load suggested prompts for the generation modal.
     * Uses cached prompts if word bank size hasn't changed.
     */
    fun loadSuggestedPrompts() {
        val currentWords = _uiState.value.words
        val currentWordCount = currentWords.size

        // Use cache if word bank size hasn't changed
        if (currentWordCount == cachedWordCountForPrompts && cachedSuggestedPrompts.isNotEmpty()) {
            _uiState.update { it.copy(suggestedPrompts = cachedSuggestedPrompts) }
            return
        }

        // Generate new suggestions
        _uiState.update { it.copy(isSuggestionsLoading = true, suggestedPrompts = emptyList()) }

        viewModelScope.launch {
            val prompts = geminiRepository.generateSuggestedPrompts(currentWords)

            if (prompts.isNotEmpty()) {
                cachedSuggestedPrompts = prompts
                cachedWordCountForPrompts = currentWordCount
            }

            _uiState.update {
                it.copy(
                    suggestedPrompts = prompts,
                    isSuggestionsLoading = false
                )
            }
        }
    }
```

**Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt
git commit -m "feat: add suggested prompts state and caching to LessonViewModel"
```

---

### Task 3: Add suggestion chips UI to WordBankGenerationModal

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/wordbank/WordBankGenerationModal.kt`

**Step 1: Add new imports**

After the existing imports (line 35), add:

```kotlin
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
```

**Step 2: Add new parameters to composable signature**

In the `WordBankGenerationModal` composable (line 38), add 3 new parameters after `onWordCountChanged` (line 45):

```kotlin
    suggestedPrompts: List<String> = emptyList(),
    isSuggestionsLoading: Boolean = false,
    onSuggestionClick: (String) -> Unit = {},
```

So the full signature becomes:
```kotlin
@Composable
fun WordBankGenerationModal(
    isVisible: Boolean,
    promptInput: String,
    isLoading: Boolean,
    generationPhase: GenerationPhase = GenerationPhase.Idle,
    error: String? = null,
    wordCount: Int = 5,
    onWordCountChanged: (Int) -> Unit = {},
    suggestedPrompts: List<String> = emptyList(),
    isSuggestionsLoading: Boolean = false,
    onSuggestionClick: (String) -> Unit = {},
    onPromptInputChanged: (String) -> Unit,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
)
```

**Step 3: Add suggestion section between preset chips and generate button**

Replace the `Spacer(modifier = Modifier.height(32.dp))` on line 292 (between the preset chips `Row` closing brace on line 290 and the `// Generate button` comment on line 294) with the suggested prompts section:

```kotlin
                    Spacer(modifier = Modifier.height(24.dp))

                    // Suggested prompts section
                    if (isSuggestionsLoading || suggestedPrompts.isNotEmpty()) {
                        Text(
                            text = "Suggested for you",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0B0B0B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isSuggestionsLoading) {
                            // Shimmer loading placeholders
                            val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                            val alpha = infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 0.7f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "shimmerAlpha"
                            )

                            repeat(2) { index ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(
                                            color = Color(0xFFF0F7FF).copy(alpha = alpha.value),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF49A9FF).copy(alpha = alpha.value * 0.5f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                )
                                if (index == 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            // Suggestion chips
                            suggestedPrompts.forEachIndexed { index, prompt ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
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
                                if (index < suggestedPrompts.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
```

**Step 4: Update preview composables**

Add the new parameters to all 3 preview functions. Update the previews to:

```kotlin
@Preview(showBackground = true)
@Composable
fun WordBankGenerationModalPreview() {
    WordBankGenerationModal(
        isVisible = true,
        promptInput = "",
        isLoading = false,
        wordCount = 5,
        onWordCountChanged = {},
        suggestedPrompts = listOf(
            "Short vowel 'a' words like cat, bat, hat",
            "Animal-themed CVC words for beginners"
        ),
        isSuggestionsLoading = false,
        onSuggestionClick = {},
        onPromptInputChanged = {},
        onGenerate = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun WordBankGenerationModalWithInputPreview() {
    WordBankGenerationModal(
        isVisible = true,
        promptInput = "Animal-themed CVC words for beginners",
        isLoading = false,
        wordCount = 10,
        onWordCountChanged = {},
        suggestedPrompts = emptyList(),
        isSuggestionsLoading = false,
        onSuggestionClick = {},
        onPromptInputChanged = {},
        onGenerate = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun WordBankGenerationModalLoadingPreview() {
    WordBankGenerationModal(
        isVisible = true,
        promptInput = "Animal-themed CVC words",
        isLoading = true,
        wordCount = 15,
        onWordCountChanged = {},
        suggestedPrompts = emptyList(),
        isSuggestionsLoading = true,
        onSuggestionClick = {},
        onPromptInputChanged = {},
        onGenerate = {},
        onDismiss = {}
    )
}
```

**Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/wordbank/WordBankGenerationModal.kt
git commit -m "feat: add suggested prompts chips UI to WordBankGenerationModal"
```

---

### Task 4: Wire suggested prompts in WordBankScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt`

**Step 1: Add LaunchedEffect import**

After the existing imports (line 31), add:

```kotlin
import androidx.compose.runtime.LaunchedEffect
```

**Step 2: Trigger suggestion loading when modal opens**

After the `generationWordCount` state declaration (line 46), add:

```kotlin
    // Load suggested prompts when generation modal becomes visible
    LaunchedEffect(isGenerationModalVisible) {
        if (isGenerationModalVisible) {
            viewModel.loadSuggestedPrompts()
        }
    }
```

**Step 3: Update WordBankGenerationModal call site**

Replace the `WordBankGenerationModal` call (lines 210-223) with the updated version that passes the new parameters:

```kotlin
        WordBankGenerationModal(
            isVisible = isGenerationModalVisible,
            promptInput = generationPrompt,
            isLoading = false,
            wordCount = generationWordCount,
            onWordCountChanged = { generationWordCount = it },
            suggestedPrompts = uiState.suggestedPrompts,
            isSuggestionsLoading = uiState.isSuggestionsLoading,
            onSuggestionClick = { suggestion ->
                generationPrompt = suggestion
            },
            onPromptInputChanged = { generationPrompt = it },
            onGenerate = { /* TODO: Wire to generation logic */ },
            onDismiss = {
                isGenerationModalVisible = false
                generationPrompt = ""
                generationWordCount = 5
            }
        )
```

**Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt
git commit -m "feat: wire suggested prompts from ViewModel to WordBankGenerationModal"
```
