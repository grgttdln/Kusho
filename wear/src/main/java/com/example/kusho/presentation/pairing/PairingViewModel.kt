package com.example.kusho.presentation.pairing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for managing watch-phone pairing state
 */
class PairingViewModel(private val context: Context) : ViewModel() {
    
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Prompt)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()
    
    private val _receivedPong = MutableStateFlow(false)
    
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == MESSAGE_PATH_PONG) {
            Log.d(TAG, "🏓 ============================================")
            Log.d(TAG, "🏓 PONG RECEIVED from mobile app!")
            Log.d(TAG, "🏓 Source: ${messageEvent.sourceNodeId}")
            Log.d(TAG, "🏓 Mobile app is running and responding")
            Log.d(TAG, "🏓 ============================================")
            _receivedPong.value = true
        }
    }
    
    companion object {
        private const val TAG = "PairingViewModel"
        private const val MESSAGE_PATH_PAIRING_HANDSHAKE = "/pairing_handshake"
        private const val MESSAGE_PATH_PING = "/kusho/ping"
        private const val MESSAGE_PATH_PONG = "/kusho/pong"
        private const val POLLING_INTERVAL_MS = 2000L // Check every 2 seconds
        private const val SUCCESS_DISPLAY_DURATION_MS = 3000L // Show success for 3 seconds
        private const val PING_TIMEOUT_MS = 3000L // Wait 3 seconds for pong response
        private const val MAX_RETRY_ATTEMPTS = 2 // Max attempts before showing skip option
    }
    
    private var isMonitoring = false
    private var retryAttempts = 0
    
    init {
        Log.d(TAG, "🎬 PairingViewModel initialized")
        Log.d(TAG, "📱 Watch will ping connected phones to verify Kusho app is running")
        messageClient.addListener(messageListener)
        
        // Check if we should show MaxRetriesReached immediately (from connection loss)
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        val showMaxRetries = prefs.getBoolean("show_max_retries", false)
        if (showMaxRetries) {
            Log.d(TAG, "⚠️ Showing MaxRetriesReached state from previous connection loss")
            _pairingState.value = PairingState.MaxRetriesReached(2)
            retryAttempts = 2
            // Clear the flag
            prefs.edit().putBoolean("show_max_retries", false).apply()
        }
    }
    
    /**
     * Start monitoring for phone connection
     */
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        Log.d(TAG, "🚀 Starting connection monitoring...")
        Log.d(TAG, "📋 Monitoring config:")
        Log.d(TAG, "  - Polling interval: ${POLLING_INTERVAL_MS}ms")
        Log.d(TAG, "  - Strategy: Check for ANY connected phone nodes")
        
        viewModelScope.launch {
            while (isMonitoring) {
                checkConnection()
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop monitoring for phone connection
     */
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    /**
     * Check if phone is connected with two-layer detection
     */
    private suspend fun checkConnection() {
        try {
            Log.d(TAG, "🔍 Starting connection check...")
            
            // Check if Bluetooth is enabled
            if (!isBluetoothEnabled()) {
                Log.w(TAG, "❌ Bluetooth is disabled")
                _pairingState.value = PairingState.BluetoothOff
                return
            }
            
            Log.d(TAG, "✅ Bluetooth is enabled")
            
            // If currently showing prompt or error, set to checking
            if (_pairingState.value is PairingState.Prompt || 
                _pairingState.value is PairingState.Error ||
                _pairingState.value is PairingState.BluetoothOff) {
                _pairingState.value = PairingState.Checking
                Log.d(TAG, "⏳ State changed to Checking")
            }
            
            // LAYER 1: Check physical Bluetooth connection
            Log.d(TAG, "🔍 Layer 1: Checking Bluetooth connection...")
            val connectedNodes = nodeClient.connectedNodes.await()
            
            Log.d(TAG, "📊 Connection query result:")
            Log.d(TAG, "  - Connected nodes: ${connectedNodes.size}")
            connectedNodes.forEachIndexed { index, node ->
                Log.d(TAG, "  - Node $index: ${node.displayName} (ID: ${node.id}, nearby: ${node.isNearby})")
            }
            
            // Filter for nearby (actively connected) nodes only
            val nearbyNodes = connectedNodes.filter { it.isNearby }
            Log.d(TAG, "  - Nearby (active) nodes: ${nearbyNodes.size}")
            
            if (nearbyNodes.isEmpty()) {
                // No phone connected via Bluetooth
                Log.w(TAG, "❌ LAYER 1 FAILED: No phone connected via Bluetooth")
                retryAttempts++
                
                if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                    Log.w(TAG, "⚠️ Max retry attempts reached - showing skip option")
                    _pairingState.value = PairingState.MaxRetriesReached(retryAttempts)
                    stopMonitoring()
                } else if (_pairingState.value is PairingState.Checking) {
                    _pairingState.value = PairingState.Prompt
                    Log.d(TAG, "↩️ State changed back to Prompt - No Bluetooth connection")
                }
                return
            }
            
            // LAYER 2: Phone found via Bluetooth, verify Kusho app is running
            val phoneNode = nearbyNodes.first()
            Log.d(TAG, "✅ Layer 1 PASSED: Phone connected via Bluetooth (${phoneNode.displayName})")
            Log.d(TAG, "🔍 Layer 2: Verifying Kusho mobile app is running...")
            
            _receivedPong.value = false
            messageClient.sendMessage(phoneNode.id, MESSAGE_PATH_PING, "ping".toByteArray()).await()
            Log.d(TAG, "📤 PING sent to ${phoneNode.displayName}")
            
            // Wait for PONG response with timeout
            Log.d(TAG, "⏱️ Waiting for PONG response (timeout: ${PING_TIMEOUT_MS}ms)...")
            val startTime = System.currentTimeMillis()
            var elapsedTime = 0L
            while (!_receivedPong.value && System.currentTimeMillis() - startTime < PING_TIMEOUT_MS) {
                delay(100)
                elapsedTime = System.currentTimeMillis() - startTime
            }
            
            if (_receivedPong.value) {
                // Kusho app is running on phone
                Log.d(TAG, "✅ ============================================")
                Log.d(TAG, "✅ LAYER 2 PASSED: Kusho app responding")
                Log.d(TAG, "✅ Response time: ${elapsedTime}ms")
                Log.d(TAG, "✅ 🟢 BOTH LAYERS PASSED: Fully connected")
                Log.d(TAG, "✅ ============================================")
                retryAttempts = 0 // Reset retry counter on success
                handleSuccessfulConnection(phoneNode)
            } else {
                // No response - Kusho app not running
                retryAttempts++
                Log.w(TAG, "❌ ============================================")
                Log.w(TAG, "❌ LAYER 2 FAILED: Kusho app not responding")
                Log.w(TAG, "❌ Timeout after ${PING_TIMEOUT_MS}ms")
                Log.w(TAG, "❌ 📱 Bluetooth: ✅ Connected | Kusho App: ❌ Not running")
                Log.w(TAG, "❌ Retry attempt: $retryAttempts / $MAX_RETRY_ATTEMPTS")
                Log.w(TAG, "❌ ============================================")
                
                if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                    // Max retries reached, show skip option
                    Log.w(TAG, "⚠️ Max retry attempts reached - showing skip option")
                    _pairingState.value = PairingState.MaxRetriesReached(retryAttempts)
                    stopMonitoring()
                } else if (_pairingState.value is PairingState.Checking) {
                    _pairingState.value = PairingState.Prompt
                    Log.d(TAG, "↩️ State changed back to Prompt - Please open Kusho app on phone")
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "⚠️ Connection check cancelled (monitoring stopped)")
            // Don't update state on cancellation - this is expected when stopping monitoring
            throw e  // Re-throw to properly cancel the coroutine
        } catch (e: com.google.android.gms.common.api.ApiException) {
            // Handle TARGET_NODE_NOT_CONNECTED (4000) - node disconnected while sending message
            when (e.statusCode) {
                4000 -> {
                    Log.w(TAG, "❌ TARGET_NODE_NOT_CONNECTED: Phone disconnected during message send")
                    retryAttempts++
                    if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                        Log.w(TAG, "⚠️ Max retry attempts reached - showing skip option")
                        _pairingState.value = PairingState.MaxRetriesReached(retryAttempts)
                        stopMonitoring()
                    } else if (_pairingState.value is PairingState.Checking) {
                        _pairingState.value = PairingState.Prompt
                        Log.d(TAG, "↩️ State changed back to Prompt - Phone disconnected")
                    }
                }
                else -> {
                    Log.e(TAG, "💥 ApiException during pairing: ${e.statusCode}", e)
                    _pairingState.value = PairingState.Error("Connection error: ${e.statusCode}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error checking connection", e)
            _pairingState.value = PairingState.Error("Failed to connect: ${e.message}")
        }
    }
    
    /**
     * Handle successful connection to phone
     */
    private suspend fun handleSuccessfulConnection(phoneNode: Node) {
        try {
            Log.d(TAG, "🤝 Handling successful connection...")
            Log.d(TAG, "📨 Sending handshake to: ${phoneNode.displayName} (${phoneNode.id})")
            
            messageClient.sendMessage(
                phoneNode.id,
                MESSAGE_PATH_PAIRING_HANDSHAKE,
                "watch_paired".toByteArray()
            ).await()
            
            Log.d(TAG, "✅ Pairing handshake sent successfully!")
            
            // Show success state
            _pairingState.value = PairingState.Success
            Log.d(TAG, "🎉 State changed to Success")
            
            // Save paired status
            savePairedStatus()
            
            // Keep success state for a few seconds before allowing navigation
            Log.d(TAG, "⏱️ Displaying success for ${SUCCESS_DISPLAY_DURATION_MS}ms")
            delay(SUCCESS_DISPLAY_DURATION_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error during handshake", e)
            _pairingState.value = PairingState.Error("Pairing failed: ${e.message}")
        }
    }
    
    /**
     * Check if Bluetooth is enabled
     */
    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Save paired status to SharedPreferences
     */
    private fun savePairedStatus() {
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_paired", true)
            .putBoolean("is_skipped", false)
            .apply()
        Log.d(TAG, "✅ Paired status saved")
    }
    
    /**
     * Save skipped status to SharedPreferences
     */
    fun skipPairing() {
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_paired", false)
            .putBoolean("is_skipped", true)
            .apply()
        Log.d(TAG, "⏭️ Pairing skipped - only Practice mode will be available")
    }
    
    /**
     * Check if watch was previously paired
     */
    fun isPreviouslyPaired(): Boolean {
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_paired", false)
    }
    
    /**
     * Retry connection check - reset retry counter and start fresh
     */
    fun retry() {
        Log.d(TAG, "🔄 Retry requested by user - resetting counter")
        retryAttempts = 0
        viewModelScope.launch {
            _pairingState.value = PairingState.Checking
            checkConnection()
        }
    }
    
    /**
     * Restart monitoring - used when user tries again after max retries
     */
    fun restartMonitoring() {
        Log.d(TAG, "🔄 Restarting monitoring - resetting retry counter")
        retryAttempts = 0
        _pairingState.value = PairingState.Checking
        startMonitoring()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
        messageClient.removeListener(messageListener)
    }
}

/**
 * Factory for creating PairingViewModel
 */
class PairingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PairingViewModel::class.java)) {
            return PairingViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
