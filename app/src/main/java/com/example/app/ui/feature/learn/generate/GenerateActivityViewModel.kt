package com.example.app.ui.feature.learn.generate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Word
import com.example.app.data.model.AiGeneratedActivity
import com.example.app.data.model.AiGeneratedSet
import com.example.app.data.model.AiGenerationResult
import com.example.app.data.repository.GeminiRepository
import com.example.app.data.repository.GenerationPhase
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI Activity Generation flow.
 * Manages word selection, generation state, and navigation through the creation process.
 */
class GenerateActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val wordDao = database.wordDao()
    private val sessionManager = SessionManager.getInstance(application)
    private val geminiRepository = GeminiRepository()
    private val setRepository = SetRepository(database)

    private val _uiState = MutableStateFlow(GenerateActivityUiState())
    val uiState: StateFlow<GenerateActivityUiState> = _uiState.asStateFlow()

    private val _generationPhase = MutableStateFlow<GenerationPhase>(GenerationPhase.Idle)
    val generationPhase: StateFlow<GenerationPhase> = _generationPhase.asStateFlow()

    private var currentUserId: Long? = null
    private var generatedData: AiGeneratedActivity? = null
    private val createdSetIds = mutableListOf<Long>()

    init {
        viewModelScope.launch {
            sessionManager.currentUser.collectLatest { user ->
                currentUserId = user?.id
                if (user != null) {
                    loadWordsForUser(user.id)
                } else {
                    _uiState.update { it.copy(words = emptyList()) }
                }
            }
        }
    }

    private fun loadWordsForUser(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWords = true) }
            try {
                val words = wordDao.getWordsByUserIdOnce(userId)
                _uiState.update {
                    it.copy(
                        words = words,
                        isLoadingWords = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingWords = false,
                        error = "Failed to load words: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update the generation prompt.
     */
    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    /**
     * Toggle word selection for generation.
     */
    fun onWordSelectionChanged(wordId: Long, isSelected: Boolean) {
        _uiState.update { currentState ->
            val currentSelection = currentState.selectedWordIds
            val newSelection = if (isSelected) {
                currentSelection + wordId
            } else {
                currentSelection - wordId
            }
            currentState.copy(selectedWordIds = newSelection)
        }
    }

    /**
     * Select all words.
     */
    fun onSelectAllWords() {
        _uiState.update { currentState ->
            val allWordIds = currentState.words.map { it.id }.toSet()
            val newSelection = if (currentState.selectedWordIds == allWordIds) {
                emptySet()
            } else {
                allWordIds
            }
            currentState.copy(selectedWordIds = newSelection)
        }
    }

    /**
     * Check if generation can proceed.
     */
    fun canGenerate(): Boolean {
        val state = _uiState.value
        return state.prompt.isNotBlank() && state.selectedWordIds.size >= 3
    }

    /**
     * Generate activity using AI.
     */
    fun generateActivity() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(error = "Please log in to generate activities") }
            return
        }

        val state = _uiState.value
        if (state.prompt.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a description") }
            return
        }

        if (state.selectedWordIds.size < 3) {
            _uiState.update { it.copy(error = "Please select at least 3 words") }
            return
        }

        val selectedWords = state.words
            .filter { state.selectedWordIds.contains(it.id) }

        _uiState.update { it.copy(isGenerating = true, error = null) }
        _generationPhase.value = GenerationPhase.Filtering

        viewModelScope.launch {
            val result = geminiRepository.generateActivity(
                state.prompt,
                selectedWords
            ) { phase ->
                _generationPhase.value = phase
            }

            when (result) {
                is AiGenerationResult.Success -> {
                    generatedData = result.data
                    _generationPhase.value = GenerationPhase.Complete
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generationComplete = true,
                            currentSetIndex = 0,
                            error = null
                        )
                    }
                }
                is AiGenerationResult.Error -> {
                    _generationPhase.value = GenerationPhase.Idle
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Get the current set to display for editing.
     */
    fun getCurrentSet(): AiGeneratedSet? {
        val data = generatedData ?: return null
        val index = _uiState.value.currentSetIndex
        return if (index < data.sets.size) data.sets[index] else null
    }

    /**
     * Get the activity info for the final step.
     */
    fun getActivityInfo(): Pair<String, String>? {
        val data = generatedData ?: return null
        return Pair(data.activity.title, data.activity.description)
    }

    /**
     * Advance to the next set after current one is saved.
     */
    fun advanceToNextSet(createdSetId: Long) {
        createdSetIds.add(createdSetId)
        val totalSets = generatedData?.sets?.size ?: 0
        val nextIndex = _uiState.value.currentSetIndex + 1
        
        _uiState.update { it.copy(currentSetIndex = nextIndex) }
    }

    /**
     * Get all created set IDs.
     */
    fun getCreatedSetIds(): List<Long> = createdSetIds.toList()

    /**
     * Regenerate a single set with a different mix of words.
     *
     * @param currentSetTitle The title of the set being regenerated
     * @param currentSetDescription The description of the set being regenerated
     * @param onResult Callback with the result - either JSON string on success or null on error
     */
    fun regenerateSet(
        currentSetTitle: String,
        currentSetDescription: String,
        onResult: (String?) -> Unit
    ) {
        val userId = currentUserId
        if (userId == null) {
            onResult(null)
            return
        }

        val state = _uiState.value
        val selectedWords = state.words.filter { state.selectedWordIds.contains(it.id) }

        if (selectedWords.isEmpty()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            val result = geminiRepository.regenerateSet(
                prompt = state.prompt,
                availableWords = selectedWords,
                currentSetTitle = currentSetTitle,
                currentSetDescription = currentSetDescription,
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
     * Reset for a new generation.
     */
    fun reset() {
        generatedData = null
        createdSetIds.clear()
        _generationPhase.value = GenerationPhase.Idle
        _uiState.update {
            GenerateActivityUiState(
                words = it.words // Keep the loaded words
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for the Generate Activity flow.
 */
data class GenerateActivityUiState(
    val words: List<Word> = emptyList(),
    val isLoadingWords: Boolean = false,
    val prompt: String = "",
    val selectedWordIds: Set<Long> = emptySet(),
    val isGenerating: Boolean = false,
    val generationComplete: Boolean = false,
    val currentSetIndex: Int = 0,
    val error: String? = null
)

