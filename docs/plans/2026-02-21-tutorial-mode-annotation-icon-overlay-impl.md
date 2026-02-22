# Tutorial Mode Annotation Icon Overlay Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a yellow annotation icon with tooltip to tutorial mode's correct overlay, replacing the text link.

**Architecture:** Modify the private `ProgressCheckDialog` composable in `TutorialSessionScreen.kt` to include an annotation icon block (copied from learn mode), with yellow tinting. Add a dedicated state variable at the call site since `showAnnotationTooltip` is already used for the grid icon tooltip.

**Tech Stack:** Jetpack Compose (Material3 TooltipBox, Card, IconButton, ColorFilter)

---

### Task 1: Add annotation tooltip state and new parameters to ProgressCheckDialog

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:1441-1449`

**Step 1: Add `showAnnotationTooltip` and `onAnnotationTooltipDismissed` parameters**

Change the function signature from:

```kotlin
private fun ProgressCheckDialog(
    isCorrect: Boolean,
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    onContinue: () -> Unit,
    onAddAnnotation: () -> Unit = {}
)
```

To:

```kotlin
private fun ProgressCheckDialog(
    isCorrect: Boolean,
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    showAnnotationTooltip: Boolean,
    onAnnotateClick: () -> Unit,
    onAnnotationTooltipDismissed: () -> Unit,
    onContinue: () -> Unit
)
```

**Step 2: Verify the change compiles mentally** — no run needed yet, this is a signature-only change that will temporarily break the call site.

---

### Task 2: Replace "Add Annotations" text link with yellow annotation icon + tooltip

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:1547-1676`

**Step 1: Add the annotation icon block inside the outer `Box` (after the opening `Box`, before the `Column`)**

Insert after line 1553 (after `contentAlignment = Alignment.Center`), before the `Column`:

```kotlin
            // Annotation icon - top left (yellow for tutorial mode)
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
                                anchorBounds: androidx.compose.ui.unit.IntRect,
                                windowSize: androidx.compose.ui.unit.IntSize,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                popupContentSize: androidx.compose.ui.unit.IntSize
                            ): androidx.compose.ui.unit.IntOffset {
                                val x = anchorBounds.left + (anchorBounds.width / 2) - 15
                                val y = anchorBounds.bottom + 16
                                return androidx.compose.ui.unit.IntOffset(x, y)
                            }
                        },
                        tooltip = {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = YellowIconColor
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
                                colorFilter = ColorFilter.tint(YellowIconColor),
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
                            colorFilter = ColorFilter.tint(YellowIconColor),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
```

**Step 2: Remove the "Add Annotations" text link block (lines 1641-1655)**

Delete:

```kotlin
                Spacer(Modifier.height(20.dp))

                // Add Annotations clickable text
                Text(
                    text = "Add Annotations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueAnnotationColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onAddAnnotation() }
                        .padding(vertical = 4.dp)
                )
```

---

### Task 3: Wire up the state at the call site

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:149-177` (state declarations) and `505-553` (call site)

**Step 1: Add a dedicated state variable for the overlay annotation tooltip**

Near line 177 (where `showAnnotationTooltip` is declared for the grid tooltip), add:

```kotlin
    var showOverlayAnnotationTooltip by remember { mutableStateOf(true) }
```

**Step 2: Update the `ProgressCheckDialog` call site (lines 506-553)**

Change from:

```kotlin
        ProgressCheckDialog(
            isCorrect = isCorrectGesture,
            studentName = studentName,
            targetLetter = currentLetter,
            targetCase = letterType,
            predictedLetter = predictedLetter,
            onContinue = {
                showProgressCheck = false
                // ... existing onContinue logic unchanged ...
            },
            onAddAnnotation = {
                showAnnotationDialog = true
            }
        )
```

To:

```kotlin
        ProgressCheckDialog(
            isCorrect = isCorrectGesture,
            studentName = studentName,
            targetLetter = currentLetter,
            targetCase = letterType,
            predictedLetter = predictedLetter,
            showAnnotationTooltip = showOverlayAnnotationTooltip,
            onAnnotateClick = {
                showAnnotationDialog = true
            },
            onAnnotationTooltipDismissed = {
                showOverlayAnnotationTooltip = false
            },
            onContinue = {
                showProgressCheck = false
                // ... existing onContinue logic unchanged ...
            }
        )
```

The `onContinue` block body stays exactly the same — only the parameter names and order change.

---

### Task 4: Verify build

**Step 1: Build the project**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt
git commit -m "feat(tutorial): replace annotation text link with yellow icon + tooltip in correct overlay"
```
