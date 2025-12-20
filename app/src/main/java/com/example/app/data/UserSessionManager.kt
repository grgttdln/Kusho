package com.example.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property to create a DataStore instance.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * Manager class for handling user session state.
 *
 * Uses DataStore to persist the currently logged-in user's information.
 * This allows the app to remember the user across app restarts.
 */
class UserSessionManager(private val context: Context) {

    companion object {
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        /**
         * Get the singleton instance of UserSessionManager.
         */
        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Data class representing the current user session.
     */
    data class UserSession(
        val userId: Long,
        val email: String,
        val name: String
    )

    /**
     * Flow of the current user session.
     * Emits null if no user is logged in.
     */
    val currentUserSession: Flow<UserSession?> = context.dataStore.data.map { preferences ->
        val userId = preferences[KEY_USER_ID]
        val email = preferences[KEY_USER_EMAIL]
        val name = preferences[KEY_USER_NAME]

        if (userId != null && userId != -1L && email != null && name != null) {
            UserSession(userId, email, name)
        } else {
            null
        }
    }

    /**
     * Flow of just the current user ID.
     * Returns null if no user is logged in.
     */
    val currentUserId: Flow<Long?> = context.dataStore.data.map { preferences ->
        val userId = preferences[KEY_USER_ID]
        if (userId != null && userId != -1L) userId else null
    }

    /**
     * Save the user session after successful login.
     *
     * @param userId The user's database ID
     * @param email The user's email
     * @param name The user's name
     */
    suspend fun saveUserSession(userId: Long, email: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_NAME] = name
        }
    }

    /**
     * Clear the user session (logout).
     */
    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = -1L
            preferences[KEY_USER_EMAIL] = ""
            preferences[KEY_USER_NAME] = ""
        }
    }

    /**
     * Check if a user is currently logged in.
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val userId = preferences[KEY_USER_ID]
        userId != null && userId != -1L
    }
}
