# AI Annotation Summary Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Generate AI summaries from teacher annotations using Gemini and display them on student annotation cards.

**Architecture:** New `AnnotationSummary` Room entity stores cached AI summaries per set/category per student. `GeminiRepository` gets a new method for summary generation. Summary generation triggers after annotation saves in session screens, with results displayed on annotation cards in `StudentDetailsScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database, Google Generative AI (Gemini 2.5 Flash Lite)

---

### Task 1: Create AnnotationSummary Entity

**Files:**
- Create: `app/src/main/java/com/example/app/data/entity/AnnotationSummary.kt`

**Step 1: Create the entity file**

```kotlin
package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "annotation_summary",
    indices = [
        Index(value = ["studentId", "setId", "sessionMode"], unique = true),
        Index(value = ["studentId"])
    ]
)
data class AnnotationSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,
    val setId: Long,
    val sessionMode: String,
    val summaryText: String,
    val generatedAt: Long = System.currentTimeMillis()
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/entity/AnnotationSummary.kt
git commit -m "feat: add AnnotationSummary Room entity"
```

---

### Task 2: Create AnnotationSummaryDao

**Files:**
- Create: `app/src/main/java/com/example/app/data/dao/AnnotationSummaryDao.kt`

**Step 1: Create the DAO file**

Follow the same patterns as `LearnerProfileAnnotationDao` at `app/src/main/java/com/example/app/data/dao/LearnerProfileAnnotationDao.kt`.

```kotlin
package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.AnnotationSummary

@Dao
interface AnnotationSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: AnnotationSummary): Long

    @Query("""
        SELECT * FROM annotation_summary
        WHERE studentId = :studentId AND setId = :setId AND sessionMode = :sessionMode
        LIMIT 1
    """)
    suspend fun getSummary(studentId: String, setId: Long, sessionMode: String): AnnotationSummary?

    @Query("""
        SELECT * FROM annotation_summary
        WHERE studentId = :studentId
        ORDER BY generatedAt DESC
    """)
    suspend fun getSummariesForStudent(studentId: String): List<AnnotationSummary>

    @Query("""
        DELETE FROM annotation_summary
        WHERE studentId = :studentId AND setId = :setId AND sessionMode = :sessionMode
    """)
    suspend fun deleteSummary(studentId: String, setId: Long, sessionMode: String)
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/dao/AnnotationSummaryDao.kt
git commit -m "feat: add AnnotationSummaryDao for summary persistence"
```

---

### Task 3: Register Entity and DAO in AppDatabase

**Files:**
- Modify: `app/src/main/java/com/example/app/data/AppDatabase.kt`

**Step 1: Add the import for the new entity and DAO**

At the top of `AppDatabase.kt`, add these imports alongside the existing ones:

```kotlin
import com.example.app.data.dao.AnnotationSummaryDao
import com.example.app.data.entity.AnnotationSummary
```

**Step 2: Add `AnnotationSummary` to the entities array**

In the `@Database` annotation, add `AnnotationSummary::class` to the entities list and bump version from 11 to 12:

Change:
```kotlin
@Database(
    entities = [
        User::class,
        Word::class,
        Student::class,
        Class::class,
        Enrollment::class,
        Activity::class,
        Set::class,
        SetWord::class,
        ActivitySet::class,
        StudentTeacher::class,
        LearnerProfileAnnotation::class,
        StudentSetProgress::class
    ],
    version = 11, // Added dominantHand column to students table
    exportSchema = false
)
```

To:
```kotlin
@Database(
    entities = [
        User::class,
        Word::class,
        Student::class,
        Class::class,
        Enrollment::class,
        Activity::class,
        Set::class,
        SetWord::class,
        ActivitySet::class,
        StudentTeacher::class,
        LearnerProfileAnnotation::class,
        StudentSetProgress::class,
        AnnotationSummary::class
    ],
    version = 12, // Added annotation_summary table
    exportSchema = false
)
```

**Step 3: Add the DAO getter**

Inside the `AppDatabase` abstract class body, add alongside the existing DAO declarations:

```kotlin
abstract fun annotationSummaryDao(): AnnotationSummaryDao
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/data/AppDatabase.kt
git commit -m "feat: register AnnotationSummary entity and DAO in AppDatabase"
```

Note: The project already uses `.fallbackToDestructiveMigration()` in `buildDatabase()`, so no explicit migration object is needed. The database will be recreated on version change during development.

---

### Task 4: Add Summary Generation to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add the import for `LearnerProfileAnnotation`**

At the top of the file, add:

```kotlin
import com.example.app.data.entity.LearnerProfileAnnotation
```

**Step 2: Add the `generateAnnotationSummary` method**

Add this new public method inside `GeminiRepository`, after the existing `regenerateSet` method (after line 229) and before the `// ========== Step Functions ==========` comment:

