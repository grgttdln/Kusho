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
        SeedSetWord("cat", "Name the Picture", hasImage = true),
        SeedSetWord("dog", "Name the Picture", hasImage = true),
        SeedSetWord("sun", "Name the Picture", hasImage = true),
        SeedSetWord("cup", "Fill in the Blank", selectedLetterIndex = 1),
        SeedSetWord("hat", "Fill in the Blank", selectedLetterIndex = 1),
        SeedSetWord("bed", "Write the Word")
    )

    private val set2Words = listOf(
        SeedSetWord("pig", "Name the Picture", hasImage = true),
        SeedSetWord("fox", "Name the Picture", hasImage = true),
        SeedSetWord("pen", "Fill in the Blank", selectedLetterIndex = 1),
        SeedSetWord("map", "Fill in the Blank", selectedLetterIndex = 1),
        SeedSetWord("bus", "Write the Word"),
        SeedSetWord("nut", "Write the Word")
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
        // Skip if user already has words (e.g., retry after partial signup)
        if (wordDao.getWordCountForUser(userId) > 0) return@withContext

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
                title = "Simple CVC Words",
                description = "Practice simple three-letter words",
                itemCount = set1Words.size,
                createdAt = now,
                updatedAt = now
            )
        )
        val set2Id = setDao.insertSet(
            Set(
                userId = userId,
                title = "More CVC Words",
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
                title = "CVC Word Practice",
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
