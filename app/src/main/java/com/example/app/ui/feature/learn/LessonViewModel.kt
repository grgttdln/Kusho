package com.example.app.ui.feature.learn

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.UserSessionManager
import com.example.app.data.entity.Word
import com.example.app.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Lesson Screen managing Word Bank state and modal interactions.
 *
 * Uses AndroidViewModel to access the application context for database and session management.
 * Words are persisted to Room database and associated with the current user.
 */
class LessonViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize database and repositories
    private val database = AppDatabase.getInstance(application)
    private val wordRepository = WordRepository(database.wordDao())
    private val sessionManager = UserSessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    // Current user ID (null if not logged in)
    private var currentUserId: Long? = null

    init {
        // Observe the current user session
        viewModelScope.launch {
            sessionManager.currentUserId.collectLatest { userId ->
                currentUserId = userId
                if (userId != null) {
                    loadWordsForUser(userId)
                } else {
                    // Clear words if no user is logged in
                    _uiState.update { it.copy(words = emptyList()) }
                }
            }
        }
    }

    /**
     * Load words for the current user from the database.
     */
    private fun loadWordsForUser(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            wordRepository.getWordsForUser(userId).collectLatest { words ->
                _uiState.update {
                    it.copy(
                        words = words,
                        isLoading = false
                    )
                }
            }
        }
    }

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
     * Saves to Room database associated with the current user.
     */
    fun addWordToBank() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(inputError = "Please log in to add words") }
            return
        }

        val currentWord = _uiState.value.wordInput.trim()

        // Basic validation before database operation
        if (!isValidWord(currentWord)) {
            _uiState.update { it.copy(inputError = "Please enter a valid word") }
            return
        }

        // Set loading state
        _uiState.update { it.copy(isLoading = true) }

        // Add word to database on background thread
        viewModelScope.launch {
            when (val result = wordRepository.addWord(userId, currentWord)) {
                is WordRepository.AddWordResult.Success -> {
                    // Success - close modal and reset input
                    _uiState.update {
                        it.copy(
                            isModalVisible = false,
                            wordInput = "",
                            selectedMediaUri = null,
                            inputError = null,
                            isLoading = false
                        )
                    }
                }
                is WordRepository.AddWordResult.Error -> {
                    // Show error message
                    _uiState.update {
                        it.copy(
                            inputError = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Delete a word from the Word Bank.
     */
    fun deleteWord(wordId: Long) {
        viewModelScope.launch {
            wordRepository.deleteWord(wordId)
        }
    }

    /**
     * Handle word item click.
     */
    fun onWordClick(word: Word) {
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
        return word.isNotBlank() && word.all { it.isLetter() } && !_uiState.value.isLoading
    }
}

/**
 * UI State for the Lesson Screen.
 */
data class LessonUiState(
    val words: List<Word> = emptyList(),
    val isModalVisible: Boolean = false,
    val wordInput: String = "",
    val selectedMediaUri: Uri? = null,
    val inputError: String? = null,
    val isLoading: Boolean = false
)

