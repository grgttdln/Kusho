package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "annotation_summary",
    indices = [
        Index(value = ["studentId", "setId", "sessionMode", "activityId"], unique = true),
        Index(value = ["studentId"])
    ]
)
data class AnnotationSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,
    val setId: Long,
    val sessionMode: String,
    val activityId: Long = 0L,
    val summaryText: String,
    val generatedAt: Long = System.currentTimeMillis()
)
