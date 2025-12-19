package com.example.app.ui.feature.learn

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the Lesson Screen managing Word Bank state and modal interactions.
 */
class LessonViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    /**
     * Show the Word Bank modal.
     */
    fun showWordBankModal() {
        _uiState.update { it.copy(isModalVisible = true) }
    }

    /**
     * Hide the Word Bank modal and reset input state.
     */
    fun hideWordBankModal() {
        _uiState.update {
            it.copy(
                isModalVisible = false,
                wordInput = "",
                selectedMediaUri = null,
                inputError = null
            )
        }
    }

    /**
     * Update the word input text.
     */
    fun onWordInputChanged(word: String) {
        _uiState.update {
            it.copy(
                wordInput = word.trim(),
                inputError = null // Clear error when user types
            )
        }
    }

    /**
     * Update the selected media URI.
     */
    fun onMediaSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedMediaUri = uri) }
    }

    /**
     * Validate and add the word to the Word Bank.
     * Returns true if successful, false otherwise.
     */
    fun addWordToBank(): Boolean {
        val currentWord = _uiState.value.wordInput.trim()

        // Validate word
        if (!isValidWord(currentWord)) {
            _uiState.update { it.copy(inputError = "Please enter a valid word") }
            return false
        }

        // Check for duplicates
        if (_uiState.value.words.any { it.equals(currentWord, ignoreCase = true) }) {
            _uiState.update { it.copy(inputError = "This word already exists") }
            return false
        }

        // Add word to the list
        _uiState.update { state ->
            state.copy(
                words = state.words + currentWord,
                isModalVisible = false,
                wordInput = "",
                selectedMediaUri = null,
                inputError = null
            )
        }
        return true
    }

    /**
     * Handle word item click.
     */
    fun onWordClick(word: String) {
        // Can be extended for word selection, editing, etc.
    }

    /**
     * Validate if the word is valid.
     * A valid word is non-empty and contains only letters.
     */
    private fun isValidWord(word: String): Boolean {
        return word.isNotBlank() && word.all { it.isLetter() }
    }

    /**
     * Check if the current input is valid for submission.
     */
    fun isSubmitEnabled(): Boolean {
        val word = _uiState.value.wordInput.trim()
        return word.isNotBlank() && word.all { it.isLetter() }
    }
}

/**
 * UI State for the Lesson Screen.
 */
data class LessonUiState(
    val words: List<String> = listOf("cat", "map", "dog", "log", "sun", "bed", "pen", "fin", "sit", "mop"),
    val isModalVisible: Boolean = false,
    val wordInput: String = "",
    val selectedMediaUri: Uri? = null,
    val inputError: String? = null,
    val isLoading: Boolean = false
)

