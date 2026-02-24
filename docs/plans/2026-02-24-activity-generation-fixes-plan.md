# Activity Generation Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix two Activity Generation bugs: (1) when the word bank has too few matching words, suggest words to add instead of silently falling back to all words; (2) use deterministic code-based filtering for structural patterns so the AI can't ignore constraints like "-un".

**Architecture:** Add a `PatternRouter` inside `GeminiRepository` that classifies teacher prompts as structural (rime/onset/vowel/coda) or semantic. Structural prompts bypass AI Step 1 entirely and use code-based CVC filtering. When fewer than 3 words match, a new `InsufficientWords` result type triggers an inline suggestion dialog in the UI. The existing `generateCVCWords()` generates suggestions matching the pattern. A post-validation layer catches pattern violations in AI responses for semantic requests.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Gemini API (existing), CVC analysis utilities (existing in GeminiRepository)

---

## Task 1: Add PatternRouter to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add the DetectedPattern sealed class and PatternRouter**

Add after the `GenerationPhase` sealed class (line 36) and before the `GeminiRepository` class (line 45):

```kotlin
/**
 * Represents a structural pattern detected in a teacher's prompt.
 * Used to bypass AI filtering and apply deterministic code-based filtering.
 */
sealed class DetectedPattern {
    abstract fun matches(analysis: GeminiRepository.CVCAnalysisPublic): Boolean
    abstract val displayName: String

    data class Rime(val rime: String) : DetectedPattern() {
        override fun matches(analysis: GeminiRepository.CVCAnalysisPublic) = analysis.rime == rime
        override val displayName = "-$rime"
    }
    data class Onset(val onset: Char) : DetectedPattern() {
        override fun matches(analysis: GeminiRepository.CVCAnalysisPublic) = analysis.onset == onset
        override val displayName = "starting with '$onset'"
    }
    data class Vowel(val vowel: Char) : DetectedPattern() {
        override fun matches(analysis: GeminiRepository.CVCAnalysisPublic) = analysis.vowel == vowel
        override val displayName = "with vowel '$vowel'"
    }
    data class Coda(val coda: Char) : DetectedPattern() {
        override fun matches(analysis: GeminiRepository.CVCAnalysisPublic) = analysis.coda == coda
        override val displayName = "ending in '$coda'"
    }
}
```

**Step 2: Add the CVCAnalysisPublic data class and classifyPrompt function inside GeminiRepository**

Add inside the `GeminiRepository` class, after the `shouldThrottle` function (after line 90):

