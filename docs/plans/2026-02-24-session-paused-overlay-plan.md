# Session Paused Overlay Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** When the watch exits a mode mid-session, show a non-blocking overlay on the phone so the teacher can explicitly resume, and put the watch in a "Waiting for teacher..." state until resume.

**Architecture:** Watch sends exit/enter screen signals via Wearable MessageClient. Phone receives these, shows `SessionPausedOverlay`. Teacher taps Continue to send a resume message with current word/letter data. Watch receives resume, restores data, shows "Tap to begin!".

**Tech Stack:** Kotlin, Jetpack Compose, Google Wearable Data Layer API (MessageClient), Kotlin StateFlows

---

### Task 1: Add Message Path Constants (Phone Side)

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:207-210`

**Step 1: Add new message path constants**

In the `companion object`, after the existing state request/response constants (line 210), add:

```kotlin
        // Session pause/resume message paths (watch screen exit/enter)
        private const val MESSAGE_PATH_LEARN_MODE_WATCH_EXITED_SCREEN = "/learn_mode_watch_exited_screen"
        private const val MESSAGE_PATH_LEARN_MODE_WATCH_ENTERED_SCREEN = "/learn_mode_watch_entered_screen"
        private const val MESSAGE_PATH_LEARN_MODE_RESUME = "/learn_mode_resume"
        private const val MESSAGE_PATH_TUTORIAL_MODE_WATCH_EXITED_SCREEN = "/tutorial_mode_watch_exited_screen"
        private const val MESSAGE_PATH_TUTORIAL_MODE_WATCH_ENTERED_SCREEN = "/tutorial_mode_watch_entered_screen"
        private const val MESSAGE_PATH_TUTORIAL_MODE_RESUME = "/tutorial_mode_resume"
```

**Step 2: Verify the file compiles**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add SessionPaused StateFlows (Phone Side)

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:136-141`

**Step 1: Add new StateFlows**

After `_sessionDisconnectReason` (line 141), add:

```kotlin
    // Session paused state - true when watch has exited the mode screen during active session
    private val _sessionPaused = MutableStateFlow(false)
    val sessionPaused: StateFlow<Boolean> = _sessionPaused.asStateFlow()

    // Whether watch is currently on the mode screen (for enabling/disabling Continue button)
    private val _watchOnModeScreen = MutableStateFlow(true)
    val watchOnModeScreen: StateFlow<Boolean> = _watchOnModeScreen.asStateFlow()
```

**Step 2: Reset state in `setLearnModeSessionActive` and `setTutorialModeSessionActive`**

In `setLearnModeSessionActive()` (line ~1188), update to:

```kotlin
    fun setLearnModeSessionActive(active: Boolean) {
        _isInLearnModeSession.value = active
        if (!active) {
            _currentLearnModeWordData.value = null
            _sessionPaused.value = false
            _watchOnModeScreen.value = true
        } else {
            _sessionPaused.value = false
            _watchOnModeScreen.value = true
        }
    }
```

In `setTutorialModeSessionActive()` (line ~1196), update to:

```kotlin
    fun setTutorialModeSessionActive(active: Boolean) {
        _isInTutorialModeSession.value = active
        if (!active) {
            _currentTutorialModeLetterData.value = null
            _sessionPaused.value = false
            _watchOnModeScreen.value = true
        } else {
            _sessionPaused.value = false
            _watchOnModeScreen.value = true
        }
    }
```

**Step 3: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 3: Handle Watch Exit/Enter Messages (Phone Side)

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:960-961`

**Step 1: Add message handlers in `handleIncomingMessage`**

In the `when` block inside `handleIncomingMessage()`, before the closing `}` at line 961, add:

```kotlin
            MESSAGE_PATH_LEARN_MODE_WATCH_EXITED_SCREEN -> {
                Log.d(TAG, "📴 Watch exited Learn Mode screen")
                if (_isInLearnModeSession.value) {
                    _sessionPaused.value = true
                    _watchOnModeScreen.value = false
                }
            }
            MESSAGE_PATH_LEARN_MODE_WATCH_ENTERED_SCREEN -> {
                Log.d(TAG, "📱 Watch entered Learn Mode screen")
                _watchOnModeScreen.value = true
            }
            MESSAGE_PATH_TUTORIAL_MODE_WATCH_EXITED_SCREEN -> {
                Log.d(TAG, "📴 Watch exited Tutorial Mode screen")
                if (_isInTutorialModeSession.value) {
                    _sessionPaused.value = true
                    _watchOnModeScreen.value = false
                }
            }
            MESSAGE_PATH_TUTORIAL_MODE_WATCH_ENTERED_SCREEN -> {
                Log.d(TAG, "📱 Watch entered Tutorial Mode screen")
                _watchOnModeScreen.value = true
            }
