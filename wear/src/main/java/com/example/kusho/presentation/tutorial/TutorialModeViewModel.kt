package com.example.kusho.presentation.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for Tutorial Mode.
 */
class TutorialModeViewModel : ViewModel() {

    data class UiState(
        val title: String = "Tutorial Mode"
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
}

class TutorialModeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TutorialModeViewModel::class.java)) {
            return TutorialModeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

