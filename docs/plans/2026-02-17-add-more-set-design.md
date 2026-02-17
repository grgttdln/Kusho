# Add More Set — Design Document

## Overview

When a user taps "Add More Set" on the AISetReviewScreen, a Kuu-styled modal (matching `ActivityCreationModal`) appears, letting the user generate an additional set using the same AI pipeline (Steps 2-3: Group + Configure). The prompt is pre-filled from the original generation, words already used in existing sets are visually marked, and unused words are pre-selected.

## User Flow

1. User taps "Add More Set" button on the last set of AISetReviewScreen
2. **AddMoreSetModal** appears (Kuu mascot, blue header, same style as `ActivityCreationModal`)
3. Modal state:
   - Prompt field pre-filled with original prompt, fully editable
   - Word chips show all words from the original word pool
   - Unused words are pre-selected; used words are de-emphasized with "(in Set X)" label
   - "Select all" selects only unused words
4. User optionally edits prompt and adjusts word selection (minimum 3 words)
5. User taps "Do the magic!" button
6. AI runs Steps 2 (Group) and 3 (Configure), skipping Step 1 (uses cached filtered words)
7. Phase progress shows: "Grouping sets... (1/2)" then "Configuring... (2/2)"
8. On success: Modal dismisses, new `EditableSet` appended to list, overlap detection runs, view navigates to new set
9. On error: Error text below button, user can retry

## Button Enable/Disable Logic

- "Add More Set" button on AISetReviewScreen is **disabled** when fewer than 3 unused words remain in the pool
- Disabled styling: subdued colors (same pattern as other disabled buttons)
- Inside the modal: "Do the magic!" disabled when fewer than 3 words selected

## Architecture

### New Component: `AddMoreSetModal`

Located in the `generate` package alongside `AISetReviewScreen.kt`. Mirrors `ActivityCreationModal` structure:
- Kuu mascot overlapping top of card
- Blue header section
- Title: "Add a New Set with Kuu"
- Prompt input field (pre-filled, editable)
- Word selection chips with used-word visual states
- "Do the magic!" button with phase progress
- Error display

### Word Chip States

| State | Background | Border | Text Color | Extra |
|-------|-----------|--------|------------|-------|
| Available + selected | `#49A9FF` (solid blue) | none | White | — |
| Available + unselected | White | `#49A9FF` | `#49A9FF` | — |
| Already used + unselected | White | `#E0E0E0` (light gray) | `#999999` | "(in Set X)" subtitle |
| Already used + selected | `#49A9FF` at 60% opacity | none | White | "(in Set X)" subtitle |

### API Layer

**`GeminiRepository.generateAdditionalSet()`** — new method:
```kotlin
suspend fun generateAdditionalSet(
    prompt: String,
    availableWords: List<Word>,
    excludeWords: List<String>,
    existingSetTitles: List<String>,
    onPhaseChange: suspend (GenerationPhase) -> Unit
): AiGenerationResult
```

- Uses cached filtered words from Step 1 (falls back to all words)
- Removes `excludeWords` from pool (unless user explicitly re-selected them)
- Passes `existingSetTitles` to Step 2 prompt so AI avoids creating duplicate groupings
- Runs Step 2 (Group) constrained to produce exactly 1 set
- Runs Step 3 (Configure) as normal
- Returns `AiGenerationResult` with a single set

**`LessonViewModel.generateAdditionalSet()`** — new wrapper:
```kotlin
fun generateAdditionalSet(
    prompt: String,
    excludeWords: List<String>,
    existingSetTitles: List<String>,
    onResult: (String?) -> Unit
)
```

Uses `lastSelectedWords` and delegates to repository.

### Navigation & State Changes in MainNavigationContainer

- New state: `showAddMoreSetModal: Boolean`
- New callback on AISetReviewScreen: `onAddMoreSetClick` triggers `showAddMoreSetModal = true`
- Modal receives: original prompt, all words, list of used words per set
- On generation success:
  1. Parse result JSON into `EditableSet`
  2. Run `setRepository.findOverlappingSets()` on new set
  3. Append to `aiEditableSets`
  4. Set `currentAiSetIndex` to new set index
  5. Dismiss modal

### Data Passed to Modal

```kotlin
AddMoreSetModal(
    isVisible = showAddMoreSetModal,
    initialPrompt = lastGenerationPrompt,
    allWords = allWordsFromOriginalPool,        // Word entities
    usedWordsPerSet = Map<String, List<String>>, // setTitle -> [word names]
    isLoading = isGeneratingAdditionalSet,
    generationPhase = currentPhase,
    error = additionalSetError,
    onPromptChanged = { ... },
    onWordSelectionChanged = { ... },
    onSelectAll = { ... },
    onGenerate = { prompt, selectedWords, excludeWords, existingTitles -> ... },
    onDismiss = { showAddMoreSetModal = false }
)
```

## Error Handling

- **< 3 words selected in modal:** "Do the magic!" button disabled
- **< 3 unused words in pool:** "Add More Set" button disabled on review screen
- **API failure:** Error text below button: "Failed to generate set. Please try again."
- **Empty AI result:** "Couldn't create a new set from selected words. Try different words."
- **Network error:** Standard retry-able error with message

## Files to Create/Modify

1. **Create:** `AddMoreSetModal.kt` in `ui/feature/learn/generate/`
2. **Modify:** `GeminiRepository.kt` — add `generateAdditionalSet()` method
3. **Modify:** `LessonViewModel.kt` — add `generateAdditionalSet()` wrapper
4. **Modify:** `AISetReviewScreen.kt` — wire `onAddMoreSetClick` callback, pass used words info
5. **Modify:** `MainNavigationContainer.kt` — add modal state, handle generation result, overlap detection, navigation
