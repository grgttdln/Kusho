package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.app.data.entity.LearnerProfileAnnotation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LearnerProfileAnnotation entity.
 * Provides methods to store and retrieve learner profile annotations
 * organized by student and item.
 */
@Dao
interface LearnerProfileAnnotationDao {

    /**
     * Insert a new annotation. If an annotation with the same (studentId, setId, itemId)
     * already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(annotation: LearnerProfileAnnotation): Long

    /**
     * Update an existing annotation
     */
    @Update
    suspend fun update(annotation: LearnerProfileAnnotation)

    /**
     * Get a specific annotation by studentId, setId, and itemId
     */
    @Query("""
        SELECT * FROM learner_profile_annotations 
        WHERE studentId = :studentId AND setId = :setId AND itemId = :itemId 
        LIMIT 1
    """)
    suspend fun getAnnotation(studentId: String, setId: Long, itemId: Int): LearnerProfileAnnotation?

    /**
     * Get all annotations for a specific student in a specific set
     */
    @Query("""
        SELECT * FROM learner_profile_annotations 
        WHERE studentId = :studentId AND setId = :setId 
        ORDER BY itemId ASC
    """)
    suspend fun getAnnotationsForStudentInSet(studentId: String, setId: Long): List<LearnerProfileAnnotation>

    /**
     * Get all annotations for a specific student in a specific set as Flow
     */
    @Query("""
        SELECT * FROM learner_profile_annotations 
        WHERE studentId = :studentId AND setId = :setId 
        ORDER BY itemId ASC
    """)
    fun observeAnnotationsForStudentInSet(studentId: String, setId: Long): Flow<List<LearnerProfileAnnotation>>

    /**
     * Get all annotations for a specific student
     */
    @Query("""
        SELECT * FROM learner_profile_annotations 
        WHERE studentId = :studentId 
        ORDER BY setId ASC, itemId ASC
    """)
    suspend fun getAnnotationsForStudent(studentId: String): List<LearnerProfileAnnotation>

    /**
     * Get all annotations for a specific set (all students)
     */
    @Query("""
        SELECT * FROM learner_profile_annotations 
        WHERE setId = :setId 
        ORDER BY studentId ASC, itemId ASC
    """)
    suspend fun getAnnotationsForSet(setId: Long): List<LearnerProfileAnnotation>

    /**
     * Delete a specific annotation
     */
    @Query("""
        DELETE FROM learner_profile_annotations 
        WHERE studentId = :studentId AND setId = :setId AND itemId = :itemId
    """)
    suspend fun deleteAnnotation(studentId: String, setId: Long, itemId: Int)

    /**
     * Delete all annotations for a student in a set
     */
    @Query("""
        DELETE FROM learner_profile_annotations 
        WHERE studentId = :studentId AND setId = :setId
    """)
    suspend fun deleteAnnotationsForStudentInSet(studentId: String, setId: Long)

    /**
     * Delete all annotations for a student
     */
    @Query("DELETE FROM learner_profile_annotations WHERE studentId = :studentId")
    suspend fun deleteAllAnnotationsForStudent(studentId: String)

    /**
     * Get count of annotations for a student in a set
     */
    @Query("""
        SELECT COUNT(*) FROM learner_profile_annotations 
        WHERE studentId = :studentId AND setId = :setId
    """)
    suspend fun getAnnotationCountForStudentInSet(studentId: String, setId: Long): Int
}

