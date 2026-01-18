package com.example.app.data.repository

import com.example.app.data.dao.StudentDao
import com.example.app.data.entity.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for Student data operations.
 *
 * Provides a clean API for the UI layer to interact with student data.
 * All database operations are performed on the IO dispatcher.
 */
class StudentRepository(private val studentDao: StudentDao) {

    /**
     * Result class for student operations.
     */
    sealed class StudentOperationResult {
        data class Success(val studentId: Long) : StudentOperationResult()
        data class Error(val message: String) : StudentOperationResult()
    }

    /**
     * Get all students as a Flow.
     *
     * @return Flow of list of all students
     */
    fun getAllStudentsFlow(): Flow<List<Student>> {
        return studentDao.getAllStudentsFlow()
    }

    /**
     * Get a student by their ID.
     *
     * @param studentId The student's ID
     * @return The student if found, null otherwise
     */
    suspend fun getStudentById(studentId: Long): Student? = withContext(Dispatchers.IO) {
        studentDao.getStudentById(studentId)
    }

    /**
     * Add a new student.
     *
     * @param fullName The student's full name
     * @param gradeLevel The student's grade level
     * @param birthday The student's birthday
     * @param pfpPath Optional path to profile picture
     * @return StudentOperationResult indicating success or failure
     */
    suspend fun addStudent(
        fullName: String,
        gradeLevel: String,
        birthday: String,
        pfpPath: String? = null
    ): StudentOperationResult = withContext(Dispatchers.IO) {
        try {
            if (fullName.isBlank()) {
                return@withContext StudentOperationResult.Error("Student name cannot be empty")
            }
            if (fullName.trim().length > 30) {
                return@withContext StudentOperationResult.Error("Student name must be 30 characters or less")
            }
            if (gradeLevel.isBlank()) {
                return@withContext StudentOperationResult.Error("Grade level cannot be empty")
            }

            val newStudent = Student(
                fullName = fullName.trim(),
                gradeLevel = gradeLevel.trim(),
                birthday = birthday.trim(),
                pfpPath = pfpPath
            )

            val studentId = studentDao.insertStudent(newStudent)
            StudentOperationResult.Success(studentId)
        } catch (e: Exception) {
            StudentOperationResult.Error("Failed to add student: ${e.message}")
        }
    }

    /**
     * Update student information.
     *
     * @param studentId The student ID
     * @param fullName New full name
     * @param gradeLevel New grade level
     * @param birthday New birthday
     * @param pfpPath Optional new profile picture path
     * @return StudentOperationResult indicating success or failure
     */
    suspend fun updateStudent(
        studentId: Long,
        fullName: String,
        gradeLevel: String,
        birthday: String,
        pfpPath: String? = null
    ): StudentOperationResult = withContext(Dispatchers.IO) {
        try {
            if (fullName.isBlank()) {
                return@withContext StudentOperationResult.Error("Student name cannot be empty")
            }
            if (fullName.trim().length > 30) {
                return@withContext StudentOperationResult.Error("Student name must be 30 characters or less")
            }
            if (gradeLevel.isBlank()) {
                return@withContext StudentOperationResult.Error("Grade level cannot be empty")
            }

            val existingStudent = studentDao.getStudentById(studentId)
                ?: return@withContext StudentOperationResult.Error("Student not found")

            val updatedStudent = existingStudent.copy(
                fullName = fullName.trim(),
                gradeLevel = gradeLevel.trim(),
                birthday = birthday.trim(),
                pfpPath = pfpPath ?: existingStudent.pfpPath
            )

            studentDao.updateStudent(updatedStudent)
            StudentOperationResult.Success(studentId)
        } catch (e: Exception) {
            StudentOperationResult.Error("Failed to update student: ${e.message}")
        }
    }

    /**
     * Delete a student.
     * Note: This will also delete associated enrollments due to CASCADE.
     *
     * @param studentId The student ID to delete
     * @return StudentOperationResult indicating success or failure
     */
    suspend fun deleteStudent(studentId: Long): StudentOperationResult = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = studentDao.deleteStudentById(studentId)
            if (rowsDeleted > 0) {
                StudentOperationResult.Success(studentId)
            } else {
                StudentOperationResult.Error("Student not found")
            }
        } catch (e: Exception) {
            StudentOperationResult.Error("Failed to delete student: ${e.message}")
        }
    }
}
