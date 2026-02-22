# White Question Mark for Non-Air-Written Operations — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Show a white `?` as the predicted letter when no air-writing motion is detected in Learn and Tutorial modes, replacing the "Oops!" mascot screen. Practice mode unchanged.

**Architecture:** Eliminate the `NO_MOVEMENT` UI branch in Learn/Tutorial modes and route no-motion results through the existing `SHOWING_PREDICTION` pipeline with `"?"` as the prediction. The phone already handles `"?"` as a wrong answer.

**Tech Stack:** Kotlin, Jetpack Compose (Wear OS), ViewModel, StateFlow

---

### Task 1: LearnModeViewModel — Route no-motion through SHOWING_PREDICTION

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt:199-221`

**Step 1: Replace the NO_MOVEMENT branch with SHOWING_PREDICTION**

At lines 199-221, the current code transitions to `State.NO_MOVEMENT`, waits 3 seconds, and returns to idle. Replace the entire block with a transition to `State.SHOWING_PREDICTION` with `prediction = "?"`, then let the normal prediction display + phone feedback timeout handle the rest.

Replace this (lines 199-221):
```kotlin
// Check for significant wrist movement
if (!hasSignificantMotion(samples)) {
    Log.w(TAG, "No significant wrist movement detected")
    _uiState.update {
        it.copy(
            state = State.NO_MOVEMENT,
            statusMessage = "Oops! You did not air write!",
            recordingProgress = 0f
        )
    }

    delay(NO_MOVEMENT_DISPLAY_SECONDS * 1000L)
    if (isActive && _uiState.value.state == State.NO_MOVEMENT) {
        _uiState.update {
            it.copy(
                state = State.IDLE,
                statusMessage = "Tap to guess",
                prediction = null
            )
        }
    }
    return@launch
}
```

With this:
```kotlin
// Check for significant wrist movement
if (!hasSignificantMotion(samples)) {
    Log.w(TAG, "No significant wrist movement detected — showing '?' as prediction")
    _uiState.update {
        it.copy(
            state = State.SHOWING_PREDICTION,
            prediction = "?",
            confidence = 0f,
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
            it.copy(state = State.IDLE, statusMessage = "Tap to guess")
        }
    }
    return@launch
}
```

**Step 2: Verify the build compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: LearnModeScreen — Update TTS and remove NO_MOVEMENT UI for Fill in the Blank

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt:479-508, 540-550`

**Step 1: Update the SHOWING_PREDICTION LaunchedEffect to handle "?" prediction**

At lines 479-491, modify the existing `LaunchedEffect` that handles SHOWING_PREDICTION to check if the prediction is `"?"`. If so, speak "Oops, you did not air write!" instead of speaking the letter. The `sendLetterInput` call remains the same (it already sends prediction, which will be `"?"`).

Replace this (lines 479-491):
```kotlin
    // Speak the prediction and send to phone when prediction is shown
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                ttsManager.speakLetter(uiState.prediction!!)
            }

            // Send letter input to phone immediately for validation
            android.util.Log.d("LearnModeScreen", "📤 Sending letter input to phone: ${uiState.prediction} at index ${wordData.maskedIndex}")
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, wordData.maskedIndex)
        }
    }
```

With this:
```kotlin
    // Speak the prediction and send to phone when prediction is shown
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                if (uiState.prediction == "?") {
                    ttsManager.speak("Oops, you did not air write!")
                } else {
                    ttsManager.speakLetter(uiState.prediction!!)
                }
            }

            // Send letter input to phone immediately for validation
            android.util.Log.d("LearnModeScreen", "📤 Sending letter input to phone: ${uiState.prediction} at index ${wordData.maskedIndex}")
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, wordData.maskedIndex)
        }
    }
```

**Step 2: Remove the NO_MOVEMENT LaunchedEffect**

Delete lines 501-508 entirely (the `LaunchedEffect` that speaks "Oops" and sends "?" on NO_MOVEMENT). This is now handled by the modified SHOWING_PREDICTION LaunchedEffect above.

```kotlin
    // Speak "oops" and notify phone when entering NO_MOVEMENT state
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.NO_MOVEMENT) {
            ttsManager.speak("Oops, you did not air write!")
            // Send wrong letter to phone so it exits "is air writing" and plays wrong sound
            phoneCommunicationManager.sendLetterInput("?", wordData.maskedIndex)
        }
    }
```

