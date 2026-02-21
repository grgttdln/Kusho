# CVC Dictionary Validation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Validate Word Bank entries against Android's built-in spell checker so only real English CVC words can be added, with suggestion chips for invalid entries.

**Architecture:** New `DictionaryValidator` wraps `SpellCheckerSession` as a suspend function. `WordRepository` calls it after CVC validation passes. ViewModel exposes suggestions; modals render them as clickable chips.

**Tech Stack:** Android `SpellCheckerSession` API, Kotlin Coroutines (`suspendCancellableCoroutine`), Jetpack Compose

---

### Task 1: Create DictionaryResult sealed class and DictionaryValidator

**Files:**
- Create: `app/src/main/java/com/example/app/util/DictionaryValidator.kt`

**Step 1: Create DictionaryValidator.kt with DictionaryResult sealed class and full implementation**

```kotlin
package com.example.app.util

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Result of dictionary validation.
 */
sealed class DictionaryResult {
    /** The word exists in the dictionary. */
    data object Valid : DictionaryResult()

    /** The word was not found. [suggestions] contains CVC-filtered alternatives. */
    data class Invalid(val suggestions: List<String>) : DictionaryResult()

    /** Spell checker service is unavailable; word is accepted by default. */
    data object Unavailable : DictionaryResult()
}

/**
 * Validates words against the Android system dictionary using [SpellCheckerSession].
 *
 * - Works offline (dictionary is stored locally on device).
 * - Falls back to [DictionaryResult.Unavailable] when no spell checker service exists.
 * - Times out after [TIMEOUT_MS] and accepts the word to avoid blocking users.
 * - Filters suggestions to CVC-pattern words via [WordValidator.isCVCPattern].
 */
class DictionaryValidator(context: Context) : Closeable {

    companion object {
        private const val TAG = "DictionaryValidator"
        private const val TIMEOUT_MS = 3000L
    }

    private val appContext = context.applicationContext
    private var session: SpellCheckerSession? = null

    /**
     * Validate whether [word] exists in the system dictionary.
     *
     * @return [DictionaryResult.Valid] if the word is recognized,
     *         [DictionaryResult.Invalid] with CVC suggestions if not,
     *         [DictionaryResult.Unavailable] if the spell checker is missing or times out.
     */
    suspend fun validateWord(word: String): DictionaryResult {
        val spellSession = getOrCreateSession()
        if (spellSession == null) {
            Log.w(TAG, "SpellCheckerSession unavailable on this device")
            return DictionaryResult.Unavailable
        }

        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SpellCheckerSession.SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                        // Not used — we call getSentenceSuggestions
                    }

                    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                        if (results.isNullOrEmpty()) {
                            continuation.resume(DictionaryResult.Valid)
                            return
                        }

                        val info = results[0]
                        if (info.suggestionsCount == 0) {
                            continuation.resume(DictionaryResult.Valid)
                            return
                        }

                        val suggestionsInfo = info.getSuggestionsInfoAt(0)
                        val attrs = suggestionsInfo.suggestionsAttributes

                        // If the word is in the dictionary, it comes back with IN_THE_DICTIONARY flag
                        if (attrs and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0) {
                            continuation.resume(DictionaryResult.Valid)
                            return
                        }

                        // Word not found — collect suggestions filtered to CVC pattern
                        val suggestions = mutableListOf<String>()
                        for (i in 0 until suggestionsInfo.suggestionsCount) {
                            val suggestion = suggestionsInfo.getSuggestionAt(i).lowercase()
                            if (WordValidator.isCVCPattern(suggestion) && suggestion != word.lowercase()) {
                                suggestions.add(suggestion)
                            }
                        }

                        continuation.resume(DictionaryResult.Invalid(suggestions.distinct().take(5)))
                    }
                }

                spellSession.getSentenceSuggestions(
                    arrayOf(TextInfo(word.lowercase())),
                    5 // max suggestions per word
                )

                // The listener is set at session creation time, so we need a different approach.
                // Actually, SpellCheckerSession uses the listener passed at creation.
                // We need to restructure to use a single listener that routes results.

                // Re-approach: since the listener is set at session creation, we handle this
                // by creating a fresh session per call or using a routing mechanism.
                // For simplicity, we close and recreate with the new listener.

                // This won't work as-is. Let me restructure.
            }
        }

        return result ?: run {
            Log.w(TAG, "SpellCheckerSession timed out for word: $word")
            DictionaryResult.Unavailable
        }
    }

    private fun getOrCreateSession(): SpellCheckerSession? {
        session?.let { return it }

        val tsm = appContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
            ?: return null

        // We'll create sessions on-demand in validateWord instead
        return null // placeholder — see restructured approach below
    }

    override fun close() {
        session?.close()
        session = null
    }
}
```

