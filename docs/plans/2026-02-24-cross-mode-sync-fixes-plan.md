# Cross-Mode Sync Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix two cross-mode sync bugs: (1) watch stuck on "Waiting..." after exit/re-enter, (2) mobile app ignores watch disconnection during sessions.

**Architecture:** Watch sends a state request on mode screen entry; phone responds with current session state. During active sessions, phone runs PING/PONG health checks every 5 seconds and shows a blocking disconnect modal when the watch is unreachable.

**Tech Stack:** Kotlin, Jetpack Compose (mobile + Wear OS), Google Wearable Data Layer API (MessageClient), Coroutines/StateFlow

---

## Task 1: Add state request/response message paths to WatchConnectionManager (Phone)

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`

**Step 1: Add message path constants**

In the `companion object` block (after line 170, after `MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK`), add:

```kotlin
// State request/response message paths (for watch re-entry sync)
private const val MESSAGE_PATH_REQUEST_LEARN_MODE_STATE = "/request_learn_mode_state"
private const val MESSAGE_PATH_LEARN_MODE_STATE_RESPONSE = "/learn_mode_state_response"
private const val MESSAGE_PATH_REQUEST_TUTORIAL_MODE_STATE = "/request_tutorial_mode_state"
private const val MESSAGE_PATH_TUTORIAL_MODE_STATE_RESPONSE = "/tutorial_mode_state_response"
```

**Step 2: Add session state tracking flags**

After the existing StateFlow declarations (around line 108, after `_learnModeGestureRecording`), add:

```kotlin
// Track whether phone is currently in an active Learn Mode session
// (set true when LearnModeSessionScreen starts, false when it ends)
private val _isInLearnModeSession = MutableStateFlow(false)
val isInLearnModeSession: StateFlow<Boolean> = _isInLearnModeSession.asStateFlow()

// Track whether phone is currently in an active Tutorial Mode session
private val _isInTutorialModeSession = MutableStateFlow(false)
val isInTutorialModeSession: StateFlow<Boolean> = _isInTutorialModeSession.asStateFlow()

// Current Learn Mode word data for state recovery (set by session screen)
private val _currentLearnModeWordData = MutableStateFlow<String?>(null)

// Current Tutorial Mode letter data for state recovery (set by session screen)
private val _currentTutorialModeLetterData = MutableStateFlow<String?>(null)
```

**Step 3: Add methods to set/clear session state**

After the existing `sendLearnModePhoneReady()` method (around line 1009), add:

```kotlin
/**
 * Mark phone as in Learn Mode session (called by LearnModeSessionScreen on entry)
 */
fun setLearnModeSessionActive(active: Boolean) {
    _isInLearnModeSession.value = active
    if (!active) _currentLearnModeWordData.value = null
}

/**
 * Mark phone as in Tutorial Mode session (called by TutorialSessionScreen on entry)
 */
fun setTutorialModeSessionActive(active: Boolean) {
    _isInTutorialModeSession.value = active
    if (!active) _currentTutorialModeLetterData.value = null
}

/**
 * Update current word data for state recovery (called when word changes)
 */
fun updateCurrentLearnModeWordData(word: String, maskedIndex: Int, configurationType: String, dominantHand: String = "RIGHT") {
    val json = org.json.JSONObject().apply {
        put("word", word)
        put("maskedIndex", maskedIndex)
        put("configurationType", configurationType)
        put("dominantHand", dominantHand)
    }.toString()
    _currentLearnModeWordData.value = json
}

/**
 * Update current letter data for state recovery (called when letter changes)
 */
