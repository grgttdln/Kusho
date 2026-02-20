package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity to track student progress on set completion.
 * Tracks when a student completes a set within an activity.
 */
@Entity(
    tableName = "student_set_progress",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Set::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["activityId"]),
        Index(value = ["setId"]),
        Index(value = ["studentId", "activityId", "setId"], unique = true)
    ]
)
data class StudentSetProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val activityId: Long,
    val setId: Long,
    val isCompleted: Boolean = false,
    val completionPercentage: Int = 0,
    val completedAt: Long? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val lastCompletedWordIndex: Int = 0,
    val correctlyAnsweredWordsJson: String? = null
)
