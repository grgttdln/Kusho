package com.example.app.data.repository

import com.example.app.data.dao.UserDao
import com.example.app.data.entity.User
import com.example.app.util.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    sealed class SignUpResult {
        data class Success(val userId: Long) : SignUpResult()
        data class Error(val message: String) : SignUpResult()
    }

    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    suspend fun signUp(
        username: String,
        name: String,
        password: String
    ): SignUpResult = withContext(Dispatchers.IO) {
        try {
            if (username.isBlank()) {
                return@withContext SignUpResult.Error("Username is required")
            }
            if (name.isBlank()) {
                return@withContext SignUpResult.Error("Name is required")
            }

            val (isValid, errorMessage) = PasswordUtils.validatePasswordStrength(password)
            if (!isValid) {
                return@withContext SignUpResult.Error(errorMessage ?: "Invalid password")
            }

            if (userDao.isUsernameExists(username.trim())) {
                return@withContext SignUpResult.Error("Username is already taken")
            }

            val salt = PasswordUtils.generateSalt()
            val passwordHash = PasswordUtils.hashPassword(password, salt)

            val user = User(
                username = username.trim(),
                name = name.trim(),
                passwordHash = passwordHash,
                salt = salt
            )

            val userId = userDao.insertUser(user)
            SignUpResult.Success(userId)

        } catch (e: Exception) {
            SignUpResult.Error("Failed to create account: ${e.message}")
        }
    }

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByUsername(username.trim())
                ?: return@withContext LoginResult.Error("Invalid username or password")

            val isPasswordValid = PasswordUtils.verifyPassword(
                password = password,
                storedHash = user.passwordHash,
                salt = user.salt
            )

            if (!isPasswordValid) {
                return@withContext LoginResult.Error("Invalid username or password")
            }

            LoginResult.Success(user)

        } catch (e: Exception) {
            LoginResult.Error("Login failed: ${e.message}")
        }
    }

    suspend fun getUserById(id: Long): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(id)
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim())
    }

    fun getAllUsersFlow(): Flow<List<User>> = userDao.getAllUsersFlow()

    suspend fun deleteUser(id: Long): Boolean = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id) > 0
    }
}
