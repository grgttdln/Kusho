# Watch Feedback: Phone as Single Source of Truth — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the watch ViewModel's local RESULT state so the phone is the sole authority for right/wrong feedback, preventing the watch from resetting to idle before feedback arrives.

**Architecture:** The watch ViewModel's state machine loses its RESULT state and isCorrect field. After SHOWING_PREDICTION, the coroutine adds a safety timeout but otherwise waits. The Screen's `showingFeedback` (set by the phone's response) overlays the correct/wrong mascot. On dismissal, `resetToIdle()` returns to IDLE. The `LaunchedEffect(showingFeedback)` is guarded to only reset on true→false transitions.

**Tech Stack:** Kotlin, Jetpack Compose for Wear OS, StateFlow, ViewModel

**Design doc:** `docs/plans/2026-02-20-watch-feedback-phone-source-of-truth-design.md`

---

### Task 1: Remove RESULT state and isCorrect from LearnModeViewModel

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt`

**Step 1: Remove RESULT from State enum**

Change the State enum at line 71-78 from:

```kotlin
enum class State {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PROCESSING,
    SHOWING_PREDICTION,
    RESULT
}
```

To:

```kotlin
enum class State {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PROCESSING,
    SHOWING_PREDICTION
}
```

**Step 2: Remove isCorrect from UiState**

Change the UiState data class at line 80-91 from:

```kotlin
data class UiState(
    val title: String = "Learn Mode",
    val state: State = State.IDLE,
    val countdownSeconds: Int = 0,
    val recordingProgress: Float = 0f,
    val prediction: String? = null,
    val confidence: Float? = null,
    val isCorrect: Boolean? = null,
    val statusMessage: String = "Tap to guess",
    val errorMessage: String? = null,
    val isModelLoaded: Boolean = false
)
```

To:

```kotlin
data class UiState(
    val title: String = "Learn Mode",
    val state: State = State.IDLE,
    val countdownSeconds: Int = 0,
    val recordingProgress: Float = 0f,
    val prediction: String? = null,
    val confidence: Float? = null,
    val statusMessage: String = "Tap to guess",
    val errorMessage: String? = null,
    val isModelLoaded: Boolean = false
)
```

**Step 3: Replace Phase 5 (RESULT + auto-reset) with safety timeout**

In `startRecording()`, replace lines 261-303 (Phase 4 prediction display + Phase 5 RESULT):

Before:
```kotlin
// === Phase 4: Show the predicted letter first ===
val predictedLetter = result.label?.trim()
val isCorrect = isLetterMatch(predictedLetter, currentMaskedLetter.trim())

Log.d(TAG, "Predicted: '$predictedLetter', Expected: '$currentMaskedLetter', Correct: $isCorrect")

_uiState.update {
    it.copy(
        state = State.SHOWING_PREDICTION,
        prediction = predictedLetter,
        confidence = result.confidence,
        isCorrect = isCorrect,
        errorMessage = null,
        recordingProgress = 0f
    )
}

delay(PREDICTION_DISPLAY_MS)

if (!isActive) return@launch

// === Phase 5: Show the result (correct/wrong) ===
_uiState.update {
    it.copy(
        state = State.RESULT,
        statusMessage = if (isCorrect) "Correct! ✓" else "Try again"
    )
}

// Auto-reset after showing result
delay(RESULT_DISPLAY_SECONDS * 1000L)
if (isActive && _uiState.value.state == State.RESULT) {
    _uiState.update {
        it.copy(
            state = State.IDLE,
            statusMessage = "Tap to guess"
        )
    }
}
```

After:
```kotlin
// === Phase 4: Show the predicted letter ===
val predictedLetter = result.label?.trim()

Log.d(TAG, "Predicted: '$predictedLetter', Expected: '$currentMaskedLetter'")

_uiState.update {
    it.copy(
        state = State.SHOWING_PREDICTION,
        prediction = predictedLetter,
        confidence = result.confidence,
        errorMessage = null,
        recordingProgress = 0f
    )
}

// Show prediction briefly, then wait for phone feedback
delay(PREDICTION_DISPLAY_MS)

if (!isActive) return@launch

// Safety timeout: if phone doesn't respond within 8 seconds, reset to idle
delay(PHONE_FEEDBACK_TIMEOUT_MS)
if (isActive && _uiState.value.state == State.SHOWING_PREDICTION) {
    Log.w(TAG, "Phone feedback timeout — resetting to idle")
    _uiState.update {
        it.copy(
            state = State.IDLE,
            statusMessage = "Tap to guess"
        )
    }
}
```

**Step 4: Add the timeout constant and remove unused ones**

In the companion object, change:
```kotlin
private const val RESULT_DISPLAY_SECONDS = 3
```

To:
```kotlin
private const val PHONE_FEEDBACK_TIMEOUT_MS = 8000L
```

**Step 5: Clean up resetToIdle and cancelRecording**

In `resetToIdle()` at line 341-354, remove `isCorrect = null` from the update:

```kotlin
fun resetToIdle() {
    Log.d(TAG, "Resetting to idle")
    recordingJob?.cancel()
    _uiState.update {
        it.copy(
            state = State.IDLE,
            statusMessage = "Tap to guess",
            errorMessage = null,
            prediction = null,
            recordingProgress = 0f,
            countdownSeconds = 0
        )
    }
}
```

Also add `recordingJob?.cancel()` so that resetting to idle also cancels the safety timeout coroutine (prevents the timeout from firing after feedback was handled).

Also clean up `startRecording()`: remove the local `isCorrect` variable and the `isLetterMatch` call (no longer needed since phone determines correctness). The `isLetterMatch` function and `similarCaseLetters` set in the companion object can also be removed since they're no longer used by the ViewModel.

**Step 6: Remove the word data observer's RESULT check**

In init block at line 133, change:
```kotlin
if (_uiState.value.state == State.RESULT) {
```
To:
```kotlin
if (_uiState.value.state != State.IDLE) {
```

This ensures new word data always resets the ViewModel to idle regardless of current state.

**Step 7: Build to verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (may have compilation errors in LearnModeScreen.kt referencing `State.RESULT` — those are fixed in Task 2)

**Step 8: Commit**

```bash
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt
git commit -m "refactor(learn): remove RESULT state from watch ViewModel

Phone is single source of truth for right/wrong feedback.
ViewModel now stays in SHOWING_PREDICTION until phone responds
or 8-second safety timeout expires."
```

---

### Task 2: Update LearnModeScreen to remove RESULT branches and fix feedback reset

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`

**Step 1: Remove RESULT branch from FillInTheBlankMainContent**

At line 491-498, remove the entire `State.RESULT` branch from the `when` block:

```kotlin
// DELETE this entire branch:
LearnModeViewModel.State.RESULT -> {
    // This state should not be reached as feedback is now controlled by phone
    // But just in case, reset to idle
    LaunchedEffect(Unit) {
        viewModel.resetToIdle()
    }
    IdleContent(maskedWord = maskedWord, viewModel = viewModel)
}
```

**Step 2: Remove RESULT branch from WriteTheWordMainContent**

At line 803-807, remove the `State.RESULT` branch:

```kotlin
// DELETE this entire branch:
LearnModeViewModel.State.RESULT -> WriteTheWordIdleContent(
    wordData = wordData,
    writeTheWordState = writeTheWordState,
    viewModel = viewModel
)
```

**Step 3: Remove RESULT branch from NameThePictureMainContent**

At line 1121-1125, remove the `State.RESULT` branch:

```kotlin
// DELETE this entire branch:
LearnModeViewModel.State.RESULT -> NameThePictureIdleContent(
    wordData = wordData,
    writeTheWordState = writeTheWordState,
    viewModel = viewModel
)
```

**Step 4: Fix LaunchedEffect(showingFeedback) in FillInTheBlankMainContent**

At line 447-451, change:

```kotlin
LaunchedEffect(showingFeedback) {
    if (!showingFeedback) {
        viewModel.resetToIdle()
    }
}
```

To:

```kotlin
// Reset ViewModel only when feedback is actually dismissed (true -> false),
// not on initial composition when showingFeedback starts as false
var previousShowingFeedback by remember { mutableStateOf(false) }
LaunchedEffect(showingFeedback) {
    if (previousShowingFeedback && !showingFeedback) {
        viewModel.resetToIdle()
    }
    previousShowingFeedback = showingFeedback
}
```

**Step 5: Apply the same fix to WriteTheWordMainContent**

At line 758-762, apply the same pattern:

```kotlin
var previousShowingFeedback by remember { mutableStateOf(false) }
LaunchedEffect(showingFeedback) {
    if (previousShowingFeedback && !showingFeedback) {
        viewModel.resetToIdle()
    }
    previousShowingFeedback = showingFeedback
}
```

**Step 6: Apply the same fix to NameThePictureMainContent**

At line 1079-1083, apply the same pattern:

```kotlin
var previousShowingFeedback by remember { mutableStateOf(false) }
LaunchedEffect(showingFeedback) {
    if (previousShowingFeedback && !showingFeedback) {
        viewModel.resetToIdle()
    }
    previousShowingFeedback = showingFeedback
}
```

**Step 7: Remove unused isCorrectPrediction variable in WriteTheWordMainContent**

At line 765, remove:
```kotlin
val isCorrectPrediction = uiState.prediction?.firstOrNull() == expectedLetter
```

This variable references `uiState.isCorrect` indirectly and is unused in the render logic.

**Step 8: Remove unused WriteTheWordResultContent composable**

Delete the entire `WriteTheWordResultContent` composable (lines 888-917) — it referenced `isCorrect` and `State.RESULT` and is no longer called from anywhere.

**Step 9: Build to verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 10: Commit**

```bash
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "fix(learn): remove RESULT branches and fix feedback reset timing

Remove State.RESULT from all when blocks. Fix LaunchedEffect(showingFeedback)
to only reset ViewModel on actual feedback dismissal (true->false), not on
initial composition."
```

---

### Task 3: Full build verification

**Step 1: Build both app and wear modules**

Run: `./gradlew :app:compileDebugKotlin :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL for both

**Step 2: Verify no remaining references to removed items**

Search for `State.RESULT`, `isCorrect` in ViewModel context, `RESULT_DISPLAY_SECONDS`:

```bash
grep -rn "State\.RESULT\|RESULT_DISPLAY_SECONDS" wear/src/
grep -n "isCorrect" wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt
```

Expected: No matches (or only unrelated matches like `isCorrect` in other files)

**Step 3: Commit design doc if not already committed**

```bash
git add docs/plans/2026-02-20-watch-feedback-phone-source-of-truth-design.md
git add docs/plans/2026-02-20-watch-feedback-phone-source-of-truth-impl.md
git commit -m "docs: add design and implementation plan for watch feedback fix"
```
