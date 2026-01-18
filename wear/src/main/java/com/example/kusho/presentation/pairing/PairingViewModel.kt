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
    }
    
    private var isMonitoring = false
    
    init {
        Log.d(TAG, "🎬 PairingViewModel initialized")
        Log.d(TAG, "📱 Watch will ping connected phones to verify Kusho app is running")
        messageClient.addListener(messageListener)
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
     * Check if phone is connected
     */
    private suspend fun checkConnection() {
        try {
            Log.d(TAG, "🔍 Checking connection...")
            
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
            
            // Check for connected phone nodes
            Log.d(TAG, "📡 Querying connected nodes...")
            val connectedNodes = nodeClient.connectedNodes.await()
            
            Log.d(TAG, "📊 Connection query result:")
            Log.d(TAG, "  - Connected nodes: ${connectedNodes.size}")
            connectedNodes.forEachIndexed { index, node ->
                Log.d(TAG, "  - Node $index: ${node.displayName} (ID: ${node.id}, nearby: ${node.isNearby})")
            }
            
            if (connectedNodes.isEmpty()) {
                // No phone connected
                Log.w(TAG, "❌ No connected nodes found")
                if (_pairingState.value is PairingState.Checking) {
                    _pairingState.value = PairingState.Prompt
                    Log.d(TAG, "↩️ State changed back to Prompt")
                }
                return
            }
            
            // Phone found, verify Kusho app is running by sending ping
            val phoneNode = connectedNodes.first()
            Log.d(TAG, "🏓 Sending PING to ${phoneNode.displayName} (${phoneNode.id})...")
            Log.d(TAG, "📱 PHONE CONNECTED VIA BLUETOOTH: YES")
            Log.d(TAG, "🔍 Verifying Kusho mobile app is running...")
            
            _receivedPong.value = false
            messageClient.sendMessage(phoneNode.id, MESSAGE_PATH_PING, "ping".toByteArray()).await()
            Log.d(TAG, "✅ PING message sent successfully")
            
            // Wait for PONG response with timeout
            Log.d(TAG, "⏱️ Waiting for PONG response (timeout: ${PING_TIMEOUT_MS}ms)...")
            val startTime = System.currentTimeMillis()
            var elapsedTime = 0L
            while (!_receivedPong.value && System.currentTimeMillis() - startTime < PING_TIMEOUT_MS) {
                delay(100)
                elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime % 1000 == 0L) {
                    Log.d(TAG, "⏳ Still waiting... ${elapsedTime / 1000}s elapsed")
                }
            }
            
            if (_receivedPong.value) {
                // Kusho app is running on phone
                Log.d(TAG, "✅ ============================================")
                Log.d(TAG, "✅ KUSHO MOBILE APP: CONNECTED")
                Log.d(TAG, "✅ Response time: ${elapsedTime}ms")
                Log.d(TAG, "✅ ============================================")
                handleSuccessfulConnection(phoneNode)
            } else {
                // No response - Kusho app not running
                Log.w(TAG, "❌ ============================================")
                Log.w(TAG, "❌ KUSHO MOBILE APP: NOT CONNECTED")
                Log.w(TAG, "❌ Timeout after ${PING_TIMEOUT_MS}ms")
                Log.w(TAG, "❌ Phone is paired but app not running/responding")
                Log.w(TAG, "❌ ============================================")
                if (_pairingState.value is PairingState.Checking) {
                    _pairingState.value = PairingState.Prompt
                    Log.d(TAG, "↩️ State changed back to Prompt - Please open Kusho app on phone")
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
        prefs.edit().putBoolean("is_paired", true).apply()
        Log.d(TAG, "Paired status saved")
    }
    
    /**
     * Check if watch was previously paired
     */
    fun isPreviouslyPaired(): Boolean {
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_paired", false)
    }
    
    /**
     * Retry connection check
     */
    fun retry() {
        Log.d(TAG, "🔄 Retry requested by user")
        viewModelScope.launch {
            _pairingState.value = PairingState.Checking
            checkConnection()
        }
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
