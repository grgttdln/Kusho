package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a learner profile annotation note.
 * Stores notes per student and per item within a set.
 *
 * The composite unique key is (studentId, setId, itemId, sessionMode) which ensures
 * each student has independent notes per mode (LEARN vs TUTORIAL).
 */
@Entity(
    tableName = "learner_profile_annotations",
    indices = [
        Index(value = ["studentId", "setId", "itemId", "sessionMode"], unique = true),
        Index(value = ["studentId"]),
        Index(value = ["setId"]),
        Index(value = ["sessionMode"])
    ]
)
data class LearnerProfileAnnotation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The student's unique identifier */
    val studentId: String,

    /** The set ID this annotation belongs to */
    val setId: Long,

    /** The item/word ID within the set (index in the set's word list) */
    val itemId: Int,

    /** Session mode: "LEARN" for Learn Mode, "TUTORIAL" for Tutorial Mode */
    val sessionMode: String = "LEARN",

    /** Selected level of progress: "Beginning", "Developing", "Proficient", "Advanced", or null */
    val levelOfProgress: String? = null,

    /** Comma-separated list of selected strengths: "Recognition", "Fluency", "Formation" */
    val strengthsObserved: String = "",

    /** Free-text note for strengths */
    val strengthsNote: String = "",

    /** Comma-separated list of selected challenges: "Recognition", "Fluency", "Formation" */
    val challenges: String = "",

    /** Free-text note for challenges */
    val challengesNote: String = "",

    /** Timestamp when the annotation was created */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp when the annotation was last updated */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts strengthsObserved string to a list
     */
    fun getStrengthsList(): List<String> {
        return if (strengthsObserved.isBlank()) emptyList()
        else strengthsObserved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Converts challenges string to a list
     */
    fun getChallengesList(): List<String> {
        return if (challenges.isBlank()) emptyList()
        else challenges.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        const val MODE_LEARN = "LEARN"
        const val MODE_TUTORIAL = "TUTORIAL"
        
        /**
         * Creates a LearnerProfileAnnotation from lists
         */
        fun create(
            studentId: String,
            setId: Long,
            itemId: Int,
            sessionMode: String = MODE_LEARN,
            levelOfProgress: String?,
            strengthsObserved: List<String>,
            strengthsNote: String,
            challenges: List<String>,
            challengesNote: String
        ): LearnerProfileAnnotation {
            return LearnerProfileAnnotation(
                studentId = studentId,
                setId = setId,
                itemId = itemId,
                sessionMode = sessionMode,
                levelOfProgress = levelOfProgress,
                strengthsObserved = strengthsObserved.joinToString(","),
                strengthsNote = strengthsNote,
                challenges = challenges.joinToString(","),
                challengesNote = challengesNote
            )
        }
    }
}

