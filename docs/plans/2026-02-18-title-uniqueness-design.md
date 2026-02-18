# Design: Guaranteed Unique Titles in AI Generation

## Problem

When Kuu AI generates set and activity titles, it sometimes produces exact duplicates of existing titles. The app then fails at save time with "A set with this title already exists." The user has no way to recover without regenerating.

## Solution: Prompt Enforcement + App-Side Safety Net

### Approach

1. **Strengthen AI prompts** to explicitly forbid exact-duplicate titles (case-insensitive).
2. **AI provides `alternateTitle`** in its `titleSimilarity` response — a natural-sounding fallback.
3. **App-side dedup** in `parseStep3Response` catches any remaining duplicates and renames them.
4. **Existing similarity banner** continues to show for semantically-similar titles, unchanged.

### Prompt Changes

Steps 2, 2-regen, and 3 system instructions gain a hard rule:

> CRITICAL: Generated titles MUST NOT be identical (case-insensitive) to any existing title listed below. If the best title matches an existing one exactly, choose a different but related title.

The `titleSimilarity` response object gains an `alternateTitle` field:

```json
{ "similarTo": "existing title", "reason": "why similar", "alternateTitle": "unique alternative" }
```

### Data Model Changes

`TitleSimilarityResponse` (GeminiApiModels.kt): add `alternateTitle: String = ""`
`TitleSimilarity` (AiGeneratedData.kt): add `alternateTitle: String = ""`

### App-Side Safety Net in `parseStep3Response`

1. Accept `existingActivityTitles` and `existingSetTitles` as parameters.
2. For the activity title: if exact match exists, use AI's `alternateTitle`. If that's also a duplicate or empty, append " II", " III", etc. (capped at 15 chars).
3. For each set title: same logic against existing set titles.
4. Also dedup within the generated batch — if two generated sets have the same title, rename the second.

### Files Changed

| File | Change |
|------|--------|
| `GeminiApiModels.kt` | Add `alternateTitle` to `TitleSimilarityResponse` |
| `AiGeneratedData.kt` | Add `alternateTitle` to `TitleSimilarity` |
| `GeminiRepository.kt` — prompts | Hard "no duplicates" rule, `alternateTitle` in response format |
| `GeminiRepository.kt` — `parseStep3Response` | Dedup logic using alternateTitle then suffix fallback |

### What Stays the Same

- Orange similarity banner UI
- `SimilarTitleDialog` for activity titles
- ViewModel title caching
- Navigation flow
