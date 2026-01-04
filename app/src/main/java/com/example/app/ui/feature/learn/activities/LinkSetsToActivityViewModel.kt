package com.example.app.ui.feature.learn.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.entity.Set
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for LinkSetsToActivityScreen
 */
data class LinkSetsToActivityUiState(
    val availableSets: List<Set> = emptyList(),
    val alreadyLinkedSetIds: kotlin.collections.Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for LinkSetsToActivityScreen
 * Manages loading user's sets and linking them to an activity.
 */
class LinkSetsToActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val setRepository = SetRepository(
        database.setDao(),
        database.setWordDao(),
        database.wordDao()
    )

    private val _uiState = MutableStateFlow(LinkSetsToActivityUiState())
    val uiState: StateFlow<LinkSetsToActivityUiState> = _uiState.asStateFlow()

    /**
     * Load all user's sets and determine which are already linked to the activity
     */
    fun loadData(userId: Long, activityId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Get all user's sets
                val allSets = setRepository.getSetsForUser(userId).first()
                
                // Get sets already linked to this activity
                val linkedSets = setRepository.getSetsForActivity(activityId).first()
                val linkedSetIds = linkedSets.map { it.id }.toSet()

                _uiState.update {
                    it.copy(
                        availableSets = allSets,
                        alreadyLinkedSetIds = linkedSetIds,
                        isLoading = false
                    )
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
     * Link multiple sets to an activity
     */
    fun linkSetsToActivity(setIds: List<Long>, activityId: Long) {
        viewModelScope.launch {
            try {
                setIds.forEach { setId ->
                    setRepository.linkSetToActivity(setId, activityId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to link sets")
                }
            }
        }
    }
}
