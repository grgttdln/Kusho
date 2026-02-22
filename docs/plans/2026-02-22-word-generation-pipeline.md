# Word Generation Pipeline Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire the "Generate CVC Words" button in WordBankGenerationModal to an end-to-end pipeline: Gemini generates CVC words → CVC regex safety filter → dedup → batch insert to DB → modal shows success state.

**Architecture:** New `generateCVCWords()` method in `GeminiRepository` handles the Gemini call with strong CVC prompt engineering. New state fields and `generateWords()` method in `LessonViewModel` orchestrate the full pipeline. `WordBankGenerationModal` gains success/error UI states. `WordBankScreen` wires everything together.

**Tech Stack:** Kotlin, Jetpack Compose, Gemini 2.5 Flash Lite, Room DB, Coroutines

---

### Task 1: Add `generateCVCWords()` to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add the response data class**

Add after the `SuggestedPromptsResponse` class (line 1183):

```kotlin
private data class GeneratedWordsResponse(
    val words: List<String> = emptyList()
)
```

**Step 2: Add the `generateCVCWords` method**

Add after `generateSuggestedPrompts()` (after line 398, before the `// ========== Step Functions ==========` section at line 422):

```kotlin
/**
 * Generate CVC words for the word bank based on teacher's prompt.
 *
 * @param prompt Teacher's description of what kind of CVC words to generate
 * @param count Desired number of words (will over-generate by ~50% to account for filtering)
 * @param existingWords Current word bank contents (for dedup context in prompt)
 * @return List of candidate CVC word strings (lowercase, trimmed)
 */
suspend fun generateCVCWords(
    prompt: String,
    count: Int,
    existingWords: List<Word>
): List<String> = withContext(Dispatchers.IO) {
    val requestCount = (count * 1.5).toInt().coerceAtLeast(count + 2)

    val existingWordList = existingWords
        .map { it.word.lowercase() }
        .distinct()
        .joinToString(", ")

    val existingSection = if (existingWords.isNotEmpty()) {
        "\n- Do NOT include any of these existing words: $existingWordList"
    } else ""

    val systemInstruction = """
You generate CVC (Consonant-Vowel-Consonant) words for early readers (ages 4-8).

RULES — follow these strictly:
- Each word must be EXACTLY 3 letters long
- Letter 1 must be a consonant (b, c, d, f, g, h, j, k, l, m, n, p, r, s, t, v, w, x, y, z)
- Letter 2 must be a vowel (a, e, i, o, u)
- Letter 3 must be a consonant (b, c, d, f, g, h, j, k, l, m, n, p, r, s, t, v, w, x, y, z)
- Every word must be a real, common English word that a child would recognize
- No proper nouns, slang, or obscure words
- No duplicate words in the list$existingSection

Return a JSON object: {"words": ["word1", "word2", ...]}
Return exactly $requestCount lowercase words. No extra text.
    """.trimIndent()

    val userPrompt = "Generate $requestCount CVC words based on this request: $prompt"

    val model = createModel(systemInstruction)
    val response = model.generateContent(userPrompt)
    val json = response.text ?: throw Exception("Empty response from CVC word generation")

    Log.d(TAG, "CVC word generation response: $json")
    val parsed = gson.fromJson(json, GeneratedWordsResponse::class.java)

    parsed.words
        .map { it.lowercase().trim() }
        .filter { it.isNotBlank() }
        .distinct()
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: add generateCVCWords method to GeminiRepository"
```

---

### Task 2: Add word generation state to LessonUiState and ViewModel methods

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt`

**Step 1: Add state fields to LessonUiState**

Add these fields to the `LessonUiState` data class (after line 861, before the closing parenthesis):

```kotlin
// Word generation modal state
val isWordGenerationLoading: Boolean = false,
val wordGenerationError: String? = null,
val generatedWords: List<String> = emptyList(),
val wordGenerationRequestedCount: Int = 0,
```

**Step 2: Add `generateWords()` method to LessonViewModel**

Add after `loadSuggestedPrompts()` (after line 824):

```kotlin
/**
 * Generate CVC words using Gemini and add them to the word bank.
 * Orchestrates: Gemini call → CVC filter → dedup → batch DB insert → success state.
 *
 * @param prompt Teacher's description of what CVC words to generate
 * @param count Number of words requested (from stepper)
 */