**Step 3: Remove NO_MOVEMENT from the when() branch**

At line 548, remove the `NO_MOVEMENT` branch from the `when` block. The `NO_MOVEMENT` state will never be entered from Learn mode anymore, but since it still exists in the enum (Practice mode uses it), add a no-op fallback or remove the line.

Replace line 548:
```kotlin
                    LearnModeViewModel.State.NO_MOVEMENT -> LearnNoMovementContent(viewModel)
```
With:
```kotlin
                    LearnModeViewModel.State.NO_MOVEMENT -> { /* Unused in Learn mode */ }
```

---

### Task 3: LearnModeScreen — Update TTS and remove NO_MOVEMENT UI for Write the Word

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt:823-850, 893`

**Step 1: Update the SHOWING_PREDICTION LaunchedEffect**

At lines 823-835, modify to check for `"?"` prediction:

Replace this (lines 823-835):
```kotlin
    // Send letter input to phone when prediction is made
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                ttsManager.speakLetter(uiState.prediction!!)
            }

            // Send letter input to phone for validation (preserve exact case)
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, currentLetterIndex)
        }
    }
```

With this:
```kotlin
    // Send letter input to phone when prediction is made
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                if (uiState.prediction == "?") {
                    ttsManager.speak("Oops, you did not air write!")
                } else {
                    ttsManager.speakLetter(uiState.prediction!!)
                }
            }

            // Send letter input to phone for validation (preserve exact case)
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, currentLetterIndex)
        }
    }
```

**Step 2: Remove the NO_MOVEMENT LaunchedEffect**

Delete lines 844-850 entirely.

**Step 3: Remove NO_MOVEMENT from the when() branch**

Replace line 893:
```kotlin
                    LearnModeViewModel.State.NO_MOVEMENT -> LearnNoMovementContent(viewModel)
```
With:
```kotlin
                    LearnModeViewModel.State.NO_MOVEMENT -> { /* Unused in Learn mode */ }
```

---

### Task 4: LearnModeScreen — Update TTS and remove NO_MOVEMENT UI for Name the Picture

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt:1248-1275, 1318`

**Step 1: Update the SHOWING_PREDICTION LaunchedEffect**

At lines 1248-1260, modify to check for `"?"` prediction:

Replace this (lines 1248-1260):
```kotlin
    // Send letter input to phone when prediction is made
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                ttsManager.speakLetter(uiState.prediction!!)
            }

            // Send letter input to phone for validation (preserve exact case)
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, currentLetterIndex)
        }
    }
```

With this:
```kotlin
    // Send letter input to phone when prediction is made
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                if (uiState.prediction == "?") {
                    ttsManager.speak("Oops, you did not air write!")
                } else {
                    ttsManager.speakLetter(uiState.prediction!!)
                }
            }

            // Send letter input to phone for validation (preserve exact case)
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, currentLetterIndex)
        }
    }
```

**Step 2: Remove the NO_MOVEMENT LaunchedEffect**

Delete lines 1269-1275 entirely.

**Step 3: Remove NO_MOVEMENT from the when() branch**

Replace line 1318:
```kotlin
                    LearnModeViewModel.State.NO_MOVEMENT -> LearnNoMovementContent(viewModel)
```
With:
```kotlin
                    LearnModeViewModel.State.NO_MOVEMENT -> { /* Unused in Learn mode */ }
```

**Step 4: Verify the build compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: TutorialModeViewModel — Add motion detection and "?" prediction

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeViewModel.kt:33-43, 170-189`

**Step 1: Add motion detection constants and import**

At the top of the companion object (line 37, after `PROGRESS_UPDATE_INTERVAL_MS`), add the motion detection thresholds:

After line 37, insert:
```kotlin
        private const val GYRO_VARIANCE_THRESHOLD = 0.3f
        private const val GYRO_RANGE_THRESHOLD = 1.0f
