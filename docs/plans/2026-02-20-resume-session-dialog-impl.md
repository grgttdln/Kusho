# Resume Session Dialog Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a shared ResumeSessionDialog composable matching the new card-based UI design, and use it in both Learn Mode and Tutorial Mode.

**Architecture:** Single shared composable in `/components/common/` with parameters for mode-specific customization (mascot, accent color, unit label). Both session screens replace their inline ~110-line dialogs with a single component call.

**Tech Stack:** Kotlin, Jetpack Compose, Material3

---

### Task 1: Create ResumeSessionDialog Composable

**Files:**
- Create: `app/src/main/java/com/example/app/ui/components/common/ResumeSessionDialog.kt`

**Step 1: Create the composable file**

```kotlin
package com.example.app.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val BlueBannerColor = Color(0xFF3FA9F8)
private val OrangeColor = Color(0xFFFF8C42)

/**
 * Shared resume session dialog used by both Learn Mode and Tutorial Mode.
 * Shows a card with blue banner, mascot in accent-colored circle, progress info,
 * and side-by-side Resume/Restart buttons.
 */
@Composable
fun ResumeSessionDialog(
    mascotDrawable: Int,
    accentColor: Color,
    studentName: String,
    completedCount: Int,
    totalCount: Int,
    unitLabel: String,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Non-dismissable — must choose */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Block clicks through overlay
                ),
            contentAlignment = Alignment.Center
        ) {
            // Card + overlapping mascot
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // White card body (offset down to leave room for mascot overlap)
                Column(
                    modifier = Modifier
                        .padding(top = 60.dp) // Room for mascot overlap
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Blue banner area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(BlueBannerColor)
                    )

                    Spacer(Modifier.height(16.dp))

                    // "Welcome Back" text
                    Text(
                        text = "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Progress text with bold fraction
                    Text(
                        text = buildAnnotatedString {
                            append("$studentName has completed ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                                append("$completedCount/$totalCount")
                            }
                            append(" $unitLabel.")
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Would you like to continue where you left off?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // Side-by-side buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Resume button (filled blue)
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueBannerColor
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = "Resume",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Restart button (outlined orange)
                        OutlinedButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            border = BorderStroke(2.dp, OrangeColor),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = "Restart",
                                color = OrangeColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // Mascot circle (overlapping the top of the card)
                Box(
                    modifier = Modifier
                        .offset(y = 10.dp)
                        .size(120.dp)
                        .border(4.dp, accentColor, CircleShape)
                        .background(Color.White, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = mascotDrawable),
                        contentDescription = "Resume session mascot",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
```

**Step 2: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Replace Learn Mode Resume Dialog with Component

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Add import**

Add after the existing imports:

```kotlin
import com.example.app.ui.components.common.ResumeSessionDialog
```

**Step 2: Replace the inline resume dialog**

Replace the entire `if (showResumeDialog) { ... }` block (lines 1068-1181) with:

```kotlin
    // Resume dialog - shown when there's saved progress from a previous session
    if (showResumeDialog) {
        val savedWords = correctlyAnsweredWords.size
        val totalWords = words.size.coerceAtLeast(1)
        ResumeSessionDialog(
            mascotDrawable = R.drawable.dis_pairing_learn,
            accentColor = PurpleColor,
            studentName = studentName,
            completedCount = savedWords,
            totalCount = totalWords,
            unitLabel = "words",
            onResume = {
                showResumeDialog = false
                // currentWordIndex and correctlyAnsweredWords already restored in LaunchedEffect
            },
            onRestart = {
                showResumeDialog = false
                currentWordIndex = 0
                correctlyAnsweredWords = emptySet()
                coroutineScope.launch {
                    val studentIdLong = studentId.toLongOrNull()
                    if (studentIdLong != null && studentIdLong > 0) {
                        withContext(Dispatchers.IO) {
                            studentSetProgressDao.clearInProgressSession(studentIdLong, activityId, setId)
                        }
                    }
                }
            }
        )
    }
```

**Step 3: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Replace Tutorial Mode Resume Dialog with Component

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Add import**

Add after the existing imports:

```kotlin
import com.example.app.ui.components.common.ResumeSessionDialog
```

**Step 2: Replace the inline resume dialog**

Replace the entire `if (showResumeDialog) { ... }` block (lines 962-1072) with:

```kotlin
    // Resume dialog — shown when there's saved progress from a previous session
    if (showResumeDialog) {
        val savedStep = currentStep - 1 // currentStep was pre-set to resume point
        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
        ResumeSessionDialog(
            mascotDrawable = R.drawable.dis_pairing_tutorial,
            accentColor = Color(0xFFEDBB00),
            studentName = studentName,
            completedCount = savedStep,
            totalCount = maxSteps,
            unitLabel = "letters",
            onResume = {
                showResumeDialog = false
                // currentStep is already set to the resume point
            },
            onRestart = {
                showResumeDialog = false
                currentStep = 1 // Reset to beginning
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        tutorialCompletionDao.deleteInProgress(studentId, tutorialSetId)
                    }
                }
            }
        )
    }
```

**Step 3: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---
