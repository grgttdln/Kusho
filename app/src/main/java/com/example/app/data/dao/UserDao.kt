package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User entity.
 * All methods are suspend functions to ensure they run on a background thread.
 */
@Dao
interface UserDao {

    /**
     * Insert a new user into the database.
     * Returns the row ID of the inserted user.
     *
     * @param user The user to insert
     * @return The row ID of the newly inserted user
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    /**
     * Get a user by their email address.
     * Used for login and checking if email already exists.
     *
     * @param email The email to search for
     * @return The user if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    /**
     * Get a user by their ID.
     *
     * @param id The user ID
     * @return The user if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    /**
     * Check if an email already exists in the database.
     *
     * @param email The email to check
     * @return true if the email exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email LIMIT 1)")
    suspend fun isEmailExists(email: String): Boolean

    /**
     * Get all users as a Flow for reactive updates.
     * Useful for admin features or debugging.
     *
     * @return Flow of all users
     */
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<User>>

    /**
     * Delete a user by their ID.
     *
     * @param id The user ID to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long): Int

    /**
     * Update user's name and school.
     *
     * @param id The user ID
     * @param name New name
     * @param school New school
     * @return Number of rows updated
     */
    @Query("UPDATE users SET name = :name, school = :school WHERE id = :id")
    suspend fun updateUserProfile(id: Long, name: String, school: String): Int
}