```

**Step 2: Add these paths to the data lockdown set**

In the `lockedDownPaths` set (around line 778-790), add the new paths:

```kotlin
        MESSAGE_PATH_LEARN_MODE_WATCH_EXITED_SCREEN,
        MESSAGE_PATH_LEARN_MODE_WATCH_ENTERED_SCREEN,
        MESSAGE_PATH_TUTORIAL_MODE_WATCH_EXITED_SCREEN,
        MESSAGE_PATH_TUTORIAL_MODE_WATCH_ENTERED_SCREEN,
```

**Step 3: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 4: Add Resume Methods (Phone Side)

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt` (after `updateCurrentTutorialModeLetterData`, around line 1226)

**Step 1: Add resume methods**

After `updateCurrentTutorialModeLetterData()`, add:

```kotlin
    /**
     * Resume Learn Mode session after watch re-entered the screen.
     * Sends current word data to watch so it can show "Tap to begin!".
     */
    fun resumeLearnModeSession() {
        _sessionPaused.value = false
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = _currentLearnModeWordData.value ?: "{}"
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_RESUME,
                        payload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Learn Mode resume sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send Learn Mode resume", e)
            }
        }
    }

    /**
     * Resume Tutorial Mode session after watch re-entered the screen.
     * Sends current letter data to watch so it can show "Tap to begin!".
     */
    fun resumeTutorialModeSession() {
        _sessionPaused.value = false
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = _currentTutorialModeLetterData.value ?: "{}"
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_RESUME,
                        payload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Tutorial Mode resume sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send Tutorial Mode resume", e)
            }
        }
    }
```

**Step 2: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 5: Create SessionPausedOverlay Composable

**Files:**
- Create: `app/src/main/java/com/example/app/ui/components/SessionPausedOverlay.kt`

**Step 1: Write the overlay composable**

```kotlin
package com.example.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Non-blocking overlay shown when the watch exits the mode screen during an active session.
 * Sits on top of session content with a semi-transparent scrim.
 *
 * - "Continue" is enabled only when [watchOnScreen] is true (watch has re-entered the mode screen).
 * - "End Session" is always enabled.
 */
@Composable
fun SessionPausedOverlay(
    watchOnScreen: Boolean,
    onContinue: () -> Unit,
    onEndSession: () -> Unit
) {
    // Scrim - semi-transparent dark background over session content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consume clicks so session screen behind is not interactive */ },
        contentAlignment = Alignment.Center
    ) {
        // Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            // Amber header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Color(0xFFFFA726))
            )

            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Session Paused",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0B0B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The watch has exited the current mode",
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status indicator
                if (watchOnScreen) {
                    Text(
                        text = "\u2713 Watch ready",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA726)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Waiting for watch...",
                            fontSize = 14.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue button - enabled only when watch is on screen
                Button(
                    onClick = onContinue,
                    enabled = watchOnScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (watchOnScreen) Color.White else Color(0xFFBDBDBD)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // End Session button - always enabled
                Button(
                    onClick = onEndSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFE0B2)
                    )
                ) {
                    Text(
                        text = "End Session",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFA726)
                    )
                }
            }
        }
    }
}
```

**Step 2: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 6: Integrate SessionPausedOverlay into LearnModeSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Add import for the overlay**

Add to imports section (top of file):

```kotlin
import com.example.app.ui.components.SessionPausedOverlay
```

**Step 2: Observe the new StateFlows**

Near existing `isConnectionLost` observation (line ~259-260), add:

```kotlin
    val isSessionPaused by watchConnectionManager.sessionPaused.collectAsState()
    val isWatchOnModeScreen by watchConnectionManager.watchOnModeScreen.collectAsState()
```

**Step 3: Show the overlay**

After the existing `if (isConnectionLost)` block (which ends around line 973), add:

```kotlin
    // Session Paused Overlay - non-blocking, shown when watch exits mode screen
    // Only show if not also showing the disconnect dialog (disconnect takes priority)
    if (isSessionPaused && !isConnectionLost) {
        SessionPausedOverlay(
            watchOnScreen = isWatchOnModeScreen,
            onContinue = {
                watchConnectionManager.resumeLearnModeSession()
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyLearnModeEnded()
                onEarlyExit()
            }
        )
    }
```

**Step 4: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 7: Integrate SessionPausedOverlay into TutorialSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Add import for the overlay**

Add to imports section:

