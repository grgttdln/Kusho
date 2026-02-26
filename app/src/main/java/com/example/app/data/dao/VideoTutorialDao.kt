package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.VideoTutorial
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTutorialDao {

    @Query("SELECT * FROM video_tutorials WHERE userId = :userId ORDER BY sortOrder ASC")
    fun getByUserId(userId: Long): Flow<List<VideoTutorial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tutorials: List<VideoTutorial>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tutorial: VideoTutorial): Long

    @Query("SELECT COUNT(*) FROM video_tutorials WHERE userId = :userId")
    suspend fun getCount(userId: Long): Int

    @Query("DELETE FROM video_tutorials WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)
}