fun updateCurrentTutorialModeLetterData(letter: String, letterCase: String, currentIndex: Int, totalLetters: Int, dominantHand: String = "RIGHT") {
    val json = org.json.JSONObject().apply {
        put("letter", letter)
        put("letterCase", letterCase)
        put("currentIndex", currentIndex)
        put("totalLetters", totalLetters)
        put("dominantHand", dominantHand)
    }.toString()
    _currentTutorialModeLetterData.value = json
}
```

**Step 4: Handle state request messages in `handleIncomingMessage`**

In the `when (messageEvent.path)` block inside `handleIncomingMessage()` (after the `MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING` handler around line 786), add:

```kotlin
MESSAGE_PATH_REQUEST_LEARN_MODE_STATE -> {
    Log.d(TAG, "📋 Watch requesting Learn Mode state")
    scope.launch {
        try {
            val json = org.json.JSONObject().apply {
                put("isActive", _isInLearnModeSession.value)
                _currentLearnModeWordData.value?.let { wordData ->
                    put("wordData", wordData)
                }
            }.toString()
            messageClient.sendMessage(
                messageEvent.sourceNodeId,
                MESSAGE_PATH_LEARN_MODE_STATE_RESPONSE,
                json.toByteArray()
            ).await()
            Log.d(TAG, "✅ Learn Mode state response sent: active=${_isInLearnModeSession.value}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send Learn Mode state response", e)
        }
    }
}
MESSAGE_PATH_REQUEST_TUTORIAL_MODE_STATE -> {
    Log.d(TAG, "📋 Watch requesting Tutorial Mode state")
    scope.launch {
        try {
            val json = org.json.JSONObject().apply {
                put("isActive", _isInTutorialModeSession.value)
                _currentTutorialModeLetterData.value?.let { letterData ->
                    put("letterData", letterData)
                }
            }.toString()
            messageClient.sendMessage(
                messageEvent.sourceNodeId,
                MESSAGE_PATH_TUTORIAL_MODE_STATE_RESPONSE,
                json.toByteArray()
            ).await()
            Log.d(TAG, "✅ Tutorial Mode state response sent: active=${_isInTutorialModeSession.value}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send Tutorial Mode state response", e)
        }
    }
}
```

Also add these two paths to the `lockedDownPaths` set (around line 651) so that only the paired watch can request state:

```kotlin
MESSAGE_PATH_REQUEST_LEARN_MODE_STATE,
MESSAGE_PATH_REQUEST_TUTORIAL_MODE_STATE,
```

---

## Task 2: Add state request/response to PhoneCommunicationManager (Watch)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt`

**Step 1: Add message path constants**

In the `companion object` block (after line 112, after `MESSAGE_PATH_TUTORIAL_MODE_SHOW_FEEDBACK`), add:

```kotlin
// State request/response message paths (for watch re-entry sync)
private const val MESSAGE_PATH_REQUEST_LEARN_MODE_STATE = "/request_learn_mode_state"
private const val MESSAGE_PATH_LEARN_MODE_STATE_RESPONSE = "/learn_mode_state_response"
private const val MESSAGE_PATH_REQUEST_TUTORIAL_MODE_STATE = "/request_tutorial_mode_state"
private const val MESSAGE_PATH_TUTORIAL_MODE_STATE_RESPONSE = "/tutorial_mode_state_response"
```

**Step 2: Add response handlers in `onMessageReceived`**

In the `when (messageEvent.path)` block (after the `MESSAGE_PATH_LEARN_MODE_PHONE_READY` handler, around line 347), add:

```kotlin
MESSAGE_PATH_LEARN_MODE_STATE_RESPONSE -> {
    android.util.Log.d("PhoneCommunicationMgr", "📋 Learn Mode state response received")
    handleLearnModeStateResponse(messageEvent.data)
}
MESSAGE_PATH_TUTORIAL_MODE_STATE_RESPONSE -> {
    android.util.Log.d("PhoneCommunicationMgr", "📋 Tutorial Mode state response received")
    handleTutorialModeStateResponse(messageEvent.data)
}
```

**Step 3: Implement response handlers**

After the existing `handleLetterData()` method (around line 482), add:

