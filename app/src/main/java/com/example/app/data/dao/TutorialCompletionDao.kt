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
        WHERE studentId = :studentId AND completedAt IS NOT NULL
        ORDER BY completedAt DESC
    """)
    suspend fun getCompletionsForStudent(studentId: Long): List<TutorialCompletion>

    @Query("""
        SELECT * FROM tutorial_completion
        WHERE studentId = :studentId AND tutorialSetId = :tutorialSetId
        LIMIT 1
    """)
    suspend fun getCompletion(studentId: Long, tutorialSetId: Long): TutorialCompletion?

    /**
     * Save or update partial progress for a tutorial session.
     * Uses REPLACE to upsert based on the unique (studentId, tutorialSetId) index.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(completion: TutorialCompletion): Long

    /**
     * Get in-progress tutorial (completedAt IS NULL) for a student + tutorial set.
     */
    @Query("""
        SELECT * FROM tutorial_completion
        WHERE studentId = :studentId AND tutorialSetId = :tutorialSetId AND completedAt IS NULL
        LIMIT 1
    """)
    suspend fun getInProgressSession(studentId: Long, tutorialSetId: Long): TutorialCompletion?

    /**
     * Delete partial progress row (called after full completion so next time starts fresh).
     */
    @Query("""
        DELETE FROM tutorial_completion
        WHERE studentId = :studentId AND tutorialSetId = :tutorialSetId AND completedAt IS NULL
    """)
    suspend fun deleteInProgress(studentId: Long, tutorialSetId: Long)

    /**
     * Mark a session as fully completed: delete any in-progress row, insert a completed row.
     */
    @Query("""
        UPDATE tutorial_completion
        SET completedAt = :completedAt, lastCompletedStep = totalSteps
        WHERE studentId = :studentId AND tutorialSetId = :tutorialSetId AND completedAt IS NULL
    """)
    suspend fun markCompleted(studentId: Long, tutorialSetId: Long, completedAt: Long = System.currentTimeMillis())
}
