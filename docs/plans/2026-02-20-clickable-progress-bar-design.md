# Clickable Progress Bar Segments in Learn Mode

## Problem

The progress bar segments in learn mode are not interactive. Students cannot navigate to a specific item by tapping its segment.

## Design

### Approach: Add `onSegmentClick` callback to `ProgressIndicator`

Add an optional `onSegmentClick: ((Int) -> Unit)? = null` parameter. When non-null, each segment Box gets a `.clickable` modifier that invokes the callback with the segment index.

### Files Changed

1. **`ProgressIndicator.kt`** — Add `onSegmentClick` parameter. When non-null, apply `.clickable { onSegmentClick(index) }` to each segment Box.

2. **`LearnModeSessionScreen.kt`** — Pass `onSegmentClick` lambda that cancels any ongoing wrong-letter animation and sets `currentWordIndex` to the clicked index. The existing `LaunchedEffect(currentWordIndex, words)` handles state reset, watch sync, and TTS automatically.

### Behavior

- Clicking any segment navigates to that item (including green/completed ones)
- Purple highlight moves to the clicked item
- Green items stay green when navigated away from (they remain in `correctlyAnsweredWords`)
- Ongoing wrong-letter animations are cancelled on click
- Backward-compatible: default `null` means no click behavior for other callers (tutorial mode)

### Color States (unchanged)

| State | Color |
|---|---|
| Correctly answered | Green (#4CAF50) |
| Current item | Purple (#AE8EFB) |
| Unanswered | Light purple (#E7DDFE) |