```kotlin
/**
 * Handle Learn Mode state response from phone.
 * Called when watch re-enters Learn Mode screen and requests current state.
 */
private fun handleLearnModeStateResponse(data: ByteArray) {
    try {
        val json = org.json.JSONObject(String(data))
        val isActive = json.optBoolean("isActive", false)

        if (isActive) {
            _isPhoneInLearnMode.value = true
            // If phone included current word data, restore it
            val wordDataStr = json.optString("wordData", "")
            if (wordDataStr.isNotEmpty()) {
                val wordJson = org.json.JSONObject(wordDataStr)
                val word = wordJson.optString("word", "")
                val maskedIndex = wordJson.optInt("maskedIndex", -1)
                val configurationType = wordJson.optString("configurationType", "")
                if (word.isNotEmpty()) {
                    com.example.kusho.presentation.learn.LearnModeStateHolder.updateWordData(word, maskedIndex, configurationType)
                }
            }
        } else {
            _isPhoneInLearnMode.value = false
        }
    } catch (e: Exception) {
        android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing Learn Mode state response", e)
    }
}

/**
 * Handle Tutorial Mode state response from phone.
 * Called when watch re-enters Tutorial Mode screen and requests current state.
 */
private fun handleTutorialModeStateResponse(data: ByteArray) {
    try {
        val json = org.json.JSONObject(String(data))
        val isActive = json.optBoolean("isActive", false)

        if (isActive) {
            _isPhoneInTutorialMode.value = true
            // If phone included current letter data, restore it
            val letterDataStr = json.optString("letterData", "")
            if (letterDataStr.isNotEmpty()) {
                val letterJson = org.json.JSONObject(letterDataStr)
                val letter = letterJson.optString("letter", "")
                val letterCase = letterJson.optString("letterCase", "")
                val currentIndex = letterJson.optInt("currentIndex", 0)
                val totalLetters = letterJson.optInt("totalLetters", 0)
                val dominantHand = letterJson.optString("dominantHand", "RIGHT")
                if (letter.isNotEmpty()) {
                    com.example.kusho.presentation.tutorial.TutorialModeStateHolder.updateLetterData(
                        letter, letterCase, currentIndex, totalLetters, dominantHand
                    )
                }
            }
        } else {
            _isPhoneInTutorialMode.value = false
        }
    } catch (e: Exception) {
        android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing Tutorial Mode state response", e)
    }
}
```

**Step 4: Add send methods for state requests**

After the existing `sendLearnModeWatchReady()` method (around line 628), add:

```kotlin
/**
 * Request current Learn Mode session state from phone.
 * Called when watch enters/re-enters LearnModeScreen.
 */
suspend fun requestLearnModeState() {
    try {
        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node ->
            try {
                messageClient.sendMessage(
                    node.id,
                    MESSAGE_PATH_REQUEST_LEARN_MODE_STATE,
                    ByteArray(0)
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to request Learn Mode state", e)
    }
}

/**
 * Request current Tutorial Mode session state from phone.
 * Called when watch enters/re-enters TutorialModeScreen.
 */
suspend fun requestTutorialModeState() {
    try {
        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node ->
            try {
                messageClient.sendMessage(
                    node.id,
                    MESSAGE_PATH_REQUEST_TUTORIAL_MODE_STATE,
                    ByteArray(0)
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to request Tutorial Mode state", e)
    }
}
```

---

