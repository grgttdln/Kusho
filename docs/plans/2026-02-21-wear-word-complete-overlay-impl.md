# Wear Word-Complete Overlay Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Show correct/wrong feedback as colored letters per-letter, and only show the correct image + TTS affirmation when the entire word is complete, for Write the Word and Name the Picture modes on the wear app.

**Architecture:** Dual-path feedback branching on `writeTheWordState.isWordComplete` inside `WriteTheWordMainContent` and `NameThePictureMainContent`. Auto-dismiss per-letter feedback via a `LaunchedEffect` in `LearnModeScreen`. Two new composables: `PerLetterFeedbackContent` and `WordCompleteFeedbackContent`.

**Tech Stack:** Kotlin, Jetpack Compose (Wear), TextToSpeechManager (existing)

---

### Task 1: Add `PerLetterFeedbackContent` composable

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (insert before `LearnModeFeedbackContent` at line 872)

**Step 1: Add the composable**

Insert this new composable before `LearnModeFeedbackContent` (before line 872):

```kotlin
/**
 * Per-letter feedback for Write the Word / Name the Picture modes.
 * Shows the predicted letter in green (correct) or red (wrong), large and centered.
 * Auto-dismissed by the parent after ~1 second.
 */
@Composable
private fun PerLetterFeedbackContent(
    prediction: String?,
    isCorrect: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = prediction ?: "",
            color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.display1
        )
    }
}
```

**Step 2: Commit**

```
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "feat(wear): add PerLetterFeedbackContent composable"
```

---

### Task 2: Add `WordCompleteFeedbackContent` composable

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (insert right after `PerLetterFeedbackContent` from Task 1)

**Step 1: Add the composable**

Insert right after `PerLetterFeedbackContent`:

```kotlin
/**
 * Word-complete feedback for Write the Word / Name the Picture modes.
 * Shows dis_watch_correct image + TTS affirmation, then transitions to "Waiting for teacher...".
 * Stays visible until teacher dismisses from phone.
 */
@Composable
private fun WordCompleteFeedbackContent(
    ttsManager: TextToSpeechManager
) {
    var showWaiting by remember { mutableStateOf(false) }

    // Play TTS affirmation and transition to waiting after 2 seconds
    LaunchedEffect(Unit) {
        ttsManager.speak("Great job!")
        kotlinx.coroutines.delay(2000L)
        showWaiting = true
    }

    if (showWaiting) {
        // Waiting for teacher state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Waiting for teacher...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Image(
                    painter = painterResource(id = R.drawable.dis_watch_wait),
                    contentDescription = "Waiting for teacher",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    } else {
        // Correct celebration image
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.dis_watch_correct),
                contentDescription = "Word complete - correct",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
```

**Step 2: Check `TextToSpeechManager` for a `speak` method**

Read `wear/src/main/java/com/example/kusho/speech/TextToSpeechManager.kt` and confirm it has a `speak(text: String)` method or similar. If it only has `speakLetter(letter: String)`, use that with "Great job!" or add a simple `speak` method. Adapt the call accordingly.

**Step 3: Commit**

```
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "feat(wear): add WordCompleteFeedbackContent composable"
```

---

### Task 3: Add auto-dismiss `LaunchedEffect` in `LearnModeScreen`

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (lines 119-148)

**Step 1: Add the auto-dismiss effect**

Insert after the "Reset feedback when word changes" block (after line 119) and before the handshake block (line 121):

```kotlin
    // Auto-dismiss per-letter feedback for Write the Word / Name the Picture
    // Keyed on both values so it cancels when isWordComplete flips to true
    val isWriteTheWordOrNameThePicture = wordData.configurationType == "Write the Word" ||
            wordData.configurationType == "Name the Picture"
    LaunchedEffect(showingFeedback, writeTheWordState.isWordComplete) {
        if (showingFeedback && !writeTheWordState.isWordComplete && isWriteTheWordOrNameThePicture) {
            kotlinx.coroutines.delay(1000L)
            showingFeedback = false
        }
    }
```

**Step 2: Commit**

