package com.example.app.data.repository

import com.example.app.data.dao.EnrollmentDao
import com.example.app.data.dao.StudentDao
import com.example.app.data.entity.Enrollment
import com.example.app.data.entity.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Repository for Enrollment data operations.
 *
 * Handles the many-to-many relationship between students and classes.
 * Provides methods for managing class rosters and student progress.
 */
class EnrollmentRepository(
    private val enrollmentDao: EnrollmentDao,
    private val studentDao: StudentDao
) {

    /**
     * Result class for enrollment operations.
     */
    sealed class EnrollmentOperationResult {
        data class Success(val enrollmentId: Long) : EnrollmentOperationResult()
        data class Error(val message: String) : EnrollmentOperationResult()
    }

    /**
     * Data class combining student info with enrollment data.
     */
    data class StudentWithEnrollment(
        val student: Student,
        val enrollment: Enrollment
    )

    /**
     * Get all enrollments for a class as a Flow.
     *
     * @param classId The class's ID
     * @return Flow of list of enrollments
     */
    fun getEnrollmentsByClassId(classId: Long): Flow<List<Enrollment>> {
        return enrollmentDao.getEnrollmentsByClassId(classId)
    }

    /**
     * Get students with their enrollment data for a class as a Flow.
     * Combines student info with enrollment data.
     *
     * @param classId The class's ID
     * @return Flow of list of students with enrollment data
     */
    fun getStudentsWithEnrollmentByClassId(classId: Long): Flow<List<StudentWithEnrollment>> {
        return combine(
            enrollmentDao.getEnrollmentsByClassId(classId),
            studentDao.getAllStudentsFlow()
        ) { enrollments, students ->
            enrollments.mapNotNull { enrollment ->
                students.find { it.studentId == enrollment.studentId }?.let { student ->
                    StudentWithEnrollment(student, enrollment)
                }
            }
        }
    }

    /**
     * Get an enrollment by student and class ID.
     *
     * @param studentId The student's ID
     * @param classId The class's ID
     * @return The enrollment if found, null otherwise
     */
    suspend fun getEnrollmentByStudentAndClass(studentId: Long, classId: Long): Enrollment? =
        withContext(Dispatchers.IO) {
            enrollmentDao.getEnrollmentByStudentAndClass(studentId, classId)
        }

    /**
     * Enroll a student in a class.
     *
     * @param studentId The student's ID
     * @param classId The class's ID
     * @return EnrollmentOperationResult indicating success or failure
     */
    suspend fun enrollStudent(
        studentId: Long,
        classId: Long
    ): EnrollmentOperationResult = withContext(Dispatchers.IO) {
        try {
            // Check if already enrolled
            val existingEnrollment = enrollmentDao.getEnrollmentByStudentAndClass(studentId, classId)
            if (existingEnrollment != null) {
                return@withContext EnrollmentOperationResult.Error("Student is already enrolled in this class")
            }

            val newEnrollment = Enrollment(
                studentId = studentId,
                classId = classId,
                vowelsProgress = 0.0f,
                consonantsProgress = 0.0f,
                stopsProgress = 0.0f,
                totalPracticeMinutes = 0,
                sessionsCompleted = 0
            )

            val enrollmentId = enrollmentDao.insertEnrollment(newEnrollment)
            EnrollmentOperationResult.Success(enrollmentId)
        } catch (e: Exception) {
            EnrollmentOperationResult.Error("Failed to enroll student: ${e.message}")
        }
    }

    /**
     * Remove a student from a class.
     *
     * @param studentId The student's ID
     * @param classId The class's ID
     * @return EnrollmentOperationResult indicating success or failure
     */
    suspend fun unenrollStudent(
        studentId: Long,
        classId: Long
    ): EnrollmentOperationResult = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = enrollmentDao.deleteEnrollment(studentId, classId)
            if (rowsDeleted > 0) {
                EnrollmentOperationResult.Success(0)
            } else {
                EnrollmentOperationResult.Error("Enrollment not found")
            }
        } catch (e: Exception) {
            EnrollmentOperationResult.Error("Failed to remove student from class: ${e.message}")
        }
    }

    /**
     * Update progress for a student in a class.
     *
     * @param enrollmentId The enrollment ID
     * @param vowelsProgress Progress on vowels (0.0 - 1.0)
     * @param consonantsProgress Progress on consonants (0.0 - 1.0)
     * @param stopsProgress Progress on stops (0.0 - 1.0)
     * @return EnrollmentOperationResult indicating success or failure
     */
    suspend fun updateProgress(
        enrollmentId: Long,
        vowelsProgress: Float,
        consonantsProgress: Float,
        stopsProgress: Float
    ): EnrollmentOperationResult = withContext(Dispatchers.IO) {
        try {
            enrollmentDao.updateProgress(enrollmentId, vowelsProgress, consonantsProgress, stopsProgress)
            EnrollmentOperationResult.Success(enrollmentId)
        } catch (e: Exception) {
            EnrollmentOperationResult.Error("Failed to update progress: ${e.message}")
        }
    }

    /**
     * Update analytics for a student in a class.
     *
     * @param enrollmentId The enrollment ID
     * @param totalPracticeMinutes Total practice time in minutes
     * @param sessionsCompleted Number of sessions completed
     * @return EnrollmentOperationResult indicating success or failure
     */
    suspend fun updateAnalytics(
        enrollmentId: Long,
        totalPracticeMinutes: Int,
        sessionsCompleted: Int
    ): EnrollmentOperationResult = withContext(Dispatchers.IO) {
        try {
            enrollmentDao.updateAnalytics(enrollmentId, totalPracticeMinutes, sessionsCompleted)
            EnrollmentOperationResult.Success(enrollmentId)
        } catch (e: Exception) {
            EnrollmentOperationResult.Error("Failed to update analytics: ${e.message}")
        }
    }

    /**
     * Update tips for a student in a class.
     *
     * @param enrollmentId The enrollment ID
     * @param firstTipTitle First tip title
     * @param firstTipDescription First tip description
     * @param firstTipSubtitle First tip subtitle
     * @param secondTipTitle Second tip title
     * @param secondTipDescription Second tip description
     * @param secondTipSubtitle Second tip subtitle
     * @return EnrollmentOperationResult indicating success or failure
     */
    suspend fun updateTips(
        enrollmentId: Long,
        firstTipTitle: String?,
        firstTipDescription: String?,
        firstTipSubtitle: String?,
        secondTipTitle: String?,
        secondTipDescription: String?,
        secondTipSubtitle: String?
    ): EnrollmentOperationResult = withContext(Dispatchers.IO) {
        try {
            enrollmentDao.updateTips(
                enrollmentId,
                firstTipTitle,
                firstTipDescription,
                firstTipSubtitle,
                secondTipTitle,
                secondTipDescription,
                secondTipSubtitle
            )
            EnrollmentOperationResult.Success(enrollmentId)
        } catch (e: Exception) {
            EnrollmentOperationResult.Error("Failed to update tips: ${e.message}")
        }
    }

    /**
     * Get the count of students in a class.
     *
     * @param classId The class's ID
     * @return Number of students enrolled
     */
    suspend fun getStudentCountByClassId(classId: Long): Int = withContext(Dispatchers.IO) {
        enrollmentDao.getStudentCountByClassId(classId)
    }
}
