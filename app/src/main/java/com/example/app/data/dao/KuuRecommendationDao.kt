package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.KuuRecommendation

@Dao
interface KuuRecommendationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recommendation: KuuRecommendation): Long

    @Query("""
        SELECT * FROM kuu_recommendations
        WHERE studentId = :studentId
        LIMIT 1
    """)
    suspend fun getRecommendationForStudent(studentId: Long): KuuRecommendation?

    @Query("""
        UPDATE kuu_recommendations
        SET completionsSinceGeneration = completionsSinceGeneration + 1,
            updatedAt = :now
        WHERE studentId = :studentId
    """)
    suspend fun incrementCompletions(studentId: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM kuu_recommendations WHERE studentId = :studentId")
    suspend fun deleteRecommendation(studentId: Long)
}
