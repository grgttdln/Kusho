# AI Activity Description Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an AI-generated description under the main title in TutorialAnnotationDetailsScreen and LearnAnnotationDetailsScreen, cached in a new DB table.

**Architecture:** New `ActivityDescriptionCache` Room entity + DAO for caching. New `generateActivityDescription()` method in GeminiRepository using Gemini 2.5 Flash Lite. Both screens load from cache first, generate on miss, save to cache, display below title.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Google Generative AI SDK (Gemini)

---

### Task 1: Create ActivityDescriptionCache Entity

**Files:**
- Create: `app/src/main/java/com/example/app/data/entity/ActivityDescriptionCache.kt`

**Step 1: Create the entity file**

```kotlin
package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_description_cache",
    indices = [
        Index(value = ["setId", "sessionMode", "activityId"], unique = true)
    ]
)
data class ActivityDescriptionCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setId: Long,
    val sessionMode: String,
    val activityId: Long = 0L,
    val descriptionText: String,
    val generatedAt: Long = System.currentTimeMillis()
)
```

---

### Task 2: Create ActivityDescriptionCacheDao

**Files:**
- Create: `app/src/main/java/com/example/app/data/dao/ActivityDescriptionCacheDao.kt`

**Step 1: Create the DAO file**

```kotlin
package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.ActivityDescriptionCache

@Dao
interface ActivityDescriptionCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: ActivityDescriptionCache): Long

    @Query("""
        SELECT * FROM activity_description_cache
        WHERE setId = :setId AND sessionMode = :sessionMode AND activityId = :activityId
        LIMIT 1
    """)
    suspend fun getDescription(setId: Long, sessionMode: String, activityId: Long = 0L): ActivityDescriptionCache?
}
```

---

### Task 3: Register in AppDatabase

**Files:**
- Modify: `app/src/main/java/com/example/app/data/AppDatabase.kt`

**Step 1: Add import for the new entity and DAO**

Add these two imports after the existing DAO/Entity imports (after line 20 and line 35 respectively):

```kotlin
import com.example.app.data.dao.ActivityDescriptionCacheDao
```

```kotlin
import com.example.app.data.entity.ActivityDescriptionCache
```

**Step 2: Add entity to the @Database annotation**

In the entities array, add `ActivityDescriptionCache::class` after `TutorialCompletion::class`:

Change:
```kotlin
        TutorialCompletion::class
    ],
    version = 15, // Renamed email to username, removed school field
```

To:
```kotlin
        TutorialCompletion::class,
        ActivityDescriptionCache::class
    ],
    version = 16, // Added activity description cache
```

**Step 3: Add abstract DAO function**

After `abstract fun tutorialCompletionDao(): TutorialCompletionDao`, add:

```kotlin
    abstract fun activityDescriptionCacheDao(): ActivityDescriptionCacheDao
```

---

### Task 4: Add generateActivityDescription to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add the new method**

Add this method after the `generateAnnotationSummary` method (after line 318, before the `// ========== Step Functions ==========` comment):

```kotlin
    /**
     * Generate a short description of an activity or tutorial lesson.
     *
     * @param activityTitle The title of the activity/lesson (e.g., "Uppercase Vowels", "cat/bat/hat")
     * @param sessionMode "LEARN" or "TUTORIAL"
     * @param items List of item names (letters for tutorials, words for learn mode)
     * @param configurations Optional list of configuration types for learn mode (e.g., "fill in the blanks", "write the word")
     * @return Generated description text, or null if generation fails
     */
    suspend fun generateActivityDescription(
        activityTitle: String,
        sessionMode: String,
        items: List<String>,
        configurations: List<String>? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val isTutorial = sessionMode == "TUTORIAL"
            val itemType = if (isTutorial) "letters" else "words"

            val configInfo = if (!isTutorial && !configurations.isNullOrEmpty()) {
                val uniqueConfigs = configurations.distinct()
                "\nActivity types used: ${uniqueConfigs.joinToString(", ")}."
            } else ""

            val systemInstruction = """
You are an educational content assistant for a children's reading and writing app (ages 4-8).
Generate a single concise sentence (max 15 words) describing what this lesson covers.
The description should be simple, encouraging, and help teachers understand the lesson at a glance.
Respond with ONLY the description text, no JSON, no markdown, no quotes.
            """.trimIndent()

            val userPrompt = """
Lesson title: $activityTitle
Mode: ${if (isTutorial) "Tutorial (letter practice)" else "Learn (word practice)"}
${itemType.replaceFirstChar { it.uppercase() }} covered: ${items.joinToString(", ")}$configInfo

Write one short sentence describing this lesson.
            """.trimIndent()

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.3f
                    topP = 0.95f
                    maxOutputTokens = 64
                },
                safetySettings = safetySettings,
                systemInstruction = content { text(systemInstruction) }
            )

            val response = model.generateContent(userPrompt)
            val description = response.text?.trim()

            if (description.isNullOrBlank()) {
                Log.w(TAG, "Activity description generation returned empty")
                return@withContext null
            }

            Log.d(TAG, "Activity description generated: $description")
            description
        } catch (e: Exception) {
            Log.e(TAG, "Activity description generation failed: ${e.message}", e)
            null
        }
    }
```

