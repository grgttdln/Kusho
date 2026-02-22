# Learn Mode Waiting State Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a two-way handshake waiting state to Learn Mode (phone and watch), mirroring Tutorial Mode's sync protocol exactly.

**Architecture:** Add `/learn_mode_phone_ready` and `/learn_mode_watch_ready` message paths. Phone shows a blocking overlay until watch replies. Watch adds a "Tap to begin!" screen after session starts. Word data is gated behind handshake completion.

**Tech Stack:** Kotlin, Jetpack Compose, Wear OS Data Layer API (MessageClient), StateFlow

---

### Task 1: Add Learn Mode Handshake Message Paths and StateFlow (Phone)

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`

**Step 1: Add message path constants**

After line 141 (`MESSAGE_PATH_LEARN_MODE_SHOW_FEEDBACK`), add:

```kotlin
        private const val MESSAGE_PATH_LEARN_MODE_PHONE_READY = "/learn_mode_phone_ready"
        private const val MESSAGE_PATH_LEARN_MODE_WATCH_READY = "/learn_mode_watch_ready"
```

**Step 2: Add StateFlow for watch ready signal**

After line 96 (`val tutorialModeWatchReady`), add:

```kotlin
    // Watch ready signal for Learn Mode - watch has entered Learn Mode screen and is ready
    private val _learnModeWatchReady = MutableStateFlow(0L)
    val learnModeWatchReady: StateFlow<Long> = _learnModeWatchReady.asStateFlow()
```

**Step 3: Reset the StateFlow when Learn Mode starts**

Inside `notifyLearnModeStarted()` (line 374), add a reset at the start of the function body before the `scope.launch`:

```kotlin
    fun notifyLearnModeStarted() {
        _learnModeWatchReady.value = 0L // Reset handshake state for fresh session
        scope.launch {
```

**Step 4: Add handler for incoming watch ready message**

In the `handleIncomingMessage` method, find the block that handles `MESSAGE_PATH_TUTORIAL_MODE_WATCH_READY` (line 755-758). After that block, add:

```kotlin
            MESSAGE_PATH_LEARN_MODE_WATCH_READY -> {
                Log.d(TAG, "✅ Watch is ready for Learn Mode")
                _learnModeWatchReady.value = System.currentTimeMillis()
            }
```

**Step 5: Add sendLearnModePhoneReady method**

After `sendTutorialModePhoneReady()` (after line 959), add:

```kotlin
    /**
     * Notify watch that the phone is on LearnModeSessionScreen and ready for handshake.
     * The watch will reply with /learn_mode_watch_ready when it receives this.
     */
    fun sendLearnModePhoneReady() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_PHONE_READY,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Learn Mode phone ready signal sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send Learn Mode phone ready signal", e)
            }
        }
    }
```

**Step 6: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add Learn Mode Handshake to Wear PhoneCommunicationManager

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt`

**Step 1: Add message path constants**

After line 81 (`MESSAGE_PATH_LEARN_MODE_SHOW_FEEDBACK`), add:

```kotlin
        private const val MESSAGE_PATH_LEARN_MODE_PHONE_READY = "/learn_mode_phone_ready"
        private const val MESSAGE_PATH_LEARN_MODE_WATCH_READY = "/learn_mode_watch_ready"
```

**Step 2: Add handler for phone ready message**

In the `onMessageReceived` method, find the block handling `MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY` (around line 294). After the closing brace of that handler block (line 304), add:

```kotlin
            MESSAGE_PATH_LEARN_MODE_PHONE_READY -> {
                // Only reply if watch user is actually on the Learn Mode screen
                if (com.example.kusho.presentation.learn.LearnModeStateHolder.isWatchOnLearnScreen.value) {
                    android.util.Log.d("PhoneCommunicationMgr", "📱 Phone is ready & watch is on Learn screen - replying with watch ready")
                    scope.launch {
                        sendLearnModeWatchReady()
                    }
                } else {
                    android.util.Log.d("PhoneCommunicationMgr", "📱 Phone is ready but watch is NOT on Learn screen - ignoring")
                }
            }
```

**Step 3: Add sendLearnModeWatchReady method**

After the existing `sendTutorialModeWatchReady()` method (after line ~490), add:

```kotlin
    /**
     * Notify phone that watch is on the Learn Mode screen and ready to receive data
     */
    suspend fun sendLearnModeWatchReady() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_WATCH_READY,
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

**Step 4: Verify the project compiles**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Add isWatchOnLearnScreen Gating to LearnModeStateHolder

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeStateHolder.kt`

**Step 1: Add isWatchOnLearnScreen StateFlow**

After line 55 (`val writeTheWordState`), add:

```kotlin
    // Tracks whether the watch user is currently on the LearnModeScreen
    // Used to gate handshake replies so we don't reply when on other screens
    private val _isWatchOnLearnScreen = MutableStateFlow(false)
    val isWatchOnLearnScreen: StateFlow<Boolean> = _isWatchOnLearnScreen.asStateFlow()

    fun setWatchOnLearnScreen(onScreen: Boolean) {
        _isWatchOnLearnScreen.value = onScreen
    }
```

**Step 2: Reset isWatchOnLearnScreen in the reset() method**

In `reset()` (line 121-127), add `_isWatchOnLearnScreen.value = false`:

```kotlin
    fun reset() {
        runOnMainThread {
            _wordData.value = WordData()
            _sessionData.value = SessionData()
            _writeTheWordState.value = WriteTheWordState()
            _isWatchOnLearnScreen.value = false
        }
    }
```

