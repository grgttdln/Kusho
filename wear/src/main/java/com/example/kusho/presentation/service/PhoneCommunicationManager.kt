package com.example.kusho.presentation.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Service to manage communication with phone app
 * Handles sending battery status and device info to the connected Android phone
 */
class PhoneCommunicationManager(private val context: Context) : MessageClient.OnMessageReceivedListener {
    
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    // StateFlow to track if mobile app is in Learn Mode session
    private val _isPhoneInLearnMode = MutableStateFlow(false)
    val isPhoneInLearnMode: StateFlow<Boolean> = _isPhoneInLearnMode.asStateFlow()
    
    // StateFlow for letter validation results from phone (for Write the Word mode)
    data class LetterResultEvent(
        val isCorrect: Boolean = false,
        val currentIndex: Int = 0,
        val totalLetters: Int = 0,
        val timestamp: Long = 0L
    )
    private val _letterResultEvent = MutableStateFlow(LetterResultEvent())
    val letterResultEvent: StateFlow<LetterResultEvent> = _letterResultEvent.asStateFlow()

    // StateFlow for word complete events from phone
    private val _wordCompleteEvent = MutableStateFlow(0L) // Timestamp
    val wordCompleteEvent: StateFlow<Long> = _wordCompleteEvent.asStateFlow()

    companion object {
        private const val MESSAGE_PATH_REQUEST_BATTERY = "/request_battery"
        private const val MESSAGE_PATH_REQUEST_DEVICE_INFO = "/request_device_info"
        private const val MESSAGE_PATH_BATTERY_STATUS = "/battery_status"
        private const val MESSAGE_PATH_DEVICE_INFO = "/device_info"
        private const val MESSAGE_PATH_LEARN_MODE_SKIP = "/learn_mode_skip"
        private const val MESSAGE_PATH_LEARN_MODE_STARTED = "/learn_mode_started"
        private const val MESSAGE_PATH_LEARN_MODE_ENDED = "/learn_mode_ended"
        private const val MESSAGE_PATH_LEARN_MODE_WORD_DATA = "/learn_mode_word_data"
        private const val MESSAGE_PATH_LETTER_INPUT = "/learn_mode_letter_input"
        private const val MESSAGE_PATH_LETTER_RESULT = "/learn_mode_letter_result"
        private const val MESSAGE_PATH_WORD_COMPLETE = "/learn_mode_word_complete"
        private const val BATTERY_UPDATE_INTERVAL_MS = 60000L // 1 minute
    }
    
    private var batteryMonitoringJob: Job? = null
    
    init {
        messageClient.addListener(this)
    }
    
