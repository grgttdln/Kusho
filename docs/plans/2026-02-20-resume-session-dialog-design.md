# Resume Session Dialog Redesign

## Summary

Replace the inline resume dialogs in Learn Mode and Tutorial Mode with a shared `ResumeSessionDialog` composable that matches the new card-based UI design. White card with blue banner, circular mascot overlapping the top, and side-by-side Resume/Restart buttons.

## Component: ResumeSessionDialog

**Location:** `app/src/main/java/com/example/app/ui/components/common/ResumeSessionDialog.kt`

**Parameters:**
- `mascotDrawable: Int` — `R.drawable.dis_pairing_learn` or `R.drawable.dis_pairing_tutorial`
- `accentColor: Color` — Circle border color (Purple for Learn, Yellow for Tutorial)
- `studentName: String`
- `completedCount: Int`
- `totalCount: Int`
- `unitLabel: String` — `"words"` or `"letters"`
- `onResume: () -> Unit`
- `onRestart: () -> Unit`

## Layout Structure

```
┌──────────────────────────────┐  Dark overlay (0.7 alpha)
│                              │
│    ┌──────────────────────┐  │
│    │   ╭──────────╮       │  │  Blue banner (#3FA9F8)
│    │   │ (mascot) │       │  │  Mascot in accent-colored circle border
│    │   ╰──────────╯       │  │
│    ├──────────────────────┤  │  White card body
│    │   Welcome Back       │  │  Black, bold, 22sp
│    │                      │  │
│    │   [Name] completed   │  │  Gray text, bold fraction
│    │   X/Y [unit].        │  │
│    │   Continue where     │  │
│    │   you left off?      │  │
│    │                      │  │
│    │  ┌────────┐┌────────┐│  │  Side-by-side buttons
│    │  │ Resume ││Restart ││  │  Blue filled / Orange outlined
│    │  └────────┘└────────┘│  │
│    └──────────────────────┘  │
│                              │
└──────────────────────────────┘
```

## Color Scheme

| Element | Color |
|---------|-------|
| Banner | `#3FA9F8` (blue) |
| Circle border (Learn) | `#AE8EFB` (purple) |
| Circle border (Tutorial) | `#EDBB00` (yellow) |
| "Welcome Back" text | `Color.Black` |
| Body text | `Color.Gray` |
| Bold fraction | `Color.Black` (via AnnotatedString) |
| Resume button fill | `#3FA9F8` (blue) |
| Resume button text | `Color.White` |
| Restart button border | `#FF8C42` (orange) |
| Restart button text | `#FF8C42` (orange) |
| Card background | `Color.White` |

## Files to Modify

- **Create:** `app/src/main/java/com/example/app/ui/components/common/ResumeSessionDialog.kt`
- **Modify:** `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` — replace inline dialog with component
- **Modify:** `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt` — replace inline dialog with component
