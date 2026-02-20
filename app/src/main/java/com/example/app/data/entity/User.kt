package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val username: String,

    val name: String,

    // Store the hashed password, never plain text
    val passwordHash: String,

    // Salt used for hashing - each user has a unique salt
    val salt: String,

    // Timestamp for when the account was created
    val createdAt: Long = System.currentTimeMillis()
)
