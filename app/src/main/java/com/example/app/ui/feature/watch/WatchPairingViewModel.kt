package com.example.app.ui.feature.watch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.service.WatchConnectionManager
import com.example.app.service.WatchDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchPairingUiState(
    val connectedDevices: List<WatchDeviceInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasCheckedConnection: Boolean = false
)

/**
 * ViewModel for Watch Pairing Screen
 * Manages watch connection detection and device list
 */
class WatchPairingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val watchConnectionManager = WatchConnectionManager.getInstance(application)
    
    private val _uiState = MutableStateFlow(WatchPairingUiState())
    val uiState: StateFlow<WatchPairingUiState> = _uiState.asStateFlow()
    
    init {
        // Collect watch connection updates
        viewModelScope.launch {
            watchConnectionManager.deviceInfo.collect { deviceInfo ->
                val devices = if (deviceInfo.isConnected) {
                    listOf(deviceInfo)
                } else {
                    emptyList()
                }
                
                _uiState.value = _uiState.value.copy(
                    connectedDevices = devices,
                    hasCheckedConnection = true
                )
            }
        }
        
        // Start monitoring and check initial connection
        checkWatchConnection()
    }
    
    /**
     * Check for connected watches
     */
    fun checkWatchConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                watchConnectionManager.startMonitoring()
                val isConnected = watchConnectionManager.checkConnection()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasCheckedConnection = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to check watch connection: ${e.message}",
                    hasCheckedConnection = true
                )
            }
        }
    }
    
    /**
     * Check if any watch is currently connected
     */
    fun hasConnectedWatch(): Boolean {
        return _uiState.value.connectedDevices.any { it.isConnected }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't cleanup singleton - it's shared across screens
        watchConnectionManager.stopMonitoring()
    }
}
