package com.example.app.ui.feature.learn

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Word
import com.example.app.data.model.AiGenerationResult
import com.example.app.data.repository.GeminiRepository
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
    private val geminiRepository = GeminiRepository()

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
        if (userId == null || userId == 0L) {
            _uiState.update { 
                it.copy(
                    inputError = "Please log in to add words",
                    isLoading = false
                ) 
            }
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
            try {
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
            } catch (e: Exception) {
                // Clean up saved image on any error
                if (imagePath != null) {
                    imageStorageManager.deleteImage(imagePath)
                }
                // Show error message
                _uiState.update {
                    it.copy(
                        inputError = "Failed to add word: ${e.message}",
                        isLoading = false
                    )
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
     * Handle word item click - opens the edit modal with the selected word.
     */
    fun onWordClick(word: Word) {
        _uiState.update {
            it.copy(
                isEditModalVisible = true,
                editingWord = word,
                editWordInput = word.word,
                editSelectedMediaUri = null,
                editInputError = null,
                editImageError = null,
                isEditLoading = false
            )
        }
    }

    /**
     * Hide the edit modal and reset its state.
     */
    fun hideEditModal() {
        _uiState.update {
            it.copy(
                isEditModalVisible = false,
                editingWord = null,
                editWordInput = "",
                editSelectedMediaUri = null,
                editInputError = null,
                editImageError = null,
                isEditLoading = false
            )
        }
    }

    /**
     * Update the edit word input text.
     */
    fun onEditWordInputChanged(word: String) {
        _uiState.update {
            it.copy(
                editWordInput = word.trim(),
                editInputError = null
            )
        }
    }

    /**
     * Update the selected media URI for editing.
     */
    fun onEditMediaSelected(uri: Uri?) {
        if (uri != null && !imageStorageManager.isValidImageUri(uri)) {
            _uiState.update {
                it.copy(
                    editImageError = "Please select a JPG, PNG, or WebP image"
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                editSelectedMediaUri = uri,
                editImageError = null
            )
        }
    }

    /**
     * Remove the selected media for editing.
     */
    fun onEditRemoveMedia() {
        _uiState.update {
            it.copy(
                editSelectedMediaUri = null,
                editImageError = null
            )
        }
    }

    /**
     * Check if the edit input is valid for submission.
     */
    fun isEditSaveEnabled(): Boolean {
        val word = _uiState.value.editWordInput.trim()
        return word.isNotBlank() && word.all { it.isLetter() } && !_uiState.value.isEditLoading
    }

    /**
     * Save the edited word to the Word Bank.
     */
    fun saveEditedWord() {
        val userId = currentUserId
        val editingWord = _uiState.value.editingWord

        if (userId == null || userId == 0L) {
            _uiState.update {
                it.copy(
                    editInputError = "Please log in to update words",
                    isEditLoading = false
                )
            }
            return
        }

        if (editingWord == null) {
            _uiState.update {
                it.copy(
                    editInputError = "No word selected for editing",
                    isEditLoading = false
                )
            }
            return
        }

        val newWord = _uiState.value.editWordInput.trim()
        val newMediaUri = _uiState.value.editSelectedMediaUri

        if (!isValidWord(newWord)) {
            _uiState.update { it.copy(editInputError = "Please enter a valid word") }
            return
        }

        _uiState.update { it.copy(isEditLoading = true) }

        viewModelScope.launch {
            var newImagePath: String? = editingWord.imagePath

            if (newMediaUri != null) {
                when (val saveResult = imageStorageManager.saveImageFromUri(newMediaUri)) {
                    is ImageStorageManager.SaveResult.Success -> {
                        if (editingWord.imagePath != null) {
                            imageStorageManager.deleteImage(editingWord.imagePath)
                        }
                        newImagePath = saveResult.imagePath
                    }
                    is ImageStorageManager.SaveResult.Error -> {
                        _uiState.update {
                            it.copy(
                                editImageError = saveResult.message,
                                isEditLoading = false
                            )
                        }
                        return@launch
                    }
                }
            }

            try {
                when (val result = wordRepository.updateWord(userId, editingWord.id, newWord, newImagePath)) {
                    is WordRepository.UpdateWordResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isEditModalVisible = false,
                                editingWord = null,
                                editWordInput = "",
                                editSelectedMediaUri = null,
                                editInputError = null,
                                editImageError = null,
                                isEditLoading = false
                            )
                        }
                    }
                    is WordRepository.UpdateWordResult.Error -> {
                        if (newMediaUri != null && newImagePath != editingWord.imagePath) {
                            newImagePath?.let { imageStorageManager.deleteImage(it) }
                        }
                        _uiState.update {
                            it.copy(
                                editInputError = result.message,
                                isEditLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (newMediaUri != null && newImagePath != editingWord.imagePath) {
                    newImagePath?.let { imageStorageManager.deleteImage(it) }
                }
                _uiState.update {
                    it.copy(
                        editInputError = "Failed to update word: ${e.message}",
                        isEditLoading = false
                    )
                }
            }
        }
    }

    /**
     * Delete the word being edited from the Word Bank.
     */
    fun deleteEditingWord() {
        val editingWord = _uiState.value.editingWord ?: return

        _uiState.update { it.copy(isEditLoading = true) }

        viewModelScope.launch {
            editingWord.imagePath?.let { imageStorageManager.deleteImage(it) }
            wordRepository.deleteWord(editingWord.id)

            _uiState.update {
                it.copy(
                    isEditModalVisible = false,
                    editingWord = null,
                    editWordInput = "",
                    editSelectedMediaUri = null,
                    editInputError = null,
                    editImageError = null,
                    isEditLoading = false
                )
            }
        }
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

    /**
     * Show the Activity Creation modal.
     */
    fun showActivityCreationModal() {
        _uiState.update { it.copy(isActivityCreationModalVisible = true) }
    }

    /**
     * Hide the Activity Creation modal and reset its state.
     */
    fun hideActivityCreationModal() {
        _uiState.update {
            it.copy(
                isActivityCreationModalVisible = false,
                activityInput = "",
                selectedActivityWordIds = emptySet(),
                isActivityCreationLoading = false
            )
        }
    }

    /**
     * Update the activity input text.
     */
    fun onActivityInputChanged(input: String) {
        _uiState.update {
            it.copy(activityInput = input)
        }
    }

    /**
     * Toggle word selection for activity creation.
     */
    fun onActivityWordSelectionChanged(wordId: Long, isSelected: Boolean) {
        _uiState.update { state: LessonUiState ->
            val currentSelection = state.selectedActivityWordIds
            val newSelection = if (isSelected) {
                currentSelection + wordId
            } else {
                currentSelection - wordId
            }
            state.copy(selectedActivityWordIds = newSelection)
        }
    }

    /**
     * Select all words for activity creation.
     */
    fun onSelectAllActivityWords() {
        _uiState.update { state: LessonUiState ->
            val allWordIds = state.words.map { it.id }.toSet()
            // If all words are already selected, deselect all; otherwise select all
            val newSelection = if (state.selectedActivityWordIds == allWordIds) {
                emptySet()
            } else {
                allWordIds
            }
            state.copy(selectedActivityWordIds = newSelection)
        }
    }

    /**
     * Generate activity using AI with selected words.
     * Stores the generated JSON result and signals completion via callback.
     */
    fun createActivity(onGenerationComplete: (String) -> Unit) {
        val userId = currentUserId
        if (userId == null || userId == 0L) {
            _uiState.update { it.copy(activityError = "Please log in to generate activities") }
            return
        }

        val activityDescription = _uiState.value.activityInput.trim()
        val selectedWordIds = _uiState.value.selectedActivityWordIds

        if (activityDescription.isBlank() || selectedWordIds.isEmpty()) {
            _uiState.update { it.copy(activityError = "Please enter a description and select at least one word") }
            return
        }

        val selectedWords = _uiState.value.words
            .filter { selectedWordIds.contains(it.id) }

        _uiState.update { it.copy(isActivityCreationLoading = true, activityError = null) }

        viewModelScope.launch {
            when (val result = geminiRepository.generateActivity(activityDescription, selectedWords)) {
                is AiGenerationResult.Success -> {
                    val jsonResult = com.google.gson.Gson().toJson(result.data)
                    _uiState.update {
                        it.copy(
                            isActivityCreationModalVisible = false,
                            activityInput = "",
                            selectedActivityWordIds = emptySet(),
                            isActivityCreationLoading = false,
                            generatedJsonResult = jsonResult
                        )
                    }
                    onGenerationComplete(jsonResult)
                }
                is AiGenerationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActivityCreationLoading = false,
                            activityError = result.message
                        )
                    }
                }
            }
        }
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
    val confirmedWord: String = "",
    // Edit modal state
    val isEditModalVisible: Boolean = false,
    val editingWord: Word? = null,
    val editWordInput: String = "",
    val editSelectedMediaUri: Uri? = null,
    val editInputError: String? = null,
    val editImageError: String? = null,
    val isEditLoading: Boolean = false,
    // Activity creation modal state
    val isActivityCreationModalVisible: Boolean = false,
    val activityInput: String = "",
    val selectedActivityWordIds: Set<Long> = emptySet(),
    val isActivityCreationLoading: Boolean = false,
    val activityError: String? = null,
    val generatedJsonResult: String? = null
)