fun generateWords(prompt: String, count: Int) {
    val userId = currentUserId
    if (userId == null || userId == 0L) {
        _uiState.update { it.copy(wordGenerationError = "Please log in to generate words") }
        return
    }

    if (prompt.isBlank()) {
        _uiState.update { it.copy(wordGenerationError = "Please enter a description") }
        return
    }

    _uiState.update {
        it.copy(
            isWordGenerationLoading = true,
            wordGenerationError = null,
            generatedWords = emptyList(),
            wordGenerationRequestedCount = count
        )
    }

    viewModelScope.launch {
        try {
            // 1. Fetch existing words for dedup context
            val existingWords = wordRepository.getWordsForUserOnce(userId)
            val existingWordSet = existingWords.map { it.word.lowercase() }.toSet()

            // 2. Call Gemini to generate CVC word candidates
            val candidates = geminiRepository.generateCVCWords(prompt, count, existingWords)

            // 3. CVC regex safety filter
            val cvcValid = candidates.filter { com.example.app.util.WordValidator.isCVCPattern(it) }

            // 4. Dedup against existing word bank
            val deduped = cvcValid.filter { it.lowercase() !in existingWordSet }

            // 5. Take requested count
            val wordsToAdd = deduped.take(count)

            if (wordsToAdd.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isWordGenerationLoading = false,
                        wordGenerationError = if (cvcValid.isEmpty()) {
                            "No valid CVC words could be generated. Try a different prompt."
                        } else {
                            "All generated words already exist in your word bank. Try a different prompt."
                        }
                    )
                }
                return@launch
            }

            // 6. Batch insert to DB
            val wordDao = database.wordDao()
            val addedWords = mutableListOf<String>()
            for (word in wordsToAdd) {
                try {
                    val id = wordDao.insertWord(
                        com.example.app.data.entity.Word(
                            userId = userId,
                            word = word
                        )
                    )
                    if (id > 0) {
                        addedWords.add(word)
                    }
                } catch (e: Exception) {
                    // Skip words that fail insertion (e.g., unique constraint)
                    android.util.Log.w("LessonViewModel", "Failed to insert word '$word': ${e.message}")
                }
            }

            // 7. Update state with results
            if (addedWords.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isWordGenerationLoading = false,
                        wordGenerationError = "Failed to add words to the word bank. Please try again."
                    )
                }
            } else {
                // Invalidate suggested prompts cache since word bank changed
                cachedWordCountForPrompts = -1

                _uiState.update {
                    it.copy(
                        isWordGenerationLoading = false,
                        generatedWords = addedWords
                    )
                }
            }
        } catch (e: Exception) {
            val errorMsg = if (e.message?.contains("blocked", ignoreCase = true) == true ||
                e.message?.contains("safety", ignoreCase = true) == true) {
                "The request couldn't be processed. Please rephrase your description."
            } else {
                "Word generation failed: ${e.message}"
            }
            _uiState.update {
                it.copy(
                    isWordGenerationLoading = false,
                    wordGenerationError = errorMsg
                )
            }
        }
    }
}

/**
 * Clear word generation state. Called when the generation modal is dismissed.
 */
