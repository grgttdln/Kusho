package com.example.kusho.presentation.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Global connection monitor that continuously checks if mobile app is connected
 * Provides a shared state that can be observed throughout the app
 */
class ConnectionMonitor private constructor(private val context: Context) {
    
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isConnected = MutableStateFlow(true) // Start optimistic
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _receivedPong = MutableStateFlow(false)
    
    private var monitoringJob: Job? = null
    
    companion object {
        @Volatile
        private var INSTANCE: ConnectionMonitor? = null
        private const val TAG = "ConnectionMonitor"
        private const val MESSAGE_PATH_PING = "/kusho/ping"
        private const val MESSAGE_PATH_PONG = "/kusho/pong"
        private const val PING_TIMEOUT_MS = 3000L
        private const val CHECK_INTERVAL_MS = 5000L // Check every 5 seconds
        
        fun getInstance(context: Context): ConnectionMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionMonitor(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == MESSAGE_PATH_PONG) {
            Log.d(TAG, "🏓 Connection verified - PONG received")
            _receivedPong.value = true
        }
    }
    
    init {
        messageClient.addListener(messageListener)
        Log.d(TAG, "🔍 ConnectionMonitor initialized")
    }
    
    /**
     * Start monitoring connection continuously
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) {
            Log.d(TAG, "⚠️ Monitoring already active")
            return
        }
        
        Log.d(TAG, "🚀 Starting continuous connection monitoring")
        monitoringJob = scope.launch {
            while (isActive) {
                checkConnection()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop monitoring connection
     */
    fun stopMonitoring() {
        Log.d(TAG, "🛑 Stopping connection monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Check connection by sending ping and waiting for pong
     */
    private suspend fun checkConnection() {
        try {
            // Get connected nodes
            val connectedNodes = nodeClient.connectedNodes.await()
            
            if (connectedNodes.isEmpty()) {
                Log.w(TAG, "❌ No phone connected")
                updateConnectionState(false)
                return
            }
            
            // Phone is connected, verify Kusho app is running
            val phoneNode = connectedNodes.first()
            _receivedPong.value = false
            
            messageClient.sendMessage(phoneNode.id, MESSAGE_PATH_PING, "ping".toByteArray()).await()
            
            // Wait for pong with timeout
            val startTime = System.currentTimeMillis()
            while (!_receivedPong.value && System.currentTimeMillis() - startTime < PING_TIMEOUT_MS) {
                delay(100)
            }
            
            if (_receivedPong.value) {
                Log.d(TAG, "✅ Mobile app connected and responding")
                updateConnectionState(true)
            } else {
                Log.w(TAG, "❌ Mobile app not responding (timeout)")
                updateConnectionState(false)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error checking connection", e)
            updateConnectionState(false)
        }
    }
    
    /**
     * Update connection state and log changes
     */
    private fun updateConnectionState(connected: Boolean) {
        val previousState = _isConnected.value
        _isConnected.value = connected
        
        if (previousState != connected) {
            if (connected) {
                Log.d(TAG, "🟢 CONNECTION RESTORED")
            } else {
                Log.w(TAG, "🔴 CONNECTION LOST")
            }
        }
    }
    
    /**
     * Clear pairing status from SharedPreferences
     */
    fun clearPairingStatus() {
        Log.d(TAG, "🗑️ Clearing pairing status")
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_paired", false).apply()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up ConnectionMonitor")
        stopMonitoring()
        messageClient.removeListener(messageListener)
    }
}
