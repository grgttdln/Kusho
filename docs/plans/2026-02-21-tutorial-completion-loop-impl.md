# Tutorial Mode Completion Loop Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make tutorial mode loop back to unanswered items and only show the congrats screen when all items are completed, with auto-advance on revisit.

**Architecture:** Single-file change to `TutorialSessionScreen.kt`. Add `attemptedIndices` state to distinguish first attempts (teacher-gated dialog) from revisits (auto-advance/retry). Modify the gesture result handler to branch on revisit, modify skip buttons to wrap around to first unanswered item.

**Tech Stack:** Kotlin, Jetpack Compose, WatchConnectionManager (Wear Data Layer)

**Design doc:** `docs/plans/2026-02-21-tutorial-completion-loop-design.md`

---

### Task 1: Add `attemptedIndices` state variable

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:160-161`

**Step 1: Add state after `completedIndices`**

After line 160 (`var completedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }`), add:

```kotlin
    // Track which items the student has attempted air-writing (for revisit auto-advance)
    var attemptedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
```

This goes between the existing `completedIndices` line and the `showCardStackGrid` line.

**Step 2: Verify the app compiles**

Build the project to confirm no syntax errors.

---

### Task 2: Modify gesture result handler for revisit branching

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:476-492`

**Step 1: Replace the gesture result LaunchedEffect**

Replace lines 476-492 (the `// Listen for gesture results from watch` LaunchedEffect) with:

```kotlin
    // Listen for gesture results from watch
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastGestureTime = 0L
        watchConnectionManager.tutorialModeGestureResult.collect { result ->
            val timestamp = result["timestamp"] as? Long ?: 0L
            if (timestamp > sessionStartTime && timestamp > lastGestureTime && result.isNotEmpty()) {
                lastGestureTime = timestamp
                val gestureCorrect = result["isCorrect"] as? Boolean ?: false
                val gesturePredicted = result["predictedLetter"] as? String ?: ""
                isStudentWriting = false // Recording finished, student is no longer writing

                // Send feedback to watch (phone is single source of truth)
                watchConnectionManager.sendTutorialModeFeedback(gestureCorrect, gesturePredicted)

                val itemIndex = currentStep - 1
                val isRevisit = itemIndex in attemptedIndices
                attemptedIndices = attemptedIndices + itemIndex

                if (isRevisit) {
                    // REVISIT: auto-advance or auto-retry (no dialog)
                    val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                    if (gestureCorrect) {
                        // Auto-mark complete and advance
                        val newCompleted = completedIndices + itemIndex
                        completedIndices = newCompleted
                        // Dismiss feedback on watch immediately
                        watchConnectionManager.notifyTutorialModeFeedbackDismissed()
                        if (newCompleted.size >= maxSteps) {
                            completeTutorialAndEnd()
                        } else {
                            // Auto-navigate to next unanswered
                            val nextIncomplete = ((currentStep) until maxSteps).firstOrNull { it !in newCompleted }
                                ?: (0 until currentStep).firstOrNull { it !in newCompleted }
                            if (nextIncomplete != null) {
                                currentStep = nextIncomplete + 1
                            }
                        }
                    } else {
                        // Auto-retry: show wrong feedback briefly, then retry same item
                        showAnimation = false
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1500L)
                            watchConnectionManager.notifyTutorialModeFeedbackDismissed()
                            watchConnectionManager.notifyTutorialModeRetry()
                            if (currentLetter.isNotEmpty()) {
                                watchConnectionManager.sendTutorialModeLetterData(
                                    letter = currentLetter,
                                    letterCase = letterType,
                                    currentIndex = currentStep,
                                    totalLetters = calculatedTotalSteps,
                                    dominantHand = dominantHand
                                )
                            }
                        }
                    }
                } else {
                    // FIRST ATTEMPT: show teacher-gated ProgressCheckDialog
                    isCorrectGesture = gestureCorrect
                    predictedLetter = gesturePredicted
                    showProgressCheck = true
                }
            }
        }
    }
```