```kotlin
/**
 * Public-facing CVC analysis for pattern matching.
 */
data class CVCAnalysisPublic(
    val word: String,
    val onset: Char,
    val vowel: Char,
    val coda: Char,
    val rime: String
)

/**
 * Classify a teacher's prompt to detect structural patterns.
 * Returns null for semantic/thematic prompts that need AI filtering.
 */
fun classifyPrompt(prompt: String): DetectedPattern? {
    val lower = prompt.lowercase().trim()

    // Rime patterns: "-un", "_un", "ending in -un", "words ending in un", "-un words"
    val rimeRegex = Regex("""[-_]([aeiou][bcdfghjklmnprstvwxyz])\b""")
    val rimeMatch = rimeRegex.find(lower)
    if (rimeMatch != null) {
        return DetectedPattern.Rime(rimeMatch.groupValues[1])
    }

    // "ending in -un" / "end in un" / "that end with un"
    val endingInRimeRegex = Regex("""end(?:ing)?\s+(?:in|with)\s+[-_]?([aeiou][bcdfghjklmnprstvwxyz])\b""")
    val endingInRimeMatch = endingInRimeRegex.find(lower)
    if (endingInRimeMatch != null) {
        return DetectedPattern.Rime(endingInRimeMatch.groupValues[1])
    }

    // Onset patterns: "starting with b", "words that start with c", "b__ words"
    val onsetRegex = Regex("""start(?:ing)?\s+with\s+['"]?([bcdfghjklmnprstvwxyz])['"]?""")
    val onsetMatch = onsetRegex.find(lower)
    if (onsetMatch != null) {
        return DetectedPattern.Onset(onsetMatch.groupValues[1][0])
    }
    val onsetBlankRegex = Regex("""\b([bcdfghjklmnprstvwxyz])_+\b""")
    val onsetBlankMatch = onsetBlankRegex.find(lower)
    if (onsetBlankMatch != null) {
        return DetectedPattern.Onset(onsetBlankMatch.groupValues[1][0])
    }

    // Vowel patterns: "short a words", "words with 'a'", "vowel a", "short 'e'"
    val vowelRegex = Regex("""(?:short|long|vowel)\s+['"]?([aeiou])['"]?""")
    val vowelMatch = vowelRegex.find(lower)
    if (vowelMatch != null) {
        return DetectedPattern.Vowel(vowelMatch.groupValues[1][0])
    }
    val wordsWithVowelRegex = Regex("""words?\s+with\s+['"]([aeiou])['"]""")
    val wordsWithVowelMatch = wordsWithVowelRegex.find(lower)
    if (wordsWithVowelMatch != null) {
        return DetectedPattern.Vowel(wordsWithVowelMatch.groupValues[1][0])
    }

    // Coda patterns: "ending in t" (single consonant, not a rime)
    val codaRegex = Regex("""end(?:ing)?\s+(?:in|with)\s+['"]?([bcdfghjklmnprstvwxyz])['"]?(?:\s|$)""")
    val codaMatch = codaRegex.find(lower)
    if (codaMatch != null) {
        return DetectedPattern.Coda(codaMatch.groupValues[1][0])
    }

    // No structural pattern detected — needs AI
    return null
}

/**
 * Filter words using a detected structural pattern (no AI needed).
 */
fun filterWordsByPattern(
    pattern: DetectedPattern,
    availableWords: List<Word>
): List<String> {
    return availableWords.mapNotNull { w ->
        val lower = w.word.lowercase()
        if (!isValidCVC(lower)) return@mapNotNull null
        val analysis = CVCAnalysisPublic(
            word = w.word,
            onset = lower[0],
            vowel = lower[1],
            coda = lower[2],
            rime = "${lower[1]}${lower[2]}"
        )
        if (pattern.matches(analysis)) w.word else null
    }
}
```

**Step 3: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: add PatternRouter for deterministic CVC pattern detection"
```

---

## Task 2: Add InsufficientWords result type to AiGenerationResult

**Files:**
- Modify: `app/src/main/java/com/example/app/data/model/AiGeneratedData.kt`

**Step 1: Add the InsufficientWords variant to AiGenerationResult**

In `AiGeneratedData.kt`, modify the `AiGenerationResult` sealed class (lines 58-61) to add a new variant:

```kotlin
sealed class AiGenerationResult {
    data class Success(val data: AiGeneratedActivity) : AiGenerationResult()
    data class Error(val message: String, val canRetry: Boolean = true) : AiGenerationResult()
    data class InsufficientWords(
        val pattern: String,
        val matchingWords: List<String>,
        val needed: Int = 3
    ) : AiGenerationResult()
}
```

**Step 2: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/data/model/AiGeneratedData.kt
git commit -m "feat: add InsufficientWords variant to AiGenerationResult"
```

---

## Task 3: Integrate PatternRouter into generateActivity()

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Modify the generateActivity function to use PatternRouter before Step 1**

Replace the Step 1 section of `generateActivity()` (lines 141-160) with logic that:
1. Calls `classifyPrompt(prompt)` to detect structural patterns
2. If structural: uses `filterWordsByPattern()` instead of AI Step 1
3. If fewer than 3 matches: returns `AiGenerationResult.InsufficientWords`
4. If semantic: uses existing AI Step 1 + post-validation

