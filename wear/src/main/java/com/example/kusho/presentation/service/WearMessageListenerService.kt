package com.example.kusho.presentation.service

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        Log.d(TAG, "📨 Message received: ${messageEvent.path}")
        
        when (messageEvent.path) {
            MESSAGE_PATH_REQUEST_BATTERY -> {
                Log.d(TAG, "🔋 Battery request received from phone")
                // Phone is requesting battery status
                sendBatteryStatusToPhone()
            }
            MESSAGE_PATH_REQUEST_DEVICE_INFO -> {
                Log.d(TAG, "📱 Device info request received from phone")
                // Phone is requesting device info
                sendDeviceInfoToPhone()
            }
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
            android.os.Build.MODEL ?: "Galaxy Watch"
        } catch (e: Exception) {
            "Galaxy Watch"
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
