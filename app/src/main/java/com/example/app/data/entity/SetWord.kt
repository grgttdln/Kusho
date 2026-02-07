package com.example.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing the relationship between a Set and a Word.
 * This is a junction table that links words to sets and stores the configuration type.
 *
 * @property id Auto-generated primary key
 * @property setId Foreign key reference to the Set
 * @property wordId Foreign key reference to the Word
 * @property configurationType The type of configuration for this word (e.g., "fill in the blank", "identification", "air writing")
 * @property selectedLetterIndex The index of the selected letter for "Fill in the Blank" configuration (default: 0)
 * @property imagePath Optional image path for "Name the Picture" question type
 */
@Entity(
    tableName = "set_words",
    foreignKeys = [
        ForeignKey(
            entity = Set::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["setId"]),
        Index(value = ["wordId"]),
        Index(value = ["setId", "wordId"], unique = true) // Prevent duplicate word in set
    ]
)
data class SetWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setId: Long,
    val wordId: Long,
    val configurationType: String, // "fill in the blank", "identification", "air writing"
    val selectedLetterIndex: Int = 0, // Index of selected letter for "Fill in the Blank"
    @ColumnInfo(name = "imagePath")
    val imagePath: String? = null // Optional image path for "Name the Picture" question type
)
