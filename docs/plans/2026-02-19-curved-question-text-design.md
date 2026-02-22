# Curved Question Text in Practice Mode

## Problem

Practice mode shows a centered letter/emoji/avatar but the question text (e.g., "Can you trace the letter A?") is only spoken via audio — not visible on screen.

## Solution

Add curved text along the top arc of the watch face in `QuestionContent` using Wear OS `CurvedLayout` + `curvedText`.

## Scope

- **All four question categories**: Tracing/Copying, Uppercase/Lowercase, Letter Sound, Picture Match
- **File changed**: `PracticeModeScreen.kt` — only the `QuestionContent` composable

## Design

- Use `CurvedLayout` with `anchor = 270f` (top center) as an overlay in a `Box`
- `curvedText` renders `question.question` string
- White text, ~12sp, to avoid competing with the large centered content
- Existing centered content (letter, emoji, avatar) stays unchanged

## Approach chosen

Wear OS `CurvedLayout` + `curvedText` (official API) over custom Canvas `drawCurvedText()` because:
- Purpose-built for edge-of-screen text on round watches
- Less code than manual Canvas arc math
- Handles screen shape variations automatically

## Alternatives considered

1. **Reuse ArcDrawing.kt `drawCurvedText()`** — more control but overkill for a simple label
2. **Android `drawTextOnPath()`** — deprecated, inconsistent rendering
