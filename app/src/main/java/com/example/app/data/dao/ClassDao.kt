package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.app.data.entity.Class
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Class entity.
 *
 * Provides methods for CRUD operations on the classes table.
 * All methods are either suspend functions for one-shot operations
 * or return Flow for observable queries.
 */
@Dao
interface ClassDao {

    /**
     * Insert a new class into the database.
     *
     * @param classEntity The class entity to insert
     * @return The row ID of the newly inserted class
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertClass(classEntity: Class): Long

    /**
     * Update an existing class's information.
     *
     * @param classEntity The class with updated information
     * @return Number of rows updated
     */
    @Update
    suspend fun updateClass(classEntity: Class): Int

    /**
     * Get a class by its ID.
     *
     * @param classId The class's ID
     * @return The class if found, null otherwise
     */
    @Query("SELECT * FROM classes WHERE classId = :classId LIMIT 1")
    suspend fun getClassById(classId: Long): Class?

    /**
     * Get all active (non-archived) classes for a specific user as a Flow.
     * The Flow emits new values whenever the data changes.
     *
     * @param userId The user's ID (teacher)
     * @return Flow of list of active classes belonging to the user
     */
    @Query("SELECT * FROM classes WHERE userId = :userId AND isArchived = 0 ORDER BY className ASC")
    fun getActiveClassesByUserId(userId: Long): Flow<List<Class>>

    /**
     * Get all archived classes for a specific user as a Flow.
     *
     * @param userId The user's ID (teacher)
     * @return Flow of list of archived classes belonging to the user
     */
    @Query("SELECT * FROM classes WHERE userId = :userId AND isArchived = 1 ORDER BY className ASC")
    fun getArchivedClassesByUserId(userId: Long): Flow<List<Class>>

    /**
     * Get all classes (active and archived) for a specific user as a Flow.
     *
     * @param userId The user's ID (teacher)
     * @return Flow of list of all classes belonging to the user
     */
    @Query("SELECT * FROM classes WHERE userId = :userId ORDER BY isArchived ASC, className ASC")
    fun getAllClassesByUserId(userId: Long): Flow<List<Class>>

    /**
     * Archive a class (soft delete).
     *
     * @param classId The class ID to archive
     * @return Number of rows updated
     */
    @Query("UPDATE classes SET isArchived = 1 WHERE classId = :classId")
    suspend fun archiveClass(classId: Long): Int

    /**
     * Unarchive a class.
     *
     * @param classId The class ID to unarchive
     * @return Number of rows updated
     */
    @Query("UPDATE classes SET isArchived = 0 WHERE classId = :classId")
    suspend fun unarchiveClass(classId: Long): Int

    /**
     * Delete a class by its ID (hard delete).
     * Note: This will also delete associated enrollments due to CASCADE.
     *
     * @param classId The class ID to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM classes WHERE classId = :classId")
    suspend fun deleteClassById(classId: Long): Int

    /**
     * Update class's basic information.
     *
     * @param classId The class ID
     * @param className New class name
     * @param classCode New class code
     * @return Number of rows updated
     */
    @Query("UPDATE classes SET className = :className, classCode = :classCode WHERE classId = :classId")
    suspend fun updateClassInfo(classId: Long, className: String, classCode: String): Int

    /**
     * Update class's banner image path.
     *
     * @param classId The class ID
     * @param bannerPath New banner path
     * @return Number of rows updated
     */
    @Query("UPDATE classes SET bannerPath = :bannerPath WHERE classId = :classId")
    suspend fun updateClassBanner(classId: Long, bannerPath: String?): Int
}
