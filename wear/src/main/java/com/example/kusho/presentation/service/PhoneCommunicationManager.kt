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

    // StateFlow for Learn Mode feedback from phone (to show correct/incorrect screen)
    data class LearnModeFeedbackEvent(
        val isCorrect: Boolean = false,
        val predictedLetter: String = "",
        val timestamp: Long = 0L
    )
    private val _learnModeFeedbackEvent = MutableStateFlow(LearnModeFeedbackEvent())
    val learnModeFeedbackEvent: StateFlow<LearnModeFeedbackEvent> = _learnModeFeedbackEvent.asStateFlow()

    // StateFlow for Learn Mode feedback dismissed from phone
    private val _learnModeFeedbackDismissed = MutableStateFlow(0L)
    val learnModeFeedbackDismissed: StateFlow<Long> = _learnModeFeedbackDismissed.asStateFlow()

    // StateFlow to track if mobile app is in Tutorial Mode session
    private val _isPhoneInTutorialMode = MutableStateFlow(false)
    val isPhoneInTutorialMode: StateFlow<Boolean> = _isPhoneInTutorialMode.asStateFlow()

    companion object {
        private const val MESSAGE_PATH_REQUEST_BATTERY = "/request_battery"
        private const val MESSAGE_PATH_REQUEST_DEVICE_INFO = "/request_device_info"
        private const val MESSAGE_PATH_BATTERY_STATUS = "/battery_status"
        private const val MESSAGE_PATH_DEVICE_INFO = "/device_info"

        // Learn Mode Message Paths
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
            MESSAGE_PATH_ACTIVITY_COMPLETE -> {
                android.util.Log.d("PhoneCommunicationMgr", "🎉 Activity complete received")
                // Update state holder to show completion screen
                com.example.kusho.presentation.learn.LearnModeStateHolder.onActivityComplete()
            }
            MESSAGE_PATH_LEARN_MODE_SHOW_FEEDBACK -> {
                android.util.Log.d("PhoneCommunicationMgr", "🎯 Learn Mode feedback received")
                handleLearnModeFeedback(messageEvent.data)
            }
            MESSAGE_PATH_LEARN_MODE_FEEDBACK_DISMISSED -> {
                android.util.Log.d("PhoneCommunicationMgr", "👆 Mobile dismissed Learn Mode feedback")
                _learnModeFeedbackDismissed.value = System.currentTimeMillis()
            }
            MESSAGE_PATH_TUTORIAL_MODE_STARTED -> {
                android.util.Log.d("PhoneCommunicationMgr", "📝 Phone Tutorial Mode started")
                _isPhoneInTutorialMode.value = true
                handleTutorialModeStarted(messageEvent.data)
            }
            MESSAGE_PATH_TUTORIAL_MODE_ENDED -> {
                android.util.Log.d("PhoneCommunicationMgr", "📝 Phone Tutorial Mode ended")
                _isPhoneInTutorialMode.value = false
                com.example.kusho.presentation.tutorial.TutorialModeStateHolder.endSession()
            }
            MESSAGE_PATH_TUTORIAL_MODE_LETTER_DATA -> {
                android.util.Log.d("PhoneCommunicationMgr", "📝 Letter data received")
                handleLetterData(messageEvent.data)
            }
            MESSAGE_PATH_TUTORIAL_MODE_SESSION_COMPLETE -> {
                android.util.Log.d("PhoneCommunicationMgr", "🎊 Session complete")
                com.example.kusho.presentation.tutorial.TutorialModeStateHolder.markSessionComplete()
            }
            MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED -> {
                android.util.Log.d("PhoneCommunicationMgr", "👆 Mobile dismissed feedback - clearing watch feedback")
                com.example.kusho.presentation.tutorial.TutorialModeStateHolder.clearFeedback()
            }
            MESSAGE_PATH_TUTORIAL_MODE_RETRY -> {
                android.util.Log.d("PhoneCommunicationMgr", "🔄 Mobile requested retry")
                com.example.kusho.presentation.tutorial.TutorialModeStateHolder.triggerRetry()
            }
            MESSAGE_PATH_TUTORIAL_MODE_SESSION_RESET -> {
                android.util.Log.d("PhoneCommunicationMgr", "♻️ Mobile requested session reset - clearing all state")
                com.example.kusho.presentation.tutorial.TutorialModeStateHolder.resetSession()
            }            MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY -> {
                // Only reply if watch user is actually on the Tutorial Mode screen
                if (com.example.kusho.presentation.tutorial.TutorialModeStateHolder.isWatchOnTutorialScreen.value) {
                    android.util.Log.d("PhoneCommunicationMgr", "📱 Phone is ready & watch is on Tutorial screen - replying with watch ready")
                    scope.launch {
                        sendTutorialModeWatchReady()
                    }
                } else {
                    android.util.Log.d("PhoneCommunicationMgr", "📱 Phone is ready but watch is NOT on Tutorial screen - ignoring")
                }
            }        }
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
     * Handle Learn Mode feedback from phone
     */
    private fun handleLearnModeFeedback(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = org.json.JSONObject(jsonString)

            val isCorrect = json.optBoolean("isCorrect", false)
            val predictedLetter = json.optString("predictedLetter", "")

            android.util.Log.d("PhoneCommunicationMgr", "🎯 Learn Mode feedback: correct=$isCorrect, letter=$predictedLetter")

            _learnModeFeedbackEvent.value = LearnModeFeedbackEvent(
                isCorrect = isCorrect,
                predictedLetter = predictedLetter,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing Learn Mode feedback", e)
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
     * Handle incoming Tutorial Mode session start data
     */
    private fun handleTutorialModeStarted(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = org.json.JSONObject(jsonString)

            val studentName = json.optString("studentName", "")
            val lessonTitle = json.optString("lessonTitle", "")

            com.example.kusho.presentation.tutorial.TutorialModeStateHolder.startSession(studentName, lessonTitle)
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing tutorial start data", e)
        }
    }

    /**
     * Handle incoming letter data
     */
    private fun handleLetterData(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = org.json.JSONObject(jsonString)

            val letter = json.optString("letter", "")
            val letterCase = json.optString("letterCase", "")
            val currentIndex = json.optInt("currentIndex", 0)
            val totalLetters = json.optInt("totalLetters", 0)
            val dominantHand = json.optString("dominantHand", "RIGHT")

            com.example.kusho.presentation.tutorial.TutorialModeStateHolder.updateLetterData(
                letter, letterCase, currentIndex, totalLetters, dominantHand
            )
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Error parsing letter data", e)
        }
    }

    /**
     * Send skip command to phone app's Tutorial Mode
     * This is triggered when user swipes left on the watch
     */
    suspend fun sendTutorialModeSkipCommand() {
        try {
            val nodes = nodeClient.connectedNodes.await()

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_SKIP,
                        ByteArray(0)
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
     * Send gesture recognition result to phone app
     * @param isCorrect Whether the gesture was recognized correctly
     * @param predictedLetter The letter that was predicted by the model
     */
    suspend fun sendTutorialModeGestureResult(isCorrect: Boolean, predictedLetter: String = "") {
        try {
            val nodes = nodeClient.connectedNodes.await()
            val jsonPayload = org.json.JSONObject().apply {
                put("isCorrect", isCorrect)
                put("predictedLetter", predictedLetter)
            }.toString()

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_GESTURE_RESULT,
                        jsonPayload.toByteArray()
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
     * Notify phone that watch is on the Tutorial Mode screen and ready to receive data
     */
    suspend fun sendTutorialModeWatchReady() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_WATCH_READY,
                        ByteArray(0)
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
     * Notify phone that watch has dismissed the feedback display
     */
    suspend fun sendTutorialModeFeedbackDismissed() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_TUTORIAL_MODE_FEEDBACK_DISMISSED,
                        ByteArray(0)
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
     * Notify phone that watch has dismissed the Learn Mode feedback display
     */
    suspend fun sendLearnModeFeedbackDismissed() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_LEARN_MODE_FEEDBACK_DISMISSED,
                        ByteArray(0)
                    ).await()
                    android.util.Log.d("PhoneCommunicationMgr", "📤 Learn Mode feedback dismissed sent to phone")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PhoneCommunicationMgr", "❌ Failed to send Learn Mode feedback dismissed", e)
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