Key changes from original:
- Check `isRevisit` before deciding whether to show dialog
- On revisit correct: auto-mark complete, dismiss watch feedback, navigate to next unanswered or complete session
- On revisit wrong: 1.5s delay for student to see feedback, then auto-retry same item
- On first attempt: set state variables and show ProgressCheckDialog (unchanged behavior)

---

### Task 3: Modify phone-side skip button to wrap around

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:755-762`

**Step 1: Replace the skip button onClick logic**

Replace lines 755-762:

```kotlin
                onClick = {
                    if (!isStudentWriting) {
                        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                        if (currentStep < maxSteps) {
                            currentStep++
                        }
                        onSkip()
                    }
                },
```

With:

```kotlin
                onClick = {
                    if (!isStudentWriting) {
                        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                        // Wrap around to first unanswered item instead of stopping at end
                        val nextIncomplete = ((currentStep) until maxSteps).firstOrNull { it !in completedIndices }
                            ?: (0 until currentStep - 1).firstOrNull { it !in completedIndices }
                        if (nextIncomplete != null) {
                            currentStep = nextIncomplete + 1
                        }
                        onSkip()
                    }
                },
```

This makes the skip button find the next incomplete item with wraparound, matching the behavior in the ProgressCheckDialog's onContinue handler.

---

### Task 4: Modify watch-side skip trigger to wrap around

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:460-474`

**Step 1: Replace the watch skip LaunchedEffect logic**

Replace lines 460-474:

```kotlin
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastSkipTime = 0L
        watchConnectionManager.tutorialModeSkipTrigger.collect { skipTime ->
            val timeSinceLastSkip = skipTime - lastSkipTime
            if (skipTime > sessionStartTime && skipTime > lastSkipTime && timeSinceLastSkip >= 500) {
                lastSkipTime = skipTime
                val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                if (currentStep < maxSteps) {
                    currentStep++
                }
                onSkip()
            }
        }
    }
```

With:

```kotlin
    // Listen for skip commands from watch with debouncing
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastSkipTime = 0L
        watchConnectionManager.tutorialModeSkipTrigger.collect { skipTime ->
            val timeSinceLastSkip = skipTime - lastSkipTime
            if (skipTime > sessionStartTime && skipTime > lastSkipTime && timeSinceLastSkip >= 500) {
                lastSkipTime = skipTime
                val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                // Wrap around to first unanswered item instead of stopping at end
                val nextIncomplete = ((currentStep) until maxSteps).firstOrNull { it !in completedIndices }
                    ?: (0 until currentStep - 1).firstOrNull { it !in completedIndices }
                if (nextIncomplete != null) {
                    currentStep = nextIncomplete + 1
                }
                onSkip()
            }
        }
    }
```

Same wraparound logic as the phone-side skip button — both skip triggers behave identically.

---

### Task 5: Manual testing checklist

Test on device/emulator with watch connected:

1. **First pass — all correct**: Go through all letters, answer correctly on each. Verify ProgressCheckDialog shows for every item (teacher-gated). After the last correct answer, verify congrats screen appears.

2. **First pass — skip some**: Skip items 2 and 4. Answer items 1, 3, 5 correctly. After item 5, verify it auto-navigates to item 2 (first unanswered). Verify ProgressCheckDialog still shows (first attempt for item 2).

3. **Revisit — correct**: On the revisit of item 2, answer correctly. Verify NO dialog appears. Verify it auto-advances to item 4 (next unanswered).

4. **Revisit — wrong then correct**: On the revisit of item 4, answer wrong. Verify NO dialog appears. Verify it stays on item 4 (retry-lock). Answer correctly. Verify it auto-advances. After all items complete, verify congrats screen appears.

5. **Skip button wrapping**: Navigate to last item. Click skip. Verify it wraps to the first unanswered item (not stuck at end).

6. **Grid navigation**: Open card stack grid. Verify completed items show green, incomplete show yellow. Tap an incomplete item. Verify it navigates there.

7. **End session with partial progress**: Complete some items, skip others. Click End Session. Verify progress is saved. Re-enter the same tutorial. Verify resume dialog shows with correct count. Verify all incomplete items are treated as first-attempt (dialog shows).
