# Cross-Mode Sync Fixes: State Desync & Disconnect Blindspot

**Date**: 2026-02-24
**Status**: Approved

## Overview

Two cross-mode sync issues between the mobile (teacher) app and the watch (student) app during Learn Mode and Tutorial Mode sessions.

---

## Issue 1: State Desync on Watch Navigation

### Problem

If a student exits a mode on the watch (presses back / swipes away) and re-enters it, the watch gets permanently stuck on "Waiting..." even though the phone session is still active.

### Root Cause

The phone's heartbeat loop in `LearnModeSessionScreen.kt:329` and `TutorialSessionScreen.kt:397` exits once the initial handshake completes:

```kotlin
while (!isWatchReady) {
    kotlinx.coroutines.delay(2000)
    if (!isWatchReady) {
        watchConnectionManager.sendLearnModePhoneReady()
    }
}
```

Once `isWatchReady` becomes true, the loop exits and the phone stops sending heartbeats. When the watch re-enters the mode screen:

1. A new `PhoneCommunicationManager` instance is created via `remember { PhoneCommunicationManager(context) }` with `_isPhoneInLearnMode = false`
2. The phone is no longer sending heartbeats
3. The watch waits forever for a signal that will never come

### Solution: Watch-Initiated State Request

When the watch enters a mode screen, it sends a state request message. The phone responds with its current session state.

**New message paths:**
- `/request_learn_mode_state` (watch -> phone)
- `/learn_mode_state_response` (phone -> watch, JSON payload: `{ isActive, wordData }`)
- `/request_tutorial_mode_state` (watch -> phone)
- `/tutorial_mode_state_response` (phone -> watch, JSON payload: `{ isActive, letterData }`)

**Watch side (on mode screen entry):**
1. `DisposableEffect` fires as today
2. Immediately send `/request_learn_mode_state` (or tutorial equivalent)
3. On receiving the response, set `_isPhoneInLearnMode = true` and populate state holders with current word/letter data
4. Screen transitions from "Waiting..." to "Tap to begin!" (or directly to content if data is present)

**Phone side (new handler):**
1. On receiving `/request_learn_mode_state`, check if the session screen is currently active
2. If active, respond with `/learn_mode_state_response` containing `isActive=true` + current word/letter data
3. If not active, respond with `isActive=false`

### Files Changed

| File | Change |
|------|--------|
| `wear/.../PhoneCommunicationManager.kt` | Add state request send method + response handler |
| `wear/.../LearnModeScreen.kt` | Send state request in DisposableEffect on entry |
| `wear/.../TutorialModeScreen.kt` | Send state request in DisposableEffect on entry |
| `app/.../WatchConnectionManager.kt` | Add state request handler + response sender |

---

## Issue 2: Mobile Disconnect Blindspot

### Problem

When Bluetooth disconnects mid-session, the watch detects it immediately (via `ConnectionMonitor` with 5-second PING/PONG checks), but the mobile app does not notice. The teacher's phone keeps showing the session screen as if everything is fine.

### Root Cause

The mobile app's connection monitoring is not session-aware:
- Normal polling interval: 30 seconds (too slow for active sessions)
- No PING/PONG health check (only responds to PINGs from the watch, never initiates)
- `CapabilityClient` listener fires on capability changes, not reliable for BT disconnect detection
- `BluetoothAdapter.ACTION_STATE_CHANGED` receiver only detects the phone's own BT toggle, not watch disconnection

The watch has two-layer detection (5s polling + real-time capability listener + PING/PONG). The phone has none of this during sessions.

### Solution: Session-Aware Health Check + Disconnect Modal

**Mobile-side active monitoring during sessions:**
1. When a session starts, switch from 30s polling to 5-second PING/PONG health checks
2. Send `/kusho/ping` to the watch, wait up to 3 seconds for `/kusho/pong`
3. If 2 consecutive checks fail, mark as disconnected and show the modal
4. When the session ends, revert to normal 30s polling

**Disconnect modal UI:**
```
+----------------------------+
|   Watch Disconnected       |
|                            |
|   Reconnecting...          |
|   (auto-retrying)          |
|                            |
|       [ End Session ]      |
+----------------------------+
```

- Blocks all session interaction (modal overlay)
- Auto-retries PING/PONG every 5 seconds in the background
- Dismisses itself when PING/PONG succeeds, session resumes automatically
- "End Session" button navigates back and cleans up the session

### Files Changed

| File | Change |
|------|--------|
| `app/.../WatchConnectionManager.kt` | Add `startSessionMonitoring()` / `stopSessionMonitoring()` with faster PING/PONG loop, expose `sessionConnectionLost` StateFlow |
| `app/.../LearnModeSessionScreen.kt` | Start session monitoring on entry, stop on exit, observe `sessionConnectionLost` to show modal |
| `app/.../TutorialSessionScreen.kt` | Same pattern as LearnModeSessionScreen |
| New: `WatchDisconnectedDialog.kt` | Reusable composable for the disconnect modal (or inline in session screens) |

---

## Architecture Summary

```
BEFORE (broken):
  Phone heartbeat: one-shot (stops after handshake)
  Phone disconnect detection: 30s poll only

AFTER (fixed):
  Phone heartbeat: one-shot + watch can request state on re-entry
  Phone disconnect detection: 5s PING/PONG during active sessions
```

### Detection Comparison (After Fix)

| Concern | Watch | Mobile |
|---------|-------|--------|
| Connection check interval | 5s (always) | 5s (during sessions), 30s (idle) |
| Health check method | PING/PONG | PING/PONG (during sessions) |
| Real-time listener | CapabilityClient | CapabilityClient + BT state receiver |
| Disconnect UI | Disconnect screen | "Watch Disconnected" modal |
| Recovery | Auto-reconnect + state request | Auto-reconnect + modal dismissal |
