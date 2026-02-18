package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a student in the local database.
 *
 * @property studentId Auto-generated primary key
 * @property fullName Student's full name
 * @property pfpPath File path to profile picture (nullable)
 * @property gradeLevel Student's grade level
 * @property birthday Student's birthday
 */
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val studentId: Long = 0,

    val fullName: String,

    val pfpPath: String? = null,

    val gradeLevel: String,

    val birthday: String,

    val dominantHand: String = "RIGHT"
)
