# Green Progress Bar for Answered Items — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make completed/answered progress bar segments turn green in learn mode, while the current item stays purple and future items stay light purple.

**Architecture:** Add two optional parameters (`completedSteps`, `completedColor`) to the existing `ProgressIndicator` composable. The segment coloring logic gains a third state. Only the learn mode caller passes these new params; tutorial mode is unaffected.

**Tech Stack:** Jetpack Compose, Kotlin

---

### Task 1: Add `completedSteps` and `completedColor` parameters to `ProgressIndicator`

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/common/ProgressIndicator.kt:21-48`

**Step 1: Add the two new parameters to the composable signature**

Change the function signature at line 21 to add `completedSteps` and `completedColor` after `inactiveColor`:

```kotlin
@Composable
fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = DefaultActiveColor,
    inactiveColor: Color = DefaultInactiveColor,
    completedSteps: Int = 0,
    completedColor: Color = activeColor,
    height: Dp = 8.dp,
    spacing: Dp = 8.dp,
    cornerRadius: Dp = 4.dp
)
```

**Step 2: Update the segment coloring logic**

Replace the current two-state logic (lines 35-44):

```kotlin
repeat(totalSteps) { index ->
    val isActive = index < currentStep
    Box(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (isActive) activeColor else inactiveColor
            )
    )
}
```

With this three-state logic:

```kotlin
repeat(totalSteps) { index ->
    val segmentColor = when {
        index < completedSteps -> completedColor
        index < currentStep -> activeColor
        else -> inactiveColor
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(segmentColor)
    )
}
```

**Step 3: Verify previews still compile**

The existing previews don't pass `completedSteps`, so they default to `0` and behave identically to before. No changes needed.

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/common/ProgressIndicator.kt
git commit -m "feat: add completedSteps/completedColor to ProgressIndicator for three-state segments"
```

---

### Task 2: Pass green completed color from `LearnModeSessionScreen`

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:824-830`

**Step 1: Update the ProgressIndicator call**

Replace the current call at line 824:

```kotlin
ProgressIndicator(
    currentStep = currentStep,
    totalSteps = totalWords,
    modifier = Modifier.fillMaxWidth(),
    activeColor = PurpleColor,
    inactiveColor = LightPurpleColor
)
```

With:

```kotlin
ProgressIndicator(
    currentStep = currentStep,
    totalSteps = totalWords,
    modifier = Modifier.fillMaxWidth(),
    activeColor = PurpleColor,
    inactiveColor = LightPurpleColor,
    completedSteps = currentWordIndex,
    completedColor = Color(0xFF4CAF50)
)
```

Note: `currentStep` is `currentWordIndex + 1` (defined at line 796), so `completedSteps = currentWordIndex` means all items before the current one show as green, and the current item's segment shows as purple (`activeColor`).

**Step 2: Verify no import needed**

`Color` is already imported in this file. No new imports required.

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt
git commit -m "feat: show green segments for answered items in learn mode progress bar"
```

---

### Task 3: Manual verification

**Step 1: Build the project**

```bash
./gradlew assembleDebug
```

Expected: Build succeeds with no errors.

**Step 2: Visual check**

Launch the app, start a learn mode session with multiple words. Verify:
- Items before current word: **green** segments
- Current word: **purple** segment
- Future words: **light purple** segments
- On first word (index 0): no green segments visible (all purple/light purple) — correct since nothing is completed yet
- Tutorial mode: unchanged (no green segments)
