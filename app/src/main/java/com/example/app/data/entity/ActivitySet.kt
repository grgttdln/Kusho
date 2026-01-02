package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between Activity and Set
 * Allows an Activity to contain multiple Sets
 */
@Entity(
    tableName = "activity_set",
    primaryKeys = ["activityId", "setId"],
    foreignKeys = [
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
        Index(value = ["activityId"]),
        Index(value = ["setId"])
    ]
)
data class ActivitySet(
    val activityId: Long,
    val setId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
