# Learn Mode End Session Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add early exit with partial progress saving and resume support to Learn Mode, mirroring Tutorial Mode.

**Architecture:** Extend the existing `StudentSetProgress` entity with two new columns for partial state. Add end-session UI (button + confirmation dialog + resume dialog) to `LearnModeSessionScreen`. Wire a new `onEarlyExit` callback in the navigation container.

**Tech Stack:** Kotlin, Jetpack Compose, Room (with fallbackToDestructiveMigration), Coroutines

---

### Task 1: Extend StudentSetProgress Entity

**Files:**
- Modify: `app/src/main/java/com/example/app/data/entity/StudentSetProgress.kt:41-51`

**Step 1: Add two new columns to the data class**

In `StudentSetProgress.kt`, add `lastCompletedWordIndex` and `correctlyAnsweredWordsJson` after the existing `lastAccessedAt` field:

```kotlin
data class StudentSetProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val activityId: Long,
    val setId: Long,
    val isCompleted: Boolean = false,
    val completionPercentage: Int = 0,
    val completedAt: Long? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val lastCompletedWordIndex: Int = 0,
    val correctlyAnsweredWordsJson: String? = null
)
```

**Step 2: Bump the database version**

In `app/src/main/java/com/example/app/data/AppDatabase.kt:58`, change version from `15` to `16`:

```kotlin
    version = 16, // Added lastCompletedWordIndex, correctlyAnsweredWordsJson to StudentSetProgress
```

The database uses `fallbackToDestructiveMigration()` so no explicit migration is needed.

**Step 3: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add DAO Queries for Partial Progress

**Files:**
- Modify: `app/src/main/java/com/example/app/data/dao/StudentSetProgressDao.kt:12-144`

**Step 1: Add getInProgressSession query**

Add this after the existing `getProgress` method (after line 41):

```kotlin
    /**
     * Get an in-progress (not completed) session for a student, activity, and set.
     * Returns null if no in-progress session exists.
     */
    @Query("SELECT * FROM student_set_progress WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId AND isCompleted = 0 AND lastCompletedWordIndex > 0")
    suspend fun getInProgressSession(studentId: Long, activityId: Long, setId: Long): StudentSetProgress?
```

**Step 2: Add deleteInProgressSession query**

Add this after the new `getInProgressSession` method:

```kotlin
    /**
     * Delete in-progress session data by resetting partial progress fields.
     * Called when "Start Over" is chosen or when a session completes normally.
     */
    @Query("""
        UPDATE student_set_progress
        SET lastCompletedWordIndex = 0, correctlyAnsweredWordsJson = NULL
        WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId AND isCompleted = 0
    """)
    suspend fun clearInProgressSession(studentId: Long, activityId: Long, setId: Long): Int
```

**Step 3: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Add onEarlyExit Callback and End Session State

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:142-154` (function signature)
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:187-201` (state variables)

**Step 1: Add onEarlyExit parameter to the function signature**

At line 153, add `onEarlyExit` callback after `onSessionComplete`:

```kotlin
fun LearnModeSessionScreen(
    setId: Long = 0L,
    activityId: Long = 0L,
    activityTitle: String = "",
    sessionKey: Int = 0,
    studentId: String = "",
    studentName: String = "",
    dominantHand: String = "RIGHT",
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSessionComplete: () -> Unit = {},
    onEarlyExit: () -> Unit = {}
) {
```

**Step 2: Add dialog state variables**

After the `shouldSaveAndComplete` state (line 201), add:

```kotlin
    // State for End Session confirmation dialog
    var showEndSessionConfirmation by remember(sessionKey) { mutableStateOf(false) }

    // State for Resume dialog
    var showResumeDialog by remember(sessionKey) { mutableStateOf(false) }
```

**Step 3: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Add Resume Check on Session Load

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:239-261` (after existing LaunchedEffect for annotations)

**Step 1: Add LaunchedEffect to check for in-progress session**

After the existing `LaunchedEffect(setId, studentId, sessionKey)` block that loads annotations (after line 261), add:

```kotlin
    // Check for in-progress session to offer resume
    LaunchedEffect(setId, studentId, activityId, sessionKey) {
        val studentIdLong = studentId.toLongOrNull()
        if (studentIdLong != null && studentIdLong > 0 && activityId > 0 && setId > 0) {
            val inProgress = withContext(Dispatchers.IO) {
                studentSetProgressDao.getInProgressSession(studentIdLong, activityId, setId)
            }
            if (inProgress != null && inProgress.lastCompletedWordIndex > 0) {
                // Restore state from saved progress
                currentWordIndex = inProgress.lastCompletedWordIndex
                // Restore correctly answered words from JSON
                val savedWords = inProgress.correctlyAnsweredWordsJson
                if (!savedWords.isNullOrBlank()) {
                    try {
                        val indices = org.json.JSONArray(savedWords)
                        val restoredSet = mutableSetOf<Int>()
                        for (i in 0 until indices.length()) {
                            restoredSet.add(indices.getInt(i))
                        }
                        correctlyAnsweredWords = restoredSet
                    } catch (e: Exception) {
                        Log.e("LearnModeSession", "Failed to parse correctlyAnsweredWordsJson", e)
                    }
                }
                showResumeDialog = true
            }
        }
    }
