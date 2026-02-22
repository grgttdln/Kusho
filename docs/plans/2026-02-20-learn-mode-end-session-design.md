# Learn Mode End Session Design

## Summary

Add an "End Session" feature to Learn Mode, mirroring Tutorial Mode's existing implementation. Teachers can exit a Learn Mode session early, save partial progress, and resume later.

## End Session Button & Confirmation Dialog

- "End Session" button at the bottom of `LearnModeSessionScreen`, same placement as Tutorial Mode
- Tapping shows a confirmation dialog with:
  - `dis_remove` drawable image
  - "Are you sure?" heading
  - Progress message: "You've completed X/Y words! Your progress and annotations will be saved."
  - Red "End Session" button (`#FF6B6B`) to confirm early exit
  - Blue "Cancel" button to dismiss and return to session
- Visual style matches `TutorialSessionScreen` dialog (lines 843-960)

## Data Layer

Extend `StudentSetProgress` entity with two new columns:

- `lastCompletedWordIndex: Int = 0` - index of last completed word (0-based)
- `correctlyAnsweredWordsJson: String? = null` - JSON set of correctly answered word indices (e.g., `"[0,2,5]"`)

New DAO queries on `StudentSetProgressDao`:

- `getInProgressSession(studentId, activityId, setId)` - returns progress where `isCompleted = false AND lastCompletedWordIndex > 0`
- `upsertPartialProgress(...)` - saves partial progress with computed `completionPercentage`
- `deleteInProgressSession(studentId, activityId, setId)` - clears in-progress state on "Start Over" or normal completion

Room auto-migration handles the schema change.

## Resume Dialog

On entering `LearnModeSessionScreen`, a `LaunchedEffect` checks for in-progress sessions. If found, shows a non-dismissable resume dialog:

- `dis_pairing_tutorial` drawable image
- "Welcome back!" heading in accent color
- "[Student name] has completed X/Y words. Would you like to continue where you left off?"
- Blue "Resume Session" button - restores `currentWordIndex` and `correctlyAnsweredWords`
- Orange "Start Over" button - resets to word 0, deletes in-progress record

## Early Exit Flow

When teacher confirms "End Session":

1. Save accumulated annotations to database
2. Generate AI annotation summary for completed words (if annotations exist)
3. Save partial progress to `StudentSetProgress` (`lastCompletedWordIndex`, `correctlyAnsweredWordsJson`, `completionPercentage`)
4. Notify watch: `watchConnectionManager.notifyLearnModeEnded()`
5. Navigate to Learn Screen (screen 1 from classroom, screen 0 from dashboard)

## Navigation Changes

- Add `onEarlyExit` callback to `LearnModeSessionScreen` alongside existing `onSessionComplete`
- Wire `onEarlyExit` in `MainNavigationContainer` to navigate to Learn Screen (classroom) or Dashboard (dashboard context)

## Files to Modify

- `StudentSetProgress.kt` - add two columns
- `StudentSetProgressDao.kt` - add three queries
- `LearnModeSessionScreen.kt` - add End Session button, confirmation dialog, resume dialog, save-progress-and-exit logic
- `MainNavigationContainer.kt` - wire `onEarlyExit` callback at screens 33 and 43
- Room database migration
