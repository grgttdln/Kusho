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

class WatchConnectionManager private constructor(private val context: Context) {
    
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _deviceInfo = MutableStateFlow(WatchDeviceInfo())
    val deviceInfo: StateFlow<WatchDeviceInfo> = _deviceInfo.asStateFlow()
    
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
                            // Bluetooth turned off - immediately update to disconnected
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
                    // CASE 3: Watch paired AND Kusho app installed/running
                    val deviceName = getDeviceName(watchNodeWithApp.displayName)
                    _deviceInfo.value = WatchDeviceInfo(
                        name = deviceName,
                        nodeId = watchNodeWithApp.id,
                        isConnected = true,
                        connectionState = ConnectionState.WATCH_CONNECTED,
                        batteryPercentage = 0, // Will be updated by battery request
                        lastUpdated = System.currentTimeMillis()
                    )
                    requestBatteryStatus(watchNodeWithApp)
                    return true
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
                
                // Only request if watch is fully connected with app
                if (currentDevice.isConnected && 
                    currentDevice.connectionState == ConnectionState.WATCH_CONNECTED &&
                    currentDevice.nodeId.isNotEmpty()) {
                    
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
        Log.d(TAG, "📥 Processing message: ${messageEvent.path}")
        when (messageEvent.path) {
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
