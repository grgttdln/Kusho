package com.example.app.ui.feature.learn

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object SessionState {
    var selectedSets by mutableStateOf<List<String>>(emptyList())
}
