package com.example.app.ui.feature.auth.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.User
import com.example.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the LoginScreen.
 *
 * Handles the business logic for user authentication:
 * - Input validation
 * - Database lookup by email
 * - Password verification against stored hash
 * - UI state management
 *
 * Uses AndroidViewModel to access the application context for database initialization.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize database and repository
    private val database = AppDatabase.getInstance(application)
    private val userRepository = UserRepository(database.userDao())
    private val sessionManager = SessionManager.getInstance(application)

    // UI State
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Attempt to log in a user.
     *
     * @param email User's email
     * @param password User's password (plain text - will be hashed for comparison)
     * @param staySignedIn Whether to keep the user signed in (for future implementation)
     * @param onSuccess Callback when login is successful, provides the logged-in user
     */
    fun login(
        email: String,
        password: String,
        staySignedIn: Boolean = false,
        onSuccess: (User) -> Unit
    ) {
        // Validate email is not empty
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter your email"
            )
            return
        }

        // Validate password is not empty
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter your password"
            )
            return
        }

        // Set loading state
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Launch coroutine for database operation (off main thread)
        viewModelScope.launch {
            when (val result = userRepository.login(email, password)) {
                is UserRepository.LoginResult.Success -> {
                    // Save user session with staySignedIn preference
                    sessionManager.saveUserSession(result.user, staySignedIn)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        loggedInUser = result.user,
                        errorMessage = null
                    )

                    onSuccess(result.user)
                }
                is UserRepository.LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Reset the UI state.
     */
    fun resetState() {
        _uiState.value = LoginUiState()
    }
}

/**
 * UI State for the LoginScreen.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val loggedInUser: User? = null,
    val errorMessage: String? = null
)

