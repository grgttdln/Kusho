package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tutorial_completion",
    indices = [
        Index(value = ["studentId", "tutorialSetId"], unique = true),
        Index(value = ["studentId"])
    ]
)
data class TutorialCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val tutorialSetId: Long,
    val lastCompletedStep: Int = 0,   // Count of completed letters (non-linear safe)
    val totalSteps: Int = 0,
    val completedAt: Long? = null,    // null = in-progress, non-null = fully completed
    val completedIndicesJson: String? = null  // JSON array of completed letter indices, e.g. "[0,2,4]"
)

/** Parse the JSON string back into a set of completed indices. Returns empty set if null/blank. */
fun TutorialCompletion.getCompletedIndices(): kotlin.collections.Set<kotlin.Int> {
    if (completedIndicesJson.isNullOrBlank()) return emptySet()
    return try {
        completedIndicesJson
            .removeSurrounding("[", "]")
            .split(",")
            .filter { it.isNotBlank() }
            .map { it.trim().toInt() }
            .toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

/** Serialize a set of indices to a JSON array string. */
fun serializeCompletedIndices(indices: kotlin.collections.Set<kotlin.Int>): String {
    return indices.toList().sorted().joinToString(",", "[", "]")
}
