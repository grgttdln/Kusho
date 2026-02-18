# Improved Merged Set UI Design

**Date:** 2026-02-18
**Status:** Approved

## Problem

When a teacher merges an AI-generated set into an existing set, the word list filters down to only the new words. The existing set's contents disappear, leaving teachers blind to what's already in the set they're merging into. There's no clear indication they're modifying a saved set.

## Goal

Show the existing set's contents alongside new words with clear labeling, full editability, and explicit messaging that the teacher is modifying an existing set.

## Design

### UI Layout (Merged State)

When a teacher taps "Merge into existing," the `SetReviewCard` transforms:

#### 1. Persistent Header Bar
Replaces the current green merge confirmation banner.

- Green background (`#E8F5E9`), green border (`#A5D6A7`), rounded corners (12dp)
- Line 1: Pencil icon + `Editing "CVC -at family"` (bold, `#2E7D32`)
- Line 2: `Changes will update this existing set` (grey, `#666666`)
- No undo button. Discard This Set is the escape hatch.

#### 2. Title + Description Fields
Pre-filled with the existing set's title and description. Fully editable with existing 15/30 character limits.

#### 3. "Already in set (N words)" Section
- Collapsible with chevron toggle (down/right arrow)
- Starts expanded so teachers see existing contents immediately
- Existing words rendered using the standard `WordReviewItem` composable
- Fully editable: configuration type, letter selection, and removable (X button)
- Words indexed 1 through N

#### 4. "Adding to set (M new words)" Section
- Always visible, non-collapsible
- Section header includes "Regenerate" button (regenerates only this section)
- New words rendered using the standard `WordReviewItem` composable
- Fully editable with same capabilities as existing words
- Words indexed N+1 through N+M (continuous numbering from existing section)
- "Add More Words" button appears below this section

#### 5. Action Buttons
Unchanged: Discard This Set, Add More Set, Proceed.

### Visual Layout (ASCII)

```
+----------------------------------------------+
|  [pencil] Editing "CVC -at family"           |  green header bar
|  Changes will update this existing set       |
+----------------------------------------------+
|                                              |
|  Add a Set Title                             |
|  +----------------------------------------+ |
|  | CVC -at family                         | |
|  +----------------------------------------+ |
|                                              |
|  Add Description                             |
|  +----------------------------------------+ |
|  | Words ending in -at                    | |
|  +----------------------------------------+ |
|                                              |
|  [v] Already in set (5 words)                |  collapsible, starts expanded
|  +----------------------------------------+ |
|  | 1  c a t    [Fill in the Blank v]  [x] | |
|  | 2  h a t    [Fill in the Blank v]  [x] | |
|  | 3  m a t    [Write the Word v]     [x] | |
|  | 4  s a t    [Fill in the Blank v]  [x] | |
|  | 5  b a t    [Name the Picture v]   [x] | |
|  +----------------------------------------+ |
|                                              |
|  Adding to set (2 new words)   [Regenerate]  |
|  +----------------------------------------+ |
|  | 6  r a t    [Fill in the Blank v]  [x] | |
|  | 7  v a t    [Write the Word v]     [x] | |
|  +----------------------------------------+ |
|                                              |
|  [+ Add More Words]                          |
|  [Discard This Set]                          |
|  [Add More Set]  [Proceed]                   |
+----------------------------------------------+
```

### Data Layer Changes

#### 1. New DAO Query
`getSetWordsWithDetails(setId: Long)` - Returns each word's name, configuration type, selected letter index, and image status for a given set.

#### 2. New Repository Method
`getExistingSetWords(setId: Long, userId: Long): List<EditableWord>` - Fetches existing set words from DB and maps them to `EditableWord` objects suitable for the review UI.

#### 3. EditableSet Model Update
Add new fields:
- `existingWords: List<EditableWord> = emptyList()` - The existing set's words (separate from the generated `words` list)
- `existingDescription: String? = null` - The existing set's description (for pre-filling)

#### 4. New Save Method
`updateExistingSetWithMerge(setId, userId, existingWords, newWords, title, description)` replaces `addWordsToExistingSet` for the merge path. Within a single DB transaction:
1. Update set metadata (title, description, updatedAt)
2. Sync existing words: update changed configurations, delete removed words
3. Add new words (skip duplicates)
4. Recalculate and update itemCount

### Behavior Matrix

| Action | Scope | Details |
|--------|-------|---------|
| Edit word config | Both sections | Change Fill in the Blank / Name the Picture / Write the Word |
| Edit letter selection | Both sections | Select which letter is blanked |
| Remove word | Both sections | Existing word removal = deletion on save; new word removal = won't be added |
| Regenerate | New words only | AI regenerates "Adding to set" words; existing words untouched |
| Add More Words | New words section | Opens word picker; added words go to "Adding to set" |
| Discard | Entire set | Removes set from review entirely |
| Proceed | Full save | Syncs existing words + adds new words + updates metadata |

### Undo Behavior
Removed. Once a teacher taps "Merge into existing," the decision is final within the review screen. To back out, they use "Discard This Set."

The "Create as new" path also has no undo; choosing it dismisses the overlap banner and proceeds as a normal new set.

### What Stays the Same
- UNDECIDED state blue overlap banner (unchanged)
- CREATE_NEW path behavior (unchanged)
- Title similarity banners (orange, unchanged)
- AI generation pipeline (unchanged)
- `WordReviewItem` composable (reused as-is for both sections)
- "Add More Set" button behavior (unchanged)

### Merge Flow (Step by Step)

1. AI generation completes, overlap detection runs
2. Teacher sees blue "Similar to X" banner with "Merge into existing" and "Create as new" buttons
3. Teacher taps "Merge into existing"
4. System fetches existing set's words + description from DB
5. UI transforms: green header bar, pre-filled title/description, two word sections
6. Teacher edits freely: changes configs, removes words, adds words
7. Teacher taps "Regenerate" if needed (only regenerates new words section)
8. Teacher taps "Proceed" -> full save in single transaction

### Edge Cases

- **All new words removed:** Save still works. Updates existing set only (config changes, deletions, title/description edits).
- **All existing words removed:** Save still works. Adds new words, updates metadata. Existing set ends up with only the new words.
- **No changes made:** Save still works. Effectively a no-op for existing words, adds new words.
- **Large existing set:** Collapsible section handles this. Teacher can collapse to focus on new words.
