package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.ActivitySet
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
     * Insert an activity-set relationship into the junction table.
     *
     * @param activitySet The activity-set relationship to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivitySet(activitySet: ActivitySet)

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

    /**
     * Update a set's title and description.
     *
     * @param setId The ID of the set to update
     * @param title The new title
     * @param description The new description
     * @param itemCount The new item count
     * @param updatedAt The timestamp of the update
     * @return The number of rows updated
     */
    @Query("UPDATE sets SET title = :title, description = :description, itemCount = :itemCount, updatedAt = :updatedAt WHERE id = :setId")
    suspend fun updateSet(setId: Long, title: String, description: String?, itemCount: Int, updatedAt: Long): Int

    /**
     * Get sets for a specific activity through the activity_set junction table.
     *
     * @param activityId The ID of the activity
     * @return Flow of sets belonging to the activity
     */
    @Query("""
        SELECT s.* FROM sets s 
        INNER JOIN activity_set act_set ON s.id = act_set.setId 
        WHERE act_set.activityId = :activityId 
        ORDER BY s.createdAt DESC
    """)
    fun getSetsForActivity(activityId: Long): Flow<List<Set>>

    /**
     * Remove a set from an activity (unlink).
     * This only removes the link, not the actual set.
     *
     * @param setId The ID of the set
     * @param activityId The ID of the activity
     * @return The number of rows deleted
     */
    @Query("DELETE FROM activity_set WHERE setId = :setId AND activityId = :activityId")
    suspend fun unlinkSetFromActivity(setId: Long, activityId: Long): Int

    /**
     * Check if a set with the given title already exists for a user (case-insensitive).
     *
     * @param userId The user's ID
     * @param title The set title to check
     * @return True if a set with this title exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM sets WHERE userId = :userId AND LOWER(title) = LOWER(:title))")
    suspend fun setTitleExistsForUser(userId: Long, title: String): Boolean

    /**
     * Check if a set with the given title already exists for a user, excluding a specific set ID (case-insensitive).
     * Used when updating a set to allow keeping the same title.
     *
     * @param userId The user's ID
     * @param title The set title to check
     * @param excludeSetId The set ID to exclude from the check
     * @return True if another set with this title exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM sets WHERE userId = :userId AND LOWER(title) = LOWER(:title) AND id != :excludeSetId)")
    suspend fun setTitleExistsForUserExcluding(userId: Long, title: String, excludeSetId: Long): Boolean
}
