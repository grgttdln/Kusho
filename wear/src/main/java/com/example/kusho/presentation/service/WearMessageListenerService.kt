package com.example.kusho.presentation.service

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.kusho.presentation.learn.LearnModeStateHolder
import com.example.kusho.presentation.tutorial.TutorialModeStateHolder
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Background service that listens for messages from phone even when app is closed
 * This ensures battery status can be sent even when the watch app is not actively running
 */
class WearMessageListenerService : WearableListenerService() {
    
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "WearMessageListener"
        private const val MESSAGE_PATH_REQUEST_BATTERY = "/request_battery"
        private const val MESSAGE_PATH_REQUEST_DEVICE_INFO = "/request_device_info"
        private const val MESSAGE_PATH_BATTERY_STATUS = "/battery_status"
        private const val MESSAGE_PATH_DEVICE_INFO = "/device_info"
        private const val MESSAGE_PATH_PAIRING_ACCEPTED = "/pairing_accepted"

        // Learn Mode message paths
        private const val MESSAGE_PATH_LEARN_MODE_WORD_DATA = "/learn_mode_word_data"
        private const val MESSAGE_PATH_LEARN_MODE_SESSION_START = "/learn_mode_started"
        private const val MESSAGE_PATH_LEARN_MODE_SESSION_END = "/learn_mode_ended"

        // Tutorial Mode message paths
        private const val MESSAGE_PATH_TUTORIAL_MODE_STARTED = "/tutorial_mode_started"
        private const val MESSAGE_PATH_TUTORIAL_MODE_ENDED = "/tutorial_mode_ended"
        private const val MESSAGE_PATH_TUTORIAL_MODE_LETTER_DATA = "/tutorial_mode_letter_data"
        private const val MESSAGE_PATH_TUTORIAL_MODE_SESSION_COMPLETE = "/tutorial_mode_session_complete"
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        Log.d(TAG, "📨 Message received: ${messageEvent.path}")
        