**Step 3: Verify the project compiles**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Add Waiting Overlay and Handshake to Phone LearnModeSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Add isWatchReady state variable**

After the `showResumeDialog` state variable (around line 208), add:

```kotlin
    // State for watch handshake - blocks session until watch is ready
    var isWatchReady by remember(sessionKey) { mutableStateOf(false) }
```

**Step 2: Add handshake LaunchedEffects**

After the existing `LaunchedEffect(sessionKey)` that calls `watchConnectionManager.notifyLearnModeStarted()` (around line 301-304), replace it with:

```kotlin
    // Notify watch when Learn Mode session starts and begin handshake
    LaunchedEffect(sessionKey) {
        watchConnectionManager.notifyLearnModeStarted()
        // Send initial phone_ready signal
        watchConnectionManager.sendLearnModePhoneReady()
        // Heartbeat: keep pinging every 2 seconds until watch responds
        while (!isWatchReady) {
            kotlinx.coroutines.delay(2000)
            if (!isWatchReady) {
                watchConnectionManager.sendLearnModePhoneReady()
            }
        }
    }

    // Listen for watch ready signal
    LaunchedEffect(sessionKey) {
        watchConnectionManager.learnModeWatchReady.collect { timestamp ->
            if (timestamp > 0L) {
                isWatchReady = true
            }
        }
    }
```

Note: This replaces the existing single `LaunchedEffect(sessionKey) { watchConnectionManager.notifyLearnModeStarted() }` block.

**Step 3: Gate word data sending behind isWatchReady**

Find the existing `LaunchedEffect(currentWordIndex, words)` that sends word data (around line 544). Change the condition to also require `isWatchReady`:

```kotlin
    // Send current word data to watch whenever current word changes (only after handshake)
    LaunchedEffect(currentWordIndex, words, isWatchReady) {
        if (!isWatchReady) return@LaunchedEffect
        val currentWord = words.getOrNull(currentWordIndex)
        if (currentWord != null) {
```

The rest of this LaunchedEffect body stays identical.

**Step 4: Add the waiting overlay UI**

After the resume dialog block (the `if (showResumeDialog) { ... }` block), and before the loading check (`if (isLoading) { ... }`), add:

```kotlin
    // Waiting for watch overlay - shown when !isWatchReady
    if (!isWatchReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Block clicks through overlay
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Spacer(Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.dis_pairing_tutorial),
                    contentDescription = "Waiting for watch",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Waiting for $studentName...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEDBB00),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Please open Learn Mode\non the watch to connect.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.weight(1f))

                // Cancel Session button
                Button(
                    onClick = {
                        watchConnectionManager.notifyLearnModeEnded()
                        onEarlyExit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel Session",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
```

**Step 5: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Add "Tap to Begin" Screen and Handshake to Watch LearnModeScreen

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`

**Step 1: Set isWatchOnLearnScreen in DisposableEffect**

Find the existing `DisposableEffect(Unit)` that sets `view.keepScreenOn` (around line 75-80). Add the screen presence flag:

```kotlin
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        LearnModeStateHolder.setWatchOnLearnScreen(true)
        onDispose {
            LearnModeStateHolder.setWatchOnLearnScreen(false)
            view.keepScreenOn = false
        }
    }
```

**Step 2: Add showWaitScreen state variable**

After the `lastSkipTime` state (around line 123), add:

```kotlin
    // "Tap to begin" wait screen - shown after handshake but before user taps
    var showWaitScreen by remember { mutableStateOf(true) }
```

**Step 3: Add watch ready signal and heartbeat LaunchedEffects**

After the existing `LaunchedEffect(wordData.timestamp)` block that resets feedback (around line 120), add:

```kotlin
    // Two-way handshake: send watch_ready when session becomes active
    LaunchedEffect(isPhoneInLearnMode) {
        if (isPhoneInLearnMode) {
            scope.launch {
                phoneCommunicationManager.sendLearnModeWatchReady()
            }
        }
    }

    // Heartbeat: keep sending watch_ready while no word data yet
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            if (wordData.word.isEmpty() && LearnModeStateHolder.isWatchOnLearnScreen.value) {
                scope.launch {
                    phoneCommunicationManager.sendLearnModeWatchReady()
                }
            }
        }
    }
```

**Step 4: Add showWaitScreen state to the when block**

In the `when` block (line 135), after the `!isPhoneInLearnMode` case (line 140-143) and before the `isFillInTheBlank` case (line 144), add a new case:

```kotlin
                    !isPhoneInLearnMode -> {
                        // Waiting state - phone hasn't started Learn Mode session yet
                        WaitingContent()
                    }
                    showWaitScreen -> {
                        // Tap to begin - after handshake, before user starts
                        WaitScreenContent(
                            onTap = {
                                showWaitScreen = false
                            },
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendSkipCommand()
                                    }
                                }
                            }
                        )
                    }
                    isFillInTheBlank && wordData.word.isNotEmpty() -> {
```

**Step 5: Add WaitScreenContent composable**

After the existing `WaitingContent()` composable (after line 265), add:

```kotlin
@Composable
private fun WaitScreenContent(
    onTap: () -> Unit,
    onSkip: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -50f) {
                        change.consume()
                        onSkip()
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tap to begin!",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = R.drawable.dis_watch_wait),
                contentDescription = "Tap to start",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
```

Note: Ensure `detectHorizontalDragGestures` is imported. Check existing imports — the file already imports `androidx.compose.foundation.gestures.detectHorizontalDragGestures` (line 7).

**Step 6: Verify the project compiles**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---