## Task 3: Send state request on watch screen entry

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeScreen.kt`

**Step 1: Add state request to LearnModeScreen**

In `LearnModeScreen.kt`, after the existing `DisposableEffect(Unit)` block (line 80), add a new `LaunchedEffect`:

```kotlin
// Request current session state from phone on entry (handles re-entry recovery)
LaunchedEffect(Unit) {
    phoneCommunicationManager.requestLearnModeState()
}
```

**Step 2: Add state request to TutorialModeScreen**

In `TutorialModeScreen.kt`, after the existing `DisposableEffect(Unit)` block (line 75), add a new `LaunchedEffect`:

```kotlin
// Request current session state from phone on entry (handles re-entry recovery)
LaunchedEffect(Unit) {
    phoneCommunicationManager.requestTutorialModeState()
}
```

---

## Task 4: Set session state flags from mobile session screens

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Set Learn Mode session active on entry/exit**

In `LearnModeSessionScreen.kt`, add a `DisposableEffect` near the existing watch notification code (around line 323):

```kotlin
// Track active session state for watch re-entry sync
DisposableEffect(sessionKey) {
    watchConnectionManager.setLearnModeSessionActive(true)
    onDispose {
        watchConnectionManager.setLearnModeSessionActive(false)
    }
}
```

**Step 2: Update current word data when it changes**

Find where `sendLearnModeWordData` is called in `LearnModeSessionScreen.kt` (search for `watchConnectionManager.sendLearnModeWordData`). Right after each call, also call `updateCurrentLearnModeWordData` with the same parameters. For example, if the existing code is:

```kotlin
watchConnectionManager.sendLearnModeWordData(word, maskedIndex, configurationType, dominantHand)
```

Add immediately after:

```kotlin
watchConnectionManager.updateCurrentLearnModeWordData(word, maskedIndex, configurationType, dominantHand)
```

**Step 3: Set Tutorial Mode session active on entry/exit**

In `TutorialSessionScreen.kt`, add a `DisposableEffect` near the existing watch notification code (around line 388):

```kotlin
// Track active session state for watch re-entry sync
DisposableEffect(Unit) {
    watchConnectionManager.setTutorialModeSessionActive(true)
    onDispose {
        watchConnectionManager.setTutorialModeSessionActive(false)
    }
}
```

**Step 4: Update current letter data when it changes**

Find where `sendTutorialModeLetterData` is called in `TutorialSessionScreen.kt`. Right after each call, also call `updateCurrentTutorialModeLetterData` with the same parameters.

---

## Task 5: Add session-aware PING/PONG health check to WatchConnectionManager

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`

**Step 1: Add session monitoring constants and state**

In the `companion object` (after `POLLING_INTERVAL_MS` on line 173), add:

```kotlin
private const val SESSION_HEALTH_CHECK_INTERVAL_MS = 5000L // 5 seconds
private const val PING_TIMEOUT_MS = 3000L // 3 seconds to wait for PONG
private const val MAX_CONSECUTIVE_FAILURES = 2 // failures before marking disconnected
```

After the existing StateFlow declarations (near the session tracking flows added in Task 1), add:

```kotlin
// Session health monitoring state
private var sessionMonitoringJob: Job? = null
private val _receivedPong = MutableStateFlow(false)

// Exposed to session screens - true when watch is unreachable during active session
private val _sessionConnectionLost = MutableStateFlow(false)
val sessionConnectionLost: StateFlow<Boolean> = _sessionConnectionLost.asStateFlow()
```

**Step 2: Handle PONG messages**

In the `handleIncomingMessage` method, add a new case in the `when` block (after the PING handler around line 703):

```kotlin
MESSAGE_PATH_PONG -> {
    Log.d(TAG, "🏓 Received PONG from watch")
    _receivedPong.value = true
}
```

**Step 3: Add session monitoring methods**

After the existing `stopMonitoring()` method (around line 263), add:

```kotlin
/**
 * Start aggressive health check monitoring during active sessions.
 * Sends PING every 5 seconds and waits for PONG.
 * After 2 consecutive failures, sets sessionConnectionLost = true.
 */
fun startSessionMonitoring() {
    sessionMonitoringJob?.cancel()
    _sessionConnectionLost.value = false
    var consecutiveFailures = 0

    sessionMonitoringJob = scope.launch {
        while (isActive) {
            delay(SESSION_HEALTH_CHECK_INTERVAL_MS)

            val pairedNodeId = getPairedWatchNodeId() ?: continue

            try {
                // Send PING
                _receivedPong.value = false
                messageClient.sendMessage(
                    pairedNodeId,
                    MESSAGE_PATH_PING,
                    "ping".toByteArray()
                ).await()

                // Wait for PONG with timeout
                val startTime = System.currentTimeMillis()
                while (!_receivedPong.value && System.currentTimeMillis() - startTime < PING_TIMEOUT_MS) {
                    delay(100)
                }

                if (_receivedPong.value) {
                    if (consecutiveFailures > 0) {
                        Log.d(TAG, "✅ Session health check recovered after $consecutiveFailures failures")
                    }
                    consecutiveFailures = 0
                    if (_sessionConnectionLost.value) {
                        Log.d(TAG, "🔄 Watch reconnected during session - resuming")
                        _sessionConnectionLost.value = false
                    }
                } else {
                    consecutiveFailures++
                    Log.w(TAG, "⚠️ Session health check failed ($consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)")
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        Log.e(TAG, "❌ Watch unreachable during session - marking connection lost")
                        _sessionConnectionLost.value = true
                    }
                }
            } catch (e: Exception) {
                consecutiveFailures++
                Log.w(TAG, "⚠️ Session health check exception ($consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)", e)
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    _sessionConnectionLost.value = true
                }
            }
        }
    }
}

/**
 * Stop session health check monitoring. Call when leaving session screen.
 */
fun stopSessionMonitoring() {
    sessionMonitoringJob?.cancel()
    sessionMonitoringJob = null
    _sessionConnectionLost.value = false
}
```

