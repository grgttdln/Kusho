# Activity Creation: Auto-Select All Words + Suggested Prompts

**Date**: 2026-02-22
**Status**: Design
**Scope**: Two changes to the Activity Creation flow

---

## 1. Auto-Select All Words (Already Done)

Word selection UI removed from `ActivityCreationModal`. `LessonViewModel.createActivity()` now uses `_uiState.value.words` (all words). The `selectedActivityWordIds` field was removed from `LessonUiState`.

No further work needed for this item.

## 2. Add AI-Generated Suggested Prompts to Activity Creation Modal

### 2.1 GeminiRepository

Add `generateActivitySuggestedPrompts(words: List<Word>): List<String>`:

- Only handles non-empty word bank (empty case handled by UI — see 2.4)
- Analyzes existing words using `analyzeCVCWords`/`detectCVCPatterns`
- System instruction focuses on activity grouping strategies and learning goals (not CVC pattern gaps like word generation prompts)
- Returns exactly 2 prompt strings
- Reuses `SuggestedPromptsResponse` data class and `isValidPromptSuggestion()` validation
- Uses same `createModel()` with `gemini-2.5-flash-lite`

### 2.2 LessonViewModel

New cache fields:
- `cachedActivitySuggestedPrompts: List<String>`
- `cachedActivityWordCountForPrompts: Int`

New method `loadActivitySuggestedPrompts()`:
- Mirrors `loadSuggestedPrompts()` pattern
- Cache key: word bank size
- Calls `geminiRepository.generateActivitySuggestedPrompts()`
- Updates UI state

### 2.3 LessonUiState

New fields:
- `activitySuggestedPrompts: List<String> = emptyList()`
- `isActivitySuggestionsLoading: Boolean = false`

### 2.4 ActivityCreationModal UI

New parameters:
- `words: List<Word>` (needed to check empty state)
- `suggestedPrompts: List<String>`
- `isSuggestionsLoading: Boolean`
- `onSuggestionClick: (String) -> Unit`

**Empty word bank state**: When `words.isEmpty()`, show a message like "Add words to your Word Bank first to create activities!" instead of the input/button UI. The modal still opens but shows this empty state.

**Suggested prompts section**: Between the activity input and the magic button. Same visual style as `WordBankGenerationModal`:
- "Need ideas? Start with these!" header
- Loading spinner while fetching
- `FlowRow` with wand-icon suggestion chips
- Clicking a chip auto-fills the activity input field

### 2.5 YourSetsScreen (Call Site)

- Wire new parameters from `lessonUiState` to `ActivityCreationModal`
- Add `LaunchedEffect` on `lessonUiState.isActivityCreationModalVisible` to trigger `loadActivitySuggestedPrompts()` when modal opens

---

## Files Changed

| File | Change |
|------|--------|
| `GeminiRepository.kt` | Add `generateActivitySuggestedPrompts()` |
| `LessonViewModel.kt` | Add cache fields, `loadActivitySuggestedPrompts()`, new UI state fields |
| `ActivityCreationModal.kt` | Add suggested prompts UI section, empty word bank state, new parameters |
| `YourSetsScreen.kt` | Wire new parameters, add LaunchedEffect trigger |
