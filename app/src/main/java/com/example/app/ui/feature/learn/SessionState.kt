package com.example.app.ui.feature.learn

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object SessionState {
    private var _selectedSets by mutableStateOf<List<String>>(emptyList())

    val selectedSets: List<String>
        get() = _selectedSets

    fun updateSelectedSets(sets: List<String>) {
        _selectedSets = sets
    }
}