---

## Task 6: Add WatchDisconnectedDialog composable

**Files:**
- Create: `app/src/main/java/com/example/app/ui/components/WatchDisconnectedDialog.kt`

**Step 1: Create the dialog composable**

```kotlin
package com.example.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Blocking modal shown when the watch disconnects during an active session.
 * Auto-dismisses when connection is restored. "End Session" allows teacher to bail out.
 */
@Composable
fun WatchDisconnectedDialog(
    onEndSession: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Blocked - cannot dismiss by tapping outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Watch Disconnected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Reconnecting...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onEndSession,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End Session")
                }
            }
        }
    }
}
```

---

## Task 7: Integrate session monitoring and disconnect dialog into session screens

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Add session monitoring to LearnModeSessionScreen**

Near the top of the composable (after the `watchConnectionManager` remember block around line 247), add:

```kotlin
// Session health monitoring
val isConnectionLost by watchConnectionManager.sessionConnectionLost.collectAsState()
```

Add a `DisposableEffect` to start/stop monitoring:

```kotlin
// Start session health monitoring for disconnect detection
DisposableEffect(Unit) {
    watchConnectionManager.startSessionMonitoring()
    onDispose {
        watchConnectionManager.stopSessionMonitoring()
    }
}
```

At the end of the composable's content (before the closing `}`), add the dialog:

```kotlin
// Show disconnect dialog when watch becomes unreachable
if (isConnectionLost) {
    WatchDisconnectedDialog(
        onEndSession = {
            watchConnectionManager.stopSessionMonitoring()
            watchConnectionManager.notifyLearnModeEnded()
            navController.popBackStack()
        }
    )
}
```

Add the import at the top:

```kotlin
import com.example.app.ui.components.WatchDisconnectedDialog
```

**Step 2: Add session monitoring to TutorialSessionScreen**

Same pattern. Near the top:

```kotlin
val isConnectionLost by watchConnectionManager.sessionConnectionLost.collectAsState()
```

DisposableEffect:

```kotlin
DisposableEffect(Unit) {
    watchConnectionManager.startSessionMonitoring()
    onDispose {
        watchConnectionManager.stopSessionMonitoring()
    }
}
```

Dialog at end of content:

```kotlin
if (isConnectionLost) {
    WatchDisconnectedDialog(
        onEndSession = {
            watchConnectionManager.stopSessionMonitoring()
            watchConnectionManager.notifyTutorialModeEnded()
            navController.popBackStack()
        }
    )
}
```

---

## Summary of All Changes

| # | File | What Changes |
|---|------|-------------|
| 1 | `app/.../WatchConnectionManager.kt` | New message paths, session state tracking, state request handlers, PING/PONG session monitoring |
| 2 | `wear/.../PhoneCommunicationManager.kt` | New message paths, state response handlers, state request send methods |
| 3 | `wear/.../LearnModeScreen.kt` | Send state request on entry via LaunchedEffect |
| 4 | `wear/.../TutorialModeScreen.kt` | Send state request on entry via LaunchedEffect |
| 5 | `app/.../LearnModeSessionScreen.kt` | Set session active flags, update current word data, start/stop session monitoring, show disconnect dialog |
| 6 | `app/.../TutorialSessionScreen.kt` | Set session active flags, update current letter data, start/stop session monitoring, show disconnect dialog |
| 7 | `app/.../ui/components/WatchDisconnectedDialog.kt` | New file: reusable disconnect modal composable |
