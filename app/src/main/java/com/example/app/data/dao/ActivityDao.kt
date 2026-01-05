package com.example.app.data.dao

import androidx.room.*
import com.example.app.data.entity.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivity(activity: Activity): Long

    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY createdAt DESC")
    fun getActivitiesByUserId(userId: Long): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getActivitiesByUserIdOnce(userId: Long): List<Activity>

    @Query("SELECT * FROM activities WHERE id = :activityId LIMIT 1")
    suspend fun getActivityById(activityId: Long): Activity?

    @Query("SELECT EXISTS(SELECT 1 FROM activities WHERE userId = :userId AND LOWER(title) = LOWER(:title) LIMIT 1)")
    suspend fun activityTitleExistsForUser(userId: Long, title: String): Boolean

    @Update
    suspend fun updateActivity(activity: Activity): Int

    @Query("UPDATE activities SET title = :title, description = :description, coverImagePath = :coverImagePath, updatedAt = :updatedAt WHERE id = :activityId")
    suspend fun updateActivityDetails(
        activityId: Long,
        title: String,
        description: String?,
        coverImagePath: String?,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("DELETE FROM activities WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: Long): Int

    @Query("DELETE FROM activities WHERE userId = :userId")
    suspend fun deleteAllActivitiesForUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM activities WHERE userId = :userId")
    suspend fun getActivityCountForUser(userId: Long): Int
}
