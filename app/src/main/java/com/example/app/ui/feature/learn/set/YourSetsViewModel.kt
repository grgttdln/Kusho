package com.example.app.ui.feature.learn.set

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.entity.Set
import com.example.app.data.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for YourSetsScreen
 */
data class YourSetsUiState(
    val sets: List<Set> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedSetDetails: SetRepository.SetDetails? = null,
    val showModal: Boolean = false
)

/**
 * ViewModel for YourSetsScreen
 * Manages the fetching and deletion of sets for a specific activity
 */
class YourSetsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val setRepository = SetRepository(
        database.setDao(),
        database.setWordDao(),
        database.wordDao()
    )

    private val _uiState = MutableStateFlow(YourSetsUiState())
    val uiState: StateFlow<YourSetsUiState> = _uiState.asStateFlow()

    /**
     * Load sets for a specific user (all independent sets they created)
     *
     * @param userId The user ID to load sets for
     */
    fun loadSets(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                setRepository.getSetsForUser(userId).collectLatest { sets ->
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
     * Delete a set by its ID
     *
     * @param setId The ID of the set to delete
     */
    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            try {
                setRepository.deleteSet(setId)
                // Close modal after successful deletion
                closeModal()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to delete set"
                    )
                }
            }
        }
    }

    /**
     * Show modal with set details
     *
     * @param setId The ID of the set to show details for
     */
    fun showSetDetails(setId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val setDetails = setRepository.getSetDetails(setId)
                _uiState.update {
                    it.copy(
                        selectedSetDetails = setDetails,
                        showModal = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load set details"
                    )
                }
            }
        }
    }

    /**
     * Close the modal
     */
    fun closeModal() {
        _uiState.update {
            it.copy(
                showModal = false,
                selectedSetDetails = null
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
