package com.example.app.data.entity

import androidx.room.Entity

/**
 * Join table to represent the many-to-many relationship between students and teachers (users).
 */
@Entity(
    tableName = "student_teachers",
    primaryKeys = ["studentId", "userId"]
)
data class StudentTeacher(
    val studentId: Long,
    val userId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

