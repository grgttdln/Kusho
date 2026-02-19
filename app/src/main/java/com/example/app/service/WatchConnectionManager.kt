package com.example.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

enum class ConnectionState {
    BLUETOOTH_OFF,           // Bluetooth is disabled
    NO_WATCH,               // Bluetooth on, but no watch paired at all
    WATCH_PAIRED_NO_APP,    // Watch paired, but Kusho app not installed/running
    WATCH_NEEDS_HANDSHAKE,  // Kusho app found but pairing handshake not completed
    WATCH_CONNECTED         // Watch paired AND Kusho app running
}

data class WatchDeviceInfo(
    val name: String = "Unknown Watch",
    val model: String = "",
    val nodeId: String = "",
    val isConnected: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.BLUETOOTH_OFF,
    val batteryPercentage: Int? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Represents an incoming pairing request from a watch
 */
data class PairingRequestEvent(
    val nodeId: String = "",
    val watchName: String = "Smartwatch",
    val timestamp: Long = 0L
)

class WatchConnectionManager private constructor(private val context: Context) {
    
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _deviceInfo = MutableStateFlow(WatchDeviceInfo())
    val deviceInfo: StateFlow<WatchDeviceInfo> = _deviceInfo.asStateFlow()
    
    // Flow for Learn Mode skip commands from watch
    private val _learnModeSkipTrigger = MutableStateFlow(0L) // Timestamp of skip event
    val learnModeSkipTrigger: StateFlow<Long> = _learnModeSkipTrigger.asStateFlow()

    // Flow for letter input events from watch (for Write the Word mode)
    data class LetterInputEvent(
        val letter: Char = ' ',
        val letterIndex: Int = 0,
        val timestamp: Long = 0L
    )
    private val _letterInputEvent = MutableStateFlow(LetterInputEvent())
    val letterInputEvent: StateFlow<LetterInputEvent> = _letterInputEvent.asStateFlow()

    // Learn Mode feedback dismissed from watch trigger
    private val _learnModeFeedbackDismissed = MutableStateFlow(0L)
    val learnModeFeedbackDismissed: StateFlow<Long> = _learnModeFeedbackDismissed.asStateFlow()

    // Tutorial Mode flows
    private val _tutorialModeSkipTrigger = MutableStateFlow(0L)
    val tutorialModeSkipTrigger: StateFlow<Long> = _tutorialModeSkipTrigger.asStateFlow()

    // Gesture result: {"isCorrect": true/false, "timestamp": 123456}
    private val _tutorialModeGestureResult = MutableStateFlow<Map<String, Any>>(emptyMap())
    val tutorialModeGestureResult: StateFlow<Map<String, Any>> = _tutorialModeGestureResult.asStateFlow()

    // Feedback dismissed from watch trigger
    private val _tutorialModeFeedbackDismissed = MutableStateFlow(0L)
    val tutorialModeFeedbackDismissed: StateFlow<Long> = _tutorialModeFeedbackDismissed.asStateFlow()

    // Watch ready signal - watch has entered Tutorial Mode screen and is ready to receive data
    private val _tutorialModeWatchReady = MutableStateFlow(0L)
    val tutorialModeWatchReady: StateFlow<Long> = _tutorialModeWatchReady.asStateFlow()

    // Pairing request from watch
    private val _pairingRequest = MutableStateFlow<PairingRequestEvent?>(null)
    val pairingRequest: StateFlow<PairingRequestEvent?> = _pairingRequest.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: WatchConnectionManager? = null
        private const val TAG = "WatchConnectionMgr"
        private const val NOTIFICATION_CHANNEL_ID = "kusho_watch_install"
        private const val NOTIFICATION_ID_INSTALL_APP = 1001
        
        fun getInstance(context: Context): WatchConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchConnectionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        private const val CAPABILITY_WEAR_APP = "kusho_wear_app"
        private const val MESSAGE_PATH_REQUEST_BATTERY = "/request_battery"
        private const val MESSAGE_PATH_REQUEST_DEVICE_INFO = "/request_device_info"
        private const val MESSAGE_PATH_BATTERY_STATUS = "/battery_status"
        private const val MESSAGE_PATH_DEVICE_INFO = "/device_info"
        private const val MESSAGE_PATH_PING = "/kusho/ping"
        private const val MESSAGE_PATH_PONG = "/kusho/pong"

        // Pairing handshake message paths
        private const val MESSAGE_PATH_PAIRING_REQUEST = "/pairing_request"
        private const val MESSAGE_PATH_PAIRING_ACCEPTED = "/pairing_accepted"
        private const val MESSAGE_PATH_PAIRING_DECLINED = "/pairing_declined"
        private const val PREFS_PAIRING = "kusho_pairing"

        // Learn Mode message paths
        private const val MESSAGE_PATH_LEARN_MODE_SKIP = "/learn_mode_skip"
        private const val MESSAGE_PATH_LEARN_MODE_STARTED = "/learn_mode_started"
        private const val MESSAGE_PATH_LEARN_MODE_ENDED = "/learn_mode_ended"
        private const val MESSAGE_PATH_LEARN_MODE_WORD_DATA = "/learn_mode_word_data"
        private const val MESSAGE_PATH_LETTER_INPUT = "/learn_mode_letter_input"
        private const val MESSAGE_PATH_LETTER_RESULT = "/learn_mode_letter_result"
        private const val MESSAGE_PATH_WORD_COMPLETE = "/learn_mode_word_complete"
        private const val MESSAGE_PATH_ACTIVITY_COMPLETE = "/learn_mode_activity_complete"
        private const val MESSAGE_PATH_LEARN_MODE_FEEDBACK_DISMISSED = "/learn_mode_feedback_dismissed"
        private const val MESSAGE_PATH_LEARN_MODE_SHOW_FEEDBACK = "/learn_mode_show_feedback"

        // Tutorial Mode message paths
        private const val MESSAGE_PATH_TUTORIAL_MODE_STARTED = "/tutorial_mode_started"
        private const val MESSAGE_PATH_TUTORIAL_MODE_ENDED = "/tutorial_mode_ended"
        private const val MESSAGE_PATH_TUTORIAL_MODE_LETTER_DATA = "/tutorial_mode_letter_data"
        private const val MESSAGE_PATH_TUTORIAL_MODE_SKIP = "/tutorial_mode_skip"
        private const val MESSAGE_PATH_TUTORIAL_MODE_GESTURE_RESULT = "/tutorial_mode_gesture_result"
        private const val MESSAGE_PATH_TUTORIAL_MODE_SESSION_COMPLETE = "/tutorial_mode_session_complete"
        private const val MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED = "/tutorial_mode_feedback_dismissed"
        private const val MESSAGE_PATH_TUTORIAL_MODE_RETRY = "/tutorial_mode_retry"
        private const val MESSAGE_PATH_TUTORIAL_MODE_SESSION_RESET = "/tutorial_mode_session_reset"
        private const val MESSAGE_PATH_TUTORIAL_MODE_WATCH_READY = "/tutorial_mode_watch_ready"
        private const val MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY = "/tutorial_mode_phone_ready"

        private const val POLLING_INTERVAL_MS = 30000L // 30 seconds
    }
    
    private var monitoringJob: Job? = null
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        Log.d(TAG, "📨 Phone received message: ${messageEvent.path}")
        handleIncomingMessage(messageEvent)
    }
    
    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
        // Real-time updates when watch connects/disconnects
        scope.launch {
            checkConnection()
        }
    }
    
    // Bluetooth state receiver for real-time Bluetooth ON/OFF detection
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            // Bluetooth turned off - clear pairing so handshake is required on reconnect
                            this@WatchConnectionManager.context.getSharedPreferences(PREFS_PAIRING, Context.MODE_PRIVATE)
                                .edit().remove("paired_watch_node_id").apply()
                            Log.d(TAG, "🔵 Bluetooth OFF — cleared paired_watch_node_id")
                            _deviceInfo.value = WatchDeviceInfo(
                                isConnected = false,
                                connectionState = ConnectionState.BLUETOOTH_OFF
                            )
                        }
                        BluetoothAdapter.STATE_ON -> {
                            // Bluetooth turned on - check for watches
                            scope.launch {
                                checkConnection()
                            }
                        }
                    }
                }
            }
        }
    }
    
    init {
        messageClient.addListener(messageListener)
        // Add capability listener for real-time watch connection changes
        capabilityClient.addListener(capabilityListener, CAPABILITY_WEAR_APP)
        
        // Register Bluetooth state receiver
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
    }
    
    /**
     * Start monitoring watch connection and battery status
     */
    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                checkConnection()

                // If watch is connected but battery is still 0, request it again
                val currentDevice = _deviceInfo.value
                if (currentDevice.isConnected && 
                    currentDevice.connectionState == ConnectionState.WATCH_CONNECTED &&
                    currentDevice.batteryPercentage == 0) {
                    // Try to get battery info from all connected nodes with Kusho app
                    try {
                        val capabilityInfo = capabilityClient
                            .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_REACHABLE)
                            .await()
                        capabilityInfo.nodes.firstOrNull()?.let { node ->
                            requestBatteryStatus(node)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop monitoring watch connection
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Check if watch is connected and update device info
     * Now with proper Bluetooth state checking
     */
    suspend fun checkConnection(): Boolean {
        return try {
            // STEP 1: Check if Bluetooth is enabled
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                // Bluetooth is OFF or not available
                _deviceInfo.value = WatchDeviceInfo(
                    isConnected = false,
                    connectionState = ConnectionState.BLUETOOTH_OFF
                )
                return false
            }
            
            // STEP 2: Bluetooth is ON - Check for ANY connected Wear OS nodes
            val allNodes = nodeClient.connectedNodes.await()
            
            // STEP 3: Check for nodes with Kusho capability
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_REACHABLE)
                .await()
            
            val watchNodeWithApp = capabilityInfo.nodes.firstOrNull()
            
            when {
                watchNodeWithApp != null -> {
                    val deviceName = getDeviceName(watchNodeWithApp.displayName)
                    val pairedNodeId = getPairedWatchNodeId()
                    
                    if (pairedNodeId != null && watchNodeWithApp.id == pairedNodeId) {
                        // CASE 3a: Kusho app found AND matches paired node — fully connected
                        _deviceInfo.value = WatchDeviceInfo(
                            name = deviceName,
                            nodeId = watchNodeWithApp.id,
                            isConnected = true,
                            connectionState = ConnectionState.WATCH_CONNECTED,
                            batteryPercentage = _deviceInfo.value.batteryPercentage, // preserve existing
                            lastUpdated = System.currentTimeMillis()
                        )
                        requestBatteryStatus(watchNodeWithApp)
                        return true
                    } else {
                        // CASE 3b: Kusho app found but node not paired — needs handshake
                        Log.d(TAG, "⏳ Watch ${watchNodeWithApp.id} has Kusho but is not paired (saved: $pairedNodeId)")
                        _deviceInfo.value = WatchDeviceInfo(
                            name = deviceName,
                            nodeId = watchNodeWithApp.id,
                            isConnected = false,
                            connectionState = ConnectionState.WATCH_NEEDS_HANDSHAKE,
                            batteryPercentage = null, // do NOT leak battery
                            lastUpdated = System.currentTimeMillis()
                        )
                        return false
                    }
                }
                
                allNodes.isNotEmpty() -> {
                    // CASE 2: Watch paired, but no Kusho app
                    val deviceName = getDeviceName(allNodes.first().displayName)
                    _deviceInfo.value = WatchDeviceInfo(
                        name = deviceName,
                        nodeId = allNodes.first().id,
                        isConnected = false,
                        connectionState = ConnectionState.WATCH_PAIRED_NO_APP,
                        batteryPercentage = 0,
                        lastUpdated = System.currentTimeMillis()
                    )
                    return false
                }
                
                else -> {
                    // CASE 1: Bluetooth ON, but no watch paired
                    _deviceInfo.value = WatchDeviceInfo(
                        isConnected = false,
                        connectionState = ConnectionState.NO_WATCH
                    )
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _deviceInfo.value = WatchDeviceInfo(
                isConnected = false,
                connectionState = ConnectionState.NO_WATCH
            )
            false
        }
    }
    
    /**
     * Request battery status from connected watch
     * Now with retry mechanism to ensure battery data is received
     */
    private fun requestBatteryStatus(node: Node) {
        scope.launch {
            try {
                // Send multiple requests with delays to ensure watch receives it
                repeat(3) { attempt ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_REQUEST_BATTERY,
                        ByteArray(0)
                    ).await()
                    
                    // Wait a bit between requests
                    if (attempt < 2) {
                        delay(2000L) // 2 seconds between requests
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notify watch that Learn Mode session has started
     */
    fun notifyLearnModeStarted() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_STARTED,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Learn Mode started notification sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of Learn Mode start", e)
            }
        }
    }

    /**
     * Notify watch that Learn Mode session has ended
     */
    fun notifyLearnModeEnded() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_ENDED,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Learn Mode ended notification sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of Learn Mode end", e)
            }
        }
    }

    /**
     * Send word data to watch for fill-in-the-blanks mode
     * @param word The full word
     * @param maskedIndex Index of the masked letter (for fill-in-the-blank)
     * @param configurationType The configuration type (e.g., "Fill in the Blank")
     */
    fun sendLearnModeWordData(word: String, maskedIndex: Int, configurationType: String, dominantHand: String = "RIGHT") {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()

                // Create JSON payload
                val jsonPayload = org.json.JSONObject().apply {
                    put("word", word)
                    put("maskedIndex", maskedIndex)
                    put("configurationType", configurationType)
                    put("dominantHand", dominantHand)
                }.toString()

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_WORD_DATA,
                        jsonPayload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Word data sent to watch: $word (masked: $maskedIndex, type: $configurationType, hand: $dominantHand)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send word data to watch", e)
            }
        }
    }

    /**
     * Send letter validation result back to watch for Write the Word mode
     * @param isCorrect Whether the input letter was correct
     * @param currentIndex Current letter index being spelled (next letter to input)
     * @param totalLetters Total letters in the word
     */
    fun sendLetterResult(isCorrect: Boolean, currentIndex: Int, totalLetters: Int) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = org.json.JSONObject().apply {
                    put("isCorrect", isCorrect)
                    put("currentIndex", currentIndex)
                    put("totalLetters", totalLetters)
                }.toString()

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LETTER_RESULT,
                        payload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Letter result sent: correct=$isCorrect, index=$currentIndex/$totalLetters")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send letter result", e)
            }
        }
    }

    /**
     * Notify watch that word was completed successfully
     */
    fun sendWordComplete() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_WORD_COMPLETE,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Word complete notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send word complete", e)
            }
        }
    }

    /**
     * Send feedback result to watch so it shows the same correct/incorrect screen
     * @param isCorrect Whether the answer was correct
     * @param predictedLetter The letter that was predicted
     */
    fun sendLearnModeFeedback(isCorrect: Boolean, predictedLetter: String) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = org.json.JSONObject().apply {
                    put("isCorrect", isCorrect)
                    put("predictedLetter", predictedLetter)
                }.toString()

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_SHOW_FEEDBACK,
                        payload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Learn Mode feedback sent: correct=$isCorrect, letter=$predictedLetter")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send Learn Mode feedback", e)
            }
        }
    }

    /**
     * Notify watch that mobile has dismissed the Learn Mode feedback dialog
     */
    fun notifyLearnModeFeedbackDismissed() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_FEEDBACK_DISMISSED,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Learn Mode feedback dismissal sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of Learn Mode feedback dismissal", e)
            }
        }
    }

    /**
     * Notify watch that the entire activity (all items in the set) is complete
     */
    fun notifyActivityComplete() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_ACTIVITY_COMPLETE,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Activity complete notification sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send activity complete", e)
            }
        }
    }

    /**
     * Request device info from connected watch
     */
    fun requestDeviceInfo() {
        scope.launch {
            try {
                val currentInfo = _deviceInfo.value
                if (currentInfo.isConnected && currentInfo.nodeId.isNotEmpty()) {
                    messageClient.sendMessage(
                        currentInfo.nodeId,
                        MESSAGE_PATH_REQUEST_DEVICE_INFO,
                        ByteArray(0)
                    ).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Request battery from currently connected watch
     * Call this when UI needs fresh battery data (e.g., Dashboard onResume)
     */
    fun requestBatteryFromConnectedWatch() {
        scope.launch {
            try {
                val currentDevice = _deviceInfo.value
                
                Log.d(TAG, "🔋 Phone requesting battery - isConnected: ${currentDevice.isConnected}, state: ${currentDevice.connectionState}, nodeId: ${currentDevice.nodeId}")
                
                // Only request if watch is fully connected with app AND paired
                val pairedNodeId = getPairedWatchNodeId()
                if (currentDevice.isConnected && 
                    currentDevice.connectionState == ConnectionState.WATCH_CONNECTED &&
                    currentDevice.nodeId.isNotEmpty() &&
                    pairedNodeId != null && currentDevice.nodeId == pairedNodeId) {
                    
                    // Send immediate battery request (with retry)
                    repeat(3) { attempt ->
                        Log.d(TAG, "📤 Sending battery request (attempt ${attempt + 1}/3) to ${currentDevice.nodeId}")
                        messageClient.sendMessage(
                            currentDevice.nodeId,
                            MESSAGE_PATH_REQUEST_BATTERY,
                            ByteArray(0)
                        ).await()
                        Log.d(TAG, "✅ Battery request sent successfully (attempt ${attempt + 1}/3)")
                        
                        if (attempt < 2) {
                            delay(1000L) // 1 second between retries
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Cannot request battery - watch not fully connected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send battery request", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Handle incoming messages from watch
     */
    private fun handleIncomingMessage(messageEvent: MessageEvent) {
        Log.d(TAG, "📥 Processing message: ${messageEvent.path} from ${messageEvent.sourceNodeId}")
        
        // Data lockdown: sensor, gesture, and battery data must come from the paired watch node
        val lockedDownPaths = setOf(
            MESSAGE_PATH_LEARN_MODE_SKIP,
            MESSAGE_PATH_LETTER_INPUT,
            MESSAGE_PATH_LEARN_MODE_FEEDBACK_DISMISSED,
            MESSAGE_PATH_TUTORIAL_MODE_SKIP,
            MESSAGE_PATH_TUTORIAL_MODE_GESTURE_RESULT,
            MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED,
            MESSAGE_PATH_TUTORIAL_MODE_WATCH_READY,
            MESSAGE_PATH_BATTERY_STATUS
        )
        
        if (messageEvent.path in lockedDownPaths) {
            val pairedNodeId = getPairedWatchNodeId()
            if (pairedNodeId == null || messageEvent.sourceNodeId != pairedNodeId) {
                Log.w(TAG, "⚠️ DATA LOCKDOWN: Ignoring ${messageEvent.path} from node ${messageEvent.sourceNodeId} (paired: $pairedNodeId)")
                return
            }
        }
        
        when (messageEvent.path) {
            MESSAGE_PATH_PAIRING_REQUEST -> {
                Log.d(TAG, "🤝 Pairing request received from watch: ${messageEvent.sourceNodeId}")
                try {
                    val json = org.json.JSONObject(String(messageEvent.data))
                    val watchName = json.optString("watchName", "Smartwatch")
                    _pairingRequest.value = PairingRequestEvent(
                        nodeId = messageEvent.sourceNodeId,
                        watchName = watchName,
                        timestamp = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    _pairingRequest.value = PairingRequestEvent(
                        nodeId = messageEvent.sourceNodeId,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
            MESSAGE_PATH_PING -> {
                // Watch is checking if Kusho app is running - respond with PONG
                Log.d(TAG, "🏓 Received PING from watch, sending PONG response")
                scope.launch {
                    try {
                        messageClient.sendMessage(
                            messageEvent.sourceNodeId,
                            MESSAGE_PATH_PONG,
                            "pong".toByteArray()
                        ).await()
                        Log.d(TAG, "✅ PONG sent successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to send PONG", e)
                    }
                }
            }
            MESSAGE_PATH_BATTERY_STATUS -> {
                val batteryLevel = String(messageEvent.data).toIntOrNull()
                Log.d(TAG, "🔋 Received battery status: ${batteryLevel?.let { "$it%" } ?: "parsing failed"}")
                _deviceInfo.value = _deviceInfo.value.copy(
                    batteryPercentage = batteryLevel,
                    lastUpdated = System.currentTimeMillis()
                )
                Log.d(TAG, "✅ Updated device info with battery: ${batteryLevel?.let { "$it%" } ?: "null"}")
            }
            MESSAGE_PATH_DEVICE_INFO -> {
                val deviceName = String(messageEvent.data)
                Log.d(TAG, "📱 Received device info: $deviceName")
                _deviceInfo.value = _deviceInfo.value.copy(
                    name = deviceName,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            MESSAGE_PATH_LEARN_MODE_SKIP -> {
                Log.d(TAG, "⏭️ Received Learn Mode skip command from watch")
                // Trigger skip by updating the timestamp
                _learnModeSkipTrigger.value = System.currentTimeMillis()
            }
            MESSAGE_PATH_LETTER_INPUT -> {
                try {
                    val jsonString = String(messageEvent.data)
                    val json = org.json.JSONObject(jsonString)
                    val letter = json.optString("letter", "").firstOrNull() ?: return
                    val letterIndex = json.optInt("letterIndex", 0)
                    Log.d(TAG, "🔤 Received letter input from watch: $letter at index $letterIndex")
                    _letterInputEvent.value = LetterInputEvent(
                        letter = letter,
                        letterIndex = letterIndex,
                        timestamp = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to parse letter input", e)
                }
            }
            MESSAGE_PATH_LEARN_MODE_FEEDBACK_DISMISSED -> {
                Log.d(TAG, "👆 Watch dismissed Learn Mode feedback - dismissing mobile dialog")
                _learnModeFeedbackDismissed.value = System.currentTimeMillis()
            }
            MESSAGE_PATH_TUTORIAL_MODE_SKIP -> {
                Log.d(TAG, "⏭️ Received Tutorial Mode skip command from watch")
                _tutorialModeSkipTrigger.value = System.currentTimeMillis()
            }
            MESSAGE_PATH_TUTORIAL_MODE_GESTURE_RESULT -> {
                try {
                    val jsonString = String(messageEvent.data)
                    val json = org.json.JSONObject(jsonString)
                    val isCorrect = json.getBoolean("isCorrect")
                    val predictedLetter = json.optString("predictedLetter", "")
                    Log.d(TAG, if (isCorrect) "✅ Gesture correct from watch" else "❌ Gesture incorrect from watch")
                    _tutorialModeGestureResult.value = mapOf(
                        "isCorrect" to isCorrect,
                        "predictedLetter" to predictedLetter,
                        "timestamp" to System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing gesture result", e)
                }
            }
            MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED -> {
                Log.d(TAG, "👆 Watch dismissed feedback - dismissing mobile dialog")
                _tutorialModeFeedbackDismissed.value = System.currentTimeMillis()
            }
            MESSAGE_PATH_TUTORIAL_MODE_WATCH_READY -> {
                Log.d(TAG, "✅ Watch is ready for Tutorial Mode")
                _tutorialModeWatchReady.value = System.currentTimeMillis()
            }
        }
    }
    
    /**
     * Parse device name to get user-friendly name
     */
    private fun getDeviceName(displayName: String): String {
        return when {
            displayName.contains("Galaxy Watch", ignoreCase = true) -> displayName
            displayName.contains("Watch", ignoreCase = true) -> displayName
            else -> "Smartwatch" // Default name for non-Galaxy watches
        }
    }
    
    /**
     * Accept a pairing request from a watch.
     * Saves the watch Node ID locally and sends /pairing_accepted back.
     */
    fun acceptPairing(nodeId: String) {
        // Save the paired watch node ID
        val prefs = context.getSharedPreferences(PREFS_PAIRING, Context.MODE_PRIVATE)
        prefs.edit().putString("paired_watch_node_id", nodeId).apply()
        Log.d(TAG, "✅ Saved paired watch node ID: $nodeId")
        
        // Send acceptance message back to watch
        scope.launch {
            try {
                messageClient.sendMessage(
                    nodeId,
                    MESSAGE_PATH_PAIRING_ACCEPTED,
                    "accepted".toByteArray()
                ).await()
                Log.d(TAG, "✅ Pairing acceptance sent to watch $nodeId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send pairing acceptance", e)
            }
        }
        
        // Clear the request and immediately promote to WATCH_CONNECTED
        _pairingRequest.value = null
        _deviceInfo.value = _deviceInfo.value.copy(
            isConnected = true,
            connectionState = ConnectionState.WATCH_CONNECTED
        )
        
        // Trigger connection check + battery request now that pairing is saved
        scope.launch {
            checkConnection()
        }
    }
    
    /**
     * Decline a pairing request from a watch.
     * Sends /pairing_declined back to the requesting node.
     */
    fun declinePairing(nodeId: String) {
        scope.launch {
            try {
                messageClient.sendMessage(
                    nodeId,
                    MESSAGE_PATH_PAIRING_DECLINED,
                    "declined".toByteArray()
                ).await()
                Log.d(TAG, "❌ Pairing declined for watch $nodeId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send pairing decline", e)
            }
        }
        
        // Remove saved pairing so device is truly unpaired
        context.getSharedPreferences(PREFS_PAIRING, Context.MODE_PRIVATE)
            .edit().remove("paired_watch_node_id").apply()
        Log.d(TAG, "🗑️ Cleared paired_watch_node_id from SharedPreferences")
        
        // Clear the request, reset battery, and move to WATCH_NEEDS_HANDSHAKE
        _pairingRequest.value = null
        _deviceInfo.value = _deviceInfo.value.copy(
            batteryPercentage = null,
            isConnected = false,
            connectionState = ConnectionState.WATCH_NEEDS_HANDSHAKE
        )
    }
    
    /**
     * Get the saved paired watch node ID (or null if not paired via handshake yet)
     */
    fun getPairedWatchNodeId(): String? {
        return context.getSharedPreferences(PREFS_PAIRING, Context.MODE_PRIVATE)
            .getString("paired_watch_node_id", null)
    }
    
    /**
     * Get list of all connected nodes
     */
    suspend fun getConnectedNodes(): List<Node> {
        return try {
            nodeClient.connectedNodes.await()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Clear connection state (useful for testing or when user manually disconnects)
     */
    fun clearConnection() {
        _deviceInfo.value = WatchDeviceInfo(
            isConnected = false,
            connectionState = ConnectionState.NO_WATCH
        )
    }
    
    /**
     * Open Play Store on watch using RemoteActivityHelper
     * This works even if the Kusho app is NOT installed on the watch yet.
     */
    fun openPlayStoreOnWatch() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                
                if (nodes.isEmpty()) {
                    Log.w(TAG, "⚠️ Cannot open Play Store - no connected watch nodes")
                    return@launch
                }

                // RemoteActivityHelper needs a context and an executor
                val remoteActivityHelper = RemoteActivityHelper(context, Executors.newSingleThreadExecutor())

                // The Intent to open the Play Store specifically for YOUR app package
                val intent = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse("market://details?id=com.example.kusho"))

                for (node in nodes) {
                    remoteActivityHelper.startRemoteActivity(
                        intent,
                        node.id
                    )
                    Log.d(TAG, "✅ Sent Remote Intent to open Play Store on: ${node.displayName}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to open Play Store on watch", e)
            }
        }
    }
    
    /**
     * Notify watch that Tutorial Mode session has started
     */
    fun notifyTutorialModeStarted(studentName: String, lessonTitle: String) {
        // Clear previous state before starting new session
        _tutorialModeSkipTrigger.value = 0L
        _tutorialModeGestureResult.value = emptyMap()
        _tutorialModeWatchReady.value = 0L

        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val jsonPayload = org.json.JSONObject().apply {
                    put("studentName", studentName)
                    put("lessonTitle", lessonTitle)
                }.toString()

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_STARTED,
                        jsonPayload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Tutorial Mode started notification sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of Tutorial Mode start", e)
            }
        }
    }

    /**
     * Notify watch that the phone is on TutorialSessionScreen and ready for handshake.
     * The watch will reply with /tutorial_mode_watch_ready when it receives this.
     */
    fun sendTutorialModePhoneReady() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "\u2705 Phone ready signal sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "\u274C Failed to send phone ready signal", e)
            }
        }
    }

    /**
     * Notify watch that Tutorial Mode session has ended
     */
    fun notifyTutorialModeEnded() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_ENDED,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Tutorial Mode ended notification sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of Tutorial Mode end", e)
            }
        }
    }

    /**
     * Send letter data to watch for air writing practice
     * @param letter The target letter (e.g., "A", "b")
     * @param letterCase "uppercase" or "lowercase"
     * @param currentIndex Current letter index (1-based)
     * @param totalLetters Total number of letters in session
     * @param dominantHand "LEFT" or "RIGHT" for model selection
     */
    fun sendTutorialModeLetterData(letter: String, letterCase: String, currentIndex: Int, totalLetters: Int, dominantHand: String = "RIGHT") {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()

                val jsonPayload = org.json.JSONObject().apply {
                    put("letter", letter)
                    put("letterCase", letterCase)
                    put("currentIndex", currentIndex)
                    put("totalLetters", totalLetters)
                    put("dominantHand", dominantHand)
                }.toString()

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_LETTER_DATA,
                        jsonPayload.toByteArray()
                    ).await()
                }
                Log.d(TAG, "✅ Letter data sent to watch: $letter ($letterCase) [$currentIndex/$totalLetters] hand=$dominantHand")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send letter data to watch", e)
            }
        }
    }

    /**
     * Notify watch that the session is complete (show completion screen)
     */
    fun notifyTutorialModeSessionComplete() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_SESSION_COMPLETE,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Tutorial Mode session complete notification sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of session complete", e)
            }
        }
    }

    /**
     * Notify watch that mobile has dismissed the feedback dialog
     */
    fun notifyTutorialModeFeedbackDismissed() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Mobile feedback dismissal sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to notify watch of feedback dismissal", e)
            }
        }
    }

    /**
     * Notify watch to retry gesture recognition (after incorrect result)
     */
    fun notifyTutorialModeRetry() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_RETRY,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Retry command sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send retry command to watch", e)
            }
        }
    }

    fun notifyTutorialModeSessionReset() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_SESSION_RESET,
                        ByteArray(0)
                    ).await()
                }
                Log.d(TAG, "✅ Session reset command sent to watch")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send session reset command to watch", e)
            }
        }
    }

    /**
     * Create notification channel for install prompts
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Watch App Installation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for installing the Kusho watch app"
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Clean up resources
     * Note: Does NOT cancel scope since this is a singleton that persists across screens
     */
    fun cleanup() {
        stopMonitoring()
        messageClient.removeListener(messageListener)
        capabilityClient.removeListener(capabilityListener)
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        // DO NOT cancel scope - singleton needs persistent scope
    }
}
