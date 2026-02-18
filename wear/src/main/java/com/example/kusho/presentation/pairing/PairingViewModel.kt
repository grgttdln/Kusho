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
    private val _pairingAccepted = MutableStateFlow(false)
    private val _pairingDeclined = MutableStateFlow(false)
    
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        when (messageEvent.path) {
            MESSAGE_PATH_PONG -> {
                Log.d(TAG, "🏓 PONG RECEIVED from mobile app! Source: ${messageEvent.sourceNodeId}")
                _receivedPong.value = true
            }
            MESSAGE_PATH_PAIRING_ACCEPTED -> {
                Log.d(TAG, "✅ Pairing ACCEPTED by phone! Source: ${messageEvent.sourceNodeId}")
                _pairingAccepted.value = true
            }
            MESSAGE_PATH_PAIRING_DECLINED -> {
                Log.d(TAG, "❌ Pairing DECLINED by phone. Source: ${messageEvent.sourceNodeId}")
                _pairingDeclined.value = true
            }
        }
    }
    
    companion object {
        private const val TAG = "PairingViewModel"
        private const val MESSAGE_PATH_PAIRING_REQUEST = "/pairing_request"
        private const val MESSAGE_PATH_PAIRING_ACCEPTED = "/pairing_accepted"
        private const val MESSAGE_PATH_PAIRING_DECLINED = "/pairing_declined"
        private const val MESSAGE_PATH_PING = "/kusho/ping"
        private const val MESSAGE_PATH_PONG = "/kusho/pong"
        private const val PING_TIMEOUT_MS = 3000L // Wait 3 seconds for pong response
        private const val ACCEPTANCE_TIMEOUT_MS = 30000L // Wait 30 seconds for phone user to accept
        private const val SUCCESS_DISPLAY_DURATION_MS = 3000L // Show success for 3 seconds
        private const val MAX_RETRY_ATTEMPTS = 2 // Max attempts before showing skip option
    }
    
    private var retryAttempts = 0
    
    init {
        Log.d(TAG, "🎬 PairingViewModel initialized")
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
     * User-initiated connection attempt.
     * Called when "Connect to Phone" button is tapped on the watch.
     * Flow: Bluetooth check → Node discovery → PING/PONG → Send pairing request → Wait for acceptance
     */
    fun connectToPhone() {
        viewModelScope.launch {
            _pairingState.value = PairingState.Checking
            
            try {
                // Step 1: Check Bluetooth
                if (!isBluetoothEnabled()) {
                    _pairingState.value = PairingState.BluetoothOff
                    return@launch
                }
                
                // Step 2: Find nearby phone nodes
                val connectedNodes = nodeClient.connectedNodes.await()
                val nearbyNodes = connectedNodes.filter { it.isNearby }
                Log.d(TAG, "📊 Connected nodes: ${connectedNodes.size}, Nearby: ${nearbyNodes.size}")
                
                if (nearbyNodes.isEmpty()) {
                    retryAttempts++
                    if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                        _pairingState.value = PairingState.MaxRetriesReached(retryAttempts)
                    } else {
                        _pairingState.value = PairingState.Error("No phone found nearby")
                    }
                    return@launch
                }
                
                val phoneNode = nearbyNodes.first()
                Log.d(TAG, "✅ Phone found: ${phoneNode.displayName} (${phoneNode.id})")
                
                // Step 3: Verify Kusho app is running via PING/PONG
                _receivedPong.value = false
                messageClient.sendMessage(phoneNode.id, MESSAGE_PATH_PING, "ping".toByteArray()).await()
                
                val pingStart = System.currentTimeMillis()
                while (!_receivedPong.value && System.currentTimeMillis() - pingStart < PING_TIMEOUT_MS) {
                    delay(100)
                }
                
                if (!_receivedPong.value) {
                    retryAttempts++
                    if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                        _pairingState.value = PairingState.MaxRetriesReached(retryAttempts)
                    } else {
                        _pairingState.value = PairingState.Error("Open Kusho app on phone")
                    }
                    return@launch
                }
                
                Log.d(TAG, "✅ Kusho app responding on phone")
                
                // Step 4: Send pairing request to phone
                Log.d(TAG, "📤 Sending pairing request to ${phoneNode.displayName} (${phoneNode.id})")
                _pairingAccepted.value = false
                _pairingDeclined.value = false
                
                val requestPayload = org.json.JSONObject().apply {
                    put("watchName", android.os.Build.MODEL ?: "Smartwatch")
                }.toString()
                
                messageClient.sendMessage(
                    phoneNode.id,
                    MESSAGE_PATH_PAIRING_REQUEST,
                    requestPayload.toByteArray()
                ).await()
                
                _pairingState.value = PairingState.WaitingForAcceptance
                Log.d(TAG, "⏳ Waiting for phone user to accept/decline...")
                
                // Step 5: Wait for acceptance, decline, or timeout
                val requestStart = System.currentTimeMillis()
                while (!_pairingAccepted.value && !_pairingDeclined.value &&
                       System.currentTimeMillis() - requestStart < ACCEPTANCE_TIMEOUT_MS) {
                    delay(200)
                }
                
                when {
                    _pairingAccepted.value -> {
                        Log.d(TAG, "✅ Pairing accepted! Saving paired status.")
                        retryAttempts = 0
                        _pairingState.value = PairingState.Success
                        savePairedStatus(phoneNode.id)
                        delay(SUCCESS_DISPLAY_DURATION_MS)
                    }
                    _pairingDeclined.value -> {
                        Log.d(TAG, "❌ Pairing declined by phone user.")
                        _pairingState.value = PairingState.Declined
                    }
                    else -> {
                        Log.w(TAG, "⏱️ Pairing request timed out after ${ACCEPTANCE_TIMEOUT_MS}ms")
                        retryAttempts++
                        _pairingState.value = PairingState.Error("No response from phone")
                    }
                }
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: com.google.android.gms.common.api.ApiException) {
                when (e.statusCode) {
                    4000 -> {
                        retryAttempts++
                        if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                            _pairingState.value = PairingState.MaxRetriesReached(retryAttempts)
                        } else {
                            _pairingState.value = PairingState.Error("Phone disconnected")
                        }
                    }
                    else -> {
                        _pairingState.value = PairingState.Error("Connection error: ${e.statusCode}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error during pairing", e)
                _pairingState.value = PairingState.Error("Failed to connect: ${e.message}")
            }
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
    private fun savePairedStatus(phoneNodeId: String) {
        val prefs = context.getSharedPreferences("kusho_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_paired", true)
            .putBoolean("is_skipped", false)
            .putString("paired_phone_node_id", phoneNodeId)
            .apply()
        Log.d(TAG, "✅ Paired status saved with phone node ID: $phoneNodeId")
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
     * Retry connection - called from error or declined states
     */
    fun retry() {
        Log.d(TAG, "🔄 Retry requested by user")
        retryAttempts = 0
        connectToPhone()
    }
    
    /**
     * Restart pairing - used when user tries again after max retries
     */
    fun restartMonitoring() {
        Log.d(TAG, "🔄 Restarting pairing - resetting retry counter")
        retryAttempts = 0
        connectToPhone()
    }
    
    override fun onCleared() {
        super.onCleared()
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
