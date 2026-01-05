package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.app.data.entity.Enrollment
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Enrollment entity.
 *
 * Provides methods for CRUD operations on the enrollments table.
 * Handles the many-to-many relationship between students and classes,
 * along with progress tracking and analytics.
 */
@Dao
interface EnrollmentDao {

    /**
     * Insert a new enrollment (add student to class).
     *
     * @param enrollment The enrollment entity to insert
     * @return The row ID of the newly inserted enrollment
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEnrollment(enrollment: Enrollment): Long

    /**
     * Update an existing enrollment's progress and analytics.
     *
     * @param enrollment The enrollment with updated information
     * @return Number of rows updated
     */
    @Update
    suspend fun updateEnrollment(enrollment: Enrollment): Int

    /**
     * Get an enrollment by student and class ID.
     *
     * @param studentId The student's ID
     * @param classId The class's ID
     * @return The enrollment if found, null otherwise
     */
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId AND classId = :classId LIMIT 1")
    suspend fun getEnrollmentByStudentAndClass(studentId: Long, classId: Long): Enrollment?

    /**
     * Get all enrollments for a specific class as a Flow.
     * Useful for displaying the class roster.
     *
     * @param classId The class's ID
     * @return Flow of list of enrollments in the class
     */
    @Query("SELECT * FROM enrollments WHERE classId = :classId")
    fun getEnrollmentsByClassId(classId: Long): Flow<List<Enrollment>>

    /**
     * Get all enrollments for a specific student as a Flow.
     * Useful for seeing all classes a student is enrolled in.
     *
     * @param studentId The student's ID
     * @return Flow of list of enrollments for the student
     */
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId")
    fun getEnrollmentsByStudentId(studentId: Long): Flow<List<Enrollment>>

    /**
     * Delete an enrollment (remove student from class).
     *
     * @param studentId The student's ID
     * @param classId The class's ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM enrollments WHERE studentId = :studentId AND classId = :classId")
    suspend fun deleteEnrollment(studentId: Long, classId: Long): Int

    /**
     * Delete an enrollment by its ID.
     *
     * @param enrollmentId The enrollment ID to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM enrollments WHERE enrollmentId = :enrollmentId")
    suspend fun deleteEnrollmentById(enrollmentId: Long): Int

    /**
     * Update progress for a specific enrollment.
     *
     * @param enrollmentId The enrollment ID
     * @param vowelsProgress Progress on vowels (0.0 - 1.0)
     * @param consonantsProgress Progress on consonants (0.0 - 1.0)
     * @param stopsProgress Progress on stops (0.0 - 1.0)
     * @return Number of rows updated
     */
    @Query("""
        UPDATE enrollments 
        SET vowelsProgress = :vowelsProgress, 
            consonantsProgress = :consonantsProgress, 
            stopsProgress = :stopsProgress 
        WHERE enrollmentId = :enrollmentId
    """)
    suspend fun updateProgress(
        enrollmentId: Long,
        vowelsProgress: Float,
        consonantsProgress: Float,
        stopsProgress: Float
    ): Int

    /**
     * Update analytics for a specific enrollment.
     *
     * @param enrollmentId The enrollment ID
     * @param totalPracticeMinutes Total practice time in minutes
     * @param sessionsCompleted Number of sessions completed
     * @return Number of rows updated
     */
    @Query("""
        UPDATE enrollments 
        SET totalPracticeMinutes = :totalPracticeMinutes, 
            sessionsCompleted = :sessionsCompleted 
        WHERE enrollmentId = :enrollmentId
    """)
    suspend fun updateAnalytics(
        enrollmentId: Long,
        totalPracticeMinutes: Int,
        sessionsCompleted: Int
    ): Int

    /**
     * Update tips for a specific enrollment.
     *
     * @param enrollmentId The enrollment ID
     * @param firstTipTitle First tip title
     * @param firstTipDescription First tip description
     * @param firstTipSubtitle First tip subtitle
     * @param secondTipTitle Second tip title
     * @param secondTipDescription Second tip description
     * @param secondTipSubtitle Second tip subtitle
     * @return Number of rows updated
     */
    @Query("""
        UPDATE enrollments 
        SET firstTipTitle = :firstTipTitle, 
            firstTipDescription = :firstTipDescription, 
            firstTipSubtitle = :firstTipSubtitle,
            secondTipTitle = :secondTipTitle,
            secondTipDescription = :secondTipDescription,
            secondTipSubtitle = :secondTipSubtitle
        WHERE enrollmentId = :enrollmentId
    """)
    suspend fun updateTips(
        enrollmentId: Long,
        firstTipTitle: String?,
        firstTipDescription: String?,
        firstTipSubtitle: String?,
        secondTipTitle: String?,
        secondTipDescription: String?,
        secondTipSubtitle: String?
    ): Int

    /**
     * Get count of students in a class.
     *
     * @param classId The class's ID
     * @return Number of students enrolled in the class
     */
    @Query("SELECT COUNT(*) FROM enrollments WHERE classId = :classId")
    suspend fun getStudentCountByClassId(classId: Long): Int

    /**
     * Check if a student is already enrolled in a class.
     *
     * @param studentId The student's ID
     * @param classId The class's ID
     * @return true if enrolled, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM enrollments WHERE studentId = :studentId AND classId = :classId LIMIT 1)")
    suspend fun isStudentEnrolled(studentId: Long, classId: Long): Boolean
}
