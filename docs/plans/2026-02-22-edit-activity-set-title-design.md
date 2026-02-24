# Edit Activity Set Title - Design

## Problem

There is no way to edit the title of an Activity Set (Activity entity) after creation. The `ActivitySetsScreen` displays the title as static text, and while the data layer already supports updates via `ActivityRepository.updateActivity()`, no UI exposes this functionality.

## Solution

Add an always-visible `OutlinedTextField` on `ActivitySetsScreen` that lets users edit the Activity Set title inline, with an explicit Save button.

## Architecture

### Approach: ViewModel-driven editable title

The `ActivitySetsViewModel` owns the title state. It loads the Activity entity on screen entry, exposes an editable title field in UI state, and provides a `saveTitle()` method that calls `ActivityRepository.updateActivity()`. A callback propagates the updated title back to `MainNavigationContainer`.

### Data Flow

```
ActivitySetsScreen
  |
  +-- LaunchedEffect(activityId) -> viewModel.loadActivity(activityId)
  |                                  viewModel.loadSetsForActivity(activityId)
  |
  +-- OutlinedTextField <-> uiState.editableTitle
  |     onValueChange -> viewModel.onTitleChanged(newValue)
  |
  +-- Save Button (visible when title changed and valid)
  |     onClick -> viewModel.saveTitle()
  |         -> ActivityRepository.updateActivity(activityId, title=newTitle)
  |         -> on success: onTitleUpdated(newTitle)
  |
  +-- Error text <- uiState.titleError
```

## UI Layout

The title section changes from static text to:

- `OutlinedTextField` (28sp, bold) containing the editable title
- "Activities" label below the text field as a separate Text composable
- Save button (blue, rounded) visible only when title differs from original
- Error text (red) shown inline when validation fails

## Files Changed

| File | Change |
|------|--------|
| `ActivitySetsViewModel.kt` | Add `ActivityRepository`, load activity, manage title state, `onTitleChanged()`, `saveTitle()` |
| `ActivitySetsUiState` (same file) | Add `editableTitle`, `originalTitle`, `titleError`, `isSaving`, `titleSaved` |
| `ActivitySetsScreen.kt` | Replace `Text` with `OutlinedTextField`, add Save button, wire `onTitleUpdated` callback |
| `MainNavigationContainer.kt` | Pass `onTitleUpdated` to `ActivitySetsScreen`, update `selectedActivityTitle` on callback |

## Validation & Error Handling

- Blank title: "Title cannot be empty", Save button disabled
- Over 30 chars: Input truncated at 30 characters in `onValueChange`
- Duplicate title: Error from repository after save attempt
- DB/network error: Generic error message, Save button stays enabled for retry

## Existing Infrastructure Used

- `ActivityRepository.updateActivity()` - already validates title length, duplicates, and handles DB update
- `ActivityDao.updateActivity()` - Room update method
- `ActivityDao.activityTitleExistsForUserExcluding()` - duplicate check excluding current activity
