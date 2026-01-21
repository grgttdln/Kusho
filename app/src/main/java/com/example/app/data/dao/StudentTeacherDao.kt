package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.StudentTeacher
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentTeacherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(studentTeacher: StudentTeacher): Long

    @Query("SELECT userId FROM student_teachers WHERE studentId = :studentId")
    suspend fun getTeacherIdsForStudent(studentId: Long): List<Long>

    @Query("SELECT studentId FROM student_teachers WHERE userId = :userId")
    suspend fun getStudentIdsForTeacher(userId: Long): List<Long>

    @Query("SELECT COUNT(DISTINCT studentId) FROM student_teachers WHERE userId = :userId")
    fun getStudentCountForTeacherFlow(userId: Long): Flow<Int>

    @Query("DELETE FROM student_teachers WHERE studentId = :studentId")
    suspend fun deleteMappingsForStudent(studentId: Long)
}
