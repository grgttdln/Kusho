# Watch Feedback: Phone as Single Source of Truth

**Date:** 2026-02-20
**Status:** Approved

## Problem

After a gesture on the watch, the ViewModel auto-transitions from `SHOWING_PREDICTION` to `RESULT` after 1.5 seconds, and the Screen immediately resets to IDLE. The phone's right/wrong feedback arrives over Bluetooth after this reset, so the user never sees it. The watch goes straight back to "Tap to begin" after every gesture.

## Root Cause

The watch ViewModel has a local state machine (`SHOWING_PREDICTION` -> `RESULT` -> `IDLE`) that races against the phone's network response. The Screen's `RESULT` handler calls `viewModel.resetToIdle()` immediately or renders idle content, so the watch is back at IDLE before the phone's `sendLearnModeFeedback()` message arrives.

## Design

### Approach: Remove RESULT State

Remove the ViewModel's local `RESULT` state and `isCorrect` computation entirely. The phone is the single source of truth for right/wrong determination. The watch's role is gesture recognition and display only.

### State Machine (After)

```
IDLE -> COUNTDOWN -> RECORDING -> PROCESSING -> SHOWING_PREDICTION -> (wait)
                                                                        |
                                                              phone feedback arrives
                                                              showingFeedback = true
                                                                        |
                                                              user/phone dismisses
                                                              showingFeedback = false
                                                              resetToIdle()
                                                                        |
                                                                      IDLE
```

### Changes

#### LearnModeViewModel.kt
- Remove `RESULT` from `State` enum
- Remove `isCorrect` from `UiState`
- Remove Phase 5 (RESULT transition and auto-reset timer) from `startRecording()`
- Add 8-second safety timeout after prediction: if phone doesn't respond, auto-reset to IDLE

#### LearnModeScreen.kt
- Remove all `State.RESULT ->` branches from `when` blocks in FillInTheBlankMainContent, WriteTheWordMainContent, NameThePictureMainContent
- Fix `LaunchedEffect(showingFeedback)` to only call `resetToIdle()` on actual dismissal (feedback going from true to false), not on initial composition

### Safety

An 8-second timeout after the prediction prevents the watch from getting stuck if the phone never responds (e.g., Bluetooth disconnection, phone app crash).

### Files Changed
- `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeViewModel.kt`
- `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`
