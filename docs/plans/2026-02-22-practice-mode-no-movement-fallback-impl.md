# Practice Mode No-Movement Fallback Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Detect when the user doesn't move their wrist during practice mode recording and show a fallback "oops" screen with `dis_ops.png` instead of running the model on static data.

**Architecture:** Add variance-based motion detection in `PracticeModeViewModel` after recording stops, before classification. A new `NO_MOVEMENT` state triggers the fallback UI with `dis_ops.png` + TTS, then retries the same question.

**Tech Stack:** Kotlin, Jetpack Compose (Wear OS), TFLite, MotionSensorManager

---

### Task 1: Add NO_MOVEMENT state and motion threshold constant

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeViewModel.kt:146-154` (State enum)
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeViewModel.kt:48-53` (companion constants)

**Step 1: Add the NO_MOVEMENT state to the State enum**

In `PracticeModeViewModel.kt`, find the `State` enum at line 146 and add `NO_MOVEMENT` after `PROCESSING`:

```kotlin
enum class State {
    IDLE,
    QUESTION,
    COUNTDOWN,
    RECORDING,
    PROCESSING,
    NO_MOVEMENT,            // User didn't move wrist during recording
    SHOWING_PREDICTION,
    RESULT
}
```

**Step 2: Add the motion variance threshold constant**

In the `companion object` block at line 48, add:

```kotlin
private const val MOTION_VARIANCE_THRESHOLD = 0.5f  // Total variance below this = no movement. Tune on device.
private const val NO_MOVEMENT_DISPLAY_SECONDS = 3
```

Add them after the existing `PROGRESS_UPDATE_INTERVAL_MS` constant (line 53).

