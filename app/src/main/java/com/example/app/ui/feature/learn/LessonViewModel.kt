package com.example.app.ui.feature.learn

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Word
import com.example.app.data.repository.WordRepository
import com.example.app.util.ImageStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val sessionManager = SessionManager.getInstance(application)
    private val imageStorageManager = ImageStorageManager(application)

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    // Current user ID (null if not logged in)
    private var currentUserId: Long? = null

    init {
        // Observe the current user session
        viewModelScope.launch {
            sessionManager.currentUser.collectLatest { user ->
                currentUserId = user?.id
                if (user != null) {
                    loadWordsForUser(user.id)
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
                inputError = null,
                imageError = null
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
     * Validates the image type before accepting.
     */
    fun onMediaSelected(uri: Uri?) {
        if (uri != null && !imageStorageManager.isValidImageUri(uri)) {
            _uiState.update {
                it.copy(
                    imageError = "Please select a JPG, PNG, or WebP image"
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                selectedMediaUri = uri,
                imageError = null
            )
        }
    }

    /**
     * Remove the selected media.
     */
    fun onRemoveMedia() {
        _uiState.update {
            it.copy(
                selectedMediaUri = null,
                imageError = null
            )
        }
    }

    /**
     * Validate and add the word to the Word Bank.
     * Saves to Room database associated with the current user.
     * If an image is selected, it will be saved to local storage and linked to the word.
     */
    fun addWordToBank() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(inputError = "Please log in to add words") }
            return
        }

        val currentWord = _uiState.value.wordInput.trim()
        val selectedUri = _uiState.value.selectedMediaUri

        // Basic validation before database operation
        if (!isValidWord(currentWord)) {
            _uiState.update { it.copy(inputError = "Please enter a valid word") }
            return
        }

        // Set loading state
        _uiState.update { it.copy(isLoading = true) }

        // Add word to database on background thread
        viewModelScope.launch {
            // Save image if one is selected
            var imagePath: String? = null
            if (selectedUri != null) {
                when (val saveResult = imageStorageManager.saveImageFromUri(selectedUri)) {
                    is ImageStorageManager.SaveResult.Success -> {
                        imagePath = saveResult.imagePath
                    }
                    is ImageStorageManager.SaveResult.Error -> {
                        _uiState.update {
                            it.copy(
                                imageError = saveResult.message,
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                }
            }

            // Add word with optional image path
            when (val result = wordRepository.addWord(userId, currentWord, imagePath)) {
                is WordRepository.AddWordResult.Success -> {
                    // Success - close input modal and show confirmation
                    _uiState.update {
                        it.copy(
                            isModalVisible = false,
                            wordInput = "",
                            selectedMediaUri = null,
                            inputError = null,
                            imageError = null,
                            isLoading = false,
                            // Show confirmation modal with the added word
                            isConfirmationVisible = true,
                            confirmedWord = currentWord
                        )
                    }
                }
                is WordRepository.AddWordResult.Error -> {
                    // Clean up saved image if word insertion failed
                    if (imagePath != null) {
                        imageStorageManager.deleteImage(imagePath)
                    }
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
     * Dismiss the confirmation modal and reset its state.
     */
    fun dismissConfirmation() {
        _uiState.update {
            it.copy(
                isConfirmationVisible = false,
                confirmedWord = ""
            )
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
    val imageError: String? = null,
    val isLoading: Boolean = false,
    val isConfirmationVisible: Boolean = false,
    val confirmedWord: String = ""
)

