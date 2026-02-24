# Move Annotation Icon to Correct Overlay — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move the annotation icon from the always-visible toolbar into the ProgressCheckDialog (correct answer overlay) so teachers annotate in context.

**Architecture:** Single-file change in `LearnModeSessionScreen.kt`. Remove annotation UI from the toolbar Row, add annotation icon + tooltip to `ProgressCheckDialog` via new callback parameters. The annotation dialog itself is unchanged.

**Tech Stack:** Jetpack Compose, Material3 TooltipBox

---

### Task 1: Add annotation parameters to ProgressCheckDialog

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1838-1844`

**Step 1: Update ProgressCheckDialog signature**

Change the function signature from:

```kotlin
@Composable
private fun ProgressCheckDialog(
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    onDismiss: () -> Unit
)
```

To:

```kotlin
@Composable
private fun ProgressCheckDialog(
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    showAnnotationTooltip: Boolean,
    onAnnotateClick: () -> Unit,
    onAnnotationTooltipDismissed: () -> Unit,
    onDismiss: () -> Unit
)
```

**Step 2: Add annotation icon inside the overlay Box**

Inside the `Dialog` composable, after the opening `Box(...)` block (line ~1913), add the annotation icon aligned to `TopStart`. The existing centered `Column` stays as-is. Replace the `Box` content block so it contains both the icon and the centered column:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.7f))
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onDismiss() },
    contentAlignment = Alignment.Center
) {
    // Annotation icon - top left
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 16.dp, top = 48.dp)
    ) {
        if (showAnnotationTooltip) {
            val tooltipState = rememberTooltipState(isPersistent = false)

            LaunchedEffect(Unit) {
                tooltipState.show()
            }

            TooltipBox(
                positionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val x = anchorBounds.left + (anchorBounds.width / 2) - 15
                        val y = anchorBounds.bottom + 16
                        return IntOffset(x, y)
                    }
                },
                tooltip = {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE7DDFE)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Click here to Add Note",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                },
                state = tooltipState
            ) {
                IconButton(onClick = {
                    onAnnotateClick()
                    onAnnotationTooltipDismissed()
                }) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_annotate),
                        contentDescription = "Annotate",
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        } else {
            IconButton(onClick = { onAnnotateClick() }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_annotate),
                    contentDescription = "Annotate",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // Existing centered content (Column with mascot, text, etc.) stays here unchanged
    Column(
        // ... existing code unchanged ...
    )
}
```

**Step 3: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`

---

### Task 2: Update the call site to pass new parameters

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:925-939`

**Step 1: Pass annotation parameters at the call site**

Change from:

```kotlin
if (showProgressCheckDialog) {
    ProgressCheckDialog(
        studentName = studentName,
        targetLetter = targetLetter,
        targetCase = targetCase,
        predictedLetter = predictedLetter,
        onDismiss = {
            showProgressCheckDialog = false
            watchConnectionManager.notifyLearnModeFeedbackDismissed()
            pendingCorrectAction?.invoke()
            pendingCorrectAction = null
        }
    )
}
```

To:

```kotlin
if (showProgressCheckDialog) {
    ProgressCheckDialog(
        studentName = studentName,
        targetLetter = targetLetter,
        targetCase = targetCase,
        predictedLetter = predictedLetter,
        showAnnotationTooltip = showAnnotationTooltip,
        onAnnotateClick = {
            showAnnotationDialog = true
        },
        onAnnotationTooltipDismissed = {
            showAnnotationTooltip = false
        },
        onDismiss = {
            showProgressCheckDialog = false
            watchConnectionManager.notifyLearnModeFeedbackDismissed()
            pendingCorrectAction?.invoke()
            pendingCorrectAction = null
        }
    )
}
```

**Step 2: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`

---

### Task 3: Remove annotation icon from toolbar

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1060-1161`

**Step 1: Remove tooltip state and LaunchedEffect**

Delete lines ~1062-1070:

```kotlin
// Tooltip state for annotation button
val tooltipState = rememberTooltipState(isPersistent = false)

// Show tooltip on first load
LaunchedEffect(showAnnotationTooltip) {
    if (showAnnotationTooltip) {
        tooltipState.show()
    }
}
```

**Step 2: Remove annotation icon from the Row, keep skip button**

Replace the entire Row block (lines ~1072-1161) — which contains the annotate button + skip button — with just the skip button. Since the Row was using `Arrangement.SpaceBetween` for two buttons, and now we only have the skip button, align it to the end:

```kotlin
// Skip Button Row
Row(
    modifier = Modifier
        .fillMaxWidth()
        .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
) {
    // Skip button
    IconButton(
        onClick = {
            if (!isStudentWriting) {
                onSkip()
                handleSkipOrNext()
            }
        },
        enabled = !isStudentWriting
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_skip),
            contentDescription = "Skip",
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit
        )
    }
}
```

**Step 3: Verify build compiles and run on device**

Run: `./gradlew :app:compileDebugKotlin`

Manual test:
1. Start a Learn Mode session
2. Verify the annotation icon is NOT visible in the toolbar (only skip button)
3. Get a correct answer → ProgressCheckDialog appears
4. Verify annotation icon appears at top-left of the overlay
5. First time: tooltip "Click here to Add Note" appears
6. Tap annotation icon → LearnerProfileAnnotationDialog opens on top of the overlay
7. Close annotation dialog → overlay is still showing
8. Tap overlay to dismiss → flow continues normally

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt
git commit -m "feat(learn): move annotation icon from toolbar to correct overlay"
```
