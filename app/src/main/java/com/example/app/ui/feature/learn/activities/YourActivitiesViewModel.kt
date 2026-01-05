package com.example.app.ui.feature.learn.activities

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

data class YourActivitiesUiState(
    val activities: List<Activity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class YourActivitiesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val activityRepository = ActivityRepository(database.activityDao())

    private val _uiState = MutableStateFlow(YourActivitiesUiState())
    val uiState: StateFlow<YourActivitiesUiState> = _uiState.asStateFlow()

    /**
     * Load activities for a specific user
     */
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

    /**
     * Delete an activity
     */
    fun deleteActivity(activityId: Long, userId: Long) {
        viewModelScope.launch {
            activityRepository.deleteActivity(activityId)
                .onSuccess {
                    // Reload activities after deletion
                    loadActivities(userId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to delete activity")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