fun clearWordGenerationState() {
    _uiState.update {
        it.copy(
            isWordGenerationLoading = false,
            wordGenerationError = null,
            generatedWords = emptyList(),
            wordGenerationRequestedCount = 0
        )
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt
git commit -m "feat: add generateWords pipeline and state to LessonViewModel"
```

---

### Task 3: Add success state UI to WordBankGenerationModal

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/wordbank/WordBankGenerationModal.kt`

**Step 1: Add new parameters to the composable signature**

Update the `WordBankGenerationModal` parameters (lines 40-54) to add:

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
    onDismiss: () -> Unit,
    // New: success state parameters
    generatedWords: List<String> = emptyList(),
    requestedCount: Int = 0,
    onDone: () -> Unit = {}
) {
```

**Step 2: Add success state UI**

Replace the entire white content section (the `Column` that starts at line 91 inside the card) with a conditional that checks for success state. The success state shows when `generatedWords.isNotEmpty()` and `!isLoading`.

After the `if (!isVisible) return` and before the `Dialog(...)`, add:

```kotlin
val isSuccess = generatedWords.isNotEmpty() && !isLoading
```

Then inside the white content Column (line 91-441), wrap the existing content in `if (!isSuccess)` and add an `else` branch for success:

```kotlin
// White content section
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(top = 24.dp, bottom = 32.dp)
        .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    if (isSuccess) {
        // === SUCCESS STATE ===
        // Checkmark icon
        Icon(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = "Success",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Success header
        Text(
            text = "${generatedWords.size} words added!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0B0B0B),
            textAlign = TextAlign.Center
        )

        // Partial success note
        if (generatedWords.size < requestedCount) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Some words were duplicates or invalid and were skipped",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Word list
        generatedWords.forEach { word ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        color = Color(0xFFF0F7FF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0B0B0B)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Done button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF49A9FF)
            )
        ) {
            Text(
                text = "Done",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    } else {
        // === EXISTING INPUT STATE (everything currently in this Column) ===
        // Title
        Text(
            text = "Let Kuu Generate CVC Words!",
            // ... (all existing code from line 100-440)
        )
        // ... rest of existing input/loading/error UI ...
    }
}
```

Note: The loading phase text (lines 403-408) should be updated to show word-generation-appropriate text:

```kotlin
text = when (generationPhase) {
    is GenerationPhase.Filtering -> "Generating words..."
    is GenerationPhase.Grouping -> "Adding to word bank..."
    else -> "Generating..."
},
```

**Step 3: Add `ic_check` drawable resource if it doesn't exist**

Check if `R.drawable.ic_check` exists. If not, use `Icons.Default.Check` from Material icons instead:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle

// Replace the Icon painter call with:
Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = "Success",
    tint = Color(0xFF4CAF50),
    modifier = Modifier.size(48.dp)
)
```

**Step 4: Update preview composables**

Add a new preview for the success state after the existing previews (after line 512):

```kotlin
@Preview(showBackground = true)
@Composable
fun WordBankGenerationModalSuccessPreview() {
    WordBankGenerationModal(
        isVisible = true,
        promptInput = "",
        isLoading = false,
        generatedWords = listOf("cat", "bat", "hat", "mat", "sat"),
        requestedCount = 5,
        onPromptInputChanged = {},
        onGenerate = {},
        onDismiss = {},
        onDone = {}
    )
}

@Preview(showBackground = true)
@Composable
fun WordBankGenerationModalPartialSuccessPreview() {
    WordBankGenerationModal(
        isVisible = true,
        promptInput = "",
        isLoading = false,
        generatedWords = listOf("cat", "bat", "hat"),
        requestedCount = 5,
        onPromptInputChanged = {},
        onGenerate = {},
        onDismiss = {},
        onDone = {}
    )
}
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/wordbank/WordBankGenerationModal.kt
git commit -m "feat: add success state UI to WordBankGenerationModal"
```

---

### Task 4: Wire generation flow in WordBankScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt`

**Step 1: Update the WordBankGenerationModal call**

Replace the current WordBankGenerationModal invocation (lines 218-236) with fully wired version:

```kotlin
// Word Bank Generation Modal (AI CVC word generation)
WordBankGenerationModal(
    isVisible = isGenerationModalVisible,
    promptInput = generationPrompt,
    isLoading = uiState.isWordGenerationLoading,
    error = uiState.wordGenerationError,
    wordCount = generationWordCount,
    onWordCountChanged = { generationWordCount = it },
    suggestedPrompts = uiState.suggestedPrompts,
    isSuggestionsLoading = uiState.isSuggestionsLoading,
    onSuggestionClick = { suggestion ->
        generationPrompt = suggestion
    },
    onPromptInputChanged = { generationPrompt = it },
    onGenerate = {
        viewModel.generateWords(generationPrompt, generationWordCount)
    },
    onDismiss = {
        isGenerationModalVisible = false
        generationPrompt = ""
        generationWordCount = 5
        viewModel.clearWordGenerationState()
    },
    generatedWords = uiState.generatedWords,
    requestedCount = uiState.wordGenerationRequestedCount,
    onDone = {
        isGenerationModalVisible = false
        generationPrompt = ""
        generationWordCount = 5
        viewModel.clearWordGenerationState()
    }
)
```

Key changes from current code:
- `isLoading` now reads from `uiState.isWordGenerationLoading` (was hardcoded `false`)
- `error` now reads from `uiState.wordGenerationError` (was missing)
- `onGenerate` now calls `viewModel.generateWords(...)` (was `{ /* TODO */ }`)
- `onDismiss` now also calls `viewModel.clearWordGenerationState()`
- New: `generatedWords`, `requestedCount`, `onDone` parameters wired

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt
git commit -m "feat: wire word generation pipeline in WordBankScreen"
```

---

### Task 5: Build and verify

**Step 1: Build the project**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

**Step 2: Fix any compilation errors**

If the build fails, fix the errors (likely import statements or missing resources). Common things to check:
- `import com.example.app.util.WordValidator` in LessonViewModel
- `import androidx.compose.material.icons.filled.CheckCircle` in WordBankGenerationModal
- `import androidx.compose.material3.Icon` already imported in modal

**Step 3: Final commit after fixes (if any)**

```bash
git add -A
git commit -m "fix: resolve compilation issues in word generation pipeline"
```
