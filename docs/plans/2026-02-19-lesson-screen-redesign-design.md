# LessonScreen Redesign: Air Writing Activities Hub

**Date:** 2026-02-19
**Status:** Approved

## Summary

Redesign LessonScreen from a combined hub + word bank screen into a clean navigation hub with 3 full-width tiles. Extract all Word Bank functionality into a new standalone WordBankScreen.

## Changes

### 1. LessonScreen (screen 3) - Navigation Hub

**Title:** "Air Writing Activities" (was "Customize Activities")

**Layout:** Matches LearnScreen pattern - vertically stacked full-width tiles.

- Kusho logo at top (existing)
- "Air Writing Activities" title (28sp, bold)
- 3 full-width rectangular tiles (160dp height each, 20dp gaps):
  - **Word Bank** → navigates to new WordBankScreen (screen 52)
  - **Your Activities** → navigates to YourActivitiesScreen (screen 6)
  - **Your Sets** → navigates to YourSetsScreen (screen 7)
- All tiles use `#5DB7FF` blue background
- Each tile uses the ModeCard pattern: background image fills card, title text bottom-left, white circle arrow button bottom-right
- Placeholder images: `ic_tutorial.png` for Word Bank, `ic_learn.png` for Activities, `ic_tutorial.png` for Sets
- Bottom nav bar (selectedTab = 3)

**Removed from LessonScreen:**
- Word Bank grid and list
- Add Word Bank button
- Magic wand AI button
- All modals (WordBankModal, WordBankEditModal, WordAddedConfirmationModal, ActivityCreationModal)
- LessonViewModel dependency

### 2. New WordBankScreen (screen 52)

**Layout:** Matches YourActivitiesScreen/YourSetsScreen inner-page pattern.

- Header: back arrow (left) + Kusho logo (centered). No edit button in header.
- "Word Bank" title (28sp, bold)
- WordBankList (2-column LazyVerticalGrid) - tap word to edit
- Empty state with mascot image when no words
- Action buttons above bottom nav: "+ Word Bank" button + magic wand button
- Bottom nav bar (selectedTab = 3)
- Modals: WordBankModal, WordBankEditModal, WordAddedConfirmationModal, ActivityCreationModal

**ViewModel:** Reuses existing LessonViewModel (already has all word bank logic).

### 3. Navigation Wiring

| Action | From | To | Screen ID |
|--------|------|----|-----------|
| Tap "Word Bank" tile | LessonScreen | WordBankScreen | 52 |
| Tap "Your Activities" tile | LessonScreen | YourActivitiesScreen | 6 |
| Tap "Your Sets" tile | LessonScreen | YourSetsScreen | 7 |
| Back button | WordBankScreen | LessonScreen | 3 |

**MainNavigationContainer changes:**
- Add `52 -> WordBankScreen(...)` to the `when` block
- Add `onNavigateToWordBank` callback to LessonScreen
- Wire WordBankScreen with `onNavigate`, `onBackClick`, `onNavigateToAIGenerate`

**LessonScreen signature changes:**
- Remove `viewModel` parameter
- Add `onNavigateToWordBank: () -> Unit`
- Keep `onNavigateToActivities`, `onNavigateToSets`
- Remove `onNavigateToAIGenerate` (moves to WordBankScreen)

## Files Affected

- `LessonScreen.kt` - Major rewrite (hub layout)
- `WordBankScreen.kt` - New file (extracted word bank)
- `MainNavigationContainer.kt` - Add screen 52, update LessonScreen wiring
