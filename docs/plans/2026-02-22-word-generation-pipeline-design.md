# AI Word Generation Pipeline for WordBankGenerationModal

## Date: 2026-02-22

## Summary

Wire the "Generate CVC Words" button in `WordBankGenerationModal` to a full end-to-end pipeline: Gemini generates CVC words based on the teacher's prompt and stepper count, the app applies a lightweight CVC regex safety check and deduplication against the existing word bank, valid words are batch-inserted into the DB, and the modal transitions to a success state showing the added words.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Validation strategy | Gemini self-validates + CVC regex safety net | Strong prompt engineering handles most validation; regex catches edge cases for free. No DictionaryValidator needed. |
| Shortfall handling | Over-generate and trim | Request ~50% extra from Gemini. If fewer pass, show partial result with note. No retry loops. |
| Success UI | Simple success list | Checkmark, count, word list, "Done" button. Minimal friction. |
| Architecture | New method in GeminiRepository | Follows existing patterns. No new classes or files. |

## New Repository Method

`GeminiRepository.generateCVCWords(prompt: String, count: Int, existingWords: List<Word>): List<String>`

- Uses `gemini-2.5-flash-lite` with JSON response format
- System prompt defines CVC rules explicitly (3 letters: consonant-vowel-consonant, real English words)
- Includes existing word bank words in prompt so Gemini avoids duplicates
- Requests `ceil(count * 1.5)` candidates to account for any that fail validation
- Returns raw `List<String>` of candidate words (lowercase, trimmed)
- Uses existing `retryStep()` pattern for error handling

### Prompt Structure

```
System: You generate CVC (Consonant-Vowel-Consonant) words for early readers (ages 4-8).

Rules:
- Each word must be exactly 3 letters: consonant, vowel, consonant
- Consonants: b, c, d, f, g, h, j, k, l, m, n, p, q, r, s, t, v, w, x, y, z
- Vowels: a, e, i, o, u
- Every word must be a real, common English word a child would know
- Do NOT include any of these existing words: [list]

Return a JSON array of exactly N lowercase words. No duplicates.

User: [teacher's prompt, e.g. "short vowel 'a' words about animals"]
```

## New ViewModel State

```kotlin
// In LessonUiState
val isWordGenerationLoading: Boolean = false,
val wordGenerationError: String? = null,
val generatedWords: List<String> = emptyList(),
val wordGenerationRequestedCount: Int = 0,
```

The modal visibility, prompt text, and word count stepper state already exist in the current UI state.

## New ViewModel Methods

### `generateWords()`

1. Set `isWordGenerationLoading = true`, `wordGenerationError = null`, `generatedWords = emptyList()`
2. Store `wordGenerationRequestedCount = current stepper value`
3. Fetch existing words via `wordRepository.getWordsForUserOnce(userId)`
4. Call `geminiRepository.generateCVCWords(prompt, count, existingWords)`
5. Filter results through `WordValidator.isCVCPattern()` — safety net
6. Remove any that exist in current word bank (case-insensitive comparison)
7. Take first `count` valid words
8. For each valid word: call `wordDao.insertWord(Word(userId, word))` — skip on conflict
9. Set `generatedWords = addedWords`, `isWordGenerationLoading = false`
10. If zero words added: set `wordGenerationError = "No valid words could be generated. Try a different prompt."`

### `clearWordGenerationState()`

Resets all generation state fields to defaults. Called when modal is dismissed.

## UI States

### State 1: Input (default)
- Prompt text field
- Word count stepper + preset chips
- Suggested prompts chips
- "Generate CVC Words" button (enabled when prompt is non-empty)

### State 2: Loading
- All inputs disabled
- Button replaced with spinner + phase text
- Phase text: "Generating words..." → "Adding to word bank..."

### State 3: Success (`generatedWords.isNotEmpty()`)
- Checkmark icon (existing app icon or simple composable)
- Header: "X words added!" (where X = generatedWords.size)
- If `generatedWords.size < wordGenerationRequestedCount`: subtitle "Some words were duplicates or invalid and were skipped"
- Scrollable column of word chips/items showing each added word
- "Done" button → calls `clearWordGenerationState()` + dismiss modal

### Error State
- Error text displayed below button (existing error pattern)
- "Try Again" resets error and re-enables inputs

## Data Flow

```
Teacher enters prompt + sets count via stepper
    ↓
Taps "Generate CVC Words"
    ↓
LessonViewModel.generateWords()
    ↓
1. wordRepository.getWordsForUserOnce(userId)
   → List<Word> (existing word bank)
    ↓
2. geminiRepository.generateCVCWords(prompt, count, existingWords)
   → List<String> (candidate words from Gemini)
    ↓
3. candidates.filter { WordValidator.isCVCPattern(it) }
   → List<String> (CVC-valid words)
    ↓
4. filtered.filterNot { existing.any { e -> e.word.equals(it, ignoreCase = true) } }
   → List<String> (non-duplicate words)
    ↓
5. validated.take(count)
   → List<String> (final words to add)
    ↓
6. For each word: wordDao.insertWord(Word(userId, word))
   → Words persisted in DB
    ↓
7. Set generatedWords = addedWords
   → Modal shows success state
    ↓
8. Teacher taps "Done"
   → Modal dismissed, word bank Flow auto-refreshes
```

## Files Changed

| File | Change |
|------|--------|
| `GeminiRepository.kt` | Add `generateCVCWords()` method |
| `LessonViewModel.kt` | Add generation state fields, `generateWords()`, `clearWordGenerationState()` |
| `WordBankGenerationModal.kt` | Wire generate button, add loading/success/error UI states |
| `MainNavigationContainer.kt` | Pass new ViewModel callbacks to modal |
| `LessonScreen.kt` | Connect modal state if generation is triggered from here |

No new files created. No new dependencies.

## Edge Cases

| Case | Handling |
|------|----------|
| Gemini returns fewer words than requested | Accept what's available, show partial success |
| All words fail CVC check | Show error: "No valid words could be generated" |
| All words are duplicates | Show error: "All generated words already exist in your word bank" |
| Network failure | Show error from Gemini call, allow retry |
| Empty prompt | Generate button disabled (existing validation) |
| Word bank is empty | `existingWords` is empty list — Gemini has no dedup constraints |

## Scope

- Add `generateCVCWords()` to `GeminiRepository.kt`
- Add generation state and methods to `LessonViewModel.kt`
- Wire generation flow and UI states in `WordBankGenerationModal.kt`
- Update `MainNavigationContainer.kt` wiring
- Update `LessonScreen.kt` wiring if needed
