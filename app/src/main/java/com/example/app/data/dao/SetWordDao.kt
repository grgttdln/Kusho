package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.SetWord

/**
 * Data Access Object for SetWord operations.
 */
@Dao
interface SetWordDao {

    /**
     * Insert a new set-word relationship.
     *
     * @param setWord The SetWord entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetWord(setWord: SetWord): Long

    /**
     * Insert multiple set-word relationships.
     *
     * @param setWords List of SetWord entities to insert
     * @return List of row IDs of inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetWords(setWords: List<SetWord>): List<Long>

    /**
     * Get all words for a specific set.
     *
     * @param setId The ID of the set
     * @return List of SetWord entries for the set
     */
    @Query("SELECT * FROM set_words WHERE setId = :setId")
    suspend fun getSetWords(setId: Long): List<SetWord>

    /**
     * Delete all set-word relationships for a specific set.
     *
     * @param setId The ID of the set
     * @return Number of rows deleted
     */
    @Query("DELETE FROM set_words WHERE setId = :setId")
    suspend fun deleteSetWords(setId: Long): Int

    /**
     * Delete a specific set-word relationship.
     *
     * @param setWord The SetWord entity to delete
     */
    @Delete
    suspend fun deleteSetWord(setWord: SetWord)
}