```kotlin
    /**
     * Generate an AI summary from a list of teacher annotations for a set/category.
     *
     * @param annotations All annotations for a given (studentId, setId, sessionMode)
     * @param sessionMode "LEARN" or "TUTORIAL"
     * @return Generated summary text, or null if generation fails
     */
    suspend fun generateAnnotationSummary(
        annotations: List<LearnerProfileAnnotation>,
        sessionMode: String
    ): String? = withContext(Dispatchers.IO) {
        if (annotations.isEmpty()) return@withContext null

        try {
            val formattedAnnotations = annotations.joinToString("\n") { annotation ->
                buildString {
                    append("- Item ${annotation.itemId + 1}:")
                    annotation.levelOfProgress?.let { append(" Level: $it.") }
                    val strengths = annotation.getStrengthsList()
                    if (strengths.isNotEmpty()) append(" Strengths: ${strengths.joinToString(", ")}.")
                    if (annotation.strengthsNote.isNotBlank()) append(" Strengths note: ${annotation.strengthsNote}.")
                    val challenges = annotation.getChallengesList()
                    if (challenges.isNotEmpty()) append(" Challenges: ${challenges.joinToString(", ")}.")
                    if (annotation.challengesNote.isNotBlank()) append(" Challenges note: ${annotation.challengesNote}.")
                }
            }

            val modeLabel = if (sessionMode == LearnerProfileAnnotation.MODE_TUTORIAL) "tutorial" else "learn"

            val systemInstruction = """
You are an educational assessment assistant for young learners (ages 4-8) learning to read and write letters.
Based on the teacher's annotations, generate a concise 2-4 sentence summary.

The summary should:
- Highlight overall strengths and areas for improvement
- Note specific patterns across items
- Provide one actionable recommendation for the teacher

Respond with ONLY the summary text, no JSON, no markdown formatting.
            """.trimIndent()

            val userPrompt = """
Here are the teacher's annotations for a student's $modeLabel session:

$formattedAnnotations

Generate a concise 2-4 sentence summary.
            """.trimIndent()

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.3f
                    topP = 0.95f
                    maxOutputTokens = 256
                },
                safetySettings = safetySettings,
                systemInstruction = content { text(systemInstruction) }
            )

            val response = model.generateContent(userPrompt)
            val summary = response.text?.trim()

            if (summary.isNullOrBlank()) {
                Log.w(TAG, "Annotation summary generation returned empty")
                return@withContext null
            }

            Log.d(TAG, "Annotation summary generated: $summary")
            summary
        } catch (e: Exception) {
            Log.e(TAG, "Annotation summary generation failed: ${e.message}", e)
            null
        }
    }
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/GeminiRepository.kt
git commit -m "feat: add generateAnnotationSummary method to GeminiRepository"
```

---

### Task 5: Add Summary Loading and Generation to ClassroomViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/classroom/ClassroomViewModel.kt`

**Step 1: Add imports**

At the top of `ClassroomViewModel.kt`, add:

```kotlin
import com.example.app.data.entity.AnnotationSummary
import com.example.app.data.entity.LearnerProfileAnnotation
import com.example.app.data.repository.GeminiRepository
```

**Step 2: Add summaryDao and geminiRepository fields**

Inside the `ClassroomViewModel` class, after the existing `sessionManager` line (line 134), add:

```kotlin
    private val annotationSummaryDao = database.annotationSummaryDao()
    private val geminiRepository = GeminiRepository()
```

**Step 3: Add `annotationSummaries` map to `StudentDetailsUiState`**

Modify the `StudentDetailsUiState` data class to add a summaries map. Add this field after `completedTutorialSessions`:

```kotlin
    val annotationSummaries: Map<String, String> = emptyMap(), // key = "$setId|$sessionMode" -> summaryText
```

**Step 4: Load summaries in `loadStudentDetails()`**

In the `loadStudentDetails` function, after loading `tutorialSessions` (around line 401, just before `uiState = uiState.copy(completedLearnSets = ...)`), add code to load summaries:

