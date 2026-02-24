# Gate Voice Playback on Resume Dialog State

## Problem

In both Learn Mode and Tutorial Mode, when a user returns to a session with existing progress, a resume/restart dialog is shown. However, voice playback (TTS in Learn Mode, pre-recorded audio in Tutorial Mode) starts immediately once `isWatchReady` becomes true, regardless of whether the dialog is still visible. The user hears the voice prompt before they've chosen whether to resume or restart.

### Root Cause

The voice playback `LaunchedEffect` blocks check `isWatchReady` and `currentWordIndex`/`currentStep`, but have no awareness of `showResumeDialog`. The resume check sets the word/step index and shows the dialog, but the watch handshake completes independently, triggering voice playback while the dialog is still on screen.

## Solution

Add `showResumeDialog` as a key to the voice playback `LaunchedEffect` in both modes and return early when it's `true`. When the user dismisses the dialog (Resume or Restart), `showResumeDialog` becomes `false`, which re-triggers the `LaunchedEffect` and voice plays for the correct word/letter.

## Changes

### Learn Mode (LearnModeSessionScreen.kt, ~line 584)

Current:
```kotlin
LaunchedEffect(currentWordIndex, words, isWatchReady) {
    if (!isWatchReady) return@LaunchedEffect
```

Proposed:
```kotlin
LaunchedEffect(currentWordIndex, words, isWatchReady, showResumeDialog) {
    if (!isWatchReady || showResumeDialog) return@LaunchedEffect
```

### Tutorial Mode (TutorialSessionScreen.kt, ~line 429)

Current:
```kotlin
LaunchedEffect(currentStep, currentLetter, isWatchReady) {
    if (currentLetter.isNotEmpty() && isWatchReady) {
```

Proposed:
```kotlin
LaunchedEffect(currentStep, currentLetter, isWatchReady, showResumeDialog) {
    if (currentLetter.isNotEmpty() && isWatchReady && !showResumeDialog) {
```

## Behavior

| Scenario | Before Fix | After Fix |
|----------|-----------|-----------|
| Fresh session (no prior progress) | Voice plays when watch ready | No change - voice plays when watch ready |
| Resume dialog showing | Voice plays immediately | Voice waits for dialog dismissal |
| User taps Resume | Voice already playing | Voice plays for resumed word/letter |
| User taps Restart | Voice already playing | Voice plays for first word/letter |

## Edge Cases

- **No prior progress:** `showResumeDialog` is never `true`, so voice plays normally.
- **Restart resets index:** `currentWordIndex`/`currentStep` changes to 0 AND `showResumeDialog` becomes `false`, both re-triggering the effect.
- **Watch not ready yet when dialog dismissed:** Voice still waits for `isWatchReady` as before.
