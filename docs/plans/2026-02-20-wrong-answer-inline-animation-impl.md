# Wrong Answer Inline Animation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the ProgressCheckDialog popup on incorrect answers with an inline animation where the wrong letter appears in the active blank slot, turns red, shakes, plays wrong audio, fades out, and retries.

**Architecture:** Single coroutine sequence in a `LaunchedEffect` orchestrates the full animation timeline using Compose `Animatable` APIs. The three display composables (`WriteTheWordDisplay`, `FillInTheBlankDisplay`, `NameThePictureDisplay`) each gain parameters for the wrong-letter animation state. Audio plays concurrently via `MediaPlayer`.

**Tech Stack:** Jetpack Compose animations (`Animatable`, `tween`, `EaseOut`), `MediaPlayer` for audio, Kotlin coroutines.

---

### Task 1: Add animation state variables

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1-63` (imports)
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:147-155` (state block)

**Step 1: Add new imports**

Add these imports near the existing animation/compose imports (after line 8, among the other imports):

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
```

Note: `Modifier.offset` is already available via the existing `androidx.compose.foundation.layout.*` import. `alpha` needs `androidx.compose.ui.draw.alpha`. Check that `Animatable`, `tween`, and `EaseOut` are not already imported before adding.

**Step 2: Add state variables**

Add these after the existing `pendingCorrectAction` state variable (after line 155):

```kotlin
    // State for wrong letter inline animation (replaces ProgressCheckDialog for incorrect answers)
    var wrongLetterText by remember(sessionKey) { mutableStateOf("") }
    var wrongLetterAnimationActive by remember(sessionKey) { mutableStateOf(false) }
    val wrongLetterShakeOffset = remember(sessionKey) { Animatable(0f) }
    val wrongLetterAlpha = remember(sessionKey) { Animatable(1f) }
```

**Step 3: Verify**

Build the project to make sure there are no compilation errors from the new state variables and imports.

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add the animation coroutine sequence and audio

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:546-587` (between the skip listener LaunchedEffect and the ProgressCheckDialog block)

**Step 1: Add the wrong letter animation LaunchedEffect**

Insert this block after the skip commands `LaunchedEffect` (after line 585, before the `// Progress Check Dialog` comment on line 587):

```kotlin
    // Wrong letter inline animation sequence (~3 seconds)
    LaunchedEffect(wrongLetterAnimationActive) {
        if (wrongLetterAnimationActive) {
            // Reset animation values
            wrongLetterShakeOffset.snapTo(0f)
            wrongLetterAlpha.snapTo(1f)

            // Play wrong audio concurrently (same pattern as ProgressCheckDialog)
            val wrongAffirmatives = listOf(
                R.raw.wrong_youre_learning_so_keep_going,
                R.raw.wrong_think_carefully_and_try_one_more_time,
                R.raw.wrong_its_okay_to_make_mistakes_keep_trying,
                R.raw.wrong_believe_in_yourself_and_try_again,
                R.raw.wrong_stay_patient_and_keep_working,
                R.raw.wrong_give_it_another_try_and_do_your_best,
                R.raw.wrong_try_again_with_confidence,
                R.raw.wrong_youre_almost_there_keep_going,
                R.raw.wrong_keep_practicing_and_youll_get_it,
                R.raw.wrong_take_your_time_and_try_once_more,
                R.raw.wrong_you_can_do_better_on_the_next_try,
                R.raw.wrong_dont_worry_try_again
            )

            val mediaPlayer = MediaPlayer()
            try {
                fun playAudio(resId: Int, onComplete: (() -> Unit)? = null) {
                    try {
                        mediaPlayer.reset()
                        val afd = context.resources.openRawResourceFd(resId)
                        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        mediaPlayer.prepare()
                        mediaPlayer.setOnCompletionListener {
                            onComplete?.invoke()
                        }
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onComplete?.invoke()
                    }
                }

                val randomAffirmative = wrongAffirmatives.random()
                playAudio(R.raw.wrong) {
                    playAudio(randomAffirmative)
                }
            } catch (e: Exception) {
                Log.e("LearnModeSession", "Error playing wrong audio", e)
            }

            // Phase 1: Hold - letter visible in red for 500ms
            kotlinx.coroutines.delay(500L)

            // Phase 2: Shake - 5 oscillation cycles over 1500ms with decaying amplitude
            val shakeDurations = listOf(300, 300, 300, 300, 300) // 5 cycles = 1500ms
            val shakeAmplitudes = listOf(12f, 10f, 8f, 5f, 3f) // Decaying amplitude

            for (i in shakeDurations.indices) {
                wrongLetterShakeOffset.animateTo(
                    targetValue = shakeAmplitudes[i],
                    animationSpec = tween(durationMillis = shakeDurations[i] / 2)
                )
                wrongLetterShakeOffset.animateTo(
                    targetValue = -shakeAmplitudes[i],
                    animationSpec = tween(durationMillis = shakeDurations[i] / 2)
                )
            }
            wrongLetterShakeOffset.snapTo(0f)

            // Phase 3: Fade out over 1000ms
            wrongLetterAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = EaseOut)
            )

            // Reset and execute pending action
            wrongLetterAnimationActive = false
            wrongLetterText = ""
            wrongLetterAlpha.snapTo(1f)

            // Notify watch and execute retry logic (same as dialog dismiss)
            watchConnectionManager.notifyLearnModeFeedbackDismissed()
            pendingCorrectAction?.invoke()
            pendingCorrectAction = null

            // Release media player
            try {
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }
```

