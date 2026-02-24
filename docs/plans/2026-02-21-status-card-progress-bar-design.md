# LearnModeStatusCard Progress Bar Badge

## Problem

The LearnModeStatusCard currently shows a simple text badge with only two states: "Not Started" or "Completed". There is no visual indication of partial progress through a set.

## Solution

Replace the existing text badge at the bottom of the LearnModeStatusCard with a fill-bar badge that shows progress as a proportional fill within the rounded badge shape, with overlaid text.

## Three Visual States

| State | Condition | Bar fill | Text | Star icon |
|-------|-----------|----------|------|-----------|
| Not Started | percentage == 0 | Empty (border only) | "Not Started" | `ic_star_notstarted` |
| In Progress | 1 <= percentage <= 99 | Proportional fill | "X% Progress" | `ic_star_notstarted` |
| Completed | percentage == 100 | Full fill | "Completed" | `ic_star_completed` |

## Data Flow Changes

### ActivitySetStatus (LearnModeSetStatusScreen.kt)

Add `completionPercentage: Int` field:

```kotlin
data class ActivitySetStatus(
    val setId: Long,
    val title: String,
    val status: String,
    val completionPercentage: Int = 0
)
```

### LearnModeStatusCard (LearnModeStatusCard.kt)

Add `completionPercentage: Int = 0` parameter. Derive the status text and fill fraction from this value internally.

### MainNavigationContainer.kt

Two call sites (around lines 630 and 758) build `ActivitySetStatus` objects. They already have access to `progressList: List<StudentSetProgress>`. Instead of only checking `isCompleted`, also look up `completionPercentage` for each set and pass it through.

## UI Implementation

Replace the current badge Box (lines 71-95 of LearnModeStatusCard.kt) with:

1. **Outer Box**: Rounded rectangle with purple border (`Color(0xFFBA9BFF)`), same horizontal padding as current
2. **Fill layer**: Inner Box with `fillMaxWidth(fraction = percentage / 100f)`, background `Color(0xFFAE8EFB)`, clipped to the same rounded shape
3. **Text layer**: Centered text overlaid on top, showing "Not Started", "X% Progress", or "Completed"

Color rules (matching existing scheme):
- Not Started (default): Purple border, transparent background, purple text
- In Progress: Purple border, proportional purple fill, white text on filled portion
- Completed (not selected): Full purple fill, white text
- Selected state: White background fill, purple text (same as current selected behavior)

## Files to Modify

1. `app/.../ui/components/learnmode/LearnModeStatusCard.kt` — Add parameter, replace badge with fill-bar
2. `app/.../ui/feature/learn/learnmode/LearnModeSetStatusScreen.kt` — Add field to `ActivitySetStatus`
3. `app/.../ui/feature/home/MainNavigationContainer.kt` — Map `completionPercentage` from progress data (two call sites)
