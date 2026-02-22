# Learn Mode: "Is Air Writing" Typing Indicator

## Summary

Copy the existing Tutorial Mode "is air writing" indicator into Learn Mode. When a student begins gesture recording on the watch, the teacher's phone displays a pulsing "[Name] is air writing..." message and locks the UI until the gesture result arrives.

## Approach

Dedicated Learn Mode message path (`/learn_mode_gesture_recording`), mirroring Tutorial Mode's existing `/tutorial_mode_gesture_recording` pattern. Keeps both modes independent.

## Communication Layer

### Watch Side

**PhoneCommunicationManager.kt:**
- New constant: `MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING = "/learn_mode_gesture_recording"`
- New method: `sendLearnModeGestureRecording()` — sends empty ByteArray to phone when recording starts (same pattern as `sendTutorialModeGestureRecording()`)

**LearnModeViewModel.kt:**
- Add `onRecordingStarted` callback parameter
- Call `onRecordingStarted()` when transitioning from COUNTDOWN to RECORDING state (after countdown finishes, before `sensorManager.startRecording()`)

### Phone Side

**WatchConnectionManager.kt:**
- New constant: `MESSAGE_PATH_LEARN_MODE_GESTURE_RECORDING = "/learn_mode_gesture_recording"`
- New StateFlow: `_learnModeGestureRecording: MutableStateFlow<Long>` / `learnModeGestureRecording: StateFlow<Long>`
- New handler in `onMessageReceived()`: sets StateFlow value to `System.currentTimeMillis()` on receiving the message

## UI Layer (LearnModeSessionScreen.kt)

### State

- New variable: `var isStudentWriting by remember { mutableStateOf(false) }`

### Listening for Recording Signal

- `LaunchedEffect(Unit)` collecting from `watchConnectionManager.learnModeGestureRecording`
- Timestamp-based debouncing: checks `timestamp > sessionStartTime` and `timestamp > lastRecordingTime`
- Sets `isStudentWriting = true` when valid signal received

### Reset Triggers

- `isStudentWriting = false` when gesture result arrives (existing letter input event handler)
- Also reset on: session end, word complete transitions, error states

### UI Lockdown (when isStudentWriting = true)

- Progress bar: alpha 0.35, segment clicks disabled
- Navigation controls (arrows, card stack toggle): alpha 0.35, buttons disabled
- Annotation button: disabled
- End Session button area replaced with pulsing text

### Writing Indicator Display

- Text: `"${studentName.ifEmpty { "Student" }} is air writing..."`
- Color: YellowColor (0xFFEDBB00) with pulsing alpha animation
- Animation: `infiniteRepeatable` tween, 1f to 0.4f alpha, 800ms cycle, `RepeatMode.Reverse`
- Container: `Box` with `fillMaxWidth()`, `height(56.dp)`, centered content
- Replaces End Session button while active

## Files Changed

| File | Change |
|------|--------|
| `wear/.../PhoneCommunicationManager.kt` | Add message path constant + send method |
| `wear/.../LearnModeViewModel.kt` | Add `onRecordingStarted` callback, call at recording start |
| `wear/.../LearnModeScreen.kt` | Pass `onRecordingStarted` callback through to ViewModel |
| `app/.../WatchConnectionManager.kt` | Add StateFlow + message handler |
| `app/.../LearnModeSessionScreen.kt` | Add state, listener, UI lockdown, pulsing text |
