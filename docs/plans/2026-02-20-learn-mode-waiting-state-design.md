# Learn Mode Waiting State Design

## Summary

Add a two-way handshake waiting state to Learn Mode, mirroring Tutorial Mode's implementation. Both phone and watch wait for synchronization before starting the activity, with a "Tap to begin!" screen on the watch.

## Phone Side — Waiting Overlay

- `isWatchReady` state variable in `LearnModeSessionScreen`, defaults to `false`
- Full-screen dark overlay (0.7f alpha) blocks session content when `isWatchReady == false`
- Shows `dis_pairing_tutorial` image, "Waiting for [studentName]..." in yellow, instruction text, and "Cancel Session" button
- Cancel button calls `watchConnectionManager.notifyLearnModeEnded()` then `onEarlyExit()`
- Heartbeat: sends `sendLearnModePhoneReady()` immediately, then every 2 seconds until `isWatchReady`
- Listens to `learnModeWatchReady` StateFlow — sets `isWatchReady = true` when timestamp > 0
- Word data is gated behind `isWatchReady == true` (no data sent until handshake completes)

## WatchConnectionManager — New Protocol

New message paths:
- `/learn_mode_phone_ready` — phone → watch heartbeat ping
- `/learn_mode_watch_ready` — watch → phone acknowledgment

New StateFlow:
- `learnModeWatchReady: StateFlow<Long>` — timestamp updated when watch replies, reset to 0 on `notifyLearnModeStarted()`

New methods:
- `sendLearnModePhoneReady()` — sends ping to all connected nodes
- Handler for `/learn_mode_watch_ready` in `onMessageReceived` — updates StateFlow

## Watch Side — PhoneCommunicationManager

- `sendLearnModeWatchReady()` — sends `/learn_mode_watch_ready` to phone
- Handler for `/learn_mode_phone_ready` — replies with watch ready when on Learn Mode screen and session active

## Watch Side — LearnModeStateHolder

New singleton (mirrors TutorialModeStateHolder):
- `isWatchOnLearnScreen: MutableStateFlow<Boolean>` — gates watch replies to phone pings
- Set `true` on composition enter, `false` on dispose

## Watch Side — LearnModeScreen UI

New `showWaitScreen` state, defaults to `true`. State flow:

1. `WaitingContent` ("Waiting...") — before phone starts session (`!isPhoneInLearnMode`)
2. `WaitScreenContent` ("Tap to begin!") — after session starts, before user taps. Shows `dis_watch_wait` drawable. User taps to proceed, can swipe left to skip.
3. Activity modes (Fill in the Blank / Write the Word / Name the Picture) — after tap

When `sessionData.isActive` becomes true, watch sends `sendLearnModeWatchReady()`.

## Files to Modify

### Phone app:
- `WatchConnectionManager.kt` — new paths, StateFlow, methods, handler
- `LearnModeSessionScreen.kt` — `isWatchReady` state, waiting overlay, handshake LaunchedEffects, gate word data

### Wear app:
- `PhoneCommunicationManager.kt` — `sendLearnModeWatchReady()`, handler for phone_ready
- `LearnModeScreen.kt` — `showWaitScreen` state, `WaitScreenContent`, watch ready signal
- New file: `LearnModeStateHolder.kt` — screen presence gating
