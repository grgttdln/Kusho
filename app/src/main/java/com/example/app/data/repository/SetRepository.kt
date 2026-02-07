package com.example.app.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.app.data.AppDatabase
import com.example.app.data.dao.SetDao
import com.example.app.data.dao.SetWordDao
import com.example.app.data.dao.WordDao
import com.example.app.data.entity.ActivitySet
import com.example.app.data.entity.Set
import com.example.app.data.entity.SetWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

const val TAG_REPO = "SetRepository"

/**
 * Repository for Set data operations.
 *
 * Provides a clean API for the UI layer to interact with set data.
 * All database operations are performed on the IO dispatcher.
 */
class SetRepository(
    private val database: AppDatabase
) {
    private val setDao: SetDao = database.setDao()
    private val setWordDao: SetWordDao = database.setWordDao()
    private val wordDao: WordDao = database.wordDao()

    /**
     * Result class for add set operation.
     */
    sealed class AddSetResult {
        data class Success(val setId: Long) : AddSetResult()
        data class Error(val message: String) : AddSetResult()
    }

    /**
     * Validates that all selected words exist in the user's word list.
     * Returns a Pair of (isValid, missingWords).
     */
    private fun validateWordsExist(
        selectedWords: List<SelectedWordConfig>,
        wordMap: Map<String, com.example.app.data.entity.Word>
    ): Pair<Boolean, List<String>> {
        val missingWords = selectedWords.map { it.wordName }.filter { it !in wordMap }
        return Pair(missingWords.isEmpty(), missingWords)
    }

    /**
     * Data class representing a selected word with its configuration type.
     */
    data class SelectedWordConfig(
        val wordName: String,
        val configurationType: String,
        val selectedLetterIndex: Int = 0, // Index of the selected letter for "Fill in the Blank"
        val imagePath: String? = null // Optional image path for "Name the Picture" question type
    )

    /**
     * Add a new set (independent of activities)
     * Sets are created independently and can be linked to activities later
     *
     * @param userId The user creating the set
     * @param title The set title
     * @param description The set description
     * @return Result of the operation
     */
    suspend fun addSet(
        userId: Long,
        title: String,
        description: String? = null
    ): AddSetResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (title.isBlank()) {
                AddSetResult.Error("Set title cannot be empty")
            } else if (setDao.setTitleExistsForUser(userId, title.trim())) {
                AddSetResult.Error("A set with this title already exists")
            } else {
                val set = Set(
                    userId = userId,
                    title = title.trim(),
                    description = description?.trim(),
                    itemCount = 0
                )
                val setId = setDao.insertSet(set)
                AddSetResult.Success(setId)
            }
        } catch (e: Exception) {
            AddSetResult.Error(e.message ?: "Failed to add set")
        }
    }

    /**
     * Add a new set with associated words and their configuration types.
     * Sets are created independently - they are not tied to any activity during creation.
     * This operation is atomic - if any part fails, the entire operation is rolled back.
     *
     * @param title The set title
     * @param description The set description
     * @param userId The user ID (needed to lookup words)
     * @param selectedWords List of selected words with their configuration types
     * @return Result of the operation
     */
    suspend fun addSetWithWords(
        title: String,
        description: String? = null,
        userId: Long,
        selectedWords: List<SelectedWordConfig>
    ): AddSetResult = withContext(Dispatchers.IO) {
        Log.d(TAG_REPO, "🟢 addSetWithWords() CALLED")
        Log.d(TAG_REPO, "  - title: '$title'")
        Log.d(TAG_REPO, "  - description: '$description'")
        Log.d(TAG_REPO, "  - userId: $userId")
        Log.d(TAG_REPO, "  - selectedWords count: ${selectedWords.size}")
        
        // Pre-validation before any database operations
        if (title.isBlank()) {
            Log.e(TAG_REPO, "❌ Validation error: Title is blank")
            return@withContext AddSetResult.Error("Set title cannot be empty")
        }
        
        if (selectedWords.isEmpty()) {
            Log.e(TAG_REPO, "❌ Validation error: No words selected")
            return@withContext AddSetResult.Error("At least one word must be selected")
        }
        
        if (selectedWords.size < 3) {
            Log.e(TAG_REPO, "❌ Validation error: Less than 3 words")
            return@withContext AddSetResult.Error("Set must contain at least 3 words")
        }
        
        if (setDao.setTitleExistsForUser(userId, title.trim())) {
            Log.e(TAG_REPO, "❌ Validation error: Duplicate set title")
            return@withContext AddSetResult.Error("A set with this title already exists")
        }
        
        // Load words and validate ALL exist BEFORE any DB operations
        Log.d(TAG_REPO, "📝 Loading words for userId: $userId")
        val userWords = wordDao.getWordsByUserIdOnce(userId)
        Log.d(TAG_REPO, "✅ Loaded ${userWords.size} words for user")
        
        val wordMap = userWords.associateBy { it.word }
        val (allWordsExist, missingWords) = validateWordsExist(selectedWords, wordMap)
        
        if (!allWordsExist) {
            Log.e(TAG_REPO, "❌ Validation error: Words not found in database: $missingWords")
            return@withContext AddSetResult.Error(
                "Some words were not found: ${missingWords.joinToString(", ")}. Please refresh and try again."
            )
        }
        
        Log.d(TAG_REPO, "✅ All ${selectedWords.size} words validated successfully")
        
        // ATOMIC TRANSACTION: All operations succeed or all fail
        return@withContext try {
            database.withTransaction {
                Log.d(TAG_REPO, "📝 Starting atomic transaction...")
                
                // Create and insert the set
                val set = Set(
                    userId = userId,
                    title = title.trim(),
                    description = description?.trim(),
                    itemCount = selectedWords.size
                )
                Log.d(TAG_REPO, "📝 Inserting Set entity: $set")
                val setId = setDao.insertSet(set)
                Log.d(TAG_REPO, "✅ Set inserted with ID: $setId")

                // Create SetWord entries for each selected word (all words are guaranteed to exist)
                Log.d(TAG_REPO, "📝 Creating SetWord entries for ${selectedWords.size} selected words...")
                val setWords = selectedWords.map { selected ->
                    val word = wordMap[selected.wordName]!!
                    Log.d(TAG_REPO, "  ✅ Creating SetWord for '${selected.wordName}' with ID: ${word.id}")
                    SetWord(
                        setId = setId,
                        wordId = word.id,
                        configurationType = selected.configurationType,
                        selectedLetterIndex = selected.selectedLetterIndex,
                        imagePath = selected.imagePath // Persist the image path
                    )
                }
                
                Log.d(TAG_REPO, "📝 Inserting ${setWords.size} SetWord entries...")
                setWordDao.insertSetWords(setWords)
                Log.d(TAG_REPO, "✅ SetWords inserted successfully")
                
                Log.d(TAG_REPO, "✅ Transaction committed successfully")
                setId
            }.let { setId ->
                Log.d(TAG_REPO, "✅ addSetWithWords() SUCCESS - returning setId: $setId")
                AddSetResult.Success(setId)
            }
        } catch (e: Exception) {
            Log.e(TAG_REPO, "❌ Transaction failed in addSetWithWords: ${e.message}", e)
            AddSetResult.Error("Failed to create set: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Link a set to an activity.
     *
     * @param setId The ID of the set
     * @param activityId The ID of the activity
     * @return true if successful, false otherwise
     */
    suspend fun linkSetToActivity(setId: Long, activityId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val activitySet = ActivitySet(
                activityId = activityId,
                setId = setId
            )
            setDao.insertActivitySet(activitySet)
            Log.d(TAG_REPO, "✅ Linked set $setId to activity $activityId")
            true
        } catch (e: Exception) {
            Log.e(TAG_REPO, "❌ Failed to link set to activity: ${e.message}")
            false
        }
    }

    /**
     * Get all sets created by a specific user as a Flow.
     * Since sets are independent, all sets created by a user are returned.
     *
     * @param userId The user ID
     * @return Flow of sets created by the user
     */
    fun getSetsForUser(userId: Long): Flow<List<Set>> {
        return setDao.getSetsForUser(userId)
    }

    /**
     * Get sets for a specific activity.
     *
     * @param activityId The activity ID
     * @return Flow of sets belonging to the activity
     */
    fun getSetsForActivity(activityId: Long): Flow<List<Set>> {
        return setDao.getSetsForActivity(activityId)
    }

    /**
     * Unlink a set from an activity.
     * This only removes the link - the original set is NOT deleted.
     *
     * @param setId The ID of the set
     * @param activityId The ID of the activity
     * @return true if successful, false otherwise
     */
    suspend fun unlinkSetFromActivity(setId: Long, activityId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val rowsDeleted = setDao.unlinkSetFromActivity(setId, activityId)
            Log.d(TAG_REPO, "✅ Unlinked set $setId from activity $activityId (rows: $rowsDeleted)")
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e(TAG_REPO, "❌ Failed to unlink set from activity: ${e.message}")
            false
        }
    }

    /**
     * Get all sets from the database as a Flow.
     *
     * @return Flow of all sets
     */
    fun getAllSets(): Flow<List<Set>> {
        return setDao.getAllSets()
    }

    /**
     * Data class representing set details with words and their configuration types.
     */
    data class SetDetails(
        val set: Set,
        val words: List<WordWithConfig>
    )

    /**
     * Data class representing a word with its configuration type in a set.
     */
    data class WordWithConfig(
        val word: String,
        val configurationType: String,
        val selectedLetterIndex: Int = 0,
        val imagePath: String? = null
    )

    /**
     * Get detailed information about a set including its words and configuration types.
     * Uses imagePath from SetWord (persisted during set creation) rather than Word entity.
     *
     * @param setId The ID of the set
     * @return SetDetails containing the set info and associated words
     */
    suspend fun getSetDetails(setId: Long): SetDetails? = withContext(Dispatchers.IO) {
        return@withContext try {
            val set = setDao.getSetById(setId) ?: return@withContext null
            val setWords = setWordDao.getSetWords(setId)

            val words = setWords.map { setWord ->
                val word = wordDao.getWordById(setWord.wordId)
                WordWithConfig(
                    word = word?.word ?: "Unknown",
                    configurationType = setWord.configurationType,
                    selectedLetterIndex = setWord.selectedLetterIndex,
                    imagePath = setWord.imagePath // Use persisted imagePath from SetWord
                )
            }

            SetDetails(set = set, words = words)
        } catch (e: Exception) {
            Log.e(TAG_REPO, "Error getting set details: ${e.message}")
            null
        }
    }

    /**
     * Delete a set by its ID.
     *
     * @param setId The ID of the set to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteSet(setId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            setDao.deleteSetById(setId) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update an existing set with new title, description, and words.
     * This operation is atomic - if any part fails, the entire operation is rolled back,
     * preventing data loss (the set will retain its original words).
     *
     * @param setId The ID of the set to update
     * @param title The new title
     * @param description The new description (can be null)
     * @param userId The user ID (needed to lookup words)
     * @param selectedWords List of selected words with their configuration types
     * @return Result of the operation
     */
    suspend fun updateSetWithWords(
        setId: Long,
        title: String,
        description: String?,
        userId: Long,
        selectedWords: List<SelectedWordConfig>
    ): AddSetResult = withContext(Dispatchers.IO) {
        Log.d(TAG_REPO, "🟢 updateSetWithWords() CALLED")
        Log.d(TAG_REPO, "  - setId: $setId")
        Log.d(TAG_REPO, "  - title: '$title'")
        Log.d(TAG_REPO, "  - description: '$description'")
        Log.d(TAG_REPO, "  - selectedWords count: ${selectedWords.size}")

        // Pre-validation before any database operations
        if (title.isBlank()) {
            Log.e(TAG_REPO, "❌ Validation error: Title is blank")
            return@withContext AddSetResult.Error("Set title cannot be empty")
        }

        if (title.trim().length > 30) {
            Log.e(TAG_REPO, "❌ Validation error: Title too long")
            return@withContext AddSetResult.Error("Set title must be 30 characters or less")
        }

        if (selectedWords.isEmpty()) {
            Log.e(TAG_REPO, "❌ Validation error: No words selected")
            return@withContext AddSetResult.Error("At least one word must be selected")
        }

        if (selectedWords.size < 3) {
            Log.e(TAG_REPO, "❌ Validation error: Less than 3 words")
            return@withContext AddSetResult.Error("Set must contain at least 3 words")
        }

        if (setDao.setTitleExistsForUserExcluding(userId, title.trim(), setId)) {
            Log.e(TAG_REPO, "❌ Validation error: Duplicate set title")
            return@withContext AddSetResult.Error("A set with this title already exists")
        }

        // Load words and validate ALL exist BEFORE any DB operations
        Log.d(TAG_REPO, "📝 Loading words for userId: $userId")
        val userWords = wordDao.getWordsByUserIdOnce(userId)
        Log.d(TAG_REPO, "✅ Loaded ${userWords.size} words for user")

        val wordMap = userWords.associateBy { it.word }
        val (allWordsExist, missingWords) = validateWordsExist(selectedWords, wordMap)

        if (!allWordsExist) {
            Log.e(TAG_REPO, "❌ Validation error: Words not found in database: $missingWords")
            return@withContext AddSetResult.Error(
                "Some words were not found: ${missingWords.joinToString(", ")}. Please refresh and try again."
            )
        }

        Log.d(TAG_REPO, "✅ All ${selectedWords.size} words validated successfully")

        // ATOMIC TRANSACTION: All operations succeed or all fail (prevents data loss)
        return@withContext try {
            database.withTransaction {
                Log.d(TAG_REPO, "📝 Starting atomic transaction...")

                // Update the set
                val updatedRows = setDao.updateSet(
                    setId = setId,
                    title = title.trim(),
                    description = description?.trim(),
                    itemCount = selectedWords.size,
                    updatedAt = System.currentTimeMillis()
                )
                Log.d(TAG_REPO, "✅ Set updated, rows affected: $updatedRows")

                // Delete existing set-word relationships
                val deletedCount = setWordDao.deleteSetWords(setId)
                Log.d(TAG_REPO, "✅ Deleted $deletedCount existing SetWord entries")

                // Create new SetWord entries for each selected word (all guaranteed to exist)
                Log.d(TAG_REPO, "📝 Creating SetWord entries for ${selectedWords.size} selected words...")
                val setWords = selectedWords.map { selected ->
                    val word = wordMap[selected.wordName]!!
                    Log.d(TAG_REPO, "  ✅ Creating SetWord for '${selected.wordName}' with ID: ${word.id}")
                    SetWord(
                        setId = setId,
                        wordId = word.id,
                        configurationType = selected.configurationType,
                        selectedLetterIndex = selected.selectedLetterIndex,
                        imagePath = selected.imagePath // Persist the image path
                    )
                }

                Log.d(TAG_REPO, "📝 Inserting ${setWords.size} SetWord entries...")
                setWordDao.insertSetWords(setWords)
                Log.d(TAG_REPO, "✅ SetWords inserted successfully")

                Log.d(TAG_REPO, "✅ Transaction committed successfully")
                setId
            }.let { setId ->
                Log.d(TAG_REPO, "✅ updateSetWithWords() SUCCESS")
                AddSetResult.Success(setId)
            }
        } catch (e: Exception) {
            Log.e(TAG_REPO, "❌ Transaction failed in updateSetWithWords: ${e.message}", e)
            AddSetResult.Error("Failed to update set: ${e.message ?: "Unknown error"}")
        }
    }
}
