package com.example.app.ui.feature.learn.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.entity.Set
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for ActivitySetsScreen
 */
data class ActivitySetsUiState(
    val sets: List<Set> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val editableTitle: String = "",
    val originalTitle: String = "",
    val titleError: String? = null,
    val isSaving: Boolean = false,
    val titleSaved: Boolean = false
)

/**
 * ViewModel for ActivitySetsScreen
 * Manages the sets linked to a specific activity.
 * Operations here only affect the activity-set links, not the original sets.
 */
class ActivitySetsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val setRepository = SetRepository(database)

    private val _uiState = MutableStateFlow(ActivitySetsUiState())
    val uiState: StateFlow<ActivitySetsUiState> = _uiState.asStateFlow()

    private var currentLoadJob: Job? = null
    private var currentActivityId: Long? = null

    /**
     * Load sets linked to a specific activity
     *
     * @param activityId The activity ID to load sets for
     */
    fun loadSetsForActivity(activityId: Long) {
        // Cancel any existing load job
        currentLoadJob?.cancel()
        currentActivityId = activityId

        currentLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                setRepository.getSetsForActivity(activityId).collect { sets ->
                    _uiState.update {
                        it.copy(
                            sets = sets,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load sets"
                    )
                }
            }
        }
    }

    /**
     * Unlink a set from the activity.
     * This only removes the link - the original set is NOT deleted.
     *
     * @param setId The ID of the set to unlink
     * @param activityId The ID of the activity
     */
    fun unlinkSetFromActivity(setId: Long, activityId: Long) {
        viewModelScope.launch {
            try {
                val success = setRepository.unlinkSetFromActivity(setId, activityId)
                if (!success) {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to remove set from activity")
                    }
                }
                // The Flow will automatically update the UI when the link is removed
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to remove set from activity")
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
}
