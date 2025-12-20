package com.example.app.data

import android.content.Context
import android.content.SharedPreferences
import com.example.app.data.entity.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SessionManager handles user session persistence using SharedPreferences.
 *
 * This class provides:
 * - User login/logout session management
 * - Current user information as a Flow
 * - Persistent storage across app restarts
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Load saved user on initialization
        loadSavedUser()
    }

    /**
     * Save user session after successful login
     *
     * @param user The user to save
     * @param staySignedIn If true, session persists across app restarts. If false, session is temporary.
     */
    fun saveUserSession(user: User, staySignedIn: Boolean = true) {
        // Always update the in-memory current user
        _currentUser.value = user

        // Only persist to SharedPreferences if staySignedIn is true
        if (staySignedIn) {
            prefs.edit().apply {
                putLong(KEY_USER_ID, user.id)
                putString(KEY_EMAIL, user.email)
                putString(KEY_NAME, user.name)
                putString(KEY_SCHOOL, user.school)
                putBoolean(KEY_IS_LOGGED_IN, true)
                putBoolean(KEY_STAY_SIGNED_IN, true)
                apply()
            }
        } else {
            // Clear any existing persistent session but keep in-memory session
            prefs.edit().clear().apply()
        }
    }

    /**
     * Load saved user from SharedPreferences
     * Only loads if the user chose "Stay Signed In"
     */
    private fun loadSavedUser() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val staySignedIn = prefs.getBoolean(KEY_STAY_SIGNED_IN, false)

        // Only restore session if both flags are true
        if (isLoggedIn && staySignedIn) {
            val userId = prefs.getLong(KEY_USER_ID, 0)
            val email = prefs.getString(KEY_EMAIL, null)
            val name = prefs.getString(KEY_NAME, null)
            val school = prefs.getString(KEY_SCHOOL, null)

            if (userId != 0L && email != null && name != null && school != null) {
                _currentUser.value = User(
                    id = userId,
                    email = email,
                    name = name,
                    school = school,
                    passwordHash = "", // Not needed for session
                    salt = "" // Not needed for session
                )
            }
        }
    }

    /**
     * Clear user session on logout
     */
    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    /**
     * Check if user is logged in with a persistent session
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val staySignedIn = prefs.getBoolean(KEY_STAY_SIGNED_IN, false)
        return isLoggedIn && staySignedIn
    }

    /**
     * Get user ID of currently logged in user
     */
    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0)
    }

    /**
     * Get user name of currently logged in user
     */
    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "Guest") ?: "Guest"
    }

    companion object {
        private const val PREFS_NAME = "kusho_user_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_SCHOOL = "school"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_STAY_SIGNED_IN = "stay_signed_in"

        @Volatile
        private var INSTANCE: SessionManager? = null

        /**
         * Get singleton instance of SessionManager
         */
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

