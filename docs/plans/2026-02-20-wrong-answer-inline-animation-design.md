# Wrong Answer Inline Animation Design

## Problem

When the user writes an incorrect letter via the watch in LearnModeSessionScreen, a `ProgressCheckDialog` pops up with "Not quite, [Name]!" text and a mascot image. This interrupts the flow. Instead, the wrong letter should appear inline in the active blank slot, turn red, shake, play a wrong sound, then disappear — and the question repeats.

## Approach

Single coroutine sequence (Approach B) orchestrating the full animation timeline using Compose `Animatable` APIs. All timing logic lives in one `LaunchedEffect` block.

## State Variables

New state added to `LearnModeSessionScreen`:

- `wrongLetterText: String` — the incorrect letter character
- `wrongLetterAnimationActive: Boolean` — whether animation is running
- `wrongLetterShakeOffset: Animatable<Float>` — horizontal translation for shake
- `wrongLetterAlpha: Animatable<Float>` — opacity for fade-out

## Animation Timeline (~3 seconds)

| Time | Phase | Description |
|------|-------|-------------|
| 0.0s | Appear | Wrong letter fills active blank slot in red (#FF6B6B), alpha = 1.0 |
| 0.0–0.5s | Hold | Letter visible for 500ms so user sees what they wrote |
| 0.5–2.0s | Shake | 5-6 horizontal oscillation cycles, amplitude decays from ~12dp |
| 2.0–3.0s | Fade out | Alpha 1.0 to 0.0 with EaseOut easing |
| 3.0s | Reset | Animation state cleared, blank slot restored, watch notified for retry |

## Audio

Plays concurrently with animation (does not block):
1. `wrong.mp3` starts at 0.0s
2. On completion, a random affirmative from `wrongAffirmatives` list plays

Uses `MediaPlayer` with the same pattern as the existing `ProgressCheckDialog`.

## Input Blocking

While `wrongLetterAnimationActive` is true, incoming `letterInputEvent` values are discarded to prevent overlapping animations.

## UI Changes

- Active blank slot renders `wrongLetterText` instead of blank during animation
- Applies `Modifier.offset(x = shakeOffset.dp)` and `Modifier.alpha(alpha)` during animation
- Text color = `Color(0xFFFF6B6B)` (red)

## Dialog Changes

- `ProgressCheckDialog` is **skipped** for incorrect answers — inline animation replaces it
- `ProgressCheckDialog` is **preserved** for correct answers (no change)

## Incorrect Answer Handling Changes

For all three modes (Fill in the Blank, Write the Word, Name the Picture):

**Before:**
```
showProgressCheckDialog = true
isCorrectGesture = false
predictedLetter = event.letter.toString()
```

**After:**
```
wrongLetterText = event.letter.toString()
wrongLetterAnimationActive = true
// pendingCorrectAction still set for retry logic
```

## Watch Communication

- `sendLearnModeFeedback(false, letter)` — still called immediately on wrong answer
- `notifyLearnModeFeedbackDismissed()` — called when animation completes (replaces dialog dismiss)
- Word data resent to watch after animation completes (same retry behavior via `pendingCorrectAction`)

## Files Modified

- `LearnModeSessionScreen.kt` — animation state, coroutine sequence, UI rendering, incorrect answer branches