```

**Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Add saveProgressAndExit Logic

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:276-369` (after the shouldSaveAndComplete LaunchedEffect)

**Step 1: Add the saveProgressAndExit lambda**

After the `shouldSaveAndComplete` LaunchedEffect block (after line 369), add:

```kotlin
    // Save partial progress and exit early
    val saveProgressAndExit: () -> Unit = {
        coroutineScope.launch {
            val studentIdLong = studentId.toLongOrNull()
            if (studentIdLong != null && studentIdLong > 0 && setId > 0 && activityId > 0) {
                withContext(Dispatchers.IO) {
                    // Save all annotations accumulated so far
                    annotationsMap.forEach { (itemId, annotationData) ->
                        if (annotationData.hasData()) {
                            val annotation = LearnerProfileAnnotation.create(
                                studentId = studentId,
                                setId = setId,
                                itemId = itemId,
                                sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                activityId = activityId,
                                levelOfProgress = annotationData.levelOfProgress,
                                strengthsObserved = annotationData.strengthsObserved.toList(),
                                strengthsNote = annotationData.strengthsNote,
                                challenges = annotationData.challenges.toList(),
                                challengesNote = annotationData.challengesNote
                            )
                            annotationDao.insertOrUpdate(annotation)
                        }
                    }

                    // Generate AI summary if annotations exist
                    try {
                        val allAnnotations = annotationDao.getAnnotationsForStudentInSet(
                            studentId = studentId,
                            setId = setId,
                            sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                            activityId = activityId
                        )
                        if (allAnnotations.isNotEmpty()) {
                            val wordNames = words.mapIndexed { i, w -> i to w.word }.toMap()
                            val summaryText = geminiRepository.generateAnnotationSummary(
                                allAnnotations, LearnerProfileAnnotation.MODE_LEARN, wordNames
                            )
                            if (summaryText != null) {
                                annotationSummaryDao.insertOrUpdate(
                                    com.example.app.data.entity.AnnotationSummary(
                                        studentId = studentId,
                                        setId = setId,
                                        sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                        activityId = activityId,
                                        summaryText = summaryText
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LearnModeSession", "AI summary generation failed on early exit: ${e.message}")
                    }

                    // Save partial progress
                    val completedWords = correctlyAnsweredWords.size
                    val totalWords = words.size.coerceAtLeast(1)
                    val percentage = (completedWords * 100) / totalWords
                    val correctWordsJson = org.json.JSONArray(correctlyAnsweredWords.toList()).toString()

                    val existingProgress = studentSetProgressDao.getProgress(studentIdLong, activityId, setId)
                    if (existingProgress != null) {
                        studentSetProgressDao.upsertProgress(
                            existingProgress.copy(
                                lastCompletedWordIndex = currentWordIndex,
                                correctlyAnsweredWordsJson = correctWordsJson,
                                completionPercentage = percentage,
                                isCompleted = false,
                                completedAt = null,
                                lastAccessedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        studentSetProgressDao.upsertProgress(
                            com.example.app.data.entity.StudentSetProgress(
                                studentId = studentIdLong,
                                activityId = activityId,
                                setId = setId,
                                isCompleted = false,
                                completionPercentage = percentage,
                                lastCompletedWordIndex = currentWordIndex,
                                correctlyAnsweredWordsJson = correctWordsJson,
                                lastAccessedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    Log.d("LearnModeSession", "Partial progress saved: wordIndex=$currentWordIndex, correctWords=$correctWordsJson, percentage=$percentage%")
                }
            }
            watchConnectionManager.notifyLearnModeEnded()
            onEarlyExit()
        }
    }
```

**Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: Clear In-Progress Data on Normal Completion

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:276-369` (shouldSaveAndComplete LaunchedEffect)

**Step 1: Add clearInProgressSession call when completing normally**

Inside the `shouldSaveAndComplete` LaunchedEffect, right before `studentSetProgressDao.markSetAsCompleted(...)` at line 310, add:

```kotlin
                        // Clear any in-progress session data
                        studentSetProgressDao.clearInProgressSession(studentIdLong, activityId, setId)
