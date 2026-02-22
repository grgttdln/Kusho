# Clickable Progress Bar — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make progress bar segments clickable in learn mode so students can navigate to any item, with only the current item shown in purple.

**Architecture:** Add an `onSegmentClick` callback to `ProgressIndicator` and change the active-color logic from "all items before current" to "only the current item." Tutorial mode is unaffected (uses its own private copy). The caller passes a lambda that cancels animations and sets `currentWordIndex`.

**Tech Stack:** Jetpack Compose, Kotlin

---

### Task 1: Update `ProgressIndicator` — add click support and single-item highlighting

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/common/ProgressIndicator.kt`

**Step 1: Add imports**

Add these imports at the top of the file (after the existing imports):

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
```

**Step 2: Add `onSegmentClick` parameter to the composable signature**

Add after `cornerRadius`:

```kotlin
    cornerRadius: Dp = 4.dp,
    onSegmentClick: ((Int) -> Unit)? = null
```

**Step 3: Change the active-color logic from range to exact index**

Replace:

```kotlin
index < currentStep -> activeColor
```

With:

```kotlin
index == currentStep - 1 -> activeColor
```

This ensures only the exact current item shows purple, not all items before it.

**Step 4: Make segments clickable when `onSegmentClick` is provided**

Replace the current `Box` block:

```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .height(height)
        .clip(RoundedCornerShape(cornerRadius))
        .background(segmentColor)
)
```

With:

```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .height(height)
        .clip(RoundedCornerShape(cornerRadius))
        .background(segmentColor)
        .then(
            if (onSegmentClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSegmentClick(index) }
            } else {
                Modifier
            }
        )
)
```

Note: `indication = null` removes the ripple effect since the segments are small colored bars — a ripple would look odd.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/components/common/ProgressIndicator.kt
git commit -m "feat: add clickable segments and single-item highlighting to ProgressIndicator"
```

---

### Task 2: Wire up click handler in `LearnModeSessionScreen`

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:834-842`

**Step 1: Add `onSegmentClick` to the `ProgressIndicator` call**

Replace the current call at line 834:

```kotlin
ProgressIndicator(
    currentStep = currentStep,
    totalSteps = totalWords,
    modifier = Modifier.fillMaxWidth(),
    completedIndices = correctlyAnsweredWords,
    activeColor = PurpleColor,
    inactiveColor = LightPurpleColor,
    completedColor = Color(0xFF4CAF50)
)
```

With:

```kotlin
ProgressIndicator(
    currentStep = currentStep,
    totalSteps = totalWords,
    modifier = Modifier.fillMaxWidth(),
    completedIndices = correctlyAnsweredWords,
    activeColor = PurpleColor,
    inactiveColor = LightPurpleColor,
    completedColor = Color(0xFF4CAF50),
    onSegmentClick = { index ->
        // Cancel any ongoing wrong letter animation
        if (wrongLetterAnimationActive) {
            wrongLetterAnimationActive = false
            wrongLetterText = ""
            pendingCorrectAction = null
        }
        // Navigate to the clicked item
        currentWordIndex = index
    }
)
```

Note: Setting `currentWordIndex = index` triggers the existing `LaunchedEffect(currentWordIndex, words)` at line 411 which already handles:
- Resetting `completedLetterIndices`, `currentLetterIndex`, `fillInBlankCorrect`
- Sending new word data to the watch
- Triggering TTS for the new word

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt
git commit -m "feat: enable clickable progress bar navigation in learn mode"
```

---

### Task 3: Build verification

**Step 1: Build the project**

```bash
./gradlew assembleDebug
```

Expected: Build succeeds with no errors.

**Step 2: Visual check**

Launch the app, start a learn mode session with multiple words. Verify:
- Only the current item segment is purple
- Tapping a different segment navigates to that item (word changes, watch syncs)
- Correctly answered items stay green after navigating away
- Skipped/unanswered items are light purple
- Tapping a green segment navigates back to that item (shows as purple while current)
- Navigating away from a correctly answered item returns it to green
