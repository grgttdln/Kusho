# Tutorial Mode: Phone-Driven Feedback Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the phone the single source of truth for tutorial mode feedback, mirroring Learn Mode's pattern, to fix stale "wrong" avatar and split-brain state bugs.

**Architecture:** Remove feedback state from watch-side singleton StateHolder. Add phone-to-watch feedback messages (`/tutorial_mode_show_feedback`, `/tutorial_mode_feedback_dismissed`). Watch displays feedback only when phone tells it to, using local composable state identical to Learn Mode.

**Tech Stack:** Kotlin, Jetpack Compose, Wear OS MessageClient API, StateFlow

---

### Task 1: Add `sendTutorialModeFeedback()` to Phone-Side WatchConnectionManager

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`

**Step 1: Add message path constant**

In the companion object (after line 170), add:

```kotlin
private const val MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK = "/tutorial_mode_show_feedback"
```

**Step 2: Add send method**

After the existing `notifyTutorialModeFeedbackDismissed()` method (after line 1109), add:

```kotlin
/**
 * Send feedback result to watch so it shows the same correct/incorrect screen.
 * Mirrors sendLearnModeFeedback() for single source of truth.
 * @param isCorrect Whether the gesture was correct
 * @param predictedLetter The letter that was predicted by the watch ML model
 */
fun sendTutorialModeFeedback(isCorrect: Boolean, predictedLetter: String) {
    scope.launch {
        try {
            val nodes = nodeClient.connectedNodes.await()
            val payload = org.json.JSONObject().apply {
                put("isCorrect", isCorrect)
                put("predictedLetter", predictedLetter)
            }.toString()

            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK,
                    payload.toByteArray()
                ).await()
            }
            Log.d(TAG, "✅ Tutorial Mode feedback sent: correct=$isCorrect, letter=$predictedLetter")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send Tutorial Mode feedback", e)
        }
    }
}
```

**Step 3: Commit**

```
feat(tutorial): add sendTutorialModeFeedback to WatchConnectionManager
```

---

### Task 2: Phone-Side TutorialSessionScreen — Send Feedback to Watch

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Send feedback when gesture result arrives**

In the `LaunchedEffect` that listens for gesture results (around line 477-490), add `sendTutorialModeFeedback` call after setting local state:

Change the gesture result handler block to add the send call. After line 487 (`showProgressCheck = true`), add:

```kotlin
// Send feedback to watch (phone is single source of truth)
watchConnectionManager.sendTutorialModeFeedback(isCorrectGesture, predictedLetter)
```

**Step 2: Send feedback dismissed when teacher taps Continue**

In the `onContinue` callback of `ProgressCheckDialog` (around line 520), add the dismissed notification BEFORE the retry/next-letter logic. After line 524 (`showAnimation = false`), add:

```kotlin
// Notify watch to dismiss feedback (phone-driven SSOT)
watchConnectionManager.notifyTutorialModeFeedbackDismissed()
```

**Step 3: Commit**

```
feat(tutorial): phone sends feedback + dismissed to watch on gesture result
```

---

### Task 3: Watch-Side PhoneCommunicationManager — Add Feedback Event Flows

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt`

**Step 1: Add message path constant**

After line 98 (`MESSAGE_PATH_TUTORIAL_MODE_GESTURE_RECORDING`), add:

```kotlin
private const val MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK = "/tutorial_mode_show_feedback"
```

**Step 2: Add feedback event data class and StateFlows**

After the `learnModeFeedbackDismissed` StateFlow (after line 59), add:

```kotlin
// StateFlow for Tutorial Mode feedback from phone (to show correct/incorrect screen)
data class TutorialModeFeedbackEvent(
    val isCorrect: Boolean = false,
    val predictedLetter: String = "",
    val timestamp: Long = 0L
)
private val _tutorialModeFeedbackEvent = MutableStateFlow(TutorialModeFeedbackEvent())
val tutorialModeFeedbackEvent: StateFlow<TutorialModeFeedbackEvent> = _tutorialModeFeedbackEvent.asStateFlow()

// StateFlow for Tutorial Mode feedback dismissed from phone
private val _tutorialModeFeedbackDismissed = MutableStateFlow(0L)
val tutorialModeFeedbackDismissed: StateFlow<Long> = _tutorialModeFeedbackDismissed.asStateFlow()
```

**Step 3: Handle incoming show_feedback message**