**Step 2: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Update the three display composables to accept animation parameters

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:921-961` (WriteTheWordDisplay)
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:968-1008` (FillInTheBlankDisplay)
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1015-1052` (NameThePictureDisplay)

**Step 1: Update WriteTheWordDisplay signature and rendering**

Change the function signature from:

```kotlin
@Composable
private fun WriteTheWordDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int,
    hasImage: Boolean
) {
```

To:

```kotlin
@Composable
private fun WriteTheWordDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int,
    hasImage: Boolean,
    wrongLetterText: String = "",
    wrongLetterAnimationActive: Boolean = false,
    wrongLetterShakeOffset: Float = 0f,
    wrongLetterAlpha: Float = 1f
) {
```

Then update the letter rendering inside the `word.forEachIndexed` block. Replace the `Text` composable (lines 939-947) with:

```kotlin
                val isCurrentAndWrong = index == currentIndex && wrongLetterAnimationActive
                Text(
                    text = if (isCurrentAndWrong) wrongLetterText else letter.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentAndWrong -> Color(0xFFFF6B6B) // Wrong letter - red
                        index in completedIndices -> CompletedLetterColor  // Completed - purple
                        else -> PendingLetterColor  // Pending letters - gray
                    },
                    modifier = if (isCurrentAndWrong) {
                        Modifier
                            .offset(x = wrongLetterShakeOffset.dp)
                            .alpha(wrongLetterAlpha)
                    } else {
                        Modifier
                    }
                )