```

Also add the same before the `upsertProgress` call at line 326 (when creating a new completed record), set the new fields explicitly:

```kotlin
                            val progress = com.example.app.data.entity.StudentSetProgress(
                                studentId = studentIdLong,
                                activityId = activityId,
                                setId = setId,
                                isCompleted = true,
                                completionPercentage = 100,
                                completedAt = System.currentTimeMillis(),
                                lastCompletedWordIndex = 0,
                                correctlyAnsweredWordsJson = null
                            )
```

**Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 7: Add End Session Button to UI

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1076-1080` (bottom of the main Column)

**Step 1: Add the End Session button**

Replace the closing section of the main Column (lines 1076-1080):

```kotlin
// Before:
            Spacer(Modifier.weight(0.7f))
        }

        Spacer(Modifier.height(24.dp))
    }

// After:
            Spacer(Modifier.weight(0.7f))
        }

        Spacer(Modifier.height(24.dp))

        // End Session Button
        Button(
            onClick = { showEndSessionConfirmation = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "End Session",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
```

Note: This requires adding `Button` and `ButtonDefaults` imports. Check if they already exist; if not add:
```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
```

**Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 8: Add End Session Confirmation Dialog

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` (after the annotation dialog block, around line 791)

**Step 1: Add the confirmation dialog**

After the closing brace of the annotation dialog `if (showAnnotationDialog)` block (after line 791), add:

```kotlin
    // End Session confirmation dialog
    if (showEndSessionConfirmation) {
        val completedWords = correctlyAnsweredWords.size
        val totalWords = words.size.coerceAtLeast(1)
        val progressMessage = when {
            completedWords <= 0 -> "You haven't completed any words yet.\nAny annotations you've made will be saved."
            completedWords == 1 -> "You've completed 1/$totalWords word!\nYour progress and annotations will be saved."
            else -> "You've completed $completedWords/$totalWords words!\nYour progress and annotations will be saved."
        }

        Dialog(
            onDismissRequest = { showEndSessionConfirmation = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showEndSessionConfirmation = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dis_remove),
                        contentDescription = "End session",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Are you sure?",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = progressMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showEndSessionConfirmation = false
                            saveProgressAndExit()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "End Session",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showEndSessionConfirmation = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
```

**Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 9: Add Resume Dialog

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` (after the end session confirmation dialog added in Task 8)

**Step 1: Add the resume dialog**

Immediately after the end session confirmation dialog block, add:

```kotlin
    // Resume dialog - shown when there's saved progress from a previous session
    if (showResumeDialog) {
        val savedWords = correctlyAnsweredWords.size
        val totalWords = words.size.coerceAtLeast(1)
        Dialog(
            onDismissRequest = { /* Can't dismiss - must choose */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dis_pairing_tutorial),
                        contentDescription = "Resume session",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Welcome back!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "$studentName has completed $savedWords/$totalWords words.\nWould you like to continue where you left off?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // Resume button
                    Button(
                        onClick = {
                            showResumeDialog = false
                            // currentWordIndex and correctlyAnsweredWords already restored in LaunchedEffect
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Resume Session",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Start over button
                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Start Over",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
```

**Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 10: Wire onEarlyExit in Navigation Container

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt:653-664` (screen 33)
- Modify: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt:779-791` (screen 43)

**Step 1: Add onEarlyExit to screen 33 (classroom context)**

At screen 33, add `onEarlyExit` callback that navigates to screen 1 (LearnScreen):

```kotlin
        33 -> {
            com.example.app.ui.feature.learn.learnmode.LearnModeSessionScreen(
                setId = selectedSetId,
                activityId = selectedActivityId,
                activityTitle = tutorialSessionTitle,
                sessionKey = learnModeSessionKey,
                studentId = selectedStudentId,
                studentName = selectedStudentName,
                dominantHand = selectedDominantHand,
                modifier = modifier,
                onSessionComplete = { currentScreen = 34 },
                onEarlyExit = { currentScreen = 1 }
            )
        }
```

**Step 2: Add onEarlyExit to screen 43 (dashboard context)**

At screen 43, add `onEarlyExit` callback that navigates to screen 0 (DashboardScreen):

```kotlin
        43 -> {
            com.example.app.ui.feature.learn.learnmode.LearnModeSessionScreen(
                setId = selectedSetId,
                activityId = selectedActivityId,
                activityTitle = tutorialSessionTitle,
                sessionKey = learnModeSessionKey,
                studentId = selectedStudentId,
                studentName = selectedStudentName,
                dominantHand = selectedDominantHand,
                modifier = modifier,
                onSessionComplete = { currentScreen = 44 },
                onEarlyExit = { currentScreen = 0 }
            )
        }
```

**Step 3: Verify the project compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---
