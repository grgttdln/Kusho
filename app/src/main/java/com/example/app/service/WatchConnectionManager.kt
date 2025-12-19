package com.example.app.service

import android.content.Context
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class WatchDeviceInfo(
    val name: String = "Unknown Watch",
    val nodeId: String = "",
    val isConnected: Boolean = false,
    val batteryPercentage: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

class WatchConnectionManager(private val context: Context) {
    
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _deviceInfo = MutableStateFlow(WatchDeviceInfo())
    val deviceInfo: StateFlow<WatchDeviceInfo> = _deviceInfo.asStateFlow()
    
    companion object {
        private const val CAPABILITY_WEAR_APP = "kusho_wear_app"
        private const val MESSAGE_PATH_REQUEST_BATTERY = "/request_battery"
        private const val MESSAGE_PATH_REQUEST_DEVICE_INFO = "/request_device_info"
        private const val MESSAGE_PATH_BATTERY_STATUS = "/battery_status"
        private const val MESSAGE_PATH_DEVICE_INFO = "/device_info"
        private const val POLLING_INTERVAL_MS = 30000L // 30 seconds
    }
    
    private var monitoringJob: Job? = null
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        handleIncomingMessage(messageEvent)
    }
    
    init {
        messageClient.addListener(messageListener)
    }
    
    /**
     * Start monitoring watch connection and battery status
     */
    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                checkConnection()
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
     */
    suspend fun checkConnection(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            
            if (nodes.isEmpty()) {
                _deviceInfo.value = WatchDeviceInfo(isConnected = false)
                return false
            }
            
            // Find node with Kusho capability
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_REACHABLE)
                .await()
            
            val watchNode = capabilityInfo.nodes.firstOrNull() ?: nodes.firstOrNull()
            
            if (watchNode != null) {
                val deviceName = getDeviceName(watchNode.displayName)
                _deviceInfo.value = _deviceInfo.value.copy(
                    name = deviceName,
                    nodeId = watchNode.id,
                    isConnected = true,
                    lastUpdated = System.currentTimeMillis()
                )
                
                // Request battery status from watch
                requestBatteryStatus(watchNode)
                return true
            } else {
                _deviceInfo.value = WatchDeviceInfo(isConnected = false)
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _deviceInfo.value = WatchDeviceInfo(isConnected = false)
            false
        }
    }
    
    /**
     * Request battery status from connected watch
     */
    private fun requestBatteryStatus(node: Node) {
        scope.launch {
            try {
                messageClient.sendMessage(
                    node.id,
                    MESSAGE_PATH_REQUEST_BATTERY,
                    ByteArray(0)
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
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
     * Handle incoming messages from watch
     */
    private fun handleIncomingMessage(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            MESSAGE_PATH_BATTERY_STATUS -> {
                val batteryLevel = String(messageEvent.data).toIntOrNull() ?: 0
                _deviceInfo.value = _deviceInfo.value.copy(
                    batteryPercentage = batteryLevel,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            MESSAGE_PATH_DEVICE_INFO -> {
                val deviceName = String(messageEvent.data)
                _deviceInfo.value = _deviceInfo.value.copy(
                    name = deviceName,
                    lastUpdated = System.currentTimeMillis()
                )
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
            else -> "Galaxy Watch" // Default name
        }
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
     * Clean up resources
     */
    fun cleanup() {
        stopMonitoring()
        messageClient.removeListener(messageListener)
        scope.cancel()
    }
}