```kotlin
                // Load AI annotation summaries
                val summaries = annotationSummaryDao.getSummariesForStudent(studentId.toString())
                val summaryMap = summaries.associate { "${it.setId}|${it.sessionMode}" to it.summaryText }
```

Then update the final `uiState.copy()` call to include the summaries. Change:

```kotlin
                uiState = uiState.copy(
                    completedLearnSets = completedSets,
                    completedTutorialSessions = tutorialSessions
                )
```

To:

```kotlin
                uiState = uiState.copy(
                    completedLearnSets = completedSets,
                    completedTutorialSessions = tutorialSessions,
                    annotationSummaries = summaryMap
                )
```

**Step 5: Add `generateAnnotationSummary` function**

Add this new function to `ClassroomViewModel`, after the `loadStudentDetails` function:

```kotlin
    /**
     * Generate an AI summary for a set/category's annotations and store it.
     * Called after annotation save in session screens. Runs silently — failures are logged but not surfaced.
     */
    fun generateAnnotationSummary(studentId: String, setId: Long, sessionMode: String) {
        viewModelScope.launch {
            try {
                val annotationDao = database.learnerProfileAnnotationDao()
                val annotations = annotationDao.getAnnotationsForStudentInSet(
                    studentId = studentId,
                    setId = setId,
                    sessionMode = sessionMode
                )

                if (annotations.isEmpty()) return@launch

                val summaryText = geminiRepository.generateAnnotationSummary(annotations, sessionMode)
                    ?: return@launch

                val summary = AnnotationSummary(
                    studentId = studentId,
                    setId = setId,
                    sessionMode = sessionMode,
                    summaryText = summaryText
                )
                annotationSummaryDao.insertOrUpdate(summary)
            } catch (e: Exception) {
                // Silent failure — log and move on
                android.util.Log.e("ClassroomViewModel", "Summary generation failed: ${e.message}", e)
            }
        }
    }
```

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/classroom/ClassroomViewModel.kt
git commit -m "feat: add AI summary loading and generation to ClassroomViewModel"
```

---

### Task 6: Update StudentDetailsScreen to Display Summaries

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/classroom/StudentDetailsScreen.kt`

**Step 1: Replace annotation text with summary for tutorial cards**

In `StudentDetailsScreen.kt`, find the tutorial annotation cards section (around line 432-442). Replace the `annotationText` variable construction:

Change:
```kotlin
                val annotationText = session.annotation?.let { annotation ->
                    buildString {
                        if (annotation.strengthsNote.isNotBlank()) {
                            append(annotation.strengthsNote)
                        }
                        if (annotation.challengesNote.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(annotation.challengesNote)
                        }
                    }.takeIf { it.isNotBlank() }
                } ?: "${uiState.studentName} has completed the ${session.tutorialType} ${session.letterType.lowercase()} letters tutorial."
```

To:
```kotlin
                val summaryKey = "${session.setId}|TUTORIAL"
                val annotationText = uiState.annotationSummaries[summaryKey]
                    ?: session.annotation?.let { annotation ->
                        buildString {
                            if (annotation.strengthsNote.isNotBlank()) {
                                append(annotation.strengthsNote)
                            }
                            if (annotation.challengesNote.isNotBlank()) {
                                if (isNotEmpty()) append(" ")
                                append(annotation.challengesNote)
                            }
                        }.takeIf { it.isNotBlank() }
                    }
                    ?: "${uiState.studentName} has completed the ${session.tutorialType} ${session.letterType.lowercase()} letters tutorial."
```

**Step 2: Replace annotation text with summary for learn cards**

Find the learn annotation cards section (around line 479-489). Replace the `annotationText` variable construction:

Change:
```kotlin
                val annotationText = completedSet.annotation?.let { annotation ->
                    buildString {
                        if (annotation.strengthsNote.isNotBlank()) {
                            append(annotation.strengthsNote)
                        }
                        if (annotation.challengesNote.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(annotation.challengesNote)
                        }
                    }.takeIf { it.isNotBlank() }
                } ?: "${uiState.studentName} has completed this activity set."
```

To:
```kotlin
                val summaryKey = "${completedSet.setId}|LEARN"
                val annotationText = uiState.annotationSummaries[summaryKey]
                    ?: completedSet.annotation?.let { annotation ->
                        buildString {
                            if (annotation.strengthsNote.isNotBlank()) {
                                append(annotation.strengthsNote)
                            }
                            if (annotation.challengesNote.isNotBlank()) {
                                if (isNotEmpty()) append(" ")
                                append(annotation.challengesNote)
                            }
                        }.takeIf { it.isNotBlank() }
                    }
                    ?: "${uiState.studentName} has completed this activity set."
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/classroom/StudentDetailsScreen.kt
git commit -m "feat: display AI summaries on annotation cards in StudentDetailsScreen"
```

