# Predefined Starter Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Auto-seed every new teacher account with 12 CVC words, 2 word sets, and 1 activity on signup.

**Architecture:** A new `SeedRepository` defines the starter data as constants and inserts it using existing DAOs. `SignUpViewModel` calls the seed method after successful user creation. Images are bundled as assets and copied to internal storage at seed time.

**Tech Stack:** Kotlin, Room DAOs, Android assets, coroutines

---

### Task 1: Create SeedRepository with starter data constants and seed logic

**Files:**
- Create: `app/src/main/java/com/example/app/data/repository/SeedRepository.kt`

- [ ] **Step 1: Create SeedRepository.kt**

```kotlin
package com.example.app.data.repository

import android.content.Context
import com.example.app.data.dao.ActivityDao
import com.example.app.data.dao.SetDao
import com.example.app.data.dao.SetWordDao
import com.example.app.data.dao.WordDao
import com.example.app.data.entity.Activity
import com.example.app.data.entity.ActivitySet
import com.example.app.data.entity.Set
import com.example.app.data.entity.SetWord
import com.example.app.data.entity.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SeedRepository(
    private val wordDao: WordDao,
    private val setDao: SetDao,
    private val setWordDao: SetWordDao,
    private val activityDao: ActivityDao
) {

    private data class SeedWord(
        val word: String,
        val imageAsset: String? = null
    )

    private data class SeedSetWord(
        val word: String,
        val configurationType: String,
        val selectedLetterIndex: Int = 0,
        val hasImage: Boolean = false
    )

    private val set1Words = listOf(
        SeedSetWord("cat", "name the picture", hasImage = true),
        SeedSetWord("dog", "name the picture", hasImage = true),
        SeedSetWord("sun", "name the picture", hasImage = true),
        SeedSetWord("cup", "fill in the blanks", selectedLetterIndex = 1),
        SeedSetWord("hat", "fill in the blanks", selectedLetterIndex = 1),
        SeedSetWord("bed", "write the word")
    )

    private val set2Words = listOf(
        SeedSetWord("pig", "name the picture", hasImage = true),
        SeedSetWord("fox", "name the picture", hasImage = true),
        SeedSetWord("pen", "fill in the blanks", selectedLetterIndex = 1),
        SeedSetWord("map", "fill in the blanks", selectedLetterIndex = 1),
        SeedSetWord("bus", "write the word"),
        SeedSetWord("nut", "write the word")
    )

    private val allSeedWords = listOf(
        SeedWord("cat", "cat.png"),
        SeedWord("dog", "dog.png"),
        SeedWord("sun", "sun.png"),
        SeedWord("cup"),
        SeedWord("hat"),
        SeedWord("bed"),
        SeedWord("pig", "pig.png"),
        SeedWord("fox", "fox.png"),
        SeedWord("pen"),
        SeedWord("map"),
        SeedWord("bus"),
        SeedWord("nut")
    )

    suspend fun seedDefaultData(context: Context, userId: Long) = withContext(Dispatchers.IO) {
        // Step 1: Copy images from assets to internal storage
        val imagePaths = copyStarterImages(context)

        // Step 2: Insert words and collect IDs
        val wordIdMap = mutableMapOf<String, Long>()
        for (seedWord in allSeedWords) {
            val imagePath = seedWord.imageAsset?.let { imagePaths[it] }
            val wordEntity = Word(
                userId = userId,
                word = seedWord.word,
                imagePath = imagePath
            )
            val wordId = wordDao.insertWord(wordEntity)
            wordIdMap[seedWord.word] = wordId
        }

        // Step 3: Insert sets
        val now = System.currentTimeMillis()
        val set1Id = setDao.insertSet(
            Set(
                userId = userId,
                title = "Starter: Simple CVC Words",
                description = "Practice simple three-letter words",
                itemCount = set1Words.size,
                createdAt = now,
                updatedAt = now
            )
        )
        val set2Id = setDao.insertSet(
            Set(
                userId = userId,
                title = "Starter: More CVC Words",
                description = "More three-letter words to practice",
                itemCount = set2Words.size,
                createdAt = now + 1,
                updatedAt = now + 1
            )
        )

        // Step 4: Insert set-word junctions
        val set1SetWords = set1Words.map { seedSetWord ->
            val wordId = wordIdMap[seedSetWord.word]!!
            val imagePath = if (seedSetWord.hasImage) imagePaths["${seedSetWord.word}.png"] else null
            SetWord(
                setId = set1Id,
                wordId = wordId,
                configurationType = seedSetWord.configurationType,
                selectedLetterIndex = seedSetWord.selectedLetterIndex,
                imagePath = imagePath
            )
        }
        setWordDao.insertSetWords(set1SetWords)

        val set2SetWords = set2Words.map { seedSetWord ->
            val wordId = wordIdMap[seedSetWord.word]!!
            val imagePath = if (seedSetWord.hasImage) imagePaths["${seedSetWord.word}.png"] else null
            SetWord(
                setId = set2Id,
                wordId = wordId,
                configurationType = seedSetWord.configurationType,
                selectedLetterIndex = seedSetWord.selectedLetterIndex,
                imagePath = imagePath
            )
        }
        setWordDao.insertSetWords(set2SetWords)

        // Step 5: Insert activity
        val activityId = activityDao.insertActivity(
            Activity(
                userId = userId,
                title = "Starter: CVC Word Practice",
                description = "Practice reading and writing simple CVC words",
                createdAt = now,
                updatedAt = now
            )
        )

        // Step 6: Link sets to activity
        setDao.insertActivitySet(ActivitySet(activityId = activityId, setId = set1Id, addedAt = now))
        setDao.insertActivitySet(ActivitySet(activityId = activityId, setId = set2Id, addedAt = now + 1))
    }

    private fun copyStarterImages(context: Context): Map<String, String> {
        val imageDir = File(context.filesDir, "word_images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val imageAssets = listOf("cat.png", "dog.png", "sun.png", "pig.png", "fox.png")
        val paths = mutableMapOf<String, String>()

        for (assetName in imageAssets) {
            val destFile = File(imageDir, "starter_$assetName")
            if (!destFile.exists()) {
                context.assets.open("starter_images/$assetName").use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            paths[assetName] = destFile.absolutePath
        }

        return paths
    }
}
```