```

**Step 2: Update FillInTheBlankDisplay signature and rendering**

Change the function signature from:

```kotlin
@Composable
private fun FillInTheBlankDisplay(
    word: String,
    maskedIndex: Int,
    isCorrect: Boolean,
    hasImage: Boolean
) {
```

To:

```kotlin
@Composable
private fun FillInTheBlankDisplay(
    word: String,
    maskedIndex: Int,
    isCorrect: Boolean,
    hasImage: Boolean,
    wrongLetterText: String = "",
    wrongLetterAnimationActive: Boolean = false,
    wrongLetterShakeOffset: Float = 0f,
    wrongLetterAlpha: Float = 1f
) {
```

Then replace the `Text` composable (lines 986-994) with:

```kotlin
                val isMaskedAndWrong = index == maskedIndex && !isCorrect && wrongLetterAnimationActive
                Text(
                    text = when {
                        isMaskedAndWrong -> wrongLetterText
                        index == maskedIndex && !isCorrect -> " "
                        else -> letter.toString()
                    },
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isMaskedAndWrong -> Color(0xFFFF6B6B) // Wrong letter - red
                        index == maskedIndex && isCorrect -> CompletedLetterColor  // Revealed - purple
                        else -> Color.Black  // Other letters - black
                    },
                    modifier = if (isMaskedAndWrong) {
                        Modifier
                            .offset(x = wrongLetterShakeOffset.dp)
                            .alpha(wrongLetterAlpha)
                    } else {
                        Modifier
                    }
                )
```

**Step 3: Update NameThePictureDisplay signature and rendering**

Change the function signature from:

```kotlin
@Composable
private fun NameThePictureDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int,
    hasImage: Boolean
) {
```

To:

```kotlin
@Composable
private fun NameThePictureDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int,
    hasImage: Boolean,
    wrongLetterText: String = "",
    wrongLetterAnimationActive: Boolean = false,
    wrongLetterShakeOffset: Float = 0f,
    wrongLetterAlpha: Float = 1f
) {
```

Then replace the `Text` composable (lines 1033-1038) with:

```kotlin
                val isCurrentAndWrong = index == currentIndex && index !in completedIndices && wrongLetterAnimationActive
                Text(
                    text = when {
                        isCurrentAndWrong -> wrongLetterText
                        index in completedIndices -> letter.toString()
                        else -> " "
                    },
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentAndWrong -> Color(0xFFFF6B6B) // Wrong letter - red
                        index in completedIndices -> CompletedLetterColor
                        else -> Color.Transparent
                    },
                    modifier = if (isCurrentAndWrong) {
                        Modifier
                            .offset(x = wrongLetterShakeOffset.dp)
                            .alpha(wrongLetterAlpha)
                    } else {
                        Modifier
                    }
                )
```

**Step 4: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Pass animation state to display composables at call sites

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:870-907` (the `when` block that renders displays)

**Step 1: Update WriteTheWordDisplay call site**

Change (lines 873-878):

```kotlin
                        WriteTheWordDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists
                        )
```

To:

```kotlin
                        WriteTheWordDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
```

**Step 2: Update FillInTheBlankDisplay call site**

Change (lines 882-887):

```kotlin
                        FillInTheBlankDisplay(
                            word = currentWord.word,
                            maskedIndex = currentWord.selectedLetterIndex,
                            isCorrect = fillInBlankCorrect,
                            hasImage = imageExists
                        )
```

To:

```kotlin
                        FillInTheBlankDisplay(
                            word = currentWord.word,
                            maskedIndex = currentWord.selectedLetterIndex,
                            isCorrect = fillInBlankCorrect,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
```

**Step 3: Update NameThePictureDisplay call site**

Change (lines 891-896):

```kotlin
                        NameThePictureDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists
                        )
```

To:

```kotlin
                        NameThePictureDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
```