---

### Task 7: Trigger Summary Generation in LearnModeSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Add imports and get DAO/repository instances**

Near the top of the `LearnModeSessionScreen` composable function, where the existing `database` and `annotationDao` are initialized (around line 160-165), add:

```kotlin
    val annotationSummaryDao = remember { database.annotationSummaryDao() }
    val geminiRepository = remember { com.example.app.data.repository.GeminiRepository() }
```

Also add the import at the top of the file:
```kotlin
import com.example.app.data.entity.AnnotationSummary
```

**Step 2: Add summary generation after session-complete annotation save**

In the `shouldSaveAndComplete` LaunchedEffect (around line 244-305), after all annotations are saved to the database and the set is marked as completed (after the `Log.d("LearnModeSession", "✅ Set marked as completed...")` line, around line 294), add summary generation:

```kotlin
                        // Generate AI summary for this set's annotations
                        try {
                            val allAnnotations = annotationDao.getAnnotationsForStudentInSet(
                                studentId = studentId,
                                setId = setId,
                                sessionMode = LearnerProfileAnnotation.MODE_LEARN
                            )
                            if (allAnnotations.isNotEmpty()) {
                                val summaryText = geminiRepository.generateAnnotationSummary(
                                    allAnnotations, LearnerProfileAnnotation.MODE_LEARN
                                )
                                if (summaryText != null) {
                                    annotationSummaryDao.insertOrUpdate(
                                        AnnotationSummary(
                                            studentId = studentId,
                                            setId = setId,
                                            sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                            summaryText = summaryText
                                        )
                                    )
                                    Log.d("LearnModeSession", "AI summary generated and saved for setId=$setId")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("LearnModeSession", "AI summary generation failed: ${e.message}")
                        }
```

This should be placed inside the `if (studentIdLong != null && activityId > 0)` block, after the set is marked as completed but before the closing brace.

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt
git commit -m "feat: trigger AI summary generation after Learn mode session completion"
```

---

### Task 8: Trigger Summary Generation in TutorialSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Add imports and get DAO/repository instances**

Near the top of the `TutorialSessionScreen` composable function, where the existing `database` and `annotationDao` are initialized (around line 148-149), add:

```kotlin
    val annotationSummaryDao = remember { database.annotationSummaryDao() }
    val geminiRepository = remember { com.example.app.data.repository.GeminiRepository() }
```

Also add the import at the top of the file:
```kotlin
import com.example.app.data.entity.AnnotationSummary
import com.example.app.data.entity.LearnerProfileAnnotation
```

**Step 2: Add summary generation after annotation save**

In the annotation dialog's `onAnnotationSaved` callback (around line 427-446), after `annotationDao.insertOrUpdate(annotation)` (line 443), add summary generation inside the same `withContext(Dispatchers.IO)` block:

```kotlin
                            // Generate AI summary for this tutorial's annotations
                            try {
                                val allAnnotations = annotationDao.getAnnotationsForStudentInSet(
                                    studentIdString,
                                    tutorialSetId,
                                    LearnerProfileAnnotation.MODE_TUTORIAL
                                )
                                if (allAnnotations.isNotEmpty()) {
                                    val summaryText = geminiRepository.generateAnnotationSummary(
                                        allAnnotations, LearnerProfileAnnotation.MODE_TUTORIAL
                                    )
                                    if (summaryText != null) {
                                        annotationSummaryDao.insertOrUpdate(
                                            AnnotationSummary(
                                                studentId = studentIdString,
                                                setId = tutorialSetId,
                                                sessionMode = LearnerProfileAnnotation.MODE_TUTORIAL,
                                                summaryText = summaryText
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TutorialSession", "AI summary generation failed: ${e.message}")
                            }
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt
git commit -m "feat: trigger AI summary generation after Tutorial annotation save"
```

---

### Task 9: Build Verification

**Step 1: Run Gradle build**

Run: `cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

**Step 2: If build fails, fix compilation errors**

Common issues to check:
- Import statements missing
- Mismatched parameter types
- Room annotation processor errors (entity not registered, DAO method signatures)

**Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: resolve build issues for AI annotation summary feature"
```