        when (messageEvent.path) {
            MESSAGE_PATH_PAIRING_ACCEPTED -> {
                Log.d(TAG, "✅ Pairing accepted by phone! Saving paired status.")
                val prefs = applicationContext.getSharedPreferences("kusho_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_paired", true)
                    .putBoolean("is_skipped", false)
                    .apply()
            }
            MESSAGE_PATH_REQUEST_BATTERY -> {
                Log.d(TAG, "🔋 Battery request received from phone")
                sendBatteryStatusToPhone()
            }
            MESSAGE_PATH_REQUEST_DEVICE_INFO -> {
                Log.d(TAG, "📱 Device info request received from phone")
                sendDeviceInfoToPhone()
            }
            MESSAGE_PATH_LEARN_MODE_WORD_DATA -> {
                Log.d(TAG, "📚 Word data received from phone")
                handleWordData(messageEvent.data)
            }
            MESSAGE_PATH_LEARN_MODE_SESSION_START -> {
                Log.d(TAG, "🎯 Learn mode session start received")
                handleSessionStart(messageEvent.data)
            }
            MESSAGE_PATH_LEARN_MODE_SESSION_END -> {
                Log.d(TAG, "🏁 Learn mode session end received")
                LearnModeStateHolder.endSession()
            }
            MESSAGE_PATH_TUTORIAL_MODE_STARTED -> {
                Log.d(TAG, "📝 Tutorial mode session start received")
                handleTutorialModeStarted(messageEvent.data)
            }
            MESSAGE_PATH_TUTORIAL_MODE_ENDED -> {
                Log.d(TAG, "🏁 Tutorial mode session end received")
                TutorialModeStateHolder.endSession()
            }
            MESSAGE_PATH_TUTORIAL_MODE_LETTER_DATA -> {
                Log.d(TAG, "📝 Letter data received from phone")
                handleLetterData(messageEvent.data)
            }
            MESSAGE_PATH_TUTORIAL_MODE_SESSION_COMPLETE -> {
                Log.d(TAG, "🎊 Tutorial mode session complete received")
                TutorialModeStateHolder.markSessionComplete()
            }
        }
    }

    /**
     * Handle incoming word data for fill-in-the-blanks
     * Expected JSON format: {"word": "APPLE", "maskedIndex": 2, "configurationType": "Fill in the Blank", "dominantHand": "RIGHT"}
     */
    private fun handleWordData(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = JSONObject(jsonString)

            val word = json.optString("word", "")
            val maskedIndex = json.optInt("maskedIndex", -1)
            val configurationType = json.optString("configurationType", "")
            val dominantHand = json.optString("dominantHand", "RIGHT")

            Log.d(TAG, "📚 Parsed word data: word=$word, maskedIndex=$maskedIndex, type=$configurationType, hand=$dominantHand")

            if (word.isNotEmpty()) {
                LearnModeStateHolder.updateWordData(word, maskedIndex, configurationType, dominantHand)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing word data", e)
        }
    }

    /**
     * Handle session start message
     * Expected JSON format: {"setTitle": "Animal Set", "totalWords": 10}
     */
    private fun handleSessionStart(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = JSONObject(jsonString)

            val setTitle = json.optString("setTitle", "")
            val totalWords = json.optInt("totalWords", 0)

            Log.d(TAG, "🎯 Session start: title=$setTitle, totalWords=$totalWords")

            LearnModeStateHolder.startSession(setTitle, totalWords)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing session start data", e)
        }
    }

    /**
     * Handle incoming Tutorial Mode session start message
     * Expected JSON format: {"studentName": "John", "lessonTitle": "Vowels"}
     */
    private fun handleTutorialModeStarted(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = JSONObject(jsonString)

            val studentName = json.optString("studentName", "")
            val lessonTitle = json.optString("lessonTitle", "")

            Log.d(TAG, "📝 Parsed session data: student=$studentName, lesson=$lessonTitle")

            TutorialModeStateHolder.startSession(studentName, lessonTitle)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing tutorial session data", e)
        }
    }

    /**
     * Handle incoming letter data for air writing practice
     * Expected JSON format: {"letter": "A", "letterCase": "uppercase", "currentIndex": 1, "totalLetters": 5, "dominantHand": "RIGHT"}
     */
    private fun handleLetterData(data: ByteArray) {
        try {
            val jsonString = String(data)
            val json = JSONObject(jsonString)

            val letter = json.optString("letter", "")
            val letterCase = json.optString("letterCase", "")
            val currentIndex = json.optInt("currentIndex", 0)
            val totalLetters = json.optInt("totalLetters", 0)
            val dominantHand = json.optString("dominantHand", "RIGHT")

            Log.d(TAG, "📝 Parsed letter data: letter=$letter, case=$letterCase, index=$currentIndex/$totalLetters, hand=$dominantHand")

            if (letter.isNotEmpty()) {
                TutorialModeStateHolder.updateLetterData(letter, letterCase, currentIndex, totalLetters, dominantHand)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing letter data", e)
        }
    }

    /**
     * Get current battery level from system
     */
    private fun getBatteryLevel(): Int {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                registerReceiver(null, filter)
            }
            
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            
            if (level != -1 && scale != -1) {
                (level / scale.toFloat() * 100).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
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
     * Send battery status to all connected phones
     */
    private fun sendBatteryStatusToPhone() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val batteryLevel = getBatteryLevel()
                val batteryData = batteryLevel.toString().toByteArray()
                
                Log.d(TAG, "📤 Sending battery: $batteryLevel% to ${nodes.size} phone(s)")
                
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            MESSAGE_PATH_BATTERY_STATUS,
                            batteryData
                        ).await()
                        Log.d(TAG, "✅ Battery sent successfully to ${node.displayName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to send battery to ${node.displayName}", e)
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in sendBatteryStatusToPhone", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Send device info to all connected phones
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
}
