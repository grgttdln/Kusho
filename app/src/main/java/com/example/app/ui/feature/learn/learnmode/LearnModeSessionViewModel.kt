package com.example.app.ui.feature.learn.learnmode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class representing a word item in the learn mode session.
 */
data class LearnModeWordItem(
    val word: String,
    val selectedLetterIndex: Int,
    val imagePath: String?,
    val configurationType: String
) {
    /**
     * Returns the word with the selected letter masked as "_"
     */
    fun getMaskedWord(): String {
        return word.mapIndexed { index, char ->
            if (index == selectedLetterIndex) "_" else char.uppercaseChar()
        }.joinToString(" ")
    }
    
    /**
     * Returns the letter that is hidden/masked
     */
    fun getHiddenLetter(): Char {
        return word.getOrElse(selectedLetterIndex) { ' ' }.uppercaseChar()
    }
}

data class LearnModeSessionUiState(
    val activityTitle: String = "",
    val words: List<LearnModeWordItem> = emptyList(),
    val currentWordIndex: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSessionComplete: Boolean = false
) {
    val currentWord: LearnModeWordItem?
        get() = words.getOrNull(currentWordIndex)
    
    val totalWords: Int
        get() = words.size
    
    val currentStep: Int
        get() = currentWordIndex + 1
}

class LearnModeSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val setRepository = SetRepository(
        database.setDao(),
        database.setWordDao(),
        database.wordDao()
    )

    private val _uiState = MutableStateFlow(LearnModeSessionUiState())
    val uiState: StateFlow<LearnModeSessionUiState> = _uiState.asStateFlow()

    /**
     * Load words for a specific set in learn mode
     */
    fun loadSetForLearnMode(setId: Long, activityTitle: String) {
        viewModelScope.launch {
            // Reset session state before loading new set
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    activityTitle = activityTitle,
                    isSessionComplete = false,
                    currentWordIndex = 0,
                    errorMessage = null
                ) 
            }
            
            try {
                val setDetails = setRepository.getSetDetails(setId)
                if (setDetails != null) {
                    val wordItems = setDetails.words.map { wordConfig ->
                        LearnModeWordItem(
                            word = wordConfig.word,
                            selectedLetterIndex = wordConfig.selectedLetterIndex,
                            imagePath = wordConfig.imagePath,
                            configurationType = wordConfig.configurationType
                        )
                    }
                    
                    _uiState.update {
                        it.copy(
                            words = wordItems,
                            currentWordIndex = 0,
                            isLoading = false,
                            isSessionComplete = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load set details"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Move to the next word in the session
     */
    fun nextWord() {
        _uiState.update { state ->
            if (state.currentWordIndex < state.words.size - 1) {
                state.copy(currentWordIndex = state.currentWordIndex + 1)
            } else {
                state.copy(isSessionComplete = true)
            }
        }
    }

    /**
     * Skip current word and move to next
     */
    fun skipWord() {
        nextWord()
    }

    /**
     * Reset the session to start from the beginning
     */
    fun resetSession() {
        _uiState.update {
            it.copy(
                currentWordIndex = 0,
                isSessionComplete = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