Replace lines 141-160 in `generateActivity()`:

```kotlin
            // === Step 1: Filter Words ===
            onPhaseChange(GenerationPhase.Filtering)

            val detectedPattern = classifyPrompt(prompt)

            if (detectedPattern != null) {
                // Structural pattern: use deterministic code-based filtering
                Log.d(TAG, "Structural pattern detected: ${detectedPattern.displayName}")
                val codeFiltered = filterWordsByPattern(detectedPattern, availableWords)

                if (codeFiltered.size < 3) {
                    Log.w(TAG, "Insufficient words for pattern ${detectedPattern.displayName}: ${codeFiltered.size} found")
                    return@withContext AiGenerationResult.InsufficientWords(
                        pattern = detectedPattern.displayName,
                        matchingWords = codeFiltered,
                        needed = 3
                    )
                }

                cachedFilteredWords = codeFiltered
            } else {
                // Semantic/thematic prompt: use AI Step 1
                val filteredWords = retryStep(MAX_RETRIES, "Step 1: Filter") {
                    stepFilterWords(prompt, analysisTable, patternsSummary)
                }

                val wordBank = availableWords.map { it.word }.toSet()
                if (filteredWords == null || filteredWords.words.isNullOrEmpty()) {
                    Log.w(TAG, "Step 1 failed or returned empty, using all words as fallback")
                    cachedFilteredWords = availableWords.map { it.word }
                } else {
                    val filtered = filteredWords.words.filter { it in wordBank }
                    if (filtered.size < 3) {
                        Log.w(TAG, "Too few valid words after filtering (${filtered.size}), using all words")
                        cachedFilteredWords = availableWords.map { it.word }
                    } else {
                        cachedFilteredWords = filtered
                    }
                }
            }
```

Also move the `val wordBank` declaration (originally line 147) above the if/else block since it's only used in the semantic branch now. The semantic branch already has it inline above.

**Step 2: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: integrate PatternRouter into generateActivity pipeline"
```

---

## Task 4: Add InsufficientWords state handling to LessonViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt`

**Step 1: Add suggestion-related fields to LessonUiState**

Add these fields to the `LessonUiState` data class (after `wordGenerationRequestedCount` at line ~1006):

```kotlin
    // Word suggestion dialog state (for insufficient word bank)
    val isWordSuggestionDialogVisible: Boolean = false,
    val wordSuggestionPattern: String = "",
    val wordSuggestionMatching: List<String> = emptyList(),
    val wordSuggestionCandidates: List<String> = emptyList(),
    val wordSuggestionSelected: Set<String> = emptySet(),
    val isWordSuggestionLoading: Boolean = false,
    val wordSuggestionError: String? = null
```

**Step 2: Add ViewModel functions for the suggestion flow**

Add these functions to `LessonViewModel`, after the `regenerateSet` function (after line ~728):

