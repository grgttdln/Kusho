# Design: Add More Set Button in AI Review Screen

## Problem

The "Add More Set" button on the last set's review page is a TODO stub. Teachers want to generate additional sets to add to the activity without regenerating all existing sets.

## Solution: Reuse regenerateSet Pipeline

### Data Flow

1. User taps "Add More Set" on last set page
2. AISetReviewScreen calls `onAddMoreSet` callback
3. MainNavigationContainer calls `lessonViewModel.addMoreSet()`
4. LessonViewModel calls `geminiRepository.regenerateSet()` with all existing set titles as avoidance context
5. On success: result parsed into EditableSet, overlap/similarity detection runs on just the new set, appended to editableSets
6. User stays on current page — set count updates, user swipes forward to see new set

### Design Decisions

- **Word reuse:** Any words are fine — the AI picks the best grouping even if words overlap with existing sets
- **Navigation:** Stay on current page after generation, don't auto-navigate to new set
- **No new API functions:** Reuses existing `regenerateSet` pipeline with expanded avoidance context

### Files Changed

| File | Change |
|------|--------|
| `LessonViewModel.kt` | Add `addMoreSet()` function that calls `regenerateSet` with all existing titles |
| `MainNavigationContainer.kt` | Wire callback, parse result, run overlap/similarity on new set, append to editableSets |
| `AISetReviewScreen.kt` | Wire button to callback, add `isAddingMoreSet` loading state, disable button while generating |