```
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "feat(wear): auto-dismiss per-letter feedback after 1s"
```

---

### Task 4: Update `WriteTheWordMainContent` to branch on `isWordComplete`

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (lines 776-793 in `WriteTheWordMainContent`)

**Step 1: Replace the feedback branch**

Replace the current feedback block at lines 776-793:

```kotlin
            // Show feedback image if we're in feedback state
            if (showingFeedback) {
                LearnModeFeedbackContent(
                    isCorrect = feedbackIsCorrect
                )
            } else {
                when (uiState.state) {
                    LearnModeViewModel.State.IDLE -> WriteTheWordIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                    LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                    LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                    LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                    LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                }
            }
```

With:

```kotlin
            // Show feedback based on word-complete vs per-letter
            if (showingFeedback) {
                if (writeTheWordState.isWordComplete && feedbackIsCorrect) {
                    WordCompleteFeedbackContent(ttsManager = ttsManager)
                } else {
                    PerLetterFeedbackContent(
                        prediction = uiState.prediction,
                        isCorrect = feedbackIsCorrect
                    )
                }
            } else {
                when (uiState.state) {
                    LearnModeViewModel.State.IDLE -> WriteTheWordIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                    LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                    LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                    LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                    LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                }
            }
```

Note: `ttsManager` is available because `WriteTheWordMainContent` is called from `WriteTheWordContent` which initializes it. However, `WriteTheWordMainContent` does NOT currently receive `ttsManager` as a parameter — it creates its own `viewModel`. The `ttsManager` is initialized in `WriteTheWordContent` at line 641 and passed to `WriteTheWordMainContent` at line 694... actually it is NOT passed. Check the call site at line 688-697:

```kotlin
WriteTheWordMainContent(
    wordData = wordData,
    writeTheWordState = writeTheWordState,
    sensorManager = sensorManager!!,
    classifierResult = classifierResult!!,
    ttsManager = ttsManager,       // <-- it IS passed here
    phoneCommunicationManager = phoneCommunicationManager,
    showingFeedback = showingFeedback,
    feedbackIsCorrect = feedbackIsCorrect
)
```

Confirmed: `ttsManager` is already a parameter of `WriteTheWordMainContent` (line 707). We can use it directly.

**Step 2: Commit**

```
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "feat(wear): branch WriteTheWord feedback on isWordComplete"
```

---

### Task 5: Update `NameThePictureMainContent` to branch on `isWordComplete`

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (lines 1090-1107 in `NameThePictureMainContent`)

**Step 1: Replace the feedback branch**

Replace the current feedback block at lines 1090-1107:

```kotlin
            // Show feedback image if we're in feedback state
            if (showingFeedback) {
                LearnModeFeedbackContent(
                    isCorrect = feedbackIsCorrect
                )
            } else {
                when (uiState.state) {
                    LearnModeViewModel.State.IDLE -> NameThePictureIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                    LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                    LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                    LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                    LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                }
            }
```

With:

```kotlin
            // Show feedback based on word-complete vs per-letter
            if (showingFeedback) {
                if (writeTheWordState.isWordComplete && feedbackIsCorrect) {
                    WordCompleteFeedbackContent(ttsManager = ttsManager)
                } else {
                    PerLetterFeedbackContent(
                        prediction = uiState.prediction,
                        isCorrect = feedbackIsCorrect
                    )
                }
            } else {
                when (uiState.state) {
                    LearnModeViewModel.State.IDLE -> NameThePictureIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                    LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                    LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                    LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                    LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                }
            }
```

`ttsManager` is confirmed available as a parameter of `NameThePictureMainContent` (line 1021), passed from `NameThePictureContent` at line 1007.

**Step 2: Commit**

```
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "feat(wear): branch NameThePicture feedback on isWordComplete"
```

---

### Task 6: Verify build compiles

**Step 1: Build the wear module**

```bash
cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. If there are compile errors, fix them (likely candidates: missing import, TTS method name mismatch).

**Step 2: Commit any fixes**

```
git add wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt
git commit -m "fix(wear): resolve build issues for word-complete overlay"
```
