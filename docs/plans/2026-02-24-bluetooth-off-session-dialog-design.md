# Bluetooth Off Mid-Session Dialog Design

**Goal:** When the teacher's phone Bluetooth is turned off during an active Learn Mode or Tutorial Mode session, instantly show a dialog prompting them to turn Bluetooth back on or end the session. This replaces the generic "Watch Disconnected" dialog for this specific edge case.

**Architecture:** Extend the existing PING/PONG session health monitoring with a disconnection reason enum. The existing `BroadcastReceiver` for `BluetoothAdapter.ACTION_STATE_CHANGED` in `WatchConnectionManager` already detects BT OFF — we wire it into the session monitoring flow. The `WatchDisconnectedDialog` becomes reason-aware with two variants.

---

## Edge Case Being Solved

When the teacher turns off Bluetooth mid-session (accidentally or via quick settings), the current implementation:
1. Waits 10-13 seconds for 2 failed PING/PONG checks
2. Shows generic "Watch Disconnected" + "Reconnecting..." spinner
3. Provides no indication that the issue is on the phone side
4. Offers no actionable fix (just "End Session")

**After this fix:**
1. BT OFF is detected instantly via BroadcastReceiver (0 second delay)
2. Shows "Bluetooth is Off" with clear explanation
3. Offers "Turn On Bluetooth" button that launches the system Bluetooth enable dialog
4. Auto-dismisses when BT is re-enabled

---

## Component 1: Disconnection Reason Tracking

**File:** `WatchConnectionManager.kt`

### New Enum
```kotlin
enum class SessionDisconnectReason {
    NONE,              // Connected
    BLUETOOTH_OFF,     // Phone Bluetooth disabled
    WATCH_UNREACHABLE  // BT on, watch not responding to PING
}
```

### New StateFlow
```kotlin
private val _sessionDisconnectReason = MutableStateFlow(SessionDisconnectReason.NONE)
val sessionDisconnectReason: StateFlow<SessionDisconnectReason> = _sessionDisconnectReason.asStateFlow()
```

### BroadcastReceiver Changes
In the existing `bluetoothStateReceiver`:
- **STATE_OFF:** If a session is active (`_isInLearnModeSession.value || _isInTutorialModeSession.value`), immediately set `_sessionConnectionLost = true` and `_sessionDisconnectReason = BLUETOOTH_OFF`.
- **STATE_ON:** If reason was `BLUETOOTH_OFF`, reset `_sessionConnectionLost = false` and `_sessionDisconnectReason = NONE`. The PING/PONG loop will naturally resume on next iteration.

### PING/PONG Failure Changes
When consecutive failures reach threshold, set `_sessionDisconnectReason = WATCH_UNREACHABLE` alongside `_sessionConnectionLost = true`.

### Cleanup
`stopSessionMonitoring()` resets both `_sessionConnectionLost = false` and `_sessionDisconnectReason = NONE`.

---

## Component 2: Modified WatchDisconnectedDialog

**File:** `WatchDisconnectedDialog.kt`

### Updated Signature
```kotlin
@Composable
fun WatchDisconnectedDialog(
    isBluetoothOff: Boolean,
    onTurnOnBluetooth: () -> Unit,
    onEndSession: () -> Unit
)
```

### Bluetooth Off Variant (`isBluetoothOff = true`)
- Title: "Bluetooth is Off"
- Body: "Turn on Bluetooth to continue your session."
- No spinner
- Primary button: "Turn On Bluetooth" (solid blue `0xFF49A9FF`, white text)
- Secondary button: "End Session" (light blue `0xFFD6EDFF`, blue text) — same as current

### Watch Unreachable Variant (`isBluetoothOff = false`)
- Current behavior unchanged
- Title: "Watch Disconnected"
- Spinner + "Reconnecting..."
- "End Session" button

### Same Blocking Properties
- `dismissOnBackPress = false`
- `dismissOnClickOutside = false`

---

## Component 3: Session Screen Integration

**Files:** `LearnModeSessionScreen.kt`, `TutorialSessionScreen.kt`

### New State Collection
```kotlin
val disconnectReason by watchConnectionManager.sessionDisconnectReason.collectAsState()
```

### Bluetooth Enable Launcher
Same pattern as `DashboardScreen`:
```kotlin
val bluetoothEnableLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { /* BroadcastReceiver handles the state change */ }
```

### Updated Dialog Call
```kotlin
if (isConnectionLost) {
    WatchDisconnectedDialog(
        isBluetoothOff = disconnectReason == SessionDisconnectReason.BLUETOOTH_OFF,
        onTurnOnBluetooth = {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        },
        onEndSession = {
            watchConnectionManager.stopSessionMonitoring()
            watchConnectionManager.notifyLearnModeEnded() // or notifyTutorialModeEnded()
            onEarlyExit()
        }
    )
}
```

### Auto-Dismiss Flow
1. Teacher taps "Turn On Bluetooth"
2. System Bluetooth enable dialog appears
3. Teacher enables Bluetooth
4. `BroadcastReceiver` fires `STATE_ON`
5. `_sessionConnectionLost` resets to `false`, `_sessionDisconnectReason` resets to `NONE`
6. Compose recomposes, `isConnectionLost` is now `false`, dialog disappears
7. Session resumes — PING/PONG health checks continue on next iteration

---

## Detection Timing Comparison

| Scenario | Before | After |
|----------|--------|-------|
| Phone BT turned off | ~10-13s (2 failed PINGs) | Instant (BroadcastReceiver) |
| Watch goes out of range | ~10-13s | ~10-13s (unchanged) |
| Watch app crashes | ~10-13s | ~10-13s (unchanged) |
| BT re-enabled after off | Next PING cycle (~5s) | Instant (BroadcastReceiver) |
