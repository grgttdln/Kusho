package com.example.app.data.repository

import com.example.app.data.dao.ClassDao
import com.example.app.data.entity.Class
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for Class data operations.
 *
 * Provides a clean API for the UI layer to interact with class data.
 * All database operations are performed on the IO dispatcher.
 */
class ClassRepository(private val classDao: ClassDao) {

    /**
     * Result class for class operations.
     */
    sealed class ClassOperationResult {
        data class Success(val classId: Long) : ClassOperationResult()
        data class Error(val message: String) : ClassOperationResult()
    }

    /**
     * Get all active (non-archived) classes for a user as a Flow.
     *
     * @param userId The user's ID (teacher)
     * @return Flow of list of active classes
     */
    fun getActiveClassesByUserId(userId: Long): Flow<List<Class>> {
        return classDao.getActiveClassesByUserId(userId)
    }

    /**
     * Get all archived classes for a user as a Flow.
     *
     * @param userId The user's ID (teacher)
     * @return Flow of list of archived classes
     */
    fun getArchivedClassesByUserId(userId: Long): Flow<List<Class>> {
        return classDao.getArchivedClassesByUserId(userId)
    }

    /**
     * Get a class by its ID.
     *
     * @param classId The class's ID
     * @return The class if found, null otherwise
     */
    suspend fun getClassById(classId: Long): Class? = withContext(Dispatchers.IO) {
        classDao.getClassById(classId)
    }

    /**
     * Create a new class.
     *
     * @param userId The teacher's user ID
     * @param className The name of the class
     * @param classCode The class code
     * @param bannerPath Optional path to banner image
     * @return ClassOperationResult indicating success or failure
     */
    suspend fun createClass(
        userId: Long,
        className: String,
        classCode: String,
        bannerPath: String? = null
    ): ClassOperationResult = withContext(Dispatchers.IO) {
        try {
            if (className.isBlank()) {
                return@withContext ClassOperationResult.Error("Class name cannot be empty")
            }
            if (classCode.isBlank()) {
                return@withContext ClassOperationResult.Error("Class code cannot be empty")
            }

            val newClass = Class(
                className = className.trim(),
                classCode = classCode.trim(),
                bannerPath = bannerPath,
                isArchived = false,
                userId = userId
            )

            val classId = classDao.insertClass(newClass)
            ClassOperationResult.Success(classId)
        } catch (e: Exception) {
            ClassOperationResult.Error("Failed to create class: ${e.message}")
        }
    }

    /**
     * Update an existing class.
     *
     * @param classId The class ID
     * @param className New class name
     * @param classCode New class code
     * @param bannerPath Optional new banner path
     * @return ClassOperationResult indicating success or failure
     */
    suspend fun updateClass(
        classId: Long,
        className: String,
        classCode: String,
        bannerPath: String? = null
    ): ClassOperationResult = withContext(Dispatchers.IO) {
        try {
            if (className.isBlank()) {
                return@withContext ClassOperationResult.Error("Class name cannot be empty")
            }
            if (classCode.isBlank()) {
                return@withContext ClassOperationResult.Error("Class code cannot be empty")
            }

            val existingClass = classDao.getClassById(classId)
                ?: return@withContext ClassOperationResult.Error("Class not found")

            val updatedClass = existingClass.copy(
                className = className.trim(),
                classCode = classCode.trim(),
                bannerPath = bannerPath ?: existingClass.bannerPath
            )

            classDao.updateClass(updatedClass)
            ClassOperationResult.Success(classId)
        } catch (e: Exception) {
            ClassOperationResult.Error("Failed to update class: ${e.message}")
        }
    }

    /**
     * Archive a class (soft delete).
     *
     * @param classId The class ID to archive
     * @return ClassOperationResult indicating success or failure
     */
    suspend fun archiveClass(classId: Long): ClassOperationResult = withContext(Dispatchers.IO) {
        try {
            val rowsUpdated = classDao.archiveClass(classId)
            if (rowsUpdated > 0) {
                ClassOperationResult.Success(classId)
            } else {
                ClassOperationResult.Error("Class not found")
            }
        } catch (e: Exception) {
            ClassOperationResult.Error("Failed to archive class: ${e.message}")
        }
    }

    /**
     * Unarchive a class.
     *
     * @param classId The class ID to unarchive
     * @return ClassOperationResult indicating success or failure
     */
    suspend fun unarchiveClass(classId: Long): ClassOperationResult = withContext(Dispatchers.IO) {
        try {
            val rowsUpdated = classDao.unarchiveClass(classId)
            if (rowsUpdated > 0) {
                ClassOperationResult.Success(classId)
            } else {
                ClassOperationResult.Error("Class not found")
            }
        } catch (e: Exception) {
            ClassOperationResult.Error("Failed to unarchive class: ${e.message}")
        }
    }
}