    /**
     * Start monitoring battery and sending updates to phone
     */
    fun startBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        batteryMonitoringJob = scope.launch {
            while (isActive) {
                updateBatteryLevel()
                sendBatteryStatusToPhone()
                delay(BATTERY_UPDATE_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop monitoring battery
     */
    fun stopBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        batteryMonitoringJob = null
    }
    
    /**
     * Get current battery level
     */
    private fun updateBatteryLevel() {
        try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            
            val batteryPct = if (level != -1 && scale != -1) {
                (level / scale.toFloat() * 100).toInt()
            } else {
                0
            }
            
            _batteryLevel.value = batteryPct
        } catch (e: Exception) {
            e.printStackTrace()
            _batteryLevel.value = 0
        }
    }
    
    /**
     * Send battery status to connected phone
     */
    private fun sendBatteryStatusToPhone() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val batteryData = _batteryLevel.value.toString().toByteArray()
                
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_BATTERY_STATUS,
                            batteryData
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Send device info to connected phone
     */
    private fun sendDeviceInfoToPhone() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val deviceName = getDeviceName()
                val deviceData = deviceName.toByteArray()
                
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_DEVICE_INFO,
                            deviceData
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get device name
     */
    private fun getDeviceName(): String {
        return try {
            android.os.Build.MODEL ?: "Smartwatch"
        } catch (e: Exception) {
            "Smartwatch"
        }
    }
    
    /**
     * Handle incoming messages from phone
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            MESSAGE_PATH_REQUEST_BATTERY -> {
                updateBatteryLevel()
                sendBatteryStatusToPhone()
            }
            MESSAGE_PATH_REQUEST_DEVICE_INFO -> {
                sendDeviceInfoToPhone()
            }
            MESSAGE_PATH_LEARN_MODE_STARTED -> {
                android.util.Log.d("PhoneCommunicationMgr", "📚 Phone Learn Mode started")
                _isPhoneInLearnMode.value = true
            }
            MESSAGE_PATH_LEARN_MODE_ENDED -> {
                android.util.Log.d("PhoneCommunicationMgr", "📚 Phone Learn Mode ended")
                _isPhoneInLearnMode.value = false
                // Also end session in state holder
                com.example.kusho.presentation.learn.LearnModeStateHolder.endSession()
            }
            MESSAGE_PATH_LEARN_MODE_WORD_DATA -> {
                android.util.Log.d("PhoneCommunicationMgr", "📚 Word data received")
                handleWordData(messageEvent.data)
            }
            MESSAGE_PATH_LETTER_RESULT -> {
                android.util.Log.d("PhoneCommunicationMgr", "📝 Letter result received")
                handleLetterResult(messageEvent.data)
            }
            MESSAGE_PATH_WORD_COMPLETE -> {
                android.util.Log.d("PhoneCommunicationMgr", "✅ Word complete received")
                _wordCompleteEvent.value = System.currentTimeMillis()
                // Also update state holder
                com.example.kusho.presentation.learn.LearnModeStateHolder.onWordComplete()
            }
        }
    }

    /**
     * Handle letter result from phone
     */
    private fun handleLetterResult(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = org.json.JSONObject(jsonString)

            val isCorrect = json.optBoolean("isCorrect", false)
            val currentIndex = json.optInt("currentIndex", 0)
            val totalLetters = json.optInt("totalLetters", 0)

            android.util.Log.d("PhoneCommunicationMgr", "📝 Letter result: correct=$isCorrect, index=$currentIndex/$totalLetters")

            _letterResultEvent.value = LetterResultEvent(
                isCorrect = isCorrect,
                currentIndex = currentIndex,
                totalLetters = totalLetters,
                timestamp = System.currentTimeMillis()
            )

            // Also update state holder
            com.example.kusho.presentation.learn.LearnModeStateHolder.onLetterResult(isCorrect, currentIndex, totalLetters)
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing letter result", e)
        }
    }
    
    /**
     * Handle incoming word data for fill-in-the-blanks
     */
    private fun handleWordData(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = org.json.JSONObject(jsonString)
            
            val word = json.optString("word", "")
            val maskedIndex = json.optInt("maskedIndex", -1)
            val configurationType = json.optString("configurationType", "")
            
            android.util.Log.d("PhoneCommunicationMgr", "📚 Word: $word, maskedIndex: $maskedIndex, type: $configurationType")
            
            if (word.isNotEmpty()) {
                com.example.kusho.presentation.learn.LearnModeStateHolder.updateWordData(word, maskedIndex, configurationType)
            }
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing word data", e)
        }
    }
    
    /**
     * Send initial connection info to phone
     */
    fun sendInitialInfo() {
        scope.launch {
            updateBatteryLevel()
            sendBatteryStatusToPhone()
            sendDeviceInfoToPhone()
        }
    }
    
    /**
     * Send skip command to phone app's Learn Mode
     * This is triggered when user swipes left on the watch
     */
    suspend fun sendSkipCommand() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_SKIP,
                        ByteArray(0) // Empty payload
                    ).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Send letter input to phone for Write the Word mode
     * @param letter The letter that was recognized
     * @param letterIndex The current letter index being input
     */
    suspend fun sendLetterInput(letter: String, letterIndex: Int) {
        try {
            val nodes = nodeClient.connectedNodes.await()

            val payload = org.json.JSONObject().apply {
                put("letter", letter)
                put("letterIndex", letterIndex)
            }.toString()

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LETTER_INPUT,
                        payload.toByteArray()
                    ).await()
                    android.util.Log.d("PhoneCommunicationMgr", "📤 Letter input sent: $letter at index $letterIndex")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to send letter input", e)
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopBatteryMonitoring()
        messageClient.removeListener(this)
        scope.cancel()
    }
}
