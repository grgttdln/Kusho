package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kuu_recommendations",
    indices = [
        Index(value = ["studentId"], unique = true)
    ]
)
data class KuuRecommendation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val title: String,
    val description: String,
    val activityType: String,
    val targetActivityId: Long,
    val targetSetId: Long? = null,
    val completionsSinceGeneration: Int = 0,
    val generatedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_TUTORIAL = "TUTORIAL"
        const val TYPE_LEARN = "LEARN"
    }
}
