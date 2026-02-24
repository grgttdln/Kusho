# Bluetooth Off Mid-Session Dialog Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** When the teacher's phone Bluetooth is turned off during a Learn Mode or Tutorial Mode session, instantly show a "Bluetooth is Off" dialog with a "Turn On Bluetooth" button, instead of the generic "Watch Disconnected" dialog that takes 10-13 seconds to appear.

**Architecture:** Add a `SessionDisconnectReason` enum and StateFlow to `WatchConnectionManager`. Wire the existing `bluetoothStateReceiver` BroadcastReceiver to instantly set the reason when BT goes off during a session. Modify `WatchDisconnectedDialog` to accept an `isBluetoothOff` flag and show a different variant. Update both session screens to collect the reason and pass a Bluetooth enable launcher.

**Tech Stack:** Kotlin, Jetpack Compose, Android BluetoothAdapter, ActivityResultContracts, StateFlow

---

### Task 1: Add SessionDisconnectReason enum and StateFlow to WatchConnectionManager

**Files:**
- Modify: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`

**Step 1: Add the enum after the existing `ConnectionState` enum (after line 33)**

Insert immediately after the closing `}` of `enum class ConnectionState` (line 33):

```kotlin
enum class SessionDisconnectReason {
    NONE,              // Connected / no issue
    BLUETOOTH_OFF,     // Phone Bluetooth disabled
    WATCH_UNREACHABLE  // BT on, watch not responding to PING
}
```

**Step 2: Add the StateFlow after the existing `_sessionConnectionLost` (after line 131)**

Insert immediately after `val sessionConnectionLost: StateFlow<Boolean> = _sessionConnectionLost.asStateFlow()` (line 131):

```kotlin
    // Reason for session disconnection - used to differentiate BT off vs watch unreachable
    private val _sessionDisconnectReason = MutableStateFlow(SessionDisconnectReason.NONE)
    val sessionDisconnectReason: StateFlow<SessionDisconnectReason> = _sessionDisconnectReason.asStateFlow()
```

**Step 3: Wire the existing `bluetoothStateReceiver` to set reason during active sessions**

The existing `bluetoothStateReceiver` (lines 222-245) currently only updates `_deviceInfo`. Modify the `STATE_OFF` branch to also set session disconnect state when a session is active. Replace the `STATE_OFF` case (lines 228-233):

Before:
```kotlin
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "🔵 Bluetooth OFF")
                            _deviceInfo.value = WatchDeviceInfo(
                                isConnected = false,
                                connectionState = ConnectionState.BLUETOOTH_OFF
                            )
                        }
```

After:
```kotlin
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "🔵 Bluetooth OFF")
                            _deviceInfo.value = WatchDeviceInfo(
                                isConnected = false,
                                connectionState = ConnectionState.BLUETOOTH_OFF
                            )
                            // Instantly notify session screens if a session is active
                            if (_isInLearnModeSession.value || _isInTutorialModeSession.value) {
                                Log.d(TAG, "🔵 Bluetooth OFF during active session - instant disconnect notification")
                                _sessionDisconnectReason.value = SessionDisconnectReason.BLUETOOTH_OFF
                                _sessionConnectionLost.value = true
                            }
                        }
```

Modify the `STATE_ON` branch (lines 235-239) to also clear session disconnect state when reason was BT off. Replace:

Before:
```kotlin
                        BluetoothAdapter.STATE_ON -> {
                            // Bluetooth turned on - check for watches
                            scope.launch {
                                checkConnection()
                            }
                        }
```

After:
```kotlin
                        BluetoothAdapter.STATE_ON -> {
                            // Bluetooth turned on - check for watches
                            scope.launch {
                                checkConnection()
                            }
                            // Clear session disconnect if it was caused by BT off
                            if (_sessionDisconnectReason.value == SessionDisconnectReason.BLUETOOTH_OFF) {
                                Log.d(TAG, "🔵 Bluetooth ON - clearing BT-off session disconnect")
                                _sessionDisconnectReason.value = SessionDisconnectReason.NONE
                                _sessionConnectionLost.value = false
                            }
                        }
```

**Step 4: Set WATCH_UNREACHABLE reason on PING/PONG failures**

In `startSessionMonitoring()` (lines 302-354), wherever `_sessionConnectionLost.value = true` is set, also set the reason. There are 2 places:

4a. In the "no PONG received" branch (around line 342), change:
```kotlin
                            _sessionConnectionLost.value = true
```
to:
```kotlin
                            _sessionDisconnectReason.value = SessionDisconnectReason.WATCH_UNREACHABLE
                            _sessionConnectionLost.value = true
```

4b. In the exception catch branch (around line 349), change:
```kotlin
                        _sessionConnectionLost.value = true
```
to:
```kotlin
                        _sessionDisconnectReason.value = SessionDisconnectReason.WATCH_UNREACHABLE
                        _sessionConnectionLost.value = true
```

4c. In the recovery branch (around line 335), also clear the reason. Change:
```kotlin
                            _sessionConnectionLost.value = false
```
to:
```kotlin
                            _sessionDisconnectReason.value = SessionDisconnectReason.NONE
                            _sessionConnectionLost.value = false
```

**Step 5: Reset reason in `stopSessionMonitoring()`**

In `stopSessionMonitoring()` (lines 359-363), add reason reset. Change:
```kotlin
    fun stopSessionMonitoring() {
        sessionMonitoringJob?.cancel()
        sessionMonitoringJob = null
        _sessionConnectionLost.value = false
    }
