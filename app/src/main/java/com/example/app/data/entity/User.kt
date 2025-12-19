package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a user in the local database.
 *
 * Security Note: The password field stores a hashed version of the user's password,
 * never the plain text password. We use PBKDF2WithHmacSHA256 for secure hashing.
 *
 * @property id Auto-generated primary key
 * @property email User's email address (unique constraint)
 * @property name User's display name
 * @property school User's school name
 * @property passwordHash The securely hashed password (never store plain text!)
 * @property salt The random salt used for password hashing
 * @property createdAt Timestamp when the user was created
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val email: String,

    val name: String,

    val school: String,

    // Store the hashed password, never plain text
    val passwordHash: String,

    // Salt used for hashing - each user has a unique salt
    val salt: String,

    // Timestamp for when the account was created
    val createdAt: Long = System.currentTimeMillis()
)