**Step 4: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Redirect incorrect answers to inline animation instead of dialog

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:432-541` (the letterInputEvent handler)

**Step 1: Add input blocking guard**

At the top of the event processing block, right after the debounce check (line 427), add a guard to block input during animation. Change:

```kotlin
            if (event.timestamp > 0L && event.timestamp > lastEventTime) {
                lastEventTime = event.timestamp
                val currentWord = words.getOrNull(currentWordIndex)
```

To:

```kotlin
            if (event.timestamp > 0L && event.timestamp > lastEventTime) {
                lastEventTime = event.timestamp

                // Block input while wrong letter animation is playing
                if (wrongLetterAnimationActive) return@collect

                val currentWord = words.getOrNull(currentWordIndex)
```

**Step 2: Update Fill in the Blank incorrect branch**

In the "Fill in the Blank" section, the incorrect answer currently does (lines 443-449):

```kotlin
                            isCorrectGesture = isCorrect

                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Fill in the Blank")
                            showProgressCheckDialog = true
```

Replace with logic that only shows dialog for correct answers:

```kotlin
                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            if (isCorrect) {
                                isCorrectGesture = true
                                android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Fill in the Blank (correct)")
                                showProgressCheckDialog = true
                            } else {
                                android.util.Log.d("LearnModeSession", "🎭 Starting wrong letter animation for Fill in the Blank")
                                wrongLetterText = event.letter.toString()
                                wrongLetterAnimationActive = true
                            }
```

Note: Remove the line `predictedLetter = event.letter.toString()` from before the dialog logic since it's only needed for the dialog (which no longer shows for incorrect). Keep `targetLetter` and `targetCase` assignments as they are (they're harmless and may be needed for other purposes).

**Step 3: Update Write the Word / Name the Picture incorrect branch**

Same pattern. The current code (lines 494-501):

```kotlin
                            isCorrectGesture = isCorrect

                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Write the Word/Name the Picture")
                            showProgressCheckDialog = true
```

Replace with:

```kotlin
                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            if (isCorrect) {
                                isCorrectGesture = true
                                predictedLetter = event.letter.toString()
                                android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Write the Word/Name the Picture (correct)")
                                showProgressCheckDialog = true
                            } else {
                                android.util.Log.d("LearnModeSession", "🎭 Starting wrong letter animation for Write the Word/Name the Picture")
                                wrongLetterText = event.letter.toString()
                                wrongLetterAnimationActive = true
                            }
```

**Step 4: Update watch feedback dismissal handler for animation**

In the watch feedback dismissal `LaunchedEffect` (lines 548-563), update to also handle animation dismissal. Change:

```kotlin
    LaunchedEffect(Unit) {
        var lastDismissTime = 0L
        watchConnectionManager.learnModeFeedbackDismissed.collect { timestamp ->
            if (timestamp > lastDismissTime && timestamp > 0L) {
                lastDismissTime = timestamp
                if (showProgressCheckDialog) {
                    android.util.Log.d("LearnModeSession", "👆 Watch dismissed feedback - dismissing mobile dialog")
                    showProgressCheckDialog = false
                    // Execute pending action
                    pendingCorrectAction?.invoke()
                    pendingCorrectAction = null
                }
            }
        }
    }
```

To:

```kotlin
    LaunchedEffect(Unit) {
        var lastDismissTime = 0L
        watchConnectionManager.learnModeFeedbackDismissed.collect { timestamp ->
            if (timestamp > lastDismissTime && timestamp > 0L) {
                lastDismissTime = timestamp
                if (showProgressCheckDialog) {
                    android.util.Log.d("LearnModeSession", "👆 Watch dismissed feedback - dismissing mobile dialog")
                    showProgressCheckDialog = false
                    // Execute pending action
                    pendingCorrectAction?.invoke()
                    pendingCorrectAction = null
                } else if (wrongLetterAnimationActive) {
                    android.util.Log.d("LearnModeSession", "👆 Watch dismissed feedback - cancelling wrong letter animation")
                    // Allow watch to dismiss the animation early
                    wrongLetterAnimationActive = false
                    wrongLetterText = ""
                    pendingCorrectAction?.invoke()
                    pendingCorrectAction = null
                }
            }
        }
    }
```

**Step 5: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: Final build verification

**Files:**
- All changes in: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Full build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Review the complete change**

Read the modified file end-to-end to verify:
- New imports are present
- State variables are declared with `remember(sessionKey)`
- Animation `LaunchedEffect` is in the right position
- All three display composables accept and use animation parameters
- All three call sites pass animation state
- Both incorrect branches (Fill in the Blank + Write the Word/Name the Picture) trigger animation instead of dialog
- Input is blocked during animation
- Watch dismissal handles both dialog and animation cases
- `ProgressCheckDialog` still shows for correct answers
