# Air Writing Indicator Reliability Fix — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the "Student is air writing..." indicator reliably appear whenever the student is writing on the watch, by sending periodic heartbeats instead of a single message, and auto-clearing the indicator with a timeout.

**Architecture:** Watch sends `onRecordingStarted()` on every iteration of the recording progress loop (not just once). Phone resets a 3-second timeout on each heartbeat, auto-clearing `isStudentWriting` if heartbeats stop. Also fixes indicator color from PurpleColor to YellowColor.

**Tech Stack:** Kotlin, Jetpack Compose, Wear OS MessageClient, Coroutines (Job, delay)

---

### Task 1: Add periodic heartbeat during watch recording loop

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt:167-173`

**Step 1: Add `onRecordingStarted()` call inside the recording loop**

In `startRecording()`, the recording progress loop is at lines 167-173:

```kotlin
// BEFORE (lines 167-173):
while (isActive && updateCount < totalUpdates) {
    delay(PROGRESS_UPDATE_INTERVAL_MS)
    updateCount++
    _uiState.update {
        it.copy(recordingProgress = updateCount.toFloat() / totalUpdates)
    }
}
```

Change to:

```kotlin
// AFTER:
while (isActive && updateCount < totalUpdates) {
    delay(PROGRESS_UPDATE_INTERVAL_MS)
    updateCount++
    onRecordingStarted() // Heartbeat: re-notify phone that student is still writing
    _uiState.update {
        it.copy(recordingProgress = updateCount.toFloat() / totalUpdates)
    }
}
```

The existing `onRecordingStarted()` call at line 159 (before the loop) stays as-is. It sends the first signal immediately when recording starts. The loop then sends follow-up heartbeats on each progress tick.

**Step 2: Verify the app builds**

Run: `./gradlew :wear:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add timeout-based auto-reset on phone side

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:75-77` (imports)
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:799-809` (LaunchedEffect)

**Step 1: Add missing imports**

Add these two imports alongside the existing coroutine imports at lines 75-77:

```kotlin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
```

**Step 2: Replace the gesture recording LaunchedEffect with timeout logic**

The current code at lines 799-809:

```kotlin
// BEFORE:
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

Replace with:

```kotlin
// AFTER:
// Listen for gesture recording heartbeat from watch (student is writing)
// Watch sends repeated signals during recording. A 3-second timeout auto-clears
// the indicator if heartbeats stop (e.g., watch disconnects mid-write).
LaunchedEffect(Unit) {
    val sessionStartTime = System.currentTimeMillis()
    var lastRecordingTime = 0L
    var writingTimeoutJob: Job? = null
    watchConnectionManager.learnModeGestureRecording.collect { timestamp ->
        if (timestamp > sessionStartTime && timestamp > lastRecordingTime && timestamp > 0L) {
            lastRecordingTime = timestamp
            isStudentWriting = true

            // Reset timeout on each heartbeat
            writingTimeoutJob?.cancel()
            writingTimeoutJob = launch {
                delay(3000L)
                isStudentWriting = false
            }
        }
    }
}
```

**Step 3: Verify the app builds**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Fix indicator text color

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1433`

**Step 1: Change PurpleColor to YellowColor**

Line 1433 currently reads:

```kotlin
color = PurpleColor.copy(alpha = writingAlpha),
```

Change to:

```kotlin
color = YellowColor.copy(alpha = writingAlpha),
```

`YellowColor` is already defined at line 86 of the same file as `Color(0xFFEDBB00)`. This matches the design spec and the Tutorial Mode implementation in `TutorialSessionScreen.kt:1013`.

**Step 2: Verify the app builds**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Manual verification on devices

**Step 1: Install on phone and watch**

Run: Deploy both `:app` and `:wear` modules to connected devices via Android Studio.

**Step 2: Verify indicator appears reliably**

1. Start a Learn Mode session with a student
2. Have the student write 5-10 letters
3. Confirm the "Student is air writing..." indicator appears every time the student writes
4. Confirm the indicator text is yellow (not purple)
5. Confirm the End Session button reappears after each letter is processed

**Step 3: Verify timeout auto-clears**

1. Start a recording on the watch
2. While recording, turn off the watch (or kill the watch app)
3. Confirm the indicator disappears within ~3 seconds on the phone
4. Confirm the End Session button reappears

**Step 4: Verify existing flows still work**

1. Skip button still works and clears the indicator
2. End Session button and confirmation dialog still work
3. Progress bar navigation still works (disabled while writing, enabled otherwise)
