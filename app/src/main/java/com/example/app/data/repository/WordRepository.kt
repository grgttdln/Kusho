package com.example.app.data.repository

import com.example.app.data.dao.WordDao
import com.example.app.data.entity.Word
import com.example.app.util.WordValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for Word data operations.
 *
 * Provides a clean API for the UI layer to interact with word data.
 * All database operations are performed on the IO dispatcher.
 */
class WordRepository(
    private val wordDao: WordDao
) {

    /**
     * Result class for add word operation.
     */
    sealed class AddWordResult {
        data class Success(val wordId: Long) : AddWordResult()
        data class Error(val message: String) : AddWordResult()
    }

    /**
     * Result class for update word operation.
     */
    sealed class UpdateWordResult {
        data object Success : UpdateWordResult()
        data class Error(val message: String) : UpdateWordResult()
    }

    /**
     * Result class for batch delete word operation.
     */
    sealed class BatchDeleteResult {
        data class Success(val count: Int) : BatchDeleteResult()
        data class Error(val message: String) : BatchDeleteResult()
    }

    /**
     * Add a new word to the user's Word Bank.
     *
     * This method:
     * 1. Validates the word is not empty
     * 2. Checks for duplicates (case-insensitive)
     * 3. Inserts the word into the database
     *
     * @param userId The ID of the user adding the word
     * @param word The word to add
     * @param imagePath Optional path to an associated image
     * @return AddWordResult indicating success or failure
     */
    suspend fun addWord(userId: Long, word: String, imagePath: String? = null): AddWordResult = withContext(Dispatchers.IO) {
        try {
            val trimmedWord = word.trim()

            // Validate word using WordValidator
            val (isValid, errorMessage) = WordValidator.validateWordForBank(trimmedWord)
            if (!isValid) {
                return@withContext AddWordResult.Error(errorMessage ?: "Invalid word")
            }

            // Check for duplicates (case-insensitive)
            if (wordDao.wordExistsForUser(userId, trimmedWord)) {
                return@withContext AddWordResult.Error("This word already exists in your Word Bank")
            }

            // Insert the word
            val wordEntity = Word(
                userId = userId,
                word = trimmedWord,
                imagePath = imagePath
            )
            val wordId = wordDao.insertWord(wordEntity)

            AddWordResult.Success(wordId)
        } catch (e: Exception) {
            AddWordResult.Error(e.message ?: "Failed to add word")
        }
    }

    /**
     * Get all words for a user as a Flow (observable).
     *
     * @param userId The user's ID
     * @return Flow emitting the list of words whenever it changes
     */
    fun getWordsForUser(userId: Long): Flow<List<Word>> {
        return wordDao.getWordsByUserId(userId)
    }

    /**
     * Get all words for a user (one-shot).
     *
     * @param userId The user's ID
     * @return List of words
     */
    suspend fun getWordsForUserOnce(userId: Long): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getWordsByUserIdOnce(userId)
    }

    /**
     * Delete a word by its ID.
     *
     * @param wordId The ID of the word to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteWord(wordId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            wordDao.deleteWordById(wordId) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete multiple words by their IDs (batch delete).
     *
     * @param wordIds The list of word IDs to delete
     * @return BatchDeleteResult indicating success with count or failure
     */
    suspend fun deleteWords(wordIds: List<Long>): BatchDeleteResult = withContext(Dispatchers.IO) {
        try {
            if (wordIds.isEmpty()) {
                return@withContext BatchDeleteResult.Success(0)
            }

            var deletedCount = 0
            wordIds.forEach { wordId ->
                val rowsDeleted = wordDao.deleteWordById(wordId)
                if (rowsDeleted > 0) {
                    deletedCount++
                }
            }

            BatchDeleteResult.Success(deletedCount)
        } catch (e: Exception) {
            BatchDeleteResult.Error(e.message ?: "Failed to delete words")
        }
    }

    /**
     * Update an existing word in the Word Bank.
     *
     * This method:
     * 1. Validates the word is not empty
     * 2. Checks for duplicates (case-insensitive), excluding the current word
     * 3. Updates the word in the database
     *
     * @param userId The ID of the user who owns the word
     * @param wordId The ID of the word to update
     * @param word The new word text
     * @param imagePath Optional new path to an associated image
     * @return UpdateWordResult indicating success or failure
     */
    suspend fun updateWord(
        userId: Long,
        wordId: Long,
        word: String,
        imagePath: String?
    ): UpdateWordResult = withContext(Dispatchers.IO) {
        try {
            val trimmedWord = word.trim()

            // Validate word using WordValidator
            val (isValid, errorMessage) = WordValidator.validateWordForBank(trimmedWord)
            if (!isValid) {
                return@withContext UpdateWordResult.Error(errorMessage ?: "Invalid word")
            }

            // Check for duplicates (case-insensitive), excluding current word
            if (wordDao.wordExistsForUserExcluding(userId, trimmedWord, wordId)) {
                return@withContext UpdateWordResult.Error("This word already exists in your Word Bank")
            }

            // Update the word
            val rowsUpdated = wordDao.updateWord(wordId, trimmedWord, imagePath)

            if (rowsUpdated > 0) {
                UpdateWordResult.Success
            } else {
                UpdateWordResult.Error("Word not found")
            }
        } catch (e: Exception) {
            UpdateWordResult.Error(e.message ?: "Failed to update word")
        }
    }

    /**
     * Get the word count for a user.
     *
     * @param userId The user's ID
     * @return The number of words in the user's Word Bank
     */
    suspend fun getWordCount(userId: Long): Int = withContext(Dispatchers.IO) {
        wordDao.getWordCountForUser(userId)
    }
}