**Wait — the `SpellCheckerSession` API binds the listener at creation time, not per-call.** This means we need to create a new session for each validation call (or use a routing listener). The cleaner approach is to create a short-lived session per call. Let me revise.

**Step 1 (revised): Create DictionaryValidator.kt with per-call session approach**

```kotlin
package com.example.app.util

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed class DictionaryResult {
    data object Valid : DictionaryResult()
    data class Invalid(val suggestions: List<String>) : DictionaryResult()
    data object Unavailable : DictionaryResult()
}

class DictionaryValidator(context: Context) {

    companion object {
        private const val TAG = "DictionaryValidator"
        private const val TIMEOUT_MS = 3000L
    }

    private val appContext = context.applicationContext

    suspend fun validateWord(word: String): DictionaryResult {
        val tsm = appContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
            as? TextServicesManager ?: run {
            Log.w(TAG, "TextServicesManager unavailable")
            return DictionaryResult.Unavailable
        }

        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SpellCheckerSession.SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                        // unused — we use getSentenceSuggestions
                    }

                    override fun onGetSentenceSuggestions(
                        results: Array<out SentenceSuggestionsInfo>?
                    ) {
                        if (!continuation.isActive) return

                        if (results.isNullOrEmpty() || results[0].suggestionsCount == 0) {
                            continuation.resume(DictionaryResult.Valid)
                            return
                        }

                        val suggestionsInfo = results[0].getSuggestionsInfoAt(0)
                        val attrs = suggestionsInfo.suggestionsAttributes

                        if (attrs and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0) {
                            continuation.resume(DictionaryResult.Valid)
                            return
                        }

                        val suggestions = mutableListOf<String>()
                        for (i in 0 until suggestionsInfo.suggestionsCount) {
                            val s = suggestionsInfo.getSuggestionAt(i).lowercase()
                            if (WordValidator.isCVCPattern(s) && s != word.lowercase()) {
                                suggestions.add(s)
                            }
                        }
                        continuation.resume(
                            DictionaryResult.Invalid(suggestions.distinct().take(5))
                        )
                    }
                }

                val session = tsm.newSpellCheckerSession(
                    null, Locale.ENGLISH, listener, false
                )

                if (session == null) {
                    continuation.resume(DictionaryResult.Unavailable)
                    return@suspendCancellableCoroutine
                }

                continuation.invokeOnCancellation { session.close() }

                session.getSentenceSuggestions(
                    arrayOf(TextInfo(word.lowercase())),
                    5
                )
            }
        }

        return result ?: run {
            Log.w(TAG, "Spell check timed out for: $word")
            DictionaryResult.Unavailable
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/util/DictionaryValidator.kt
git commit -m "feat: add DictionaryValidator using SpellCheckerSession API"
```

---

### Task 2: Add NotInDictionary result variant to WordRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/WordRepository.kt`

**Step 1: Add DictionaryValidator parameter and NotInDictionary result variants**

In `WordRepository.kt`:

1. Change constructor to accept `DictionaryValidator`:
```kotlin
class WordRepository(
    private val wordDao: WordDao,
    private val dictionaryValidator: DictionaryValidator? = null
)
```

2. Add `NotInDictionary` to `AddWordResult`:
```kotlin
sealed class AddWordResult {
    data class Success(val wordId: Long) : AddWordResult()
    data class Error(val message: String) : AddWordResult()
    data class NotInDictionary(val suggestions: List<String>) : AddWordResult()
}
```

3. Add `NotInDictionary` to `UpdateWordResult`:
```kotlin
sealed class UpdateWordResult {
    data object Success : UpdateWordResult()
    data class Error(val message: String) : UpdateWordResult()
    data class NotInDictionary(val suggestions: List<String>) : UpdateWordResult()
}
```