```kotlin
import com.example.app.ui.components.SessionPausedOverlay
```

**Step 2: Observe the new StateFlows**

Near existing `isConnectionLost` observation (line ~117-118), add:

```kotlin
    val isSessionPaused by watchConnectionManager.sessionPaused.collectAsState()
    val isWatchOnModeScreen by watchConnectionManager.watchOnModeScreen.collectAsState()
```

**Step 3: Show the overlay**

After the existing `if (isConnectionLost)` block (which ends around line 607), add:

```kotlin
    // Session Paused Overlay - non-blocking, shown when watch exits mode screen
    // Only show if not also showing the disconnect dialog (disconnect takes priority)
    if (isSessionPaused && !isConnectionLost) {
        SessionPausedOverlay(
            watchOnScreen = isWatchOnModeScreen,
            onContinue = {
                watchConnectionManager.resumeTutorialModeSession()
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyTutorialModeEnded()
                onEarlyExit()
            }
        )
    }
```

**Step 4: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 8: Add Message Path Constants (Watch Side)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt:118-122`

**Step 1: Add new message path constants**

In the `companion object`, after the state request/response constants (line 122), add:

```kotlin
        // Session pause/resume message paths (watch screen exit/enter)
        private const val MESSAGE_PATH_LEARN_MODE_WATCH_EXITED_SCREEN = "/learn_mode_watch_exited_screen"
        private const val MESSAGE_PATH_LEARN_MODE_WATCH_ENTERED_SCREEN = "/learn_mode_watch_entered_screen"
        private const val MESSAGE_PATH_LEARN_MODE_RESUME = "/learn_mode_resume"
        private const val MESSAGE_PATH_TUTORIAL_MODE_WATCH_EXITED_SCREEN = "/tutorial_mode_watch_exited_screen"
        private const val MESSAGE_PATH_TUTORIAL_MODE_WATCH_ENTERED_SCREEN = "/tutorial_mode_watch_entered_screen"
        private const val MESSAGE_PATH_TUTORIAL_MODE_RESUME = "/tutorial_mode_resume"
```

**Step 2: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 9: Add Send Methods and Resume StateFlows (Watch Side)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt`

**Step 1: Add resume event StateFlows**

After the existing `_tutorialModeFeedbackDismissed` StateFlow (line ~72), add:

```kotlin
    // Resume events from phone (teacher tapped Continue)
    private val _learnModeResumeEvent = MutableStateFlow(0L)
    val learnModeResumeEvent: StateFlow<Long> = _learnModeResumeEvent.asStateFlow()

    private val _tutorialModeResumeEvent = MutableStateFlow(0L)
    val tutorialModeResumeEvent: StateFlow<Long> = _tutorialModeResumeEvent.asStateFlow()
```

**Step 2: Add resume message handlers in `onMessageReceived`**

In the `when` block inside `onMessageReceived()`, before the closing `}` of the `when` (around line 381), add:

```kotlin
            MESSAGE_PATH_LEARN_MODE_RESUME -> {
                android.util.Log.d("PhoneCommunicationMgr", "▶️ Learn Mode resume received from phone")
                handleLearnModeResume(messageEvent.data)
                _learnModeResumeEvent.value = System.currentTimeMillis()
            }
            MESSAGE_PATH_TUTORIAL_MODE_RESUME -> {
                android.util.Log.d("PhoneCommunicationMgr", "▶️ Tutorial Mode resume received from phone")
                handleTutorialModeResume(messageEvent.data)
                _tutorialModeResumeEvent.value = System.currentTimeMillis()
            }
```

**Step 3: Add the resume handler methods**

After `handleTutorialModeStateResponse()` (around line 577), add:

```kotlin
    /**
     * Handle Learn Mode resume from phone.
     * Restores word data so watch can show "Tap to begin!".
     */
    private fun handleLearnModeResume(data: ByteArray) {
        try {
            val json = org.json.JSONObject(String(data))
            val word = json.optString("word", "")
            val maskedIndex = json.optInt("maskedIndex", -1)
            val configurationType = json.optString("configurationType", "")

            _isPhoneInLearnMode.value = true
            if (word.isNotEmpty()) {
                com.example.kusho.presentation.learn.LearnModeStateHolder.updateWordData(word, maskedIndex, configurationType)
            }
            android.util.Log.d("PhoneCommunicationMgr", "✅ Learn Mode resumed with word: $word")
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing Learn Mode resume", e)
        }
    }

    /**
     * Handle Tutorial Mode resume from phone.
     * Restores letter data so watch can show "Tap to begin!".
     */
    private fun handleTutorialModeResume(data: ByteArray) {
        try {
            val json = org.json.JSONObject(String(data))
            val letter = json.optString("letter", "")
            val letterCase = json.optString("letterCase", "")
            val currentIndex = json.optInt("currentIndex", 0)
            val totalLetters = json.optInt("totalLetters", 0)
            val dominantHand = json.optString("dominantHand", "RIGHT")

            _isPhoneInTutorialMode.value = true
            if (letter.isNotEmpty()) {
                com.example.kusho.presentation.tutorial.TutorialModeStateHolder.updateLetterData(
                    letter, letterCase, currentIndex, totalLetters, dominantHand
                )
            }
            android.util.Log.d("PhoneCommunicationMgr", "✅ Tutorial Mode resumed with letter: $letter")
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing Tutorial Mode resume", e)
        }
    }
```

