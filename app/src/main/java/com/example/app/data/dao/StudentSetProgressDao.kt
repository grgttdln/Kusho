package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.app.data.entity.StudentSetProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentSetProgressDao {

    /**
     * Insert a new student set progress record.
     *
     * @param progress The progress entity to insert
     * @return The row ID of the newly inserted record
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProgress(progress: StudentSetProgress): Long

    /**
     * Update an existing student set progress record.
     *
     * @param progress The progress entity to update
     * @return The number of rows updated
     */
    @Update
    suspend fun updateProgress(progress: StudentSetProgress): Int

    /**
     * Get progress for a specific student, activity, and set.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @param setId The set's ID
     * @return The progress entity or null if not found
     */
    @Query("SELECT * FROM student_set_progress WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId")
    suspend fun getProgress(studentId: Long, activityId: Long, setId: Long): StudentSetProgress?

    /**
     * Get all progress records for a specific student as a Flow.
     *
     * @param studentId The student's ID
     * @return Flow of progress records
     */
    @Query("SELECT * FROM student_set_progress WHERE studentId = :studentId ORDER BY lastAccessedAt DESC")
    fun getProgressForStudent(studentId: Long): Flow<List<StudentSetProgress>>

    /**
     * Get all progress records for a specific student and activity as a Flow.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @return Flow of progress records
     */
    @Query("SELECT * FROM student_set_progress WHERE studentId = :studentId AND activityId = :activityId ORDER BY lastAccessedAt DESC")
    fun getProgressForStudentAndActivity(studentId: Long, activityId: Long): Flow<List<StudentSetProgress>>

    /**
     * Mark a set as completed for a student.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @param setId The set's ID
     * @param completedAt The timestamp when completed
     * @return The number of rows updated
     */
    @Query("""
        UPDATE student_set_progress 
        SET isCompleted = 1, completionPercentage = 100, completedAt = :completedAt, lastAccessedAt = :completedAt 
        WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId
    """)
    suspend fun markSetAsCompleted(studentId: Long, activityId: Long, setId: Long, completedAt: Long): Int

    /**
     * Update the completion percentage for a student's set progress.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @param setId The set's ID
     * @param percentage The completion percentage (0-100)
     * @param accessedAt The timestamp of last access
     * @return The number of rows updated
     */
    @Query("""
        UPDATE student_set_progress 
        SET completionPercentage = :percentage, lastAccessedAt = :accessedAt 
        WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId
    """)
    suspend fun updateCompletionPercentage(studentId: Long, activityId: Long, setId: Long, percentage: Int, accessedAt: Long): Int

    /**
     * Delete all progress records for a specific student.
     *
     * @param studentId The student's ID
     * @return The number of rows deleted
     */
    @Query("DELETE FROM student_set_progress WHERE studentId = :studentId")
    suspend fun deleteProgressForStudent(studentId: Long): Int

    /**
     * Delete progress record for a specific student, activity, and set.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @param setId The set's ID
     * @return The number of rows deleted
     */
    @Query("DELETE FROM student_set_progress WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId")
    suspend fun deleteProgress(studentId: Long, activityId: Long, setId: Long): Int

    /**
     * Check if a set is completed for a specific student.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @param setId The set's ID
     * @return True if completed, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM student_set_progress WHERE studentId = :studentId AND activityId = :activityId AND setId = :setId AND isCompleted = 1)")
    suspend fun isSetCompleted(studentId: Long, activityId: Long, setId: Long): Boolean

    /**
     * Get the count of completed sets for a student in an activity.
     *
     * @param studentId The student's ID
     * @param activityId The activity's ID
     * @return The count of completed sets
     */
    @Query("SELECT COUNT(*) FROM student_set_progress WHERE studentId = :studentId AND activityId = :activityId AND isCompleted = 1")
    suspend fun getCompletedSetCount(studentId: Long, activityId: Long): Int

    /**
     * Insert or update progress using REPLACE strategy.
     *
     * @param progress The progress entity to upsert
     * @return The row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: StudentSetProgress): Long
}
