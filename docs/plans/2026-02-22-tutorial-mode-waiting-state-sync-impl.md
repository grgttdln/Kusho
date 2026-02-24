# Fix Tutorial Mode "Waiting" State Sync Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the bug where the watch shows "Waiting..." in Tutorial Mode even when the phone session is active.

**Architecture:** Mirror Learn Mode's pattern — use `PhoneCommunicationManager.isPhoneInTutorialMode` (set by message handlers) as the gate for the waiting screen, instead of `TutorialModeStateHolder.sessionData.isActive` (which gets cleared by `resetSession()`).

**Tech Stack:** Kotlin, Jetpack Compose, Wear OS, StateFlow

---

### Task 1: Update PhoneCommunicationManager phone_ready handler

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt:316-326`

**Step 1: Add `_isPhoneInTutorialMode.value = true` to the phone_ready handler**

In the `MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY` handler (line 316), add the unconditional state update before the screen check, mirroring the Learn Mode handler at line 327-342.

Change this:

```kotlin
            MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY -> {
                // Only reply if watch user is actually on the Tutorial Mode screen
                if (com.example.kusho.presentation.tutorial.TutorialModeStateHolder.isWatchOnTutorialScreen.value) {
```

To this:

```kotlin
            MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY -> {
                // Phone is sending phone_ready pings, so it's in Tutorial Mode.
                // Set this unconditionally so new PhoneCommunicationManager instances
                // (created when watch re-enters TutorialModeScreen) learn the phone's state
                // even if they missed the original /tutorial_mode_started message.
                _isPhoneInTutorialMode.value = true
                // Only reply if watch user is actually on the Tutorial Mode screen
                if (com.example.kusho.presentation.tutorial.TutorialModeStateHolder.isWatchOnTutorialScreen.value) {
```

---

### Task 2: Update TutorialModeScreen to use isPhoneInTutorialMode

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeScreen.kt:52,69,154-161,210`

**Step 1: Add isPhoneInTutorialMode state collection**

After line 52 (`val phoneCommunicationManager = remember { PhoneCommunicationManager(context) }`), add:

```kotlin
    val isPhoneInTutorialMode by phoneCommunicationManager.isPhoneInTutorialMode.collectAsState()
```

**Step 2: Update handshake LaunchedEffect trigger**

Change lines 154-161 from:

```kotlin
    // Two-way handshake: send watch_ready when session becomes active.
    LaunchedEffect(sessionData.isActive) {
        if (sessionData.isActive) {
            scope.launch {
                phoneCommunicationManager.sendTutorialModeWatchReady()
            }
        }
    }
```

To:

```kotlin
    // Two-way handshake: send watch_ready when session becomes active.
    LaunchedEffect(isPhoneInTutorialMode) {
        if (isPhoneInTutorialMode) {
            scope.launch {
                phoneCommunicationManager.sendTutorialModeWatchReady()
            }
        }
    }
```

**Step 3: Update waiting screen gate in the when block**

Change line 210 from:

```kotlin
                    !sessionData.isActive -> {
                        // Waiting for phone to start session
                        WaitingContent()
                    }
```

To:

```kotlin
                    !isPhoneInTutorialMode -> {
                        // Waiting for phone to start session
                        WaitingContent()
                    }
```

---

### Task 3: Verify build compiles

**Step 1: Build the wear module**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL
