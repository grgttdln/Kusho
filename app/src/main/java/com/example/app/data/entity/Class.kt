package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a class/classroom in the local database.
 *
 * @property classId Auto-generated primary key
 * @property className Name of the class
 * @property classCode Unique code for the class
 * @property bannerPath File path to class banner image (nullable)
 * @property isArchived Whether the class is archived (soft delete)
 * @property userId Foreign key to the user (teacher) who owns this class
 */
@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Class(
    @PrimaryKey(autoGenerate = true)
    val classId: Long = 0,

    val className: String,

    val classCode: String,

    val bannerPath: String? = null,

    val isArchived: Boolean = false,

    val userId: Long
)
