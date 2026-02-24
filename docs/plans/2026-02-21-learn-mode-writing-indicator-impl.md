# Learn Mode Writing Indicator Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a "student is air writing..." indicator to Learn Mode, copying the existing Tutorial Mode pattern.

**Architecture:** Watch sends `/learn_mode_gesture_recording` message when recording starts. Phone receives it via StateFlow, shows pulsing text, and locks UI. Resets when letter input event arrives.

**Tech Stack:** Kotlin, Jetpack Compose, Wear OS MessageClient API, StateFlow

---

### Task 1: Add message path constant and send method on watch

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt:83` (add constant after line 83)
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt:510` (add method after `sendTutorialModeGestureRecording`)

**Step 1: Add the message path constant**

In `PhoneCommunicationManager.kt`, add after line 83 (after `MESSAGE_PATH_LEARN_MODE_WATCH_READY`):

```kotlin
        private const val MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING = "/learn_mode_gesture_recording"
```

**Step 2: Add the send method**

After the `sendTutorialModeGestureRecording()` method (around line 512), add:

```kotlin
    /**
     * Notify phone that watch has started recording a gesture in Learn Mode (student is writing)
     */
    suspend fun sendLearnModeGestureRecording() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING,
                        ByteArray(0)
                    ).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
```

**Step 3: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add onRecordingStarted callback to LearnModeViewModel

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt:23-26` (constructor)
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt:148` (recording start)
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt:304-315` (factory)

**Step 1: Add callback parameter to ViewModel constructor**

Change line 23-26 from:

```kotlin
class LearnModeViewModel(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult
) : ViewModel() {
```

To:

```kotlin
class LearnModeViewModel(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult,
    private val onRecordingStarted: () -> Unit = {}
) : ViewModel() {
```

**Step 2: Call onRecordingStarted when recording begins**

In the `startRecording()` method, add `onRecordingStarted()` after the state update to RECORDING (line 155) and before `sensorManager.startRecording()` (line 157). Insert between them:

```kotlin
                // Notify phone that recording has started (student is writing)
                onRecordingStarted()
```

So lines 149-158 become:

```kotlin
                _uiState.update {
                    it.copy(
                        state = State.RECORDING,
                        statusMessage = "Write now!",
                        recordingProgress = 0f
                    )
                }

                // Notify phone that recording has started (student is writing)
                onRecordingStarted()

                sensorManager.startRecording()
```

**Step 3: Update the factory to pass the callback**

Change `LearnModeViewModelFactory` (lines 304-315) from:

```kotlin
class LearnModeViewModelFactory(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LearnModeViewModel::class.java)) {
            return LearnModeViewModel(sensorManager, classifierResult) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

To:

```kotlin
class LearnModeViewModelFactory(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult,
    private val onRecordingStarted: () -> Unit = {}
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LearnModeViewModel::class.java)) {
            return LearnModeViewModel(sensorManager, classifierResult, onRecordingStarted) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

**Step 4: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (default `{}` means all existing call sites still work)

---

### Task 3: Wire onRecordingStarted callback in LearnModeScreen

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`
  - Lines ~506-508 (FillInTheBlankMainContent viewModel creation)
  - Lines ~809-810 (WriteTheWordMainContent viewModel creation)
  - Lines ~1094-1095 (NameThePictureMainContent viewModel creation)

**Step 1: Update all three viewModel factory calls**

In all three content composables (`FillInTheBlankMainContent`, `WriteTheWordMainContent`, `NameThePictureMainContent`), the ViewModel is created like:

```kotlin
    val viewModel: LearnModeViewModel = viewModel(
        factory = LearnModeViewModelFactory(sensorManager, classifierResult)
    )
```

Change each to:

```kotlin
    val scope = rememberCoroutineScope()
    val viewModel: LearnModeViewModel = viewModel(
        factory = LearnModeViewModelFactory(
            sensorManager = sensorManager,
            classifierResult = classifierResult,
            onRecordingStarted = {
                scope.launch {
                    phoneCommunicationManager.sendLearnModeGestureRecording()
                }
            }
        )
    )
```

Note: Each of these composables already receives `phoneCommunicationManager: PhoneCommunicationManager` as a parameter. You'll need to add the `rememberCoroutineScope` import if not already present:

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
```

And `kotlinx.coroutines.launch` if not already imported.

**Step 2: Verify compilation**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Add StateFlow and message handler on phone side

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:104` (add StateFlow after learnModeWatchReady)
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:151` (add constant after WATCH_READY)
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:779` (add message handler after LEARN_MODE_WATCH_READY handler)

**Step 1: Add the StateFlow**

After line 104 (`val learnModeWatchReady`), add:

```kotlin
    // Gesture recording signal for Learn Mode - watch has started recording (student is writing)
    private val _learnModeGestureRecording = MutableStateFlow(0L)
    val learnModeGestureRecording: StateFlow<Long> = _learnModeGestureRecording.asStateFlow()
```

**Step 2: Add the message path constant**

After line 151 (`MESSAGE_PATH_LEARN_MODE_WATCH_READY`), add:

```kotlin
        private const val MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING = "/learn_mode_gesture_recording"
```

**Step 3: Add the message handler**

After the `MESSAGE_PATH_LEARN_MODE_WATCH_READY` handler block (lines 776-779), add:

```kotlin
            MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING -> {
                Log.d(TAG, "✍️ Watch started recording gesture in Learn Mode (student is writing)")
                _learnModeGestureRecording.value = System.currentTimeMillis()
            }
```

**Step 4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Add isStudentWriting state, listener, and reset in LearnModeSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`
  - After line 217 (add state variable)
  - After line 607 (add LaunchedEffect listener)
  - Line 612-617 (add reset in letterInputEvent handler)

**Step 1: Add state variable**

After line 217 (`var isWatchReady by remember(sessionKey) { mutableStateOf(false) }`), add:

```kotlin
    // State for "student is writing" indicator + UI lock
    var isStudentWriting by remember(sessionKey) { mutableStateOf(false) }
```

**Step 2: Add LaunchedEffect to listen for gesture recording signal**

After the existing `LaunchedEffect(Unit)` block that listens for `letterInputEvent` (after its closing `}` around line 807), add a new LaunchedEffect:

```kotlin
    // Listen for gesture recording signal from watch (student started writing)
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastRecordingTime = 0L
        watchConnectionManager.learnModeGestureRecording.collect { timestamp ->
            if (timestamp > sessionStartTime && timestamp > lastRecordingTime && timestamp > 0L) {
                lastRecordingTime = timestamp
                isStudentWriting = true
            }
        }
    }
