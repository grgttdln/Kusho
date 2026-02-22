# Practice Mode: No-Movement Fallback Design

## Problem

When a user holds their wrist still during the 3-second recording window in practice mode, the model still runs inference on static sensor noise and predicts a random letter. There is no check for whether the user actually moved their wrist to air-write.

## Solution

Add variance-based wrist movement detection in `PracticeModeViewModel`. If no significant motion is detected after recording, skip classification and show a fallback "oops" screen with `dis_ops.png`.

## Architecture

### New State: `NO_MOVEMENT`

Added to `PracticeModeViewModel.State` enum, between `PROCESSING` and `SHOWING_PREDICTION` in the flow.

### Motion Detection

**Function:** `hasSignificantMotion(samples: List<SensorSample>): Boolean`

- Computes per-channel variance across all 6 sensor channels (ax, ay, az, gx, gy, gz)
- Sums variances to get total motion energy
- Returns `false` if total variance < `MOTION_VARIANCE_THRESHOLD`
- Starting threshold: `0.5f` (needs tuning on device)
- Logs computed variance for debugging/tuning

**Location:** Private function in `PracticeModeViewModel`. Can be extracted to a utility later for learn/tutorial modes.

### Flow Change

```
Recording stops
    |
    v
Get samples from sensor manager
    |
    v
[NEW] Check hasSignificantMotion(samples)
    |
    +--> false --> NO_MOVEMENT state --> dis_ops.png + TTS "Oops" --> retry same question
    |
    +--> true --> existing flow (check sample count, classify, show prediction, show result)
```

### UI: NoMovementContent Composable

- Centered `dis_ops.png` image (full screen, similar to result mascots)
- Entire screen tappable to retry immediately
- Auto-returns to `QUESTION` state with same question after 3 seconds
- TTS speaks "Oops, you did not air write!" on entry

### Files Changed

1. `PracticeModeViewModel.kt` - Add `NO_MOVEMENT` state, `hasSignificantMotion()`, motion check in recording flow, auto-retry logic
2. `PracticeModeScreen.kt` - Add `NoMovementContent` composable, TTS LaunchedEffect for NO_MOVEMENT state, wire into state `when` block

### UiState Changes

No new fields needed. The existing `currentQuestion` is preserved when entering `NO_MOVEMENT` so the same question is used on retry.

### Future

The `hasSignificantMotion()` function is designed to be easily extracted to a shared utility for reuse in learn and tutorial modes.
