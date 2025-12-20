package com.example.app.ui.feature.auth.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.UserSessionManager
import com.example.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the SignUpScreen.
 */
class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val userRepository = UserRepository(database.userDao())
    private val sessionManager = UserSessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(
        email: String,
        name: String,
        school: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Passwords do not match"
            )
            return
        }

        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter a valid email address"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = userRepository.signUp(email, name, school, password)) {
                is UserRepository.SignUpResult.Success -> {
                    // Save user session for persistence
                    sessionManager.saveUserSession(
                        userId = result.userId,
                        email = email.lowercase().trim(),
                        name = name.trim()
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        userId = result.userId,
                        errorMessage = null
                    )
                    onSuccess()
                }
                is UserRepository.SignUpResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetState() {
        _uiState.value = SignUpUiState()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }
}

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userId: Long? = null,
    val errorMessage: String? = null
)

