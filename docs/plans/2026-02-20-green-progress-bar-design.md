# Green Progress Bar for Answered Items in Learn Mode

## Problem

The progress bar in learn mode uses only two states (active purple / inactive light purple). There's no visual distinction between items the student has already answered correctly and the item currently being worked on.

## Design

### Approach: Add `completedSteps` and `completedColor` to `ProgressIndicator`

Add two optional parameters to the existing `ProgressIndicator` composable for backward compatibility.

### Three-State Segment Logic

| Segment index         | Condition              | Color                              |
|-----------------------|------------------------|------------------------------------|
| `index < completedSteps` | Answered correctly     | Green (`completedColor` / `#4CAF50`) |
| `index < currentStep`    | Current / in-progress  | Purple (`activeColor` / `#AE8EFB`)  |
| `index >= currentStep`   | Not yet reached        | Light purple (`inactiveColor` / `#E7DDFE`) |

### Files Changed

1. **`ProgressIndicator.kt`** — Add `completedSteps: Int = 0` and `completedColor: Color = activeColor` parameters. Update segment coloring logic to check `completedSteps` first.

2. **`LearnModeSessionScreen.kt`** — Pass `completedSteps = currentWordIndex` and `completedColor = Color(0xFF4CAF50)` to the `ProgressIndicator` call.

### Backward Compatibility

Tutorial mode and any other callers are unaffected since `completedSteps` defaults to `0` (no green segments) and `completedColor` defaults to `activeColor`.
