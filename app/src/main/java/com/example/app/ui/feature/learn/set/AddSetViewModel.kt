package com.example.app.ui.feature.learn.set

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.app.data.AppDatabase
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing the creation of sets.
 * Handles set data collection and database persistence.
 */
data class AddSetUiState(
    val setTitle: String = "",
    val setDescription: String = "",
    val selectedWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AddSetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val setRepository = SetRepository(
        database.setDao(),
        database.setWordDao(),
        database.wordDao()
    )

    private val _uiState = MutableStateFlow(AddSetUiState())
    val uiState: StateFlow<AddSetUiState> = _uiState.asStateFlow()

    /**
     * Update the set title
     */
    fun setTitle(title: String) {
        _uiState.update { it.copy(setTitle = title) }
    }

    /**
     * Update the set description
     */
    fun setDescription(description: String) {
        _uiState.update { it.copy(setDescription = description) }
    }

    /**
     * Set the selected words with their configurations
     */
    fun setSelectedWords(words: List<SetRepository.SelectedWordConfig>) {
        _uiState.update { it.copy(selectedWords = words) }
    }

    /**
     * Create and save the set to the database (independent of activities)
     * Suspends until the database operation completes
     */
    suspend fun createSet(
        title: String,
        description: String,
        selectedWords: List<SetRepository.SelectedWordConfig>,
        userId: Long
    ): Boolean {
        // Validation
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Set title is required") }
            return false
        }

        if (selectedWords.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one word") }
            return false
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        return try {
            val result = setRepository.addSetWithWords(
                title = title.trim(),
                description = if (description.isNotBlank()) {
                    description.trim()
                } else {
                    null
                },
                userId = userId,
                selectedWords = selectedWords
            )

            if (result is SetRepository.AddSetResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Set '$title' created successfully",
                        setTitle = "",
                        setDescription = "",
                        selectedWords = emptyList()
                    )
                }
                true
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (result is SetRepository.AddSetResult.Error) {
                            result.message
                        } else {
                            "Failed to create set"
                        }
                    )
                }
                false
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to create set"
                )
            }
            false
        }
    }

    /**
     * Clear success message after displaying it
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Reset the form
     */
    fun resetForm() {
        _uiState.value = AddSetUiState()
    }
}