```
to:
```kotlin
    fun stopSessionMonitoring() {
        sessionMonitoringJob?.cancel()
        sessionMonitoringJob = null
        _sessionConnectionLost.value = false
        _sessionDisconnectReason.value = SessionDisconnectReason.NONE
    }
```

---

### Task 2: Update WatchDisconnectedDialog to support Bluetooth Off variant

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/WatchDisconnectedDialog.kt`

**Step 1: Replace the entire file content**

Replace the full content of `WatchDisconnectedDialog.kt` with:

```kotlin
package com.example.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Blocking modal shown when the watch disconnects during an active session.
 * Two variants:
 * - Bluetooth Off: shows "Turn On Bluetooth" + "End Session" buttons
 * - Watch Unreachable: shows reconnecting spinner + "End Session" button
 * Auto-dismisses when connection is restored (caller controls visibility via sessionConnectionLost).
 */
@Composable
fun WatchDisconnectedDialog(
    isBluetoothOff: Boolean = false,
    onTurnOnBluetooth: () -> Unit = {},
    onEndSession: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Blocked - cannot dismiss by tapping outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                // Blue header section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF49A9FF))
                )

                // White content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isBluetoothOff) "Bluetooth is Off" else "Watch Disconnected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isBluetoothOff) {
                        // Bluetooth Off variant: explanation text, no spinner
                        Text(
                            text = "Turn on Bluetooth to continue your session.",
                            fontSize = 14.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Primary: Turn On Bluetooth (solid blue)
                        Button(
                            onClick = onTurnOnBluetooth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49A9FF)
                            )
                        ) {
                            Text(
                                text = "Turn On Bluetooth",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary: End Session (light blue)
                        Button(
                            onClick = onEndSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD6EDFF)
                            )
                        ) {
                            Text(
                                text = "End Session",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF49A9FF)
                            )
                        }
                    } else {
                        // Watch Unreachable variant: spinner + reconnecting text
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = Color(0xFF49A9FF)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Reconnecting...",
                            fontSize = 14.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onEndSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD6EDFF)
                            )
                        ) {
                            Text(
                                text = "End Session",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF49A9FF)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Note:** The new parameters `isBluetoothOff` and `onTurnOnBluetooth` have defaults (`false` and `{}`) so existing call sites compile without changes. However, we will update all call sites in Tasks 3 and 4.

---

### Task 3: Integrate Bluetooth Off detection into LearnModeSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Add required imports**

Add these imports at the top of the file (after the existing imports around line 71):

```kotlin
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.app.service.SessionDisconnectReason
```

**Step 2: Add disconnect reason state collection**

After line 250 (`val isConnectionLost by watchConnectionManager.sessionConnectionLost.collectAsState()`), add:

```kotlin
    val disconnectReason by watchConnectionManager.sessionDisconnectReason.collectAsState()
```

**Step 3: Add Bluetooth enable launcher**

After the `watchConnectionManager` and state collection lines (around line 251, after the new `disconnectReason` line), add:

```kotlin
    // Bluetooth enable request launcher for "Turn On Bluetooth" button
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* BroadcastReceiver in WatchConnectionManager handles STATE_ON */ }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }
```

**Step 4: Update the WatchDisconnectedDialog call**

Replace the existing dialog block (lines 924-932):

Before:
```kotlin
    if (isConnectionLost) {
        WatchDisconnectedDialog(
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyLearnModeEnded()
                onEarlyExit()
            }
        )
    }
```

After:
```kotlin
    if (isConnectionLost) {
        WatchDisconnectedDialog(
            isBluetoothOff = disconnectReason == SessionDisconnectReason.BLUETOOTH_OFF,
            onTurnOnBluetooth = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothEnableLauncher.launch(enableBtIntent)
                        }
                        else -> {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    }
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothEnableLauncher.launch(enableBtIntent)
                }
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyLearnModeEnded()
                onEarlyExit()
            }
        )
    }
```

---

### Task 4: Integrate Bluetooth Off detection into TutorialSessionScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt`

**Step 1: Add required imports**

Add these imports at the top of the file (after the existing imports around line 67):

```kotlin
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.app.service.SessionDisconnectReason
```

**Step 2: Add disconnect reason state collection**

After line 108 (`val isConnectionLost by watchConnectionManager.sessionConnectionLost.collectAsState()`), add:

```kotlin
    val disconnectReason by watchConnectionManager.sessionDisconnectReason.collectAsState()
```

**Step 3: Add Bluetooth enable launcher**

After the new `disconnectReason` line (around line 109), add:

```kotlin
    // Bluetooth enable request launcher for "Turn On Bluetooth" button
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* BroadcastReceiver in WatchConnectionManager handles STATE_ON */ }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }
```

**Step 4: Update the WatchDisconnectedDialog call**

Replace the existing dialog block (lines 558-566):

Before:
```kotlin
    if (isConnectionLost) {
        WatchDisconnectedDialog(
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyTutorialModeEnded()
                onEarlyExit()
            }
        )
    }
```

After:
```kotlin
    if (isConnectionLost) {
        WatchDisconnectedDialog(
            isBluetoothOff = disconnectReason == SessionDisconnectReason.BLUETOOTH_OFF,
            onTurnOnBluetooth = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothEnableLauncher.launch(enableBtIntent)
                        }
                        else -> {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    }
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothEnableLauncher.launch(enableBtIntent)
                }
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyTutorialModeEnded()
                onEarlyExit()
            }
        )
    }
```
