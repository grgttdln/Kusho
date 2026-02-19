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
    val completedAt: Long = System.currentTimeMillis()
)
