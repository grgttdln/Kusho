package com.example.app.data.repository

import com.example.app.data.dao.StudentTeacherDao
import com.example.app.data.entity.StudentTeacher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StudentTeacherRepository(private val dao: StudentTeacherDao) {

    /**
     * Link a student to multiple teacher user IDs. Existing mappings for the student are removed first.
     */
    suspend fun linkStudentToTeachers(studentId: Long, teacherIds: List<Long>) = withContext(Dispatchers.IO) {
        try {
            dao.deleteMappingsForStudent(studentId)

            if (teacherIds.isEmpty()) {
                // Nothing to insert, consider this success
                return@withContext Result.success(Unit)
            }

            var insertedCount = 0
            teacherIds.forEach { userId ->
                val res = dao.insert(StudentTeacher(studentId = studentId, userId = userId))
                if (res != -1L) insertedCount++
            }

            if (insertedCount > 0) Result.success(insertedCount) else Result.failure<Unit>(Exception("No mappings inserted"))
        } catch (e: Exception) {
            Result.failure<Unit>(e)
        }
    }

    /**
     * Get teacher (user) IDs for a given student.
     */
    suspend fun getTeacherIdsForStudent(studentId: Long): List<Long> = withContext(Dispatchers.IO) {
        dao.getTeacherIdsForStudent(studentId)
    }

    /**
     * Get student IDs for a given teacher (user).
     */
    suspend fun getStudentIdsForTeacher(userId: Long): List<Long> = withContext(Dispatchers.IO) {
        dao.getStudentIdsForTeacher(userId)
    }

    /**
     * Given a StudentDao, fetch Student entities for a teacher by mapping IDs then calling studentDao.getStudentsByIds
     */
    suspend fun getStudentsForTeacher(userId: Long, studentDao: com.example.app.data.dao.StudentDao): List<com.example.app.data.entity.Student> = withContext(Dispatchers.IO) {
        val ids = dao.getStudentIdsForTeacher(userId)
        if (ids.isEmpty()) return@withContext emptyList()
        studentDao.getStudentsByIds(ids)
    }

    /**
     * Observe count of distinct students assigned to a teacher.
     */
    fun getStudentCountForTeacherFlow(userId: Long): Flow<Int> = dao.getStudentCountForTeacherFlow(userId)
}