In the `onMessageReceived` `when` block, change the existing `MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED` handler (line 287-290) and add a new handler for `MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK`.

Replace the existing feedback_dismissed handler:

```kotlin
MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED -> {
    android.util.Log.d("PhoneCommunicationMgr", "👆 Mobile dismissed Tutorial Mode feedback")
    _tutorialModeFeedbackDismissed.value = System.currentTimeMillis()
}
```

Add a new case for show_feedback (before the feedback_dismissed case):

```kotlin
MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK -> {
    android.util.Log.d("PhoneCommunicationMgr", "🎯 Tutorial Mode feedback received")
    handleTutorialModeFeedback(messageEvent.data)
}
```

**Step 4: Add handler method**

Add this private method (near the other handle methods):

```kotlin
private fun handleTutorialModeFeedback(data: ByteArray) {
    try {
        val json = org.json.JSONObject(String(data))
        val isCorrect = json.optBoolean("isCorrect", false)
        val predictedLetter = json.optString("predictedLetter", "")

        android.util.Log.d("PhoneCommunicationMgr", "🎯 Tutorial Mode feedback: correct=$isCorrect, letter=$predictedLetter")

        _tutorialModeFeedbackEvent.value = TutorialModeFeedbackEvent(
            isCorrect = isCorrect,
            predictedLetter = predictedLetter,
            timestamp = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        android.util.Log.e("PhoneCommunicationMgr", "Error parsing tutorial mode feedback", e)
    }
}
```

**Step 5: Commit**

```
feat(tutorial): add phone-driven feedback event flows to watch PhoneCommunicationManager
```

---

### Task 4: Remove Feedback State from TutorialModeStateHolder

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeStateHolder.kt`

**Step 1: Remove FeedbackData class and related members**

Remove these (lines 37-50):
- `data class FeedbackData`
- `private val _feedbackData`
- `val feedbackData`

**Step 2: Remove feedback methods**

Remove these methods:
- `showFeedback()` (lines 161-174)
- `clearFeedback()` (lines 179-183)

**Step 3: Remove feedback clearing from other methods**

In `updateLetterData()` (line 76), remove:
```kotlin
_feedbackData.value = FeedbackData(shouldShow = false)
```

In `startSession()` (line 109), remove:
```kotlin
_feedbackData.value = FeedbackData()
```

In `endSession()` (line 137), remove:
```kotlin
_feedbackData.value = FeedbackData()
```

In `triggerRetry()` (line 192), remove:
```kotlin
_feedbackData.value = FeedbackData(shouldShow = false)
```

In `resetSession()` (line 205), remove:
```kotlin
_feedbackData.value = FeedbackData()
```

In `reset()` (line 218), remove:
```kotlin
_feedbackData.value = FeedbackData()
```

**Step 4: Commit**

```
refactor(tutorial): remove FeedbackData from TutorialModeStateHolder (phone is now SSOT)
```

---

### Task 5: Rewrite TutorialModeScreen Feedback to Phone-Driven Pattern

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeScreen.kt`

This is the largest change. The goal is to mirror LearnModeScreen's feedback pattern exactly.

**Step 1: Add local feedback state variables**

After the existing state declarations (around line 80), add:

```kotlin
// Phone-driven feedback state (mirrors Learn Mode pattern)
var showingFeedback by remember { mutableStateOf(false) }
var feedbackIsCorrect by remember { mutableStateOf(false) }
```

**Step 2: Remove StateHolder feedbackData collection**

Remove line 70:
```kotlin
val feedbackData by TutorialModeStateHolder.feedbackData.collectAsState()
```

**Step 3: Add stale state reset on screen entry**

In the existing `DisposableEffect(Unit)` (line 58-66), add a `resetSession()` call at the beginning:

```kotlin
DisposableEffect(Unit) {
    // Clear stale state from any previous session before setting screen flag
    TutorialModeStateHolder.resetSession()
    TutorialModeStateHolder.setWatchOnTutorialScreen(true)
    onDispose {
        TutorialModeStateHolder.setWatchOnTutorialScreen(false)
        ttsManager.shutdown()
        phoneCommunicationManager.cleanup()
    }
}
```

**Step 4: Add feedback event listener**

After the heartbeat LaunchedEffect (after line 182), add:

```kotlin
// Listen for feedback from phone (phone is single source of truth)
LaunchedEffect(Unit) {
    phoneCommunicationManager.tutorialModeFeedbackEvent.collect { event ->
        if (event.timestamp > 0L) {
            android.util.Log.d("TutorialMode", "📥 Feedback received from phone: correct=${event.isCorrect}")
            feedbackIsCorrect = event.isCorrect
            showingFeedback = true
        }
    }
}

// Listen for feedback dismissal from phone
LaunchedEffect(Unit) {
    var lastDismissTime = 0L
    phoneCommunicationManager.tutorialModeFeedbackDismissed.collect { timestamp ->
        if (timestamp > lastDismissTime && timestamp > 0L && showingFeedback) {
            lastDismissTime = timestamp
            android.util.Log.d("TutorialMode", "👆 Phone dismissed feedback")
            showingFeedback = false
        }
    }
}

// Reset feedback when new letter data arrives
LaunchedEffect(letterData.timestamp) {
    if (letterData.timestamp > 0L) {
        android.util.Log.d("TutorialMode", "🔄 Letter changed - resetting feedback state")
        showingFeedback = false
    }
}
```

**Step 5: Update the `when` block for UI state routing**

Replace the `feedbackData.shouldShow` branch (line 206-211) with a `showingFeedback` branch that uses the Learn Mode feedback pattern (2s flash then "Waiting for teacher..."):

```kotlin
showingFeedback -> {
    // Phone-driven feedback (mirrors Learn Mode)
    TutorialModeFeedbackContent(
        isCorrect = feedbackIsCorrect
    )
}
```

**Step 6: Remove `showFeedback` from `onGestureResult` callback**

In the `GestureRecognitionContent` `onGestureResult` callback (lines 224-230), remove the `TutorialModeStateHolder.showFeedback(isCorrect)` call. The callback becomes:

```kotlin
onGestureResult = { isCorrect, predictedLetter ->
    scope.launch {
        phoneCommunicationManager.sendTutorialModeGestureResult(isCorrect, predictedLetter)
    }
    // Feedback is now phone-driven — do NOT call showFeedback() locally
    isRecognizing = false
},
```

**Step 7: Add TutorialModeFeedbackContent composable**

Add this new composable (mirrors `LearnModeFeedbackContent` from LearnModeScreen.kt:876-933):

```kotlin
@Composable
private fun TutorialModeFeedbackContent(isCorrect: Boolean) {
    var showWaiting by remember { mutableStateOf(false) }

    // After 2 seconds, transition from feedback to waiting state
    LaunchedEffect(Unit) {
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
        // Brief correct/wrong feedback flash
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = if (isCorrect) R.drawable.dis_watch_correct else R.drawable.dis_watch_wrong
                ),
                contentDescription = if (isCorrect) "Correct" else "Wrong",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
```

**Step 8: Remove old FeedbackContent composable**

Delete the old `FeedbackContent` composable (lines 422-460) since it's replaced by `TutorialModeFeedbackContent`.

**Step 9: Commit**

```
feat(tutorial): switch watch feedback to phone-driven SSOT pattern
```

---

### Task 6: Clean Up PhoneCommunicationManager — Remove StateHolder Feedback Calls

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt`

**Step 1: Remove old feedback_dismissed handler that called StateHolder**

The old handler at line 287-290 called `TutorialModeStateHolder.clearFeedback()`. This was already replaced in Task 3 to emit to the new `_tutorialModeFeedbackDismissed` StateFlow. Verify that the handler no longer references `TutorialModeStateHolder.clearFeedback()`.

**Step 2: Commit**

```
cleanup(tutorial): remove StateHolder feedback references from PhoneCommunicationManager
```

---

### Task 7: Verify and Test End-to-End Flow

**Files:**
- All modified files from Tasks 1-6

**Step 1: Build the project**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Verify the message flow**

Manual test scenario:
1. Open Tutorial Mode on watch
2. Start Tutorial session on phone (select student, vowels, capital)
3. Verify watch shows "Waiting..." then "Tap to begin!"
4. Tap watch to start gesture → perform gesture → watch sends result to phone
5. Phone shows ProgressCheckDialog AND watch shows correct/wrong mascot for 2s then "Waiting for teacher..."
6. Teacher taps Continue → phone sends feedback_dismissed → watch returns to "Tap to begin!" (or retry)
7. Close phone mid-session → reopen watch tutorial screen → verify NO stale feedback shown

**Step 3: Commit**

```
test(tutorial): verify phone-driven feedback SSOT flow
```
