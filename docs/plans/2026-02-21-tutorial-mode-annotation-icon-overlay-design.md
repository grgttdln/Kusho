# Tutorial Mode Annotation Icon in Correct Overlay

**Date:** 2026-02-21
**Status:** Approved

## Goal

Copy the annotation icon UI from Learn Mode's correct overlay into Tutorial Mode's correct overlay, with the icon tinted yellow and the tooltip background yellow.

## Current State

- **Learn Mode** (`LearnModeSessionScreen.kt`): `ProgressCheckDialog` has a top-left annotation icon (`R.drawable.ic_annotate`, 28dp) wrapped in a `TooltipBox` with purple background. Clicking opens `LearnerProfileAnnotationDialog`.
- **Tutorial Mode** (`TutorialSessionScreen.kt`): `ProgressCheckDialog` has a blue "Add Annotations" text link below the encouragement text. No icon.

## Design

### Changes to Tutorial Mode's `ProgressCheckDialog`

1. **Add yellow annotation icon** in the top-left corner:
   - Position: `Alignment.TopStart`, padding 16dp start / 48dp top
   - Image: `R.drawable.ic_annotate`, 28dp
   - Color: `ColorFilter.tint(Color(0xFFCCDB00))` (yellow)

2. **Add TooltipBox** wrapping the icon:
   - Yellow tooltip card background (`Color(0xFFCCDB00)`)
   - Dark text for contrast
   - Text: "Click here to Add Note"
   - Controlled by `showAnnotationTooltip` boolean

3. **Remove** the "Add Annotations" text link currently below the encouragement text.

4. **Add parameters** to `ProgressCheckDialog`:
   - `showAnnotationTooltip: Boolean`
   - `onAnnotationTooltipDismissed: () -> Unit`
   - Keep existing `onAddAnnotation: () -> Unit`

5. **Wire up state** at the call site with a `showAnnotationTooltip` state variable.

### Unchanged

- Overlay remains non-dismissable (teacher-gated)
- "Continue" button stays
- Mascot image, title, encouragement text, case mismatch note, audio unchanged
- `LearnerProfileAnnotationDialog` shared component unchanged

## Approach

Direct copy of learn mode icon+tooltip block with yellow tinting via `ColorFilter`. No shared composable extraction (YAGNI).

## Files Modified

- `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`
