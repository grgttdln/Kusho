package com.example.app.data.repository

import com.example.app.data.dao.WordDao
import com.example.app.data.entity.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for Word data operations.
 *
 * Provides a clean API for the UI layer to interact with word data.
 * All database operations are performed on the IO dispatcher.
 */
class WordRepository(private val wordDao: WordDao) {

    /**
     * Result class for add word operation.
     */
    sealed class AddWordResult {
        data class Success(val wordId: Long) : AddWordResult()
        data class Error(val message: String) : AddWordResult()
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
     * @return AddWordResult indicating success or failure
     */
    suspend fun addWord(userId: Long, word: String): AddWordResult = withContext(Dispatchers.IO) {
        try {
            val trimmedWord = word.trim()

            // Validate word is not empty
            if (trimmedWord.isBlank()) {
                return@withContext AddWordResult.Error("Word cannot be empty")
            }

            // Validate word contains only letters
            if (!trimmedWord.all { it.isLetter() }) {
                return@withContext AddWordResult.Error("Word can only contain letters")
            }

            // Check for duplicates (case-insensitive)
            if (wordDao.wordExistsForUser(userId, trimmedWord)) {
                return@withContext AddWordResult.Error("This word already exists in your Word Bank")
            }

            // Insert the word
            val wordEntity = Word(
                userId = userId,
                word = trimmedWord
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
     * Get the word count for a user.
     *
     * @param userId The user's ID
     * @return The number of words in the user's Word Bank
     */
    suspend fun getWordCount(userId: Long): Int = withContext(Dispatchers.IO) {
        wordDao.getWordCountForUser(userId)
    }
}
