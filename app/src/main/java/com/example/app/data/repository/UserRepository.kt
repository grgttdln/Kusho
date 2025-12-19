package com.example.app.data.repository

import com.example.app.data.dao.UserDao
import com.example.app.data.entity.User
import com.example.app.util.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for User data operations.
 *
 * This class acts as a single source of truth for user data and
 * abstracts the data sources from the rest of the app.
 *
 * All database operations are performed on the IO dispatcher
 * to avoid blocking the main thread.
 */
class UserRepository(private val userDao: UserDao) {

    /**
     * Result class for sign-up operation
     */
    sealed class SignUpResult {
        data class Success(val userId: Long) : SignUpResult()
        data class Error(val message: String) : SignUpResult()
    }

    /**
     * Result class for login operation
     */
    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    /**
     * Register a new user.
     *
     * This method:
     * 1. Validates the input
     * 2. Checks if email already exists
     * 3. Generates a salt and hashes the password
     * 4. Inserts the user into the database
     *
     * @param email User's email
     * @param name User's name
     * @param school User's school
     * @param password Plain text password (will be hashed)
     * @return SignUpResult indicating success or failure
     */
    suspend fun signUp(
        email: String,
        name: String,
        school: String,
        password: String
    ): SignUpResult = withContext(Dispatchers.IO) {
        try {
            // Validate input
            if (email.isBlank()) {
                return@withContext SignUpResult.Error("Email is required")
            }
            if (name.isBlank()) {
                return@withContext SignUpResult.Error("Name is required")
            }
            if (school.isBlank()) {
                return@withContext SignUpResult.Error("School is required")
            }

            // Validate password strength
            val (isValid, errorMessage) = PasswordUtils.validatePasswordStrength(password)
            if (!isValid) {
                return@withContext SignUpResult.Error(errorMessage ?: "Invalid password")
            }

            // Check if email already exists
            if (userDao.isEmailExists(email.lowercase().trim())) {
                return@withContext SignUpResult.Error("An account with this email already exists")
            }

            // Generate salt and hash password
            val salt = PasswordUtils.generateSalt()
            val passwordHash = PasswordUtils.hashPassword(password, salt)

            // Create user entity
            val user = User(
                email = email.lowercase().trim(),
                name = name.trim(),
                school = school.trim(),
                passwordHash = passwordHash,
                salt = salt
            )

            // Insert user and return the ID
            val userId = userDao.insertUser(user)
            SignUpResult.Success(userId)

        } catch (e: Exception) {
            SignUpResult.Error("Failed to create account: ${e.message}")
        }
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param email User's email
     * @param password Plain text password
     * @return LoginResult indicating success or failure
     */
    suspend fun login(email: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByEmail(email.lowercase().trim())
                ?: return@withContext LoginResult.Error("Invalid email or password")

            val isPasswordValid = PasswordUtils.verifyPassword(
                password = password,
                storedHash = user.passwordHash,
                salt = user.salt
            )

            if (!isPasswordValid) {
                return@withContext LoginResult.Error("Invalid email or password")
            }

            LoginResult.Success(user)

        } catch (e: Exception) {
            LoginResult.Error("Login failed: ${e.message}")
        }
    }

    /**
     * Get user by ID.
     */
    suspend fun getUserById(id: Long): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(id)
    }

    /**
     * Get user by email.
     */
    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email.lowercase().trim())
    }

    /**
     * Get all users as a Flow.
     */
    fun getAllUsersFlow(): Flow<List<User>> = userDao.getAllUsersFlow()

    /**
     * Delete user by ID.
     */
    suspend fun deleteUser(id: Long): Boolean = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id) > 0
    }
}

