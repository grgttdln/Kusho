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
import com.example.app.data.repository.GenerationPhase
import com.example.app.data.repository.WordRepository
import com.example.app.util.DictionaryValidator
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
    private val sessionManager = SessionManager.getInstance(application)
    private val imageStorageManager = ImageStorageManager(application)
    private val dictionaryValidator = DictionaryValidator(application)
    private val wordRepository = WordRepository(database.wordDao(), dictionaryValidator)
    private val activityDao = database.activityDao()
    private val setDao = database.setDao()
    private val geminiRepository = GeminiRepository()

    // Cached existing set titles for regeneration support
    private var cachedExistingSetTitles: List<String> = emptyList()

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private val _generationPhase = MutableStateFlow<GenerationPhase>(GenerationPhase.Idle)
    val generationPhase: StateFlow<GenerationPhase> = _generationPhase.asStateFlow()

    // Current user ID (null if not logged in)
    private var currentUserId: Long? = null

    // Cached generation context for regeneration
    private var lastGenerationPrompt: String = ""
    private var lastSelectedWords: List<Word> = emptyList()

    // Cached suggested prompts for generation modal
    private var cachedSuggestedPrompts: List<String> = emptyList()
    private var cachedWordCountForPrompts: Int = -1

    // Cached suggested prompts for activity creation modal
    private var cachedActivitySuggestedPrompts: List<String> = emptyList()
    private var cachedActivityWordCountForPrompts: Int = -1

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
                imageError = null,
                dictionarySuggestions = emptyList()
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
                inputError = null,
                dictionarySuggestions = emptyList()
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
                is WordRepository.AddWordResult.NotInDictionary -> {
                    if (imagePath != null) {
                        imageStorageManager.deleteImage(imagePath)
                    }
                    _uiState.update {
                        it.copy(
                            inputError = "This word was not found in the dictionary",
                            dictionarySuggestions = result.suggestions,
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
                isEditLoading = false,
                editDictionarySuggestions = emptyList()
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
                editInputError = null,
                editDictionarySuggestions = emptyList()
            )
        }
    }

    /**
     * Handle suggestion click in the add word modal.
     */
    fun onSuggestionClick(word: String) {
        _uiState.update {
            it.copy(
                wordInput = word,
                inputError = null,
                dictionarySuggestions = emptyList()
            )
        }
    }

    /**
     * Handle suggestion click in the edit word modal.
     */
    fun onEditSuggestionClick(word: String) {
        _uiState.update {
            it.copy(
                editWordInput = word,
                editInputError = null,
                editDictionarySuggestions = emptyList()
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
                    is WordRepository.UpdateWordResult.NotInDictionary -> {
                        if (newMediaUri != null && newImagePath != editingWord.imagePath) {
                            newImagePath?.let { imageStorageManager.deleteImage(it) }
                        }
                        _uiState.update {
                            it.copy(
                                editInputError = "This word was not found in the dictionary",
                                editDictionarySuggestions = result.suggestions,
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
        _generationPhase.value = GenerationPhase.Idle
        _uiState.update {
            it.copy(
                isActivityCreationModalVisible = false,
                activityInput = "",
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
     * Generate activity using AI with the entire word bank.
     * Stores the generated JSON result and signals completion via callback.
     */
    fun createActivity(onGenerationComplete: (String) -> Unit) {
        val userId = currentUserId
        if (userId == null || userId == 0L) {
            _uiState.update { it.copy(activityError = "Please log in to generate activities") }
            return
        }

        val activityDescription = _uiState.value.activityInput.trim()

        if (activityDescription.isBlank()) {
            _uiState.update { it.copy(activityError = "Please enter a description") }
            return
        }

        val allWords = _uiState.value.words
        if (allWords.isEmpty()) {
            _uiState.update { it.copy(activityError = "No words in your Word Bank yet. Add some words first!") }
            return
        }

        // Cache for regeneration
        lastGenerationPrompt = activityDescription
        lastSelectedWords = allWords

        _uiState.update { it.copy(isActivityCreationLoading = true, activityError = null) }
        _generationPhase.value = GenerationPhase.Filtering

        viewModelScope.launch {
            // Fetch existing titles for similarity detection
            val existingActivityTitles = activityDao.getActivitiesByUserIdOnce(userId).map { it.title }
            val existingSetTitles = setDao.getSetsWithWordNames(userId).map { it.setTitle }.distinct()
            cachedExistingSetTitles = existingSetTitles

            val result = geminiRepository.generateActivity(
                activityDescription,
                allWords,
                existingActivityTitles = existingActivityTitles,
                existingSetTitles = existingSetTitles
            ) { phase ->
                _generationPhase.value = phase
            }

            when (result) {
                is AiGenerationResult.Success -> {
                    _generationPhase.value = GenerationPhase.Idle
                    val jsonResult = com.google.gson.Gson().toJson(result.data)
                    _uiState.update {
                        it.copy(
                            isActivityCreationModalVisible = false,
                            activityInput = "",
                            isActivityCreationLoading = false,
                            generatedJsonResult = jsonResult
                        )
                    }
                    onGenerationComplete(jsonResult)
                }
                is AiGenerationResult.Error -> {
                    _generationPhase.value = GenerationPhase.Idle
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

    /**
     * Regenerate a single set using the same GeminiRepository that did the original generation.
     * Reuses cached filtered words from Step 1.
     */
    fun regenerateSet(
        currentSetTitle: String,
        currentSetDescription: String,
        onResult: (String?) -> Unit
    ) {
        if (lastSelectedWords.isEmpty()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            val result = geminiRepository.regenerateSet(
                prompt = lastGenerationPrompt,
                availableWords = lastSelectedWords,
                currentSetTitle = currentSetTitle,
                currentSetDescription = currentSetDescription,
                existingSetTitles = cachedExistingSetTitles,
                onPhaseChange = { phase ->
                    _generationPhase.value = phase
                }
            )

            when (result) {
                is AiGenerationResult.Success -> {
                    _generationPhase.value = GenerationPhase.Idle
                    val gson = com.google.gson.Gson()
                    onResult(gson.toJson(result.data))
                }
                is AiGenerationResult.Error -> {
                    _generationPhase.value = GenerationPhase.Idle
                    onResult(null)
                }
            }
        }
    }

    /**
     * Generate one additional set to append to the existing generated sets.
     * Reuses the regenerateSet pipeline, passing all existing titles as avoidance context.
     */
    fun addMoreSet(
        existingSetTitles: List<String>,
        existingSetDescriptions: List<String>,
        onResult: (String?) -> Unit
    ) {
        if (lastSelectedWords.isEmpty()) {
            onResult(null)
            return
        }

        // Combine existing titles into avoidance context for the AI
        val avoidTitle = existingSetTitles.joinToString(", ").take(50)
        val avoidDescription = existingSetDescriptions.joinToString("; ").take(100)

        viewModelScope.launch {
            val allTitlesToAvoid = (cachedExistingSetTitles + existingSetTitles).distinct()

            val result = geminiRepository.regenerateSet(
                prompt = lastGenerationPrompt,
                availableWords = lastSelectedWords,
                currentSetTitle = avoidTitle,
                currentSetDescription = avoidDescription,
                existingSetTitles = allTitlesToAvoid,
                onPhaseChange = { phase ->
                    _generationPhase.value = phase
                }
            )

            when (result) {
                is AiGenerationResult.Success -> {
                    _generationPhase.value = GenerationPhase.Idle
                    val gson = com.google.gson.Gson()
                    onResult(gson.toJson(result.data))
                }
                is AiGenerationResult.Error -> {
                    _generationPhase.value = GenerationPhase.Idle
                    onResult(null)
                }
            }
        }
    }

    /**
     * Load suggested prompts for the generation modal.
     * Uses cached prompts if word bank size hasn't changed.
     */
    fun loadSuggestedPrompts() {
        val currentWords = _uiState.value.words
        val currentWordCount = currentWords.size

        // Use cache if word bank size hasn't changed
        if (currentWordCount == cachedWordCountForPrompts && cachedSuggestedPrompts.isNotEmpty()) {
            _uiState.update { it.copy(suggestedPrompts = cachedSuggestedPrompts) }
            return
        }

        // Generate new suggestions
        _uiState.update { it.copy(isSuggestionsLoading = true, suggestedPrompts = emptyList()) }

        viewModelScope.launch {
            try {
                val prompts = geminiRepository.generateSuggestedPrompts(currentWords)

                if (prompts.isNotEmpty()) {
                    cachedSuggestedPrompts = prompts
                    cachedWordCountForPrompts = currentWordCount
                }

                _uiState.update {
                    it.copy(
                        suggestedPrompts = prompts,
                        isSuggestionsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        suggestedPrompts = emptyList(),
                        isSuggestionsLoading = false
                    )
                }
            }
        }
    }

    /**
     * Load suggested prompts for the activity creation modal.
     * Uses cached prompts if word bank size hasn't changed.
     */
    fun loadActivitySuggestedPrompts() {
        val currentWords = _uiState.value.words
        val currentWordCount = currentWords.size

        // Don't load suggestions for empty word bank
        if (currentWordCount == 0) return

        // Use cache if word bank size hasn't changed
        if (currentWordCount == cachedActivityWordCountForPrompts && cachedActivitySuggestedPrompts.isNotEmpty()) {
            _uiState.update { it.copy(activitySuggestedPrompts = cachedActivitySuggestedPrompts) }
            return
        }

        // Generate new suggestions
        _uiState.update { it.copy(isActivitySuggestionsLoading = true, activitySuggestedPrompts = emptyList()) }

        viewModelScope.launch {
            try {
                val prompts = geminiRepository.generateActivitySuggestedPrompts(currentWords)

                if (prompts.isNotEmpty()) {
                    cachedActivitySuggestedPrompts = prompts
                    cachedActivityWordCountForPrompts = currentWordCount
                }

                _uiState.update {
                    it.copy(
                        activitySuggestedPrompts = prompts,
                        isActivitySuggestionsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        activitySuggestedPrompts = emptyList(),
                        isActivitySuggestionsLoading = false
                    )
                }
            }
        }
    }

    /**
     * Generate CVC words using Gemini and add them to the word bank.
     * Orchestrates: Gemini call → CVC filter → dedup → batch DB insert → success state.
     *
     * @param prompt Teacher's description of what CVC words to generate
     * @param count Number of words requested (from stepper)
     */
    fun generateWords(prompt: String, count: Int) {
        val userId = currentUserId
        if (userId == null || userId == 0L) {
            _uiState.update { it.copy(wordGenerationError = "Please log in to generate words") }
            return
        }

        if (prompt.isBlank()) {
            _uiState.update { it.copy(wordGenerationError = "Please enter a description") }
            return
        }

        _uiState.update {
            it.copy(
                isWordGenerationLoading = true,
                wordGenerationError = null,
                generatedWords = emptyList(),
                wordGenerationRequestedCount = count
            )
        }

        viewModelScope.launch {
            try {
                // 1. Fetch existing words for dedup context
                val existingWords = wordRepository.getWordsForUserOnce(userId)
                val existingWordSet = existingWords.map { it.word.lowercase() }.toSet()

                // 2. Call Gemini to generate CVC word candidates
                val candidates = geminiRepository.generateCVCWords(prompt, count, existingWords)

                // 3. CVC regex safety filter
                val cvcValid = candidates.filter { com.example.app.util.WordValidator.isCVCPattern(it) }

                // 4. Dedup against existing word bank
                val deduped = cvcValid.filter { it.lowercase() !in existingWordSet }

                // 5. Take requested count
                val wordsToAdd = deduped.take(count)

                if (wordsToAdd.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isWordGenerationLoading = false,
                            wordGenerationError = if (cvcValid.isEmpty()) {
                                "No valid CVC words could be generated. Try a different prompt."
                            } else {
                                "All generated words already exist in your word bank. Try a different prompt."
                            }
                        )
                    }
                    return@launch
                }

                // 6. Batch insert to DB
                val wordDao = database.wordDao()
                val addedWords = mutableListOf<String>()
                for (word in wordsToAdd) {
                    try {
                        val id = wordDao.insertWord(
                            com.example.app.data.entity.Word(
                                userId = userId,
                                word = word
                            )
                        )
                        if (id > 0) {
                            addedWords.add(word)
                        }
                    } catch (e: Exception) {
                        // Skip words that fail insertion (e.g., unique constraint)
                        android.util.Log.w("LessonViewModel", "Failed to insert word '$word': ${e.message}")
                    }
                }

                // 7. Update state with results
                if (addedWords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isWordGenerationLoading = false,
                            wordGenerationError = "Failed to add words to the word bank. Please try again."
                        )
                    }
                } else {
                    // Invalidate suggested prompts cache since word bank changed
                    cachedWordCountForPrompts = -1

                    _uiState.update {
                        it.copy(
                            isWordGenerationLoading = false,
                            generatedWords = addedWords
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("blocked", ignoreCase = true) == true ||
                    e.message?.contains("safety", ignoreCase = true) == true) {
                    "The request couldn't be processed. Please rephrase your description."
                } else {
                    "Word generation failed: ${e.message}"
                }
                _uiState.update {
                    it.copy(
                        isWordGenerationLoading = false,
                        wordGenerationError = errorMsg
                    )
                }
            }
        }
    }

    /**
     * Clear word generation state. Called when the generation modal is dismissed.
     */
    fun clearWordGenerationState() {
        _uiState.update {
            it.copy(
                isWordGenerationLoading = false,
                wordGenerationError = null,
                generatedWords = emptyList(),
                wordGenerationRequestedCount = 0
            )
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
    val isActivityCreationLoading: Boolean = false,
    val activityError: String? = null,
    val activitySuggestedPrompts: List<String> = emptyList(),
    val isActivitySuggestionsLoading: Boolean = false,
    val generatedJsonResult: String? = null,
    // Dictionary suggestion state (add modal)
    val dictionarySuggestions: List<String> = emptyList(),
    // Dictionary suggestion state (edit modal)
    val editDictionarySuggestions: List<String> = emptyList(),
    // Suggested prompts state (generation modal)
    val suggestedPrompts: List<String> = emptyList(),
    val isSuggestionsLoading: Boolean = false,
    // Word generation modal state
    val isWordGenerationLoading: Boolean = false,
    val wordGenerationError: String? = null,
    val generatedWords: List<String> = emptyList(),
    val wordGenerationRequestedCount: Int = 0
)