**Step 4: Add send methods for exit/enter signals**

After the existing `sendLearnModeFeedbackDismissed()` (around line 884), add:

```kotlin
    /**
     * Notify phone that watch has exited the Learn Mode screen.
     * Called from LearnModeScreen's DisposableEffect onDispose.
     */
    fun sendLearnModeWatchExitedScreen() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_LEARN_MODE_WATCH_EXITED_SCREEN,
                            ByteArray(0)
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                android.util.Log.d("PhoneCommunicationMgr", "📤 Learn Mode watch exited screen sent")
            } catch (e: Exception) {
                android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to send Learn Mode watch exited screen", e)
            }
        }
    }

    /**
     * Notify phone that watch has entered the Learn Mode screen.
     * Called from LearnModeScreen's LaunchedEffect on composition.
     */
    fun sendLearnModeWatchEnteredScreen() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_LEARN_MODE_WATCH_ENTERED_SCREEN,
                            ByteArray(0)
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                android.util.Log.d("PhoneCommunicationMgr", "📤 Learn Mode watch entered screen sent")
            } catch (e: Exception) {
                android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to send Learn Mode watch entered screen", e)
            }
        }
    }

    /**
     * Notify phone that watch has exited the Tutorial Mode screen.
     */
    fun sendTutorialModeWatchExitedScreen() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_TUTORIAL_MODE_WATCH_EXITED_SCREEN,
                            ByteArray(0)
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                android.util.Log.d("PhoneCommunicationMgr", "📤 Tutorial Mode watch exited screen sent")
            } catch (e: Exception) {
                android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to send Tutorial Mode watch exited screen", e)
            }
        }
    }

    /**
     * Notify phone that watch has entered the Tutorial Mode screen.
     */
    fun sendTutorialModeWatchEnteredScreen() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_TUTORIAL_MODE_WATCH_ENTERED_SCREEN,
                            ByteArray(0)
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                android.util.Log.d("PhoneCommunicationMgr", "📤 Tutorial Mode watch entered screen sent")
            } catch (e: Exception) {
                android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to send Tutorial Mode watch entered screen", e)
            }
        }
    }
```

**Step 5: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 10: Update LearnModeScreen to Send Exit/Enter Signals and Handle Resume (Watch Side)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`

**Step 1: Update DisposableEffect to send exit signal**

Replace the existing `DisposableEffect(Unit)` (lines 73-80):

```kotlin
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        LearnModeStateHolder.setWatchOnLearnScreen(true)
        phoneCommunicationManager.sendLearnModeWatchEnteredScreen()
        onDispose {
            phoneCommunicationManager.sendLearnModeWatchExitedScreen()
            LearnModeStateHolder.setWatchOnLearnScreen(false)
            view.keepScreenOn = false
        }
    }
```

**Step 2: Add "Waiting for teacher..." state**

The watch needs a new UI state for when it has re-entered the mode screen but the teacher hasn't tapped Continue yet. Add a new private composable (near the existing `WaitingContent`, around line 263):

```kotlin
@Composable
private fun WaitingForResumeContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Waiting for\nteacher to resume...",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
```

(Requires imports: `androidx.compose.foundation.layout.Arrangement`, `androidx.compose.material3.CircularProgressIndicator` — check if already imported; if not, add them.)

**Step 3: Track "waiting for resume" state**

Near the top of `LearnModeScreen()`, after the existing state declarations, add:

```kotlin
    // Track whether we're waiting for teacher to resume after re-entering the screen
    var waitingForResume by remember { mutableStateOf(false) }
    val learnModeResumeEvent by phoneCommunicationManager.learnModeResumeEvent.collectAsState()