---

### Task 5: Update TutorialAnnotationDetailsScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/classroom/TutorialAnnotationDetailsScreen.kt`

**Step 1: Add imports**

Add these imports after the existing imports:

```kotlin
import androidx.compose.ui.text.font.FontStyle
import com.example.app.data.entity.ActivityDescriptionCache
import com.example.app.data.repository.GeminiRepository
```

**Step 2: Add state variables**

After line 66 (`var annotationsMap by remember { mutableStateOf(mapOf<Int, LearnerProfileAnnotation?>()) }`), add:

```kotlin
    var descriptionText by remember { mutableStateOf<String?>(null) }
    var isLoadingDescription by remember { mutableStateOf(true) }
```

**Step 3: Add description loading logic**

After the existing `LaunchedEffect` block (after line 85, the closing `}`), add a new `LaunchedEffect`:

```kotlin
    // Load or generate activity description
    LaunchedEffect(setId) {
        isLoadingDescription = true
        val descriptionDao = database.activityDescriptionCacheDao()
        val cached = descriptionDao.getDescription(
            setId = setId,
            sessionMode = "TUTORIAL",
            activityId = 0L
        )
        if (cached != null) {
            descriptionText = cached.descriptionText
            isLoadingDescription = false
        } else {
            // Generate via AI
            val geminiRepository = GeminiRepository()
            val generated = geminiRepository.generateActivityDescription(
                activityTitle = displayTitle,
                sessionMode = "TUTORIAL",
                items = letters
            )
            if (generated != null) {
                descriptionText = generated
                descriptionDao.insertOrUpdate(
                    ActivityDescriptionCache(
                        setId = setId,
                        sessionMode = "TUTORIAL",
                        activityId = 0L,
                        descriptionText = generated
                    )
                )
            }
            isLoadingDescription = false
        }
    }
```

**Step 4: Add description UI**

After the main title Text composable (after line 141, the closing `)` of the main title Text), add:

```kotlin

                Spacer(Modifier.height(8.dp))

                // AI-generated description
                if (isLoadingDescription) {
                    Text(
                        text = "Generating description...",
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF999999)
                    )
                } else if (descriptionText != null) {
                    Text(
                        text = descriptionText!!,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF666666)
                    )
                }
```

---

### Task 6: Update LearnAnnotationDetailsScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/classroom/LearnAnnotationDetailsScreen.kt`

**Step 1: Add imports**

Add these imports after the existing imports:

```kotlin
import androidx.compose.ui.text.font.FontStyle
import com.example.app.data.entity.ActivityDescriptionCache
import com.example.app.data.repository.GeminiRepository
```

**Step 2: Add state variables**

After line 48 (`var annotationsMap by remember { mutableStateOf(mapOf<Long, LearnerProfileAnnotation?>()) }`), add:

```kotlin
    var descriptionText by remember { mutableStateOf<String?>(null) }
    var isLoadingDescription by remember { mutableStateOf(true) }
```

**Step 3: Add description loading logic**

After the existing `LaunchedEffect` block (after line 82, the closing `}`), add a new `LaunchedEffect`:

```kotlin
    // Load or generate activity description
    LaunchedEffect(setId, activityId) {
        isLoadingDescription = true
        val descriptionDao = database.activityDescriptionCacheDao()
        val cached = descriptionDao.getDescription(
            setId = setId,
            sessionMode = "LEARN",
            activityId = activityId
        )
        if (cached != null) {
            descriptionText = cached.descriptionText
            isLoadingDescription = false
        } else {
            // Wait for words to load first, then generate
            val setWordDao = database.setWordDao()
            val wordDao = database.wordDao()
            val loadedSetWords = setWordDao.getSetWords(setId)
            val wordNames = loadedSetWords.mapNotNull { sw ->
                wordDao.getWordById(sw.wordId)?.word
            }
            val configs = loadedSetWords.map { it.configurationType }

            if (wordNames.isNotEmpty()) {
                val geminiRepository = GeminiRepository()
                val generated = geminiRepository.generateActivityDescription(
                    activityTitle = lessonName,
                    sessionMode = "LEARN",
                    items = wordNames,
                    configurations = configs
                )
                if (generated != null) {
                    descriptionText = generated
                    descriptionDao.insertOrUpdate(
                        ActivityDescriptionCache(
                            setId = setId,
                            sessionMode = "LEARN",
                            activityId = activityId,
                            descriptionText = generated
                        )
                    )
                }
            }
            isLoadingDescription = false
        }
    }
```

**Step 4: Add description UI**

After the main title Text composable (after line 138, the closing `)` of the `lessonName` Text), add:

```kotlin

                Spacer(Modifier.height(8.dp))

                // AI-generated description
                if (isLoadingDescription) {
                    Text(
                        text = "Generating description...",
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF999999)
                    )
                } else if (descriptionText != null) {
                    Text(
                        text = descriptionText!!,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF666666)
                    )
                }
```

---

### Task 7: Build Verification

**Step 1: Run the Gradle build**

```bash
cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. If compilation errors, fix before proceeding.
