package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.AnnotationSummary

@Dao
interface AnnotationSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: AnnotationSummary): Long

    @Query("""
        SELECT * FROM annotation_summary
        WHERE studentId = :studentId AND setId = :setId AND sessionMode = :sessionMode AND activityId = :activityId
        LIMIT 1
    """)
    suspend fun getSummary(studentId: String, setId: Long, sessionMode: String, activityId: Long = 0L): AnnotationSummary?

    @Query("""
        SELECT * FROM annotation_summary
        WHERE studentId = :studentId
        ORDER BY generatedAt DESC
    """)
    suspend fun getSummariesForStudent(studentId: String): List<AnnotationSummary>

    @Query("""
        DELETE FROM annotation_summary
        WHERE studentId = :studentId AND setId = :setId AND sessionMode = :sessionMode AND activityId = :activityId
    """)
    suspend fun deleteSummary(studentId: String, setId: Long, sessionMode: String, activityId: Long = 0L)
}
