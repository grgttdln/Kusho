package com.example.app.ui.feature.learn.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.entity.Activity
import com.example.app.data.entity.Set
import com.example.app.data.repository.ActivityRepository
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the creation of activities.
 * Handles activity data collection and database persistence.
 */
data class ChapterInfo(
    val title: String,
    val itemCount: Int
)

data class AddActivityUiState(
    val activityTitle: String = "",
    val activityDescription: String = "",
    val selectedChapters: List<ChapterInfo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val availableSets: List<Set> = emptyList()
)

class AddActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val activityRepository = ActivityRepository(database.activityDao())
    private val setRepository = SetRepository(
        database.setDao(),
        database.setWordDao(),
        database.wordDao()
    )

    private val _uiState = MutableStateFlow(AddActivityUiState())
    val uiState: StateFlow<AddActivityUiState> = _uiState.asStateFlow()

    /**
     * Load all sets from the database
     */
    fun loadAllSets() {
        viewModelScope.launch {
            try {
                setRepository.getAllSets().collect { sets ->
                    _uiState.update { it.copy(availableSets = sets) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to load sets")
                }
            }
        }
    }

    /**
     * Update the activity title
     */
    fun setActivityTitle(title: String) {
        _uiState.update { it.copy(activityTitle = title) }
    }

    /**
     * Update the activity description
     */
    fun setActivityDescription(description: String) {
        _uiState.update { it.copy(activityDescription = description) }
    }

    /**
     * Add a chapter to the activity
     */
    fun addChapter(chapterTitle: String, itemCount: Int = 0) {
        if (chapterTitle.isNotBlank()) {
            _uiState.update {
                it.copy(
                    selectedChapters = it.selectedChapters + ChapterInfo(
                        title = chapterTitle.trim(),
                        itemCount = itemCount
                    )
                )
            }
        }
    }

    /**
     * Remove a chapter from the activity
     */
    fun removeChapter(index: Int) {
        val currentChapters = _uiState.value.selectedChapters
        if (index in currentChapters.indices) {
            _uiState.update {
                it.copy(
                    selectedChapters = currentChapters.filterIndexed { i, _ -> i != index }
                )
            }
        }
    }

    /**
     * Clear all chapters
     */
    fun clearChapters() {
        _uiState.update { it.copy(selectedChapters = emptyList()) }
    }

    /**
     * Create and save the activity to the database
     * Returns true if successful, false otherwise
     */
    fun createActivity(userId: Long): Boolean {
        val state = _uiState.value

        // Validation
        if (state.activityTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Activity title is required") }
            return false
        }

        if (state.selectedChapters.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one chapter") }
            return false
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = activityRepository.addActivity(
                userId = userId,
                title = state.activityTitle.trim(),
                description = if (state.activityDescription.isNotBlank()) {
                    state.activityDescription.trim()
                } else {
                    null
                }
            )

            result.onSuccess { activityId ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Activity '${state.activityTitle}' created successfully",
                        activityTitle = "",
                        activityDescription = "",
                        selectedChapters = emptyList()
                    )
                }
            }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create activity"
                        )
                    }
                }
        }

        return true
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
        _uiState.value = AddActivityUiState()
    }
}