```

**Step 4: Detect re-entry scenario**

Add a `LaunchedEffect` to determine if this is a re-entry (phone is in learn mode but session was paused):

```kotlin
    // On screen entry: if phone is already in learn mode but we have no word data,
    // we're likely re-entering after an exit. Wait for teacher resume.
    LaunchedEffect(isPhoneInLearnMode) {
        if (isPhoneInLearnMode && wordData.word.isEmpty()) {
            waitingForResume = true
        }
    }
```

**Step 5: Handle resume event**

Add a `LaunchedEffect` to transition out of waiting state when resume arrives:

```kotlin
    // When resume event fires, clear the waiting state
    LaunchedEffect(learnModeResumeEvent) {
        if (learnModeResumeEvent > 0L && waitingForResume) {
            waitingForResume = false
        }
    }
```

**Step 6: Update the `when` block for state rendering**

Modify the existing state rendering logic (around line 202) to include the new waiting-for-resume state. The `!isPhoneInLearnMode` branch currently shows `WaitingContent()`. Add a check before it:

In the `when` block, add a new branch before the `!isPhoneInLearnMode` check:

```kotlin
    waitingForResume && wordData.word.isEmpty() -> {
        // Watch re-entered screen, waiting for teacher to tap Continue
        WaitingForResumeContent()
    }
```

**Step 7: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 11: Update TutorialModeScreen to Send Exit/Enter Signals and Handle Resume (Watch Side)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeScreen.kt`

**Step 1: Update DisposableEffect to send exit/enter signals**

Replace the existing `DisposableEffect(Unit)` (lines 62-75):

```kotlin
    DisposableEffect(Unit) {
        // Clear stale state from any previous session before setting screen flag
        TutorialModeStateHolder.resetSession()
        // Mark watch as on Tutorial Mode screen for handshake gating
        TutorialModeStateHolder.setWatchOnTutorialScreen(true)
        phoneCommunicationManager.sendTutorialModeWatchEnteredScreen()
        // Keep screen on during Tutorial Mode to prevent sleep during air writing
        view.keepScreenOn = true
        onDispose {
            phoneCommunicationManager.sendTutorialModeWatchExitedScreen()
            TutorialModeStateHolder.setWatchOnTutorialScreen(false)
            view.keepScreenOn = false
            ttsManager.shutdown()
            phoneCommunicationManager.cleanup()
        }
    }
```

**Step 2: Add "Waiting for teacher..." composable**

Near the existing `WaitingContent` (around line 329), add:

```kotlin
@Composable
private fun WaitingForResumeContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Waiting for\nteacher to resume...",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
```

**Step 3: Track "waiting for resume" state**

Near the top of `TutorialModeScreen()`, add:

```kotlin
    var waitingForResume by remember { mutableStateOf(false) }
    val tutorialModeResumeEvent by phoneCommunicationManager.tutorialModeResumeEvent.collectAsState()
```

**Step 4: Detect re-entry and handle resume**

```kotlin
    // On screen entry: if phone is already in tutorial mode but we have no letter data,
    // we're likely re-entering after an exit. Wait for teacher resume.
    LaunchedEffect(isPhoneInTutorialMode) {
        if (isPhoneInTutorialMode && letterData.letter.isEmpty()) {
            waitingForResume = true
        }
    }

    // When resume event fires, clear the waiting state
    LaunchedEffect(tutorialModeResumeEvent) {
        if (tutorialModeResumeEvent > 0L && waitingForResume) {
            waitingForResume = false
        }
    }
```

**Step 5: Update the `when` block**

Add a new branch before the `!isPhoneInTutorialMode` check (around line 221):

```kotlin
    waitingForResume && letterData.letter.isEmpty() -> {
        // Watch re-entered screen, waiting for teacher to tap Continue
        WaitingForResumeContent()
    }
```

**Step 6: Verify compilation**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew :wear:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 12: Full Build Verification

**Step 1: Build both modules**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 2: Manual testing checklist**

Test on device/emulator:
1. Start a Learn Mode session normally -> verify no overlay appears
2. While in Learn Mode, navigate away on watch -> verify phone shows "Session Paused" overlay with disabled Continue
3. Navigate back to Learn Mode on watch -> verify Continue button becomes enabled
4. Tap Continue -> verify watch shows "Tap to begin!" and session resumes
5. Repeat steps 2-4 for Tutorial Mode
6. While overlay is showing, tap End Session -> verify session ends on both devices
7. During a session, simulate BT disconnect -> verify WatchDisconnectedDialog appears (takes priority)
8. During BT disconnect + session paused, restore BT -> verify disconnect dialog dismisses but paused overlay remains if applicable
