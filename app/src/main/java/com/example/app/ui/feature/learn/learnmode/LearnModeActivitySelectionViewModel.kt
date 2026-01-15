package com.example.app.ui.feature.learn.learnmode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.entity.Activity
import com.example.app.data.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnModeActivitySelectionUiState(
    val activities: List<Activity> = emptyList(),
    val selectedActivity: Activity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LearnModeActivitySelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val activityRepository = ActivityRepository(database.activityDao())

    private val _uiState = MutableStateFlow(LearnModeActivitySelectionUiState())
    val uiState: StateFlow<LearnModeActivitySelectionUiState> = _uiState.asStateFlow()

    fun loadActivities(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            activityRepository.getActivitiesForUser(userId).collectLatest { activities ->
                _uiState.update {
                    it.copy(
                        activities = activities,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleActivitySelection(activity: Activity) {
        _uiState.update { state ->
            val newSelection = if (state.selectedActivity?.id == activity.id) {
                null
            } else {
                activity
            }
            state.copy(selectedActivity = newSelection)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedActivity = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