**Step 2: Add dictionary validation step to `addWord()` — insert between CVC validation and duplicate check**

After the `WordValidator.validateWordForBank()` check (line 52-55) and before the duplicate check (line 58), add:

```kotlin
// Dictionary validation (only if validator is available)
if (dictionaryValidator != null) {
    when (val dictResult = dictionaryValidator.validateWord(trimmedWord)) {
        is DictionaryResult.Invalid -> {
            return@withContext AddWordResult.NotInDictionary(dictResult.suggestions)
        }
        is DictionaryResult.Valid,
        is DictionaryResult.Unavailable -> { /* proceed */ }
    }
}
```

**Step 3: Add dictionary validation step to `updateWord()` — same position**

After the `WordValidator.validateWordForBank()` check (line 134-137) and before the duplicate check (line 140), add the same block as Step 2 but returning `UpdateWordResult.NotInDictionary(dictResult.suggestions)`.

**Step 4: Add import for DictionaryResult and DictionaryValidator**

```kotlin
import com.example.app.util.DictionaryResult
import com.example.app.util.DictionaryValidator
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/WordRepository.kt
git commit -m "feat: add dictionary validation step to WordRepository add/update flows"
```

---

### Task 3: Update LessonUiState and LessonViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt`

**Step 1: Add `dictionarySuggestions` and `editDictionarySuggestions` fields to `LessonUiState`**

In the `LessonUiState` data class (line 727), add two new fields:

```kotlin
// Dictionary suggestion state (add modal)
val dictionarySuggestions: List<String> = emptyList(),
// Dictionary suggestion state (edit modal)
val editDictionarySuggestions: List<String> = emptyList(),
```

**Step 2: Create `DictionaryValidator` instance and pass to `WordRepository`**

In `LessonViewModel`, add after `imageStorageManager` initialization (line 34):

```kotlin
private val dictionaryValidator = DictionaryValidator(application)
```

Change the `wordRepository` initialization (line 32) to:

```kotlin
private val wordRepository = WordRepository(database.wordDao(), dictionaryValidator)
```

**Step 3: Handle `NotInDictionary` result in `addWordToBank()`**

In the `when` block inside `addWordToBank()` (around line 206), add a new branch:

```kotlin
is WordRepository.AddWordResult.NotInDictionary -> {
    if (imagePath != null) {
        imageStorageManager.deleteImage(imagePath)
    }
    _uiState.update {
        it.copy(
            inputError = "This word was not found in the dictionary",
            dictionarySuggestions = result.suggestions,
            isLoading = false
        )
    }
}
```

**Step 4: Handle `NotInDictionary` result in `saveEditedWord()`**

In the `when` block inside `saveEditedWord()` (around line 421), add a new branch:

```kotlin
is WordRepository.UpdateWordResult.NotInDictionary -> {
    if (newMediaUri != null && newImagePath != editingWord.imagePath) {
        newImagePath?.let { imageStorageManager.deleteImage(it) }
    }
    _uiState.update {
        it.copy(
            editInputError = "This word was not found in the dictionary",
            editDictionarySuggestions = result.suggestions,
            isEditLoading = false
        )
    }
}
```

**Step 5: Clear suggestions when user types in `onWordInputChanged()`**

Change `onWordInputChanged()` (line 112-119) to also clear `dictionarySuggestions`:

```kotlin
fun onWordInputChanged(word: String) {
    _uiState.update {
        it.copy(
            wordInput = word.trim(),
            inputError = null,
            dictionarySuggestions = emptyList()
        )
    }
}
```

**Step 6: Clear suggestions when user types in `onEditWordInputChanged()`**

Change `onEditWordInputChanged()` (line 311-318) to also clear `editDictionarySuggestions`:

```kotlin
fun onEditWordInputChanged(word: String) {
    _uiState.update {
        it.copy(
            editWordInput = word.trim(),
            editInputError = null,
            editDictionarySuggestions = emptyList()
        )
    }
}
```

**Step 7: Clear suggestions when modals are dismissed**

In `hideWordBankModal()` (line 97-107), add `dictionarySuggestions = emptyList()` to the copy.

In `hideEditModal()` (line 294-306), add `editDictionarySuggestions = emptyList()` to the copy.

