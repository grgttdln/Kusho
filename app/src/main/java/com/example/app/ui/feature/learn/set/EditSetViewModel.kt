package com.example.app.ui.feature.learn.set

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for EditSetScreen
 */
data class EditSetUiState(
    val setId: Long = 0L,
    val setTitle: String = "",
    val setDescription: String = "",
    val selectedWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * One-time navigation events
 */
sealed class EditSetEvent {
    object UpdateSuccess : EditSetEvent()
    object DeleteSuccess : EditSetEvent()
}

/**
 * ViewModel for EditSetScreen
 * Manages loading, editing, and saving of existing sets
 */
class EditSetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val setRepository = SetRepository(database)

    private val _uiState = MutableStateFlow(EditSetUiState())
    val uiState: StateFlow<EditSetUiState> = _uiState.asStateFlow()
    
    // Channel for one-time events (navigation)
    private val _events = Channel<EditSetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    private var currentLoadedSetId: Long? = null

    /**
     * Load set details for editing.
     * Only reloads if the setId is different from the currently loaded set,
     * or if the set hasn't been loaded yet.
     *
     * @param setId The ID of the set to load
     * @param userId The user's ID (needed to check current word data from Word Bank)
     * @param forceReload Force reload even if the same setId
     */
    fun loadSet(setId: Long, userId: Long, forceReload: Boolean = false) {
        // Only reload if it's a different set or force reload is requested
        if (currentLoadedSetId == setId && !forceReload && _uiState.value.setId == setId) {
            return
        }

        currentLoadedSetId = setId

        viewModelScope.launch {
            // Reset all state flags before loading
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    isSaving = false,
                    isDeleting = false
                ) 
            }
            try {
                val setDetails = setRepository.getSetDetails(setId)
                if (setDetails != null) {
                    // Fetch current word data from Word Bank to check for updated images
                    val wordDao = database.wordDao()
                    val updatedWords = setDetails.words.map { word ->
                        // Look up the current word in the Word Bank
                        val currentWord = wordDao.getWordByTextForUser(userId, word.word)
                        // Use the current imagePath from Word Bank if available, otherwise fall back to persisted value
                        val currentImagePath = currentWord?.imagePath ?: word.imagePath
                        
                        SetRepository.SelectedWordConfig(
                            wordName = word.word,
                            configurationType = word.configurationType,
                            selectedLetterIndex = word.selectedLetterIndex,
                            imagePath = currentImagePath
                        )
                    }
                    
                    _uiState.update {
                        it.copy(
                            setId = setId,
                            setTitle = setDetails.set.title,
                            setDescription = setDetails.set.description ?: "",
                            selectedWords = updatedWords,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Set not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load set"
                    )
                }
            }
        }
    }

    /**
     * Reset the ViewModel state for editing a new set
     */
    fun resetForNewSet() {
        currentLoadedSetId = null
        _uiState.value = EditSetUiState()
    }

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
     * Remove a word from the selected words
     */
    fun removeWord(index: Int) {
        _uiState.update { state ->
            state.copy(
                selectedWords = state.selectedWords.filterIndexed { i, _ -> i != index }
            )
        }
    }

    /**
     * Update configuration type for a word at the given index
     */
    fun updateWordConfiguration(index: Int, configurationType: String) {
        _uiState.update { state ->
            state.copy(
                selectedWords = state.selectedWords.mapIndexed { i, word ->
                    if (i == index) word.copy(configurationType = configurationType) else word
                }
            )
        }
    }

    /**
     * Update selected letter index for a word at the given index
     */
    fun updateWordLetterIndex(index: Int, letterIndex: Int) {
        _uiState.update { state ->
            state.copy(
                selectedWords = state.selectedWords.mapIndexed { i, word ->
                    if (i == index) word.copy(selectedLetterIndex = letterIndex) else word
                }
            )
        }
    }

    /**
     * Update the set in the database
     * Returns true on success, false on failure
     */
    suspend fun updateSet(userId: Long): Boolean {
        val state = _uiState.value

        // Validation
        if (state.setTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Set title is required") }
            return false
        }

        if (state.selectedWords.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one word") }
            return false
        }

        if (state.selectedWords.size < 3) {
            _uiState.update { it.copy(errorMessage = "Set must contain at least 3 words") }
            return false
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        return try {
            val result = setRepository.updateSetWithWords(
                setId = state.setId,
                title = state.setTitle.trim(),
                description = if (state.setDescription.isNotBlank()) {
                    state.setDescription.trim()
                } else {
                    null
                },
                userId = userId,
                selectedWords = state.selectedWords
            )

            if (result is SetRepository.AddSetResult.Success) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Set updated successfully"
                    )
                }
                // Send one-time event for navigation
                _events.send(EditSetEvent.UpdateSuccess)
                true
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = if (result is SetRepository.AddSetResult.Error) {
                            result.message
                        } else {
                            "Failed to update set"
                        }
                    )
                }
                false
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to update set"
                )
            }
            false
        }
    }

    /**
     * Delete the set from the database
     */
    fun deleteSet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            try {
                val success = setRepository.deleteSet(_uiState.value.setId)
                if (success) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            successMessage = "Set deleted successfully"
                        )
                    }
                    // Send one-time event for navigation
                    _events.send(EditSetEvent.DeleteSuccess)
                } else {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "Failed to delete set"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = e.message ?: "Failed to delete set"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

