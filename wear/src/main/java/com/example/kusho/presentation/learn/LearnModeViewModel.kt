package com.example.kusho.presentation.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for Learn Mode.
 */
class LearnModeViewModel : ViewModel() {

    data class UiState(
        val title: String = "Learn Mode"
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
}

class LearnModeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LearnModeViewModel::class.java)) {
            return LearnModeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

