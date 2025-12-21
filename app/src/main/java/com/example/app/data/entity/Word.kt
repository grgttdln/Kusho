package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a word in the user's Word Bank.
 *
 * Each word is associated with a specific user through the userId foreign key.
 * A composite index ensures that the same word cannot be added twice by the same user.
 *
 * @property id Auto-generated primary key
 * @property userId Foreign key reference to the User who owns this word
 * @property word The word text stored in the Word Bank
 * @property imagePath Optional file path to an associated image (null if no image)
 * @property createdAt Timestamp when the word was added
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Delete words when user is deleted
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "word"], unique = true) // Prevent duplicate words per user
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Long,

    val word: String,

    val imagePath: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)