```kotlin
    /**
     * Handle InsufficientWords result by generating suggestions and showing dialog.
     */
    private fun handleInsufficientWords(
        pattern: String,
        matchingWords: List<String>,
        onGenerationComplete: (String) -> Unit
    ) {
        _uiState.update {
            it.copy(
                isActivityCreationLoading = false,
                isWordSuggestionDialogVisible = true,
                wordSuggestionPattern = pattern,
                wordSuggestionMatching = matchingWords,
                wordSuggestionCandidates = emptyList(),
                wordSuggestionSelected = emptySet(),
                isWordSuggestionLoading = true,
                wordSuggestionError = null
            )
        }
        _generationPhase.value = GenerationPhase.Idle

        viewModelScope.launch {
            try {
                val existingWords = _uiState.value.words
                val candidates = geminiRepository.generateCVCWords(
                    prompt = "$pattern words",
                    count = 8,
                    existingWords = existingWords
                )
                // Also filter out words already matching in the bank
                val existingWordSet = existingWords.map { it.word.lowercase() }.toSet()
                val filtered = candidates.filter { it.lowercase() !in existingWordSet }

                if (filtered.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isWordSuggestionLoading = false,
                            wordSuggestionError = "Could not generate suggestions. Please add words manually."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isWordSuggestionLoading = false,
                            wordSuggestionCandidates = filtered,
                            wordSuggestionSelected = filtered.toSet() // Pre-select all
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isWordSuggestionLoading = false,
                        wordSuggestionError = "Failed to generate suggestions: ${e.message}"
                    )
                }
            }
        }

        // Store callback for after words are added
        pendingGenerationCallback = onGenerationComplete
    }

    // Stored callback for resuming generation after suggestion dialog
    private var pendingGenerationCallback: ((String) -> Unit)? = null

    /**
     * Toggle selection of a suggested word.
     */
    fun toggleWordSuggestion(word: String) {
        _uiState.update { state ->
            val current = state.wordSuggestionSelected
            val updated = if (word in current) current - word else current + word
            state.copy(wordSuggestionSelected = updated)
        }
    }

    /**
     * Add selected suggested words to the word bank and resume generation.
     */
    fun confirmWordSuggestions() {
        val userId = currentUserId ?: return
        val selected = _uiState.value.wordSuggestionSelected.toList()

        if (selected.isEmpty()) {
            _uiState.update { it.copy(wordSuggestionError = "Select at least one word to add") }
            return
        }

        _uiState.update { it.copy(isWordSuggestionLoading = true, wordSuggestionError = null) }

        viewModelScope.launch {
            val wordDao = database.wordDao()
            val addedWords = mutableListOf<String>()

            for (word in selected) {
                try {
                    val id = wordDao.insertWord(
                        com.example.app.data.entity.Word(userId = userId, word = word)
                    )
                    if (id > 0) addedWords.add(word)
                } catch (e: Exception) {
                    android.util.Log.w("LessonViewModel", "Failed to insert suggestion '$word': ${e.message}")
                }
            }

            if (addedWords.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isWordSuggestionLoading = false,
                        wordSuggestionError = "Failed to add words. Please try again."
                    )
                }
                return@launch
            }

            // Invalidate caches
            cachedWordCountForPrompts = -1
            geminiRepository.invalidateWordCache()

            // Dismiss dialog
            _uiState.update {
                it.copy(
                    isWordSuggestionDialogVisible = false,
                    isWordSuggestionLoading = false,
                    wordSuggestionCandidates = emptyList(),
                    wordSuggestionSelected = emptySet()
                )
            }

            // Resume generation with the callback
            val callback = pendingGenerationCallback
            pendingGenerationCallback = null
            if (callback != null) {
                createActivity(callback)
            }
        }
    }

    /**
     * Dismiss the word suggestion dialog without adding words.
     */
    fun dismissWordSuggestionDialog() {
        pendingGenerationCallback = null
        _uiState.update {
            it.copy(
                isWordSuggestionDialogVisible = false,
                wordSuggestionCandidates = emptyList(),
                wordSuggestionSelected = emptySet(),
                wordSuggestionError = null
            )
        }
    }
```

**Step 3: Update the `createActivity` function to handle InsufficientWords**

In `createActivity()` (line ~661), add a new branch to the `when` block:

Replace the `when (result)` block (lines 661-684):

```kotlin
            when (result) {
                is AiGenerationResult.Success -> {
                    _generationPhase.value = GenerationPhase.Idle
                    val jsonResult = com.google.gson.Gson().toJson(result.data)
                    _uiState.update {
                        it.copy(
                            isActivityCreationModalVisible = false,
                            activityInput = "",
                            isActivityCreationLoading = false,
                            generatedJsonResult = jsonResult
                        )
                    }
                    onGenerationComplete(jsonResult)
                }
                is AiGenerationResult.InsufficientWords -> {
                    handleInsufficientWords(
                        pattern = result.pattern,
                        matchingWords = result.matchingWords,
                        onGenerationComplete = onGenerationComplete
                    )
                }
                is AiGenerationResult.Error -> {
                    _generationPhase.value = GenerationPhase.Idle
                    _uiState.update {
                        it.copy(
                            isActivityCreationLoading = false,
                            activityError = result.message
                        )
                    }
                }
            }
```

