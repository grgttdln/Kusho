package com.example.app.ui.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.SessionManager
import com.example.app.service.WatchConnectionManager
import com.example.app.service.WatchDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "Guest",
    val watchDevice: WatchDeviceInfo = WatchDeviceInfo(),
    val isLoading: Boolean = false,
    val totalStudents: Int = 17,
    val totalClassrooms: Int = 2
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val watchConnectionManager = WatchConnectionManager(application)
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        // Collect watch device info updates
        viewModelScope.launch {
            watchConnectionManager.deviceInfo.collect { deviceInfo ->
                _uiState.value = _uiState.value.copy(watchDevice = deviceInfo)
            }
        }
        
        // Collect current user updates
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    userName = user?.name ?: "Guest"
                )
            }
        }

        // Start monitoring watch connection
        watchConnectionManager.startMonitoring()
        
        // Initial connection check
        checkWatchConnection()
    }
    
    /**
     * Check watch connection manually
     */
    fun checkWatchConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            watchConnectionManager.checkConnection()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Request device info from watch
     */
    fun requestDeviceInfo() {
        watchConnectionManager.requestDeviceInfo()
    }
    
    /**
     * Get the greeting based on time of day
     */
    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val firstName = _uiState.value.userName.split(" ").firstOrNull() ?: "there"
        
        return when (hour) {
            in 0..11 -> "Good Morning, $firstName!"
            in 12..17 -> "Good Afternoon, $firstName!"
            else -> "Good Evening, $firstName!"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        watchConnectionManager.cleanup()
    }
}
