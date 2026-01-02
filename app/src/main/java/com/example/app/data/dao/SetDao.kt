package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.Set
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {

    /**
     * Insert a new set into the database.
     *
     * @param set The set entity to insert
     * @return The row ID of the newly inserted set
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSet(set: Set): Long

    /**
     * Get all sets for a specific user as a Flow.
     * Since sets are independent, all sets created by a user are returned.
     *
     * @param userId The user's ID
     * @return Flow of sets created by the user
     */
    @Query("SELECT * FROM sets WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSetsForUser(userId: Long): Flow<List<Set>>

    /**
     * Get all sets from the database as a Flow.
     *
     * @return Flow of all sets
     */
    @Query("SELECT * FROM sets ORDER BY createdAt DESC")
    fun getAllSets(): Flow<List<Set>>

    /**
     * Get a set by its ID.
     *
     * @param setId The ID of the set
     * @return The set entity or null if not found
     */
    @Query("SELECT * FROM sets WHERE id = :setId")
    suspend fun getSetById(setId: Long): Set?

    /**
     * Delete a set by its ID.
     *
     * @param setId The ID of the set to delete
     * @return The number of rows deleted
     */
    @Query("DELETE FROM sets WHERE id = :setId")
    suspend fun deleteSetById(setId: Long): Int
}