**Step 8: Add `onSuggestionClick` function for the add modal**

```kotlin
fun onSuggestionClick(word: String) {
    _uiState.update {
        it.copy(
            wordInput = word,
            inputError = null,
            dictionarySuggestions = emptyList()
        )
    }
}
```

**Step 9: Add `onEditSuggestionClick` function for the edit modal**

```kotlin
fun onEditSuggestionClick(word: String) {
    _uiState.update {
        it.copy(
            editWordInput = word,
            editInputError = null,
            editDictionarySuggestions = emptyList()
        )
    }
}
```

**Step 10: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt
git commit -m "feat: handle dictionary validation results in ViewModel with suggestion state"
```

---

### Task 4: Add suggestion chips to WordBankModal

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/wordbank/WordBankModal.kt`

**Step 1: Add `dictionarySuggestions` and `onSuggestionClick` parameters to `WordBankModal`**

Add two new parameters to the `WordBankModal` composable signature (after `onDismiss`):

```kotlin
dictionarySuggestions: List<String> = emptyList(),
onSuggestionClick: (String) -> Unit = {}
```

**Step 2: Pass suggestions through to `WordInputSection`**

Add `dictionarySuggestions` and `onSuggestionClick` parameters to the `WordInputSection` call inside `WordBankModal`, and update the `WordInputSection` private composable signature to accept them.

**Step 3: Add `SuggestionChipsRow` composable below the error text in `WordInputSection`**

After the `OutlinedTextField` (around line 234), add:

```kotlin
// Dictionary suggestion chips
if (dictionarySuggestions.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Did you mean:",
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dictionarySuggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFFE8F4FD),
                    labelColor = Color(0xFF49A9FF)
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = Color(0xFF49A9FF),
                    borderWidth = 1.dp
                )
            )
        }
    }
}
```

**Step 4: Add required imports**

```kotlin
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
```

**Step 5: Update Preview composables to include new parameters (use defaults — no changes needed since defaults are provided)**

No code changes needed for previews since both new parameters have default values.

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/wordbank/WordBankModal.kt
git commit -m "feat: add dictionary suggestion chips to WordBankModal"
```

---

### Task 5: Add suggestion chips to WordBankEditModal

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/wordbank/WordBankEditModal.kt`

**Step 1: Add `dictionarySuggestions` and `onSuggestionClick` parameters to `WordBankEditModal`**

Add two new parameters to the composable signature (after `onDismiss`):

```kotlin
dictionarySuggestions: List<String> = emptyList(),
onSuggestionClick: (String) -> Unit = {}
```

**Step 2: Pass suggestions through to `EditWordInputSection`**

Add `dictionarySuggestions` and `onSuggestionClick` parameters to the `EditWordInputSection` call, and update the private composable signature.

**Step 3: Add the same `SuggestionChipsRow` from Task 4 below the `OutlinedTextField` in `EditWordInputSection`**

Use identical chip code from Task 4 Step 3 (same styling, same layout).

**Step 4: Add required imports** (same as Task 4 Step 4)

```kotlin
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/wordbank/WordBankEditModal.kt
git commit -m "feat: add dictionary suggestion chips to WordBankEditModal"
```

---

### Task 6: Wire suggestions in WordBankScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt`

**Step 1: Pass `dictionarySuggestions` and `onSuggestionClick` to `WordBankModal`**

In the `WordBankModal` call (line 172-195), add:

```kotlin
dictionarySuggestions = uiState.dictionarySuggestions,
onSuggestionClick = { viewModel.onSuggestionClick(it) }
```

**Step 2: Pass `dictionarySuggestions` and `onSuggestionClick` to `WordBankEditModal`**

In the `WordBankEditModal` call (line 220-247), add:

```kotlin
dictionarySuggestions = uiState.editDictionarySuggestions,
onSuggestionClick = { viewModel.onEditSuggestionClick(it) }
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt
git commit -m "feat: wire dictionary suggestions from ViewModel to modal composables"
```

---

### Task 7: Build verification

**Step 1: Run a Gradle build to verify compilation**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. If there are compile errors, fix them before proceeding.

**Step 2: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: resolve any compilation issues from dictionary validation integration"
```

Only run this if Step 1 required fixes. If the build was clean, skip this step.
