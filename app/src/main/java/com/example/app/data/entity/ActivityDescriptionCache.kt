package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_description_cache",
    indices = [
        Index(value = ["setId", "sessionMode", "activityId"], unique = true)
    ]
)
data class ActivityDescriptionCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setId: Long,
    val sessionMode: String,
    val activityId: Long = 0L,
    val descriptionText: String,
    val generatedAt: Long = System.currentTimeMillis()
)
