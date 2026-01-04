package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing the enrollment/roster linking students to classes.
 * This acts as the many-to-many relationship bridge and stores student progress data.
 *
 * @property enrollmentId Auto-generated primary key
 * @property studentId Foreign key to student
 * @property classId Foreign key to class
 * @property vowelsProgress Progress on vowels (0.0 to 1.0)
 * @property consonantsProgress Progress on consonants (0.0 to 1.0)
 * @property stopsProgress Progress on stops (0.0 to 1.0)
 * @property totalPracticeMinutes Total practice time in minutes
 * @property sessionsCompleted Number of sessions completed
 * @property firstTipTitle Title for first tip (yellow card)
 * @property firstTipDescription Description for first tip
 * @property firstTipSubtitle Subtitle for first tip
 * @property secondTipTitle Title for second tip (purple card)
 * @property secondTipDescription Description for second tip
 * @property secondTipSubtitle Subtitle for second tip
 */
@Entity(
    tableName = "enrollments",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Class::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["classId"]),
        Index(value = ["studentId", "classId"], unique = true) // Prevent duplicate enrollments
    ]
)
data class Enrollment(
    @PrimaryKey(autoGenerate = true)
    val enrollmentId: Long = 0,

    val studentId: Long,

    val classId: Long,

    val vowelsProgress: Float = 0.0f,

    val consonantsProgress: Float = 0.0f,

    val stopsProgress: Float = 0.0f,

    val totalPracticeMinutes: Int = 0,

    val sessionsCompleted: Int = 0,

    val firstTipTitle: String? = null,

    val firstTipDescription: String? = null,

    val firstTipSubtitle: String? = null,

    val secondTipTitle: String? = null,

    val secondTipDescription: String? = null,

    val secondTipSubtitle: String? = null
)
