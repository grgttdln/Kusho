package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.app.data.entity.Student
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Student entity.
 *
 * Provides methods for CRUD operations on the students table.
 * All methods are either suspend functions for one-shot operations
 * or return Flow for observable queries.
 */
@Dao
interface StudentDao {

    /**
     * Insert a new student into the database.
     *
     * @param student The student entity to insert
     * @return The row ID of the newly inserted student
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: Student): Long

    /**
     * Update an existing student's information.
     *
     * @param student The student with updated information
     * @return Number of rows updated
     */
    @Update
    suspend fun updateStudent(student: Student): Int

    /**
     * Get a student by their ID.
     *
     * @param studentId The student's ID
     * @return The student if found, null otherwise
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): Student?

    /**
     * Get all students as a Flow for reactive updates.
     *
     * @return Flow of all students
     */
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudentsFlow(): Flow<List<Student>>

    /**
     * Delete a student by their ID.
     * Note: This will also delete associated enrollments due to CASCADE.
     *
     * @param studentId The student ID to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM students WHERE studentId = :studentId")
    suspend fun deleteStudentById(studentId: Long): Int

    /**
     * Update student's basic information.
     *
     * @param studentId The student ID
     * @param fullName New full name
     * @param gradeLevel New grade level
     * @param birthday New birthday
     * @return Number of rows updated
     */
    @Query("UPDATE students SET fullName = :fullName, gradeLevel = :gradeLevel, birthday = :birthday WHERE studentId = :studentId")
    suspend fun updateStudentInfo(studentId: Long, fullName: String, gradeLevel: String, birthday: String): Int

    /**
     * Update student's profile picture path.
     *
     * @param studentId The student ID
     * @param pfpPath New profile picture path
     * @return Number of rows updated
     */
    @Query("UPDATE students SET pfpPath = :pfpPath WHERE studentId = :studentId")
    suspend fun updateStudentPfp(studentId: Long, pfpPath: String?): Int
}
