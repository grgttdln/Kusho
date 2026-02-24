# Air Writing Indicator Reliability Fix

## Problem

In Learn Mode, the "Student is air writing..." indicator sometimes fails to appear while the student is actively writing on the watch. When this happens, the End Session button remains visible instead. The bug worsens over time during a session, though letter input still arrives correctly.

## Root Cause

The watch sends a single `sendLearnModeGestureRecording()` message at the start of each recording. This message travels over Bluetooth via Wear OS `MessageClient`, which is fire-and-forget. If that one message is lost or delayed (Bluetooth latency, connection hiccup), the phone never learns the student is writing, so `isStudentWriting` stays `false` and the End Session button remains visible.

## Design

### Watch-Side: Periodic Heartbeats During Recording

**File:** `wear/.../learn/LearnModeViewModel.kt`

Currently, `onRecordingStarted()` is called once at the start of recording (line 159). The recording phase then runs a progress update loop every `PROGRESS_UPDATE_INTERVAL_MS`.

**Change:** Call `onRecordingStarted()` on each iteration of the recording progress loop, not just once at the start. This piggybacks on the existing loop — no new timers or threads needed.

The initial call at line 159 stays. The loop body adds an `onRecordingStarted()` call alongside each progress update. This means the watch sends ~4-6 heartbeat messages per recording cycle instead of 1.

### Phone-Side: Timeout-Based Auto-Reset

**File:** `app/.../learn/learnmode/LearnModeSessionScreen.kt`

Currently, the `LaunchedEffect(Unit)` at line 800 collects from `learnModeGestureRecording` and sets `isStudentWriting = true` with no timeout. It only resets when `letterInputEvent` arrives or Skip is pressed.

**Change:** Add a 3-second timeout mechanism using a coroutine `Job`:
- When a heartbeat arrives, set `isStudentWriting = true` and start/reset a 3-second countdown
- If another heartbeat arrives before 3 seconds, the countdown resets
- If no heartbeat for 3 seconds, auto-reset `isStudentWriting = false`
- The existing `letterInputEvent` handler continues to immediately reset `isStudentWriting = false` when a letter arrives (usually before the timeout fires)

### Color Fix

**File:** `app/.../learn/learnmode/LearnModeSessionScreen.kt`, line 1433

Change indicator text color from `PurpleColor` to `YellowColor` to match the design spec and Tutorial Mode implementation.

## Files Changed

1. `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt` — Add heartbeat in recording loop
2. `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` — Add timeout mechanism + fix color