```

Add the SensorSample import at the top of the file (after existing imports around line 9):
```kotlin
import com.example.kusho.sensors.SensorSample
```

**Step 2: Add hasSignificantMotion() method**

Add this method at the bottom of the class (before the closing `}` of the class, around line 318):

```kotlin
    private fun hasSignificantMotion(samples: List<SensorSample>): Boolean {
        if (samples.size < 2) return false

        val n = samples.size.toFloat()

        val gyroMagnitudes = samples.map {
            kotlin.math.sqrt((it.gx * it.gx + it.gy * it.gy + it.gz * it.gz).toDouble()).toFloat()
        }
        val gyroMean = gyroMagnitudes.sum() / n
        val gyroVariance = gyroMagnitudes.sumOf { ((it - gyroMean) * (it - gyroMean)).toDouble() }.toFloat() / n
        val gyroRange = gyroMagnitudes.max() - gyroMagnitudes.min()

        Log.d(TAG, "Gyro: variance=%.4f (need>=%.4f), range=%.4f (need>=%.4f)".format(
            gyroVariance, GYRO_VARIANCE_THRESHOLD, gyroRange, GYRO_RANGE_THRESHOLD))

        val result = gyroVariance >= GYRO_VARIANCE_THRESHOLD && gyroRange >= GYRO_RANGE_THRESHOLD
        Log.i(TAG, "Motion detected: $result (variance=${gyroVariance >= GYRO_VARIANCE_THRESHOLD}, range=${gyroRange >= GYRO_RANGE_THRESHOLD})")
        return result
    }
```

**Step 3: Add motion check after samples are collected**

At line 173 (after `val samples = sensorManager.getCollectedSamples()` and the log), insert the motion check BEFORE the `minSamples` check. Insert after line 173:

```kotlin
                // Check for significant wrist movement
                if (!hasSignificantMotion(samples)) {
                    Log.w(TAG, "No significant wrist movement detected — showing '?' as prediction")
                    _uiState.update {
                        it.copy(
                            state = State.SHOWING_PREDICTION,
                            prediction = "?",
                            isCorrect = false,
                            statusMessage = "?",
                            errorMessage = null,
                            recordingProgress = 0f
                        )
                    }
                    onGestureResult(false, "?")
                    return@launch
                }
```

**Step 4: Verify the build compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: TutorialModeScreen — Add TTS for "?" prediction

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeScreen.kt:452-461`

**Step 1: Update the SHOWING_PREDICTION LaunchedEffect**

At lines 452-461, modify the existing TTS LaunchedEffect to handle the `"?"` prediction case:

Replace this (lines 452-461):
```kotlin
    // Speak the prediction when we enter SHOWING_PREDICTION state
    LaunchedEffect(uiState.state, uiState.prediction, uiState.isCorrect) {
        if (uiState.state == TutorialModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            if (uiState.isCorrect) {
                ttsManager.speakLetter(uiState.prediction!!)
            } else {
                ttsManager.speakTryAgain()
            }
        }
    }
```

With this:
```kotlin
    // Speak the prediction when we enter SHOWING_PREDICTION state
    LaunchedEffect(uiState.state, uiState.prediction, uiState.isCorrect) {
        if (uiState.state == TutorialModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            if (uiState.prediction == "?") {
                ttsManager.speak("Oops, you did not air write!")
            } else if (uiState.isCorrect) {
                ttsManager.speakLetter(uiState.prediction!!)
            } else {
                ttsManager.speakTryAgain()
            }
        }
    }
```

**Step 2: Verify the full build compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 7: Manual verification on device

**Verification checklist:**

1. **Learn Mode — Fill in the Blank:** Start a fill-in-the-blank session. When prompted to write, hold the watch still (do not move wrist). Verify: white `?` appears at 80sp, TTS says "Oops, you did not air write!", phone receives `?`, feedback (wrong) is shown.

2. **Learn Mode — Write the Word:** Start a write-the-word session. Hold still on a letter. Verify same as above.

3. **Learn Mode — Name the Picture:** Start a name-the-picture session. Hold still. Verify same as above.

4. **Tutorial Mode:** Start a tutorial session. Hold still during recording. Verify: white `?` appears, TTS says "Oops, you did not air write!", teacher can proceed with feedback.

5. **Practice Mode:** Start a practice session. Hold still during recording. Verify: "Oops!" mascot image still appears (unchanged behavior).

6. **Normal air-writing (regression):** In all modes, actually air-write a letter. Verify: predicted letter appears as before, feedback works normally.
