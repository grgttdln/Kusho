# Wear App: Word-Complete Correct Overlay for Write the Word & Name the Picture

## Problem

In the wear app's Learn Mode, the correct/wrong feedback image (`dis_watch_correct`/`dis_watch_wrong`) shows after every individual letter in Write the Word and Name the Picture modes. The phone app only shows the correct overlay after the entire word is completed. The wear app should match this behavior.

## Design

### Approach: Dual-path feedback at the UI layer

Differentiate per-letter feedback from word-complete feedback using the existing `writeTheWordState.isWordComplete` flag. All changes are local to `LearnModeScreen.kt` in the wear module. No phone-side changes needed.

### Per-letter feedback (Write the Word & Name the Picture)

When `showingFeedback` is true and `writeTheWordState.isWordComplete` is false:

- **Correct letter**: Show the predicted letter in green, large and centered, for ~1 second. Auto-dismiss and reset to idle for the next letter.
- **Wrong letter**: Show the predicted letter in red, large and centered, for ~1 second. Auto-dismiss and reset to idle for retry.

No image (`dis_watch_correct`/`dis_watch_wrong`), no "Waiting for teacher..." state for per-letter feedback.

Auto-dismiss is implemented via a `LaunchedEffect` in `LearnModeScreen` keyed on `showingFeedback` and `writeTheWordState.isWordComplete`. When `isWordComplete` flips to true (for the last letter), the effect cancels and the word-complete overlay takes over.

### Word-complete overlay (Write the Word & Name the Picture)

When `writeTheWordState.isWordComplete` becomes true:

- Show `dis_watch_correct` image (existing drawable)
- Play TTS affirmation (e.g., "Great job!")
- Transition to "Waiting for teacher..." after 2 seconds
- Stay visible until teacher dismisses from phone (teacher-gated)

### Last letter timing

Three messages arrive in quick succession for the final letter:
1. `/learn_mode_letter_result` - advances letter index
2. `/learn_mode_show_feedback` - sets `showingFeedback = true` (green letter briefly appears)
3. `/learn_mode_word_complete` - sets `isWordComplete = true` (cancels auto-dismiss, switches to word-complete overlay)

The user sees the last letter flash green momentarily, then the celebration overlay appears.

### Fill in the Blank - unchanged

Single masked letter mode. Current behavior (image + "Waiting for teacher...") stays as-is.

## Component Changes

All in `wear/.../presentation/learn/LearnModeScreen.kt`:

1. **`LearnModeScreen`** - Add `LaunchedEffect` for auto-dismiss of per-letter feedback (1s delay, keyed on `showingFeedback` + `isWordComplete`, scoped to Write the Word / Name the Picture)
2. **`WriteTheWordMainContent`** - Branch on `isWordComplete`: word-complete overlay vs. per-letter colored letter
3. **`NameThePictureMainContent`** - Same change as WriteTheWordMainContent
4. **New `PerLetterFeedbackContent`** - Shows predicted letter in green (`#4CAF50`) or red (`#F44336`), large and centered
5. **New `WordCompleteFeedbackContent`** - Shows `dis_watch_correct` image, plays TTS affirmation, transitions to "Waiting for teacher..." after 2s
6. **`LearnModeFeedbackContent`** - Unchanged, still used by Fill in the Blank
