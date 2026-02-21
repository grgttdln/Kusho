# Design: Move Annotation Icon to Correct Overlay

**Date:** 2026-02-21
**Status:** Approved
**File:** `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

## Summary

Remove the always-visible annotation icon from the Learn Mode toolbar. Instead, show it at the top-left of the ProgressCheckDialog (correct answer overlay), so teachers can add notes in context when a student gets a character right.

## What Changes

### Remove from toolbar
- The annotation `IconButton` (with tooltip) from the toolbar Row (~lines 1080-1142)
- The `showAnnotationTooltip` state and `LaunchedEffect` for tooltip auto-show (~lines 1063-1070)
- The toolbar Row shrinks to only contain the skip button

### Add to ProgressCheckDialog
- The annotation icon at `TopStart` alignment inside the existing full-screen `Box`
- The "Click here to Add Note" tooltip on first appearance
- New parameters: `showAnnotationTooltip: Boolean`, `onAnnotateClick: () -> Unit`, `onAnnotationTooltipDismissed: () -> Unit`

### Layout

```
ProgressCheckDialog (full-screen dark overlay Box):
  ┌──────────────────────────────────┐
  │ [Annotate Icon]                  │  ← TopStart, 16dp padding
  │  "Click here to Add Note"       │  ← Tooltip (first time)
  │                                  │
  │         (Mascot Image)           │  ← Center aligned
  │                                  │
  │      Great Job, John!            │
  │     You're doing super!          │
  │   Keep up the amazing work!      │
  │                                  │
  │   (Case mismatch note if any)    │
  │                                  │
  └──────────────────────────────────┘
```

### Behavior
- Tapping the annotation icon opens `LearnerProfileAnnotationDialog`
- The ProgressCheckDialog stays visible behind the annotation dialog
- Tapping anywhere else on the overlay still dismisses it (existing behavior)
- The annotation icon only appears during correct answers, not wrong answers

### What stays the same
- `LearnerProfileAnnotationDialog` composable (no changes)
- `annotationsMap` storage and `onAddNote` logic
- Skip button remains in the toolbar
- Watch-side behavior unchanged
- Tap-to-dismiss on overlay unchanged
