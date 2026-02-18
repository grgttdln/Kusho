package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.TutorialCompletion

@Dao
interface TutorialCompletionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(completion: TutorialCompletion): Long

    @Query("""
        SELECT * FROM tutorial_completion
        WHERE studentId = :studentId
        ORDER BY completedAt DESC
    """)
    suspend fun getCompletionsForStudent(studentId: Long): List<TutorialCompletion>

    @Query("""
        SELECT * FROM tutorial_completion
        WHERE studentId = :studentId AND tutorialSetId = :tutorialSetId
        LIMIT 1
    """)
    suspend fun getCompletion(studentId: Long, tutorialSetId: Long): TutorialCompletion?
}
