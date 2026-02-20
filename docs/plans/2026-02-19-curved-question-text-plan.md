# Curved Question Text Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Display the question text (e.g., "Can you trace the letter A?") as curved arc text along the top of the Wear OS watch face in Practice Mode's question screen.

**Architecture:** Add a `CurvedLayout` overlay with `curvedText` inside the existing `QuestionContent` composable. The curved text sits at the top arc (`anchor = 270f`) while existing centered content (letter/emoji/avatar) remains unchanged. Single file change.

**Tech Stack:** Jetpack Compose for Wear OS — `CurvedLayout` and `curvedText` from `androidx.wear.compose.foundation` (already in dependencies at version 1.2.1)

---

## Task 1: Add curved question text to QuestionContent

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeScreen.kt:389-474`

**Step 1: Add required imports**

Add these imports to the top of `PracticeModeScreen.kt`:

```kotlin
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedText
```

**Step 2: Restructure QuestionContent to include CurvedLayout**

Replace the `QuestionContent` composable. The key change: wrap everything in a `Box` that holds both the `CurvedLayout` (top arc text) and the existing centered content.

Current structure:
```kotlin
Box(modifier = fillMaxSize + clickable, contentAlignment = Center) {
    // category-specific content
}
```

New structure:
```kotlin
Box(modifier = fillMaxSize + clickable, contentAlignment = Center) {
    // Curved question text at top arc
    if (question != null) {
        CurvedLayout(anchor = 270f) {
            curvedText(
                text = question.question,
                style = CurvedTextStyle(fontSize = 12.sp, color = Color.White)
            )
        }
    }
    // Existing category-specific centered content (unchanged)
}
```

Full replacement for the `QuestionContent` function (lines ~389-474):

```kotlin
@Composable
private fun QuestionContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    val question = uiState.currentQuestion
    val isPictureMatch = question?.category == QuestionCategory.PICTURE_MATCH
    val isTracingCopying = question?.category == QuestionCategory.TRACING_COPYING
    val isUppercaseLowercase = question?.category == QuestionCategory.UPPERCASE_LOWERCASE
    val isLetterSound = question?.category == QuestionCategory.LETTER_SOUND
    val emoji = question?.emoji

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { viewModel.startAnswering() },
        contentAlignment = Alignment.Center
    ) {
        // Curved question text along the top arc of the watch face
        if (question != null) {
            CurvedLayout(anchor = 270f) {
                curvedText(
                    text = question.question,
                    style = CurvedTextStyle(
                        fontSize = 12.sp,
                        color = Color.White
                    )
                )
            }
        }

        // Existing centered content — unchanged
        if (isPictureMatch && emoji != null) {
            // For Picture Match: Show ONLY the emoji centered, no text
            Text(
                text = emoji,
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
        } else if (isTracingCopying || isUppercaseLowercase) {
            // For Tracing & Copying and Uppercase/Lowercase: Show ONLY the letter centered, no text
            Text(
                text = question?.expectedAnswer ?: "",
                color = AppColors.PracticeModeColor,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else if (isLetterSound) {
            // For Letter Sound Match (Phonics): Show ONLY the question avatar
            Image(
                painter = painterResource(id = R.drawable.dis_question),
                contentDescription = "Question avatar",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // For other categories: Show question text with expected letter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // Question text
                Text(
                    text = question?.question ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Expected letter (hint)
                Text(
                    text = question?.expectedAnswer ?: "",
                    color = AppColors.PracticeModeColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tap to continue instruction
                Text(
                    text = "Tap to answer",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
```

**Step 3: Build and verify**

Run: `./gradlew :wear:assembleDebug`
Expected: BUILD SUCCESSFUL — no compilation errors.

**Step 4: Manual test on emulator/device**

1. Launch the wear app on a round Wear OS emulator or device
2. Navigate to Practice Mode and tap to start
3. Verify:
   - Curved question text appears along the top arc (e.g., "Can you trace the letter A?")
   - Text is white, readable, ~12sp
   - The centered letter/emoji/avatar is still visible and properly centered
   - Tapping the screen still triggers `startAnswering()`
   - Test all 4 categories: Tracing/Copying (letter), Uppercase/Lowercase (letter), Letter Sound (avatar), Picture Match (emoji)

**Step 5: Commit**

```
feat: add curved question text to practice mode UI
```
