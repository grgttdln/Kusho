# White Question Mark for Non-Air-Written Operations

## Summary

In Learn mode and Tutorial mode, when the user does not air-write (no significant wrist motion detected), the watch should display a white `?` as the predicted letter instead of the current "Oops!" mascot screen. The `?` flows through the normal prediction pipeline — sent to the phone, marked incorrect, and teacher/phone feedback proceeds as usual. Practice mode remains unchanged.

## Current Behavior

| Mode | No-movement handling |
|------|---------------------|
| Learn Mode | `NO_MOVEMENT` state -> "Oops!" mascot (`dis_ops`) for 3s -> sends `"?"` to phone -> auto-returns to idle |
| Tutorial Mode | No motion detection — always classifies regardless |
| Practice Mode | `NO_MOVEMENT` state -> "Oops!" mascot -> user retries |

## Target Behavior

| Mode | No-movement handling |
|------|---------------------|
| Learn Mode | `SHOWING_PREDICTION` with `prediction = "?"` -> white `?` at 80sp -> sends `"?"` to phone -> normal feedback flow |
| Tutorial Mode | Add motion check -> `SHOWING_PREDICTION` with `prediction = "?"` -> white `?` at 80sp -> sends incorrect result to phone -> normal feedback flow |
| Practice Mode | **No change** — keeps "Oops!" mascot |

## Changes Required

### 1. LearnModeViewModel.kt

Replace the no-motion branch: instead of transitioning to `State.NO_MOVEMENT` (show mascot for 3s, auto-return to idle), transition to `State.SHOWING_PREDICTION` with `prediction = "?"`. Then follow the same pipeline as a normal prediction (the phone already receives `"?"` and marks it wrong).

### 2. TutorialModeViewModel.kt

Add `hasSignificantMotion()` method (same gyroscope variance/range logic as LearnModeViewModel). After recording, check for motion. On failure, set `State.SHOWING_PREDICTION` with `prediction = "?"` and call `onGestureResult(isCorrect = false, predictedLetter = "?")`.

### 3. LearnModeScreen.kt

In the three `LaunchedEffect` blocks (Fill in the Blank, Write the Word, Name the Picture) that trigger TTS on `NO_MOVEMENT`, change the trigger to fire when `state == SHOWING_PREDICTION && prediction == "?"`. This preserves the "Oops, you did not air write!" TTS voice. The `LearnNoMovementContent` composable will no longer be invoked.

### 4. TutorialModeScreen.kt

Add TTS trigger when `state == SHOWING_PREDICTION && prediction == "?"` to speak "Oops, you did not air write!".

### 5. Practice Mode (No changes)

`PracticeModeViewModel.kt` and `PracticeModeScreen.kt` remain untouched.

## Data Flow (No-Motion Case, After Change)

```
User taps -> Countdown -> Recording -> Motion check fails
    |
    v
ViewModel: state = SHOWING_PREDICTION, prediction = "?"
    |
    v
UI: white "?" at 80sp (reuses existing ShowingPredictionContent)
TTS: "Oops, you did not air write!"
    |
    v
Phone receives "?" -> marks incorrect -> sends feedback
    |
    v
Watch shows feedback (wrong) -> normal flow continues
```

## Testing

- Learn mode (Fill in the Blank): no-motion shows white `?`, TTS fires, phone receives `?`, feedback returns
- Learn mode (Write the Word): same as above, per-letter progress updated
- Learn mode (Name the Picture): same as above
- Tutorial mode: no-motion shows white `?`, TTS fires, phone receives incorrect result, teacher proceeds
- Practice mode: unchanged — "Oops!" mascot still shown