- [ ] **Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (may warn about missing assets, which is fine — images not added yet)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/SeedRepository.kt
git commit -m "feat: add SeedRepository with starter word bank, sets, and activity data"
```

---

### Task 2: Create placeholder starter image assets directory

**Files:**
- Create: `app/src/main/assets/starter_images/.gitkeep`

Note: The user will provide actual image files (`cat.png`, `dog.png`, `sun.png`, `pig.png`, `fox.png`). This task creates the directory structure. The app will fail gracefully if images are missing during development (the `assets.open()` call in `copyStarterImages` will throw, but the whole seed is wrapped in try/catch in the ViewModel).

- [ ] **Step 1: Create the assets directory with a placeholder**

```bash
mkdir -p app/src/main/assets/starter_images
touch app/src/main/assets/starter_images/.gitkeep
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/starter_images/.gitkeep
git commit -m "chore: add starter_images asset directory for predefined word bank images"
```

---

### Task 3: Integrate SeedRepository into SignUpViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpViewModel.kt`

- [ ] **Step 1: Add SeedRepository import and instance**

At the top of `SignUpViewModel.kt`, add the import:

```kotlin
import com.example.app.data.repository.SeedRepository
import android.util.Log
```

Inside the class body, after line 18 (`private val sessionManager = ...`), add:

```kotlin
    private val seedRepository = SeedRepository(
        database.wordDao(),
        database.setDao(),
        database.setWordDao(),
        database.activityDao()
    )
```

- [ ] **Step 2: Call seedDefaultData after successful signup**

Replace the `SignUpResult.Success` branch (lines 42-54) with:

```kotlin
                is UserRepository.SignUpResult.Success -> {
                    val newUser = userRepository.getUserById(result.userId)
                    newUser?.let { user ->
                        sessionManager.saveUserSession(user, staySignedIn = true)
                    }

                    // Seed starter word bank, sets, and activity (non-fatal)
                    try {
                        seedRepository.seedDefaultData(getApplication(), result.userId)
                    } catch (e: Exception) {
                        Log.e("SignUpViewModel", "Failed to seed starter data", e)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        userId = result.userId,
                        errorMessage = null
                    )
                    onSuccess()
                }
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpViewModel.kt
git commit -m "feat: seed starter word bank and activity on new user signup"
```

---

### Task 4: Verify DAO access methods exist on AppDatabase

**Files:**
- Read (verify only): `app/src/main/java/com/example/app/data/AppDatabase.kt`

- [ ] **Step 1: Verify AppDatabase exposes all needed DAOs**

Read `AppDatabase.kt` and confirm these abstract methods exist:
- `abstract fun wordDao(): WordDao`
- `abstract fun setDao(): SetDao`
- `abstract fun setWordDao(): SetWordDao`
- `abstract fun activityDao(): ActivityDao`

If any are missing, add them. Based on codebase exploration, all four should already exist.

- [ ] **Step 2: Full build verification**

Run: `./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

---

### Task 5: Manual smoke test checklist

This task has no code changes — it's a verification checklist for when images are provided.

- [ ] **Step 1: Add starter images**

Once the user provides the 5 image files (`cat.png`, `dog.png`, `sun.png`, `pig.png`, `fox.png`), copy them into `app/src/main/assets/starter_images/`.

- [ ] **Step 2: Build and install**

Run: `./gradlew installDebug`

- [ ] **Step 3: Smoke test**

1. Create a new account
2. Navigate to Word Bank — verify 12 words appear (cat, dog, sun, cup, hat, bed, pig, fox, pen, map, bus, nut)
3. Verify cat, dog, sun, pig, fox have images
4. Navigate to Sets — verify "Starter: Simple CVC Words" and "Starter: More CVC Words" appear
5. Navigate to Activities — verify "Starter: CVC Word Practice" appears with both sets linked
6. Verify all starter data is editable and deletable
