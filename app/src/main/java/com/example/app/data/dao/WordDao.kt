package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.Word
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Word entity.
 *
 * Provides methods for CRUD operations on the words table.
 * All methods are either suspend functions for one-shot operations
 * or return Flow for observable queries.
 */
@Dao
interface WordDao {

    /**
     * Insert a new word into the database.
     * If the word already exists for the user (based on unique constraint),
     * the operation will be aborted.
     *
     * @param word The word entity to insert
     * @return The row ID of the newly inserted word
     * @throws SQLiteConstraintException if duplicate word for user
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWord(word: Word): Long

    /**
     * Get all words for a specific user as a Flow.
     * The Flow emits new values whenever the data changes.
     *
     * @param userId The user's ID
     * @return Flow of list of words belonging to the user
     */
    @Query("SELECT * FROM words WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWordsByUserId(userId: Long): Flow<List<Word>>

    /**
     * Get all words for a specific user (one-shot query).
     *
     * @param userId The user's ID
     * @return List of words belonging to the user
     */
    @Query("SELECT * FROM words WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getWordsByUserIdOnce(userId: Long): List<Word>

    /**
     * Get a word by its ID.
     *
     * @param wordId The ID of the word
     * @return The word entity or null if not found
     */
    @Query("SELECT * FROM words WHERE id = :wordId")
    suspend fun getWordById(wordId: Long): Word?

    /**
     * Check if a word already exists for a specific user.
     *
     * @param userId The user's ID
     * @param word The word to check (case-insensitive)
     * @return true if the word exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE userId = :userId AND LOWER(word) = LOWER(:word) LIMIT 1)")
    suspend fun wordExistsForUser(userId: Long, word: String): Boolean

    /**
     * Delete a word by its ID.
     *
     * @param wordId The ID of the word to delete
     * @return The number of rows deleted
     */
    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteWordById(wordId: Long): Int

    /**
     * Update a word's text and image path.
     *
     * @param wordId The ID of the word to update
     * @param word The new word text
     * @param imagePath The new image path (can be null)
     * @return The number of rows updated
     */
    @Query("UPDATE words SET word = :word, imagePath = :imagePath WHERE id = :wordId")
    suspend fun updateWord(wordId: Long, word: String, imagePath: String?): Int

    /**
     * Check if a word already exists for a specific user, excluding a specific word ID.
     * Used for validation when updating a word.
     *
     * @param userId The user's ID
     * @param word The word to check (case-insensitive)
     * @param excludeWordId The word ID to exclude from the check
     * @return true if the word exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE userId = :userId AND LOWER(word) = LOWER(:word) AND id != :excludeWordId LIMIT 1)")
    suspend fun wordExistsForUserExcluding(userId: Long, word: String, excludeWordId: Long): Boolean

    /**
     * Delete all words for a specific user.
     *
     * @param userId The user's ID
     * @return The number of rows deleted
     */
    @Query("DELETE FROM words WHERE userId = :userId")
    suspend fun deleteAllWordsForUser(userId: Long): Int

    /**
     * Get the count of words for a specific user.
     *
     * @param userId The user's ID
     * @return The number of words in the user's Word Bank
     */
    @Query("SELECT COUNT(*) FROM words WHERE userId = :userId")
    suspend fun getWordCountForUser(userId: Long): Int
}