```

**Step 3: Reset isStudentWriting when letter input arrives**

In the existing `letterInputEvent` LaunchedEffect (around line 616), right after `lastEventTime = event.timestamp`, add:

```kotlin
                isStudentWriting = false
```

So the block becomes:

```kotlin
            if (event.timestamp > 0L && event.timestamp > lastEventTime) {
                lastEventTime = event.timestamp
                isStudentWriting = false  // Student finished writing, reset indicator
```

**Step 4: Also reset on skip**

In the `handleSkipOrNext` lambda (around lines 960-983), add `isStudentWriting = false` at the top:

```kotlin
    val handleSkipOrNext: () -> Unit = {
        isStudentWriting = false
        // Cancel any ongoing wrong letter animation
        ...
    }
```

**Step 5: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: Add UI lockdown and pulsing writing indicator

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`
  - Lines 994-1013 (progress bar — add alpha + disable clicks)
  - Lines 1027-1104 (annotate/skip row — add alpha + disable)
  - Lines 1242-1259 (End Session button — replace with writing indicator when active)

**Step 1: Add lockdown to Progress Bar**

Change lines 994-1013 from:

```kotlin
        // Progress Bar
        ProgressIndicator(
            currentStep = currentStep,
            totalSteps = totalWords,
            modifier = Modifier.fillMaxWidth(),
            completedIndices = correctlyAnsweredWords,
            activeColor = PurpleColor,
            inactiveColor = LightPurpleColor,
            completedColor = Color(0xFF4CAF50),
            onSegmentClick = { index ->
                // Cancel any ongoing wrong letter animation
                if (wrongLetterAnimationActive) {
                    wrongLetterAnimationActive = false
                    wrongLetterText = ""
                    pendingCorrectAction = null
                }
                // Navigate to the clicked item
                currentWordIndex = index
            }
        )
```

To:

```kotlin
        // Progress Bar
        ProgressIndicator(
            currentStep = currentStep,
            totalSteps = totalWords,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
            completedIndices = correctlyAnsweredWords,
            activeColor = PurpleColor,
            inactiveColor = LightPurpleColor,
            completedColor = Color(0xFF4CAF50),
            onSegmentClick = { index ->
                if (!isStudentWriting) {
                    // Cancel any ongoing wrong letter animation
                    if (wrongLetterAnimationActive) {
                        wrongLetterAnimationActive = false
                        wrongLetterText = ""
                        pendingCorrectAction = null
                    }
                    // Navigate to the clicked item
                    currentWordIndex = index
                }
            }
        )
```

Add the import if not already present:

```kotlin
import androidx.compose.ui.draw.alpha
```

**Step 2: Add lockdown to Annotate/Skip row**

Change the Row at line 1028 to include alpha:

```kotlin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
```

Wrap the annotate button onClick to check `isStudentWriting`:

For the tooltip variant (line 1066):
```kotlin
                    IconButton(onClick = {
                        if (!isStudentWriting) {
                            showAnnotationDialog = true
                            showAnnotationTooltip = false
                            onAudioClick()
                        }
                    }) {
```

For the non-tooltip variant (line 1080):
```kotlin
                IconButton(onClick = {
                    if (!isStudentWriting) {
                        showAnnotationDialog = true
                        onAudioClick()
                    }
                }) {
```

For the skip button (line 1094):
```kotlin
            IconButton(
                onClick = {
                    if (!isStudentWriting) {
                        onSkip()
                        handleSkipOrNext()
                    }
                },
                enabled = !isStudentWriting
            ) {
```

**Step 3: Replace End Session button with conditional writing indicator**

Change lines 1242-1259 from:

```kotlin
        // End Session Button
        Button(
            onClick = { showEndSessionConfirmation = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "End Session",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
```

To:

```kotlin
        // "Student is air writing..." indicator or End Session button
        if (isStudentWriting) {
            val writingPulse = rememberInfiniteTransition(label = "writingPulse")
            val writingAlpha by writingPulse.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "writingAlpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${studentName.ifEmpty { "Student" }} is air writing...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = YellowColor.copy(alpha = writingAlpha),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // End Session Button
            Button(
                onClick = { showEndSessionConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "End Session",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
```

Add these imports if not already present:

```kotlin
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
```

Check if `YellowColor` is already defined in the project's color constants. If not, define it:

```kotlin
val YellowColor = Color(0xFFEDBB00)
```

**Step 4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 7: Final verification

**Step 1: Full project build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL for both app and wear modules

**Step 2: Manual test checklist**

1. Start a Learn Mode session (teacher on phone, student on watch)
2. Student taps to begin gesture recognition on watch
3. After countdown, when recording starts:
   - Phone should show "[StudentName] is air writing..." with pulsing yellow text
   - End Session button should be replaced by the indicator
   - Progress bar should be grayed out (alpha 0.35) and unclickable
   - Annotate and Skip buttons should be grayed out and disabled
4. When gesture result arrives (student finishes writing):
   - Indicator should disappear
   - End Session button should reappear
   - All controls should be re-enabled
5. Verify this works for all three modes: Fill in the Blank, Write the Word, Name the Picture
6. Verify skip still resets the indicator
