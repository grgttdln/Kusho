# Tutorial Mode Completion Loop with Auto-Advance

**Date:** 2026-02-21
**Status:** Approved
**Branch:** update/tutorial-mode-overhaul

## Problem

In tutorial mode, when items are skipped or answered incorrectly, the session can end without all items being completed. The teacher must manually navigate back to unanswered items. The congrats/completed screen should only appear when ALL items are answered correctly, and the session should automatically loop back to unanswered items.

## Requirements

1. **Completion gate**: Congrats screen only shows when every item in the activity is answered correctly
2. **Auto-loop**: When the end of the item list is reached, automatically navigate to the first unanswered item
3. **Revisit auto-advance**: On revisit (student previously attempted this item), if correct → auto-mark complete and advance to next unanswered (no ProgressCheckDialog)
4. **Revisit retry-lock**: On revisit, if wrong → stay on the same item until correct (no skip, no dialog)
5. **First-attempt behavior unchanged**: ProgressCheckDialog (teacher-gated) still shows on first attempts
6. **Items are "not done" from**: incorrect gesture results OR teacher skipping

## Design

### New State

```kotlin
var attemptedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
```

- `completedIndices: Set<Int>` — items answered correctly (green in progress bar)
- `attemptedIndices: Set<Int>` — items the student has tried air-writing at least once
- Neither — items skipped by teacher or not yet reached

`attemptedIndices` is session-only (not persisted to database). On resume from saved progress, all incomplete items are treated as first-attempt.

### Gesture Result Handler

When `tutorialModeGestureResult` is collected:

```
Mark currentStep-1 as attempted (add to attemptedIndices)

IF item was already in attemptedIndices BEFORE this attempt (revisit):
  IF correct:
    - Add to completedIndices
    - Send feedback to watch
    - Immediately send feedback dismiss to watch
    - If all items complete → completeTutorialAndEnd()
    - Else → auto-navigate to next unanswered item
    - No ProgressCheckDialog shown
  IF wrong:
    - Send feedback to watch
    - Wait ~1.5s (student sees wrong feedback briefly)
    - Auto-dismiss feedback on watch
    - Auto-retry on same item (send retry + resend letter data)
    - No ProgressCheckDialog shown

ELSE (first attempt):
  - Show ProgressCheckDialog as today (teacher-gated)
  - Current behavior unchanged
```

### Skip Button

Current behavior stops at last item. New behavior wraps around:

```kotlin
val maxSteps = calculatedTotalSteps
val nextIncomplete = ((currentStep) until maxSteps)
    .firstOrNull { it !in completedIndices }
    ?: (0 until currentStep - 1)
        .firstOrNull { it !in completedIndices }

if (nextIncomplete != null) {
    currentStep = nextIncomplete + 1
}
```

Skip does NOT mark the item as attempted (student didn't write).

### Watch Side

No changes needed. Phone is SSOT and calls the same watch APIs:
- `sendTutorialModeFeedback()` — watch shows feedback overlay
- `notifyTutorialModeFeedbackDismissed()` — watch dismisses feedback
- `notifyTutorialModeRetry()` — watch resets for next attempt
- `sendTutorialModeLetterData()` — watch shows next letter

### Edge Cases

- **Teacher navigates to completed item via grid**: Item is already green. If student writes, it's a no-op (stays complete).
- **All items attempted but not all completed**: Loop cycles through incomplete items with auto-advance/retry.
- **Single remaining item**: Student stays on it until correct, then congrats screen shows.
- **Session resume**: `attemptedIndices` resets (not persisted). All incomplete items get first-attempt behavior.

## Files Changed

- `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt` — all changes are in this file

## Scope Exclusions

- No watch-side changes
- No database schema changes
- No new UI components
- `attemptedIndices` not persisted