**Step 3: Verify the project still compiles**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (new state is defined but not yet used anywhere — that's fine)

---

### Task 2: Add hasSignificantMotion() function

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeViewModel.kt` (add private function before `onCleared()` at line 501)

**Step 1: Add the motion detection function**

Add this private function in `PracticeModeViewModel`, before the `onCleared()` method (line 501):

```kotlin
/**
 * Check if the recorded sensor samples contain significant wrist movement.
 * Computes per-channel variance and sums them. Low total variance = no movement.
 */
private fun hasSignificantMotion(samples: List<com.example.kusho.sensors.SensorSample>): Boolean {
    if (samples.size < 2) return false

    val n = samples.size.toFloat()

    // Extract per-channel values
    val channels = listOf(
        samples.map { it.ax },
        samples.map { it.ay },
        samples.map { it.az },
        samples.map { it.gx },
        samples.map { it.gy },
        samples.map { it.gz }
    )

    var totalVariance = 0f
    for ((idx, channel) in channels.withIndex()) {
        val mean = channel.sum() / n
        val variance = channel.sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / n
        totalVariance += variance
        Log.d(TAG, "Channel $idx: mean=%.4f, variance=%.4f".format(mean, variance))
    }

    Log.i(TAG, "Total motion variance: %.4f (threshold: $MOTION_VARIANCE_THRESHOLD)".format(totalVariance))
    return totalVariance >= MOTION_VARIANCE_THRESHOLD
}
```

**Step 2: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Wire motion check into the recording flow

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeViewModel.kt:333-351` (after getting samples, before classification)

**Step 1: Add the motion check after getting samples**

In `startAnswering()`, find this block at line 333-335:

```kotlin
// Get collected samples
val samples = sensorManager.getCollectedSamples()
Log.d(TAG, "Collected ${samples.size} samples")
```

Insert the motion check **immediately after** this block, **before** the existing `minSamples` check at line 337:

```kotlin
// Check for significant wrist movement
if (!hasSignificantMotion(samples)) {
    Log.w(TAG, "No significant wrist movement detected")
    _uiState.update {
        it.copy(
            state = State.NO_MOVEMENT,
            statusMessage = "Oops! You did not air write!",
            recordingProgress = 0f
            // currentQuestion is preserved so we retry the same question
        )
    }

    // Auto-return to QUESTION state after delay
    delay(NO_MOVEMENT_DISPLAY_SECONDS * 1000L)
    if (isActive && _uiState.value.state == State.NO_MOVEMENT) {
        val sameQuestion = _uiState.value.currentQuestion
        _uiState.update {
            it.copy(
                state = State.QUESTION,
                statusMessage = sameQuestion?.question ?: "Try again",
                isAnswerCorrect = null,
                prediction = null,
                confidence = null
            )
        }
    }
    return@launch
}
```

**Step 2: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Add NoMovementContent composable and wire into UI

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeScreen.kt:320-328` (state when block)
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeScreen.kt` (add new composable)

**Step 1: Add the NO_MOVEMENT branch to the `when` block**

In `PracticeModeContent`, find the `when` block at line 320 and add the new state. The full block becomes:

```kotlin
when (uiState.state) {
    PracticeModeViewModel.State.IDLE -> IdleContent(uiState, viewModel)
    PracticeModeViewModel.State.QUESTION -> QuestionContent(uiState, viewModel)
    PracticeModeViewModel.State.COUNTDOWN -> CountdownContent(uiState, viewModel)
    PracticeModeViewModel.State.RECORDING -> RecordingContent(uiState, viewModel)
    PracticeModeViewModel.State.PROCESSING -> ProcessingContent(uiState)
    PracticeModeViewModel.State.NO_MOVEMENT -> NoMovementContent(viewModel)
    PracticeModeViewModel.State.SHOWING_PREDICTION -> PredictionContent(uiState)
    PracticeModeViewModel.State.RESULT -> ResultContent(uiState, viewModel)
}
```

**Step 2: Add the NoMovementContent composable**

Add this new composable function after `ResultContent` (after line 582):

```kotlin
@Composable
private fun NoMovementContent(
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.retryAfterNoMovement() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dis_ops),
            contentDescription = "Oops, no movement detected",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: Will fail because `retryAfterNoMovement()` doesn't exist yet — that's Task 5.

---

### Task 5: Add retryAfterNoMovement() and TTS trigger

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeViewModel.kt` (add function after `resetToIdle()` at line 499)
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeScreen.kt` (add TTS LaunchedEffect)

**Step 1: Add retryAfterNoMovement() in ViewModel**

Add this function after `resetToIdle()` (line 499) in `PracticeModeViewModel`:

```kotlin
/**
 * Retry the same question after no movement was detected.
 * Cancels any pending auto-return and immediately goes back to QUESTION.
 */
fun retryAfterNoMovement() {
    if (_uiState.value.state != State.NO_MOVEMENT) return
    Log.d(TAG, "Retrying after no movement")
    recordingJob?.cancel()
    val sameQuestion = _uiState.value.currentQuestion
    _uiState.update {
        it.copy(
            state = State.QUESTION,
            statusMessage = sameQuestion?.question ?: "Try again",
            isAnswerCorrect = null,
            prediction = null,
            confidence = null
        )
    }
}
```

**Step 2: Add TTS LaunchedEffect for NO_MOVEMENT state**

In `PracticeModeContent`, find the existing `LaunchedEffect` that speaks the predicted letter (around line 229). Add this new `LaunchedEffect` nearby (e.g., right after the prediction LaunchedEffect block):

```kotlin
// Speak "oops" message when entering NO_MOVEMENT state
LaunchedEffect(uiState.state) {
    if (uiState.state == PracticeModeViewModel.State.NO_MOVEMENT) {
        ttsManager.speak("Oops, you did not air write!")
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: Manual testing on device

**No code changes. Test the feature on the actual watch.**

**Step 1: Install and run**

Run: `./gradlew :wear:installDebug`

**Step 2: Test no-movement scenario**

1. Open Kusho on watch → Practice Mode
2. Tap to start → See question
3. Tap to answer → Countdown starts
4. **Hold wrist completely still** during the 3-second recording
5. Expected: `dis_ops.png` appears, TTS says "Oops, you did not air write!"
6. Expected: After 3 seconds, returns to same question
7. Tap the oops screen before 3 seconds → should immediately return to same question

**Step 3: Test normal movement scenario**

1. Tap to start → See question
2. Tap to answer → Countdown starts
3. **Air-write a letter** during the 3-second recording
4. Expected: Normal prediction flow (processing → predicted letter → correct/wrong)

**Step 4: Check logs for variance tuning**

Run: `adb logcat -s PracticeModeVM`

Look for lines like:
```
Total motion variance: 0.0234 (threshold: 0.5)   ← no movement
Total motion variance: 12.345 (threshold: 0.5)    ← actual writing
```

If the threshold needs adjustment, change `MOTION_VARIANCE_THRESHOLD` in the companion object and re-deploy.