**Step 4: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt
git commit -m "feat: add InsufficientWords handling with suggestion flow in LessonViewModel"
```

---

## Task 5: Create the WordSuggestionDialog composable

**Files:**
- Create: `app/src/main/java/com/example/app/ui/components/wordbank/WordSuggestionDialog.kt`

**Step 1: Create the dialog composable**

Create a new file at `app/src/main/java/com/example/app/ui/components/wordbank/WordSuggestionDialog.kt`:

```kotlin
package com.example.app.ui.components.wordbank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.flowlayout.FlowRow

/**
 * Dialog shown when the word bank has fewer than 3 words matching a structural pattern.
 * Displays AI-generated word suggestions that the teacher can select and add to the bank.
 */
@Composable
fun WordSuggestionDialog(
    pattern: String,
    matchingWords: List<String>,
    candidates: List<String>,
    selectedWords: Set<String>,
    isLoading: Boolean,
    error: String?,
    onToggleWord: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF49A9FF),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Need More Words",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Explanation
                    val matchText = if (matchingWords.isEmpty()) {
                        "No words in your Word Bank match \"$pattern\"."
                    } else {
                        "Only ${matchingWords.size} word${if (matchingWords.size == 1) "" else "s"} in your Word Bank match${if (matchingWords.size <= 3) "" else "es"} \"$pattern\" (${matchingWords.joinToString(", ")})."
                    }
                    Text(
                        text = "$matchText You need at least 3. Here are some suggestions:",
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        lineHeight = 20.sp
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF49A9FF),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else if (error != null) {
                        Text(
                            text = error,
                            fontSize = 14.sp,
                            color = Color(0xFFD32F2F)
                        )
                    } else {
                        // Word chips
                        FlowRow(
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp
                        ) {
                            candidates.forEach { word ->
                                val isSelected = word in selectedWords
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color(0xFF49A9FF) else Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { onToggleWord(word) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                text = "\u2713",
                                                fontSize = 14.sp,
                                                color = Color(0xFF49A9FF),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = word,
                                            fontSize = 14.sp,
                                            color = if (isSelected) Color(0xFF1976D2) else Color(0xFF666666),
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            )
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }

                        // Add & Generate button
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedWords.isNotEmpty() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49A9FF)
                            )
                        ) {
                            Text(
                                text = "Add & Generate",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

Note: If the project uses a different FlowRow import (e.g., `androidx.compose.foundation.layout.FlowRow` instead of Accompanist), adjust the import accordingly. Check existing usage in `ActivityCreationModal.kt` for the correct import.

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/wordbank/WordSuggestionDialog.kt
git commit -m "feat: create WordSuggestionDialog composable for insufficient word bank"
```

---

## Task 6: Wire WordSuggestionDialog into the Lesson screen

**Files:**
- Modify: The screen composable that hosts `ActivityCreationModal` — find the parent composable that reads `LessonUiState` and renders activity creation UI. This is likely `LessonScreen.kt` or the parent composable calling `ActivityCreationModal`.

**Step 1: Find the host composable**

Search for where `ActivityCreationModal` is called:

Run: `grep -rn "ActivityCreationModal" app/src/main/java/`

The result will show the file and line. This is the file to modify.

**Step 2: Add the WordSuggestionDialog call**

In the host composable, add this block near the existing `ActivityCreationModal` call:

```kotlin
// Word Suggestion Dialog
if (uiState.isWordSuggestionDialogVisible) {
    WordSuggestionDialog(
        pattern = uiState.wordSuggestionPattern,
        matchingWords = uiState.wordSuggestionMatching,
        candidates = uiState.wordSuggestionCandidates,
        selectedWords = uiState.wordSuggestionSelected,
        isLoading = uiState.isWordSuggestionLoading,
        error = uiState.wordSuggestionError,
        onToggleWord = { viewModel.toggleWordSuggestion(it) },
        onConfirm = { viewModel.confirmWordSuggestions() },
        onDismiss = { viewModel.dismissWordSuggestionDialog() }
    )
}
```

Add the import at the top of the file:
```kotlin
import com.example.app.ui.components.wordbank.WordSuggestionDialog
```

**Step 3: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add <the-host-composable-file>
git commit -m "feat: wire WordSuggestionDialog into Lesson screen"
```

---

## Task 7: Add post-validation safety net for semantic AI responses

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add post-validation function**

Add after the `filterWordsByPattern` function:

```kotlin
/**
 * Post-validate AI-filtered words against a structural pattern.
 * Used when a prompt has both semantic and structural components
 * (e.g., "animal words ending in -at").
 */
fun postValidateAiFiltered(
    aiFilteredWords: List<String>,
    prompt: String,
    availableWords: List<Word>
): List<String> {
    val pattern = classifyPrompt(prompt) ?: return aiFilteredWords
    val validWords = filterWordsByPattern(pattern, availableWords).toSet()
    return aiFilteredWords.filter { it in validWords }
}
```

**Step 2: Apply post-validation in the semantic branch of generateActivity**

In the semantic branch (the `else` block added in Task 3), after the `cachedFilteredWords` assignment, add post-validation:

After the line `cachedFilteredWords = filtered` and before `cacheTimestamp = ...`, add:

```kotlin
                // Post-validate: if prompt contains a structural pattern component,
                // strip any AI-selected words that don't match it
                val postValidated = postValidateAiFiltered(
                    cachedFilteredWords ?: availableWords.map { it.word },
                    prompt,
                    availableWords
                )
                if (postValidated.size >= 3) {
                    cachedFilteredWords = postValidated
                }
```

Note: This only takes effect if post-validation still leaves >= 3 words. If it reduces below 3, we keep the AI's original selection (since the structural pattern might be a secondary concern in a semantic request).

**Step 3: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: add post-validation safety net for AI-filtered words"
```

---

## Task 8: Handle InsufficientWords in regenerateSet flow

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt`

**Step 1: Update regenerateSet to handle InsufficientWords**

The `regenerateSet` function (lines 692-728) currently only handles Success and Error. The `regenerateSet` in GeminiRepository skips Step 1 (uses cached words), so it shouldn't hit InsufficientWords in normal flow. However, for safety, handle it gracefully:

In the `when (result)` block inside `regenerateSet()` (around line 716), add:

```kotlin
                is AiGenerationResult.InsufficientWords -> {
                    _generationPhase.value = GenerationPhase.Idle
                    onResult(null) // Treat as failure for regeneration
                }
```

**Step 2: Verify it compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt
git commit -m "feat: handle InsufficientWords in regenerateSet for safety"
```

---

## Task 9: End-to-end verification

**Step 1: Full build**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew assembleDebug 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL

**Step 2: Verify PatternRouter classification**

Manually check these test cases mentally or via a test file:
- `"-un words"` → `DetectedPattern.Rime("un")`
- `"words ending in -at"` → `DetectedPattern.Rime("at")`
- `"starting with b"` → `DetectedPattern.Onset('b')`
- `"short a words"` → `DetectedPattern.Vowel('a')`
- `"animal words"` → `null` (semantic, goes to AI)
- `"fun words"` → `null` (no structural pattern)

**Step 3: Review all changes**

Run: `git diff --stat HEAD~8` (or however many commits were made)
Verify only the expected files were changed.

**Step 4: Final commit (if any cleanup needed)**

```bash
git add -A
git commit -m "chore: cleanup after activity generation fixes"
```
