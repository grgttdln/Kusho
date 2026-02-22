package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.ActivityDescriptionCache

@Dao
interface ActivityDescriptionCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: ActivityDescriptionCache): Long

    @Query("""
        SELECT * FROM activity_description_cache
        WHERE setId = :setId AND sessionMode = :sessionMode AND activityId = :activityId
        LIMIT 1
    """)
    suspend fun getDescription(setId: Long, sessionMode: String, activityId: Long = 0L): ActivityDescriptionCache?

    @Query("DELETE FROM activity_description_cache WHERE sessionMode = :sessionMode")
    suspend fun deleteBySessionMode(sessionMode: String)
}
