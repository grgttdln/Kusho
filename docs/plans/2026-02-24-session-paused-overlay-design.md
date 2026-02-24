# Session Paused Overlay Design

**Date:** 2026-02-24
**Status:** Approved

## Problem

When the watch exits a mode (Learn or Tutorial) and re-enters it while the phone session is still active, the teacher has no visibility into what happened. The current design doc (cross-mode-sync-fixes) proposed automatic recovery in ~1s, but the teacher should have explicit control over when the session resumes.

## Solution: Watch-Initiated State Signals with Teacher-Controlled Resume

The watch sends explicit exit/enter signals. The phone shows a non-blocking "Session Paused" overlay. The teacher must tap "Continue" to resume. The watch waits in a "Waiting for teacher..." state until the teacher acts.

## Message Protocol

| Message Path | Direction | When Sent | Payload |
|---|---|---|---|
| `/learn_mode_watch_exited_screen` | Watch -> Phone | Watch LearnModeScreen `onDispose` | Empty |
| `/learn_mode_watch_entered_screen` | Watch -> Phone | Watch LearnModeScreen enters composition | Empty |
| `/learn_mode_resume` | Phone -> Watch | Teacher taps "Continue" | JSON: `{word, maskedIndex, configurationType, dominantHand}` |
| `/tutorial_mode_watch_exited_screen` | Watch -> Phone | Watch TutorialModeScreen `onDispose` | Empty |
| `/tutorial_mode_watch_entered_screen` | Watch -> Phone | Watch TutorialModeScreen enters composition | Empty |
| `/tutorial_mode_resume` | Phone -> Watch | Teacher taps "Continue" | JSON: `{letter, dominantHand, configurationType}` |

## State Flow

1. Watch exits mode screen -> sends `watch_exited_screen`
2. Phone receives -> sets `sessionPaused = true`, `watchOnModeScreen = false`
3. Phone shows non-blocking "Session Paused" overlay with disabled Continue button
4. Watch re-enters mode screen -> sends `watch_entered_screen`
5. Phone receives -> sets `watchOnModeScreen = true`, enables Continue button
6. Teacher taps Continue -> phone sends `resume` with current word/letter data
7. Watch receives `resume` -> restores word data, shows "Tap to begin!"
8. Phone sets `sessionPaused = false`, overlay dismissed

## Phone-Side Overlay: SessionPausedOverlay

Non-blocking overlay composable (not a Dialog). Sits on top of session screen content.

**Visual design:**
- Semi-transparent dark scrim over the session screen (session UI visible but dimmed)
- Centered card with:
  - Amber/yellow header bar (distinct from blue disconnect dialog - pause, not error)
  - "Session Paused" title
  - Subtitle: "The watch has exited the current mode"
  - Status indicator:
    - Watch not on screen: "Waiting for watch..." with spinner
    - Watch on screen: "Watch ready" with checkmark
  - Two buttons:
    - "Continue" (primary, enabled only when `watchOnModeScreen = true`)
    - "End Session" (secondary, always enabled)

**Component signature:**
```kotlin
@Composable
fun SessionPausedOverlay(
    watchOnScreen: Boolean,
    onContinue: () -> Unit,
    onEndSession: () -> Unit
)
```

**File:** `app/src/main/java/com/example/app/ui/components/SessionPausedOverlay.kt`

## Watch-Side Behavior

**On re-entering mode screen during paused session:**
1. Screen enters composition, sends `watch_entered_screen`
2. Shows "Waiting for teacher to resume..." with spinner
3. Listens for `/learn_mode_resume` or `/tutorial_mode_resume`
4. On receiving resume: restores word/letter data, transitions to "Tap to begin!"
5. Student taps to begin -> normal gesture recognition continues

**On exiting mode screen:**
1. `DisposableEffect` `onDispose` sends `watch_exited_screen`
2. Cleans up sensor listeners and gesture recognition (existing behavior)

## State Management

**New StateFlows on WatchConnectionManager (phone):**
```kotlin
val sessionPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)
val watchOnModeScreen: MutableStateFlow<Boolean> = MutableStateFlow(true)
```

**State transitions:**

| Event | sessionPaused | watchOnModeScreen | Overlay |
|---|---|---|---|
| Session starts | false | true | Hidden |
| Watch sends exited_screen | true | false | Shown, Continue disabled |
| Watch sends entered_screen | true (unchanged) | true | Shown, Continue enabled |
| Teacher taps Continue | false | true | Hidden |
| Teacher taps End Session | N/A | N/A | Session ends |

## Edge Cases

1. **Watch exits and never comes back:** Overlay stays with disabled Continue. Teacher can End Session. If BT drops, disconnect dialog takes priority.
2. **Watch exits/re-enters rapidly:** Each exit/enter pair updates `watchOnModeScreen`. Overlay stays shown until teacher continues.
3. **BT disconnects while paused:** PING/PONG detection still runs. `WatchDisconnectedDialog` (blocking) takes priority over `SessionPausedOverlay`. When BT restores, disconnect dialog dismisses, paused overlay remains.
4. **Session ends while paused:** End Session sends `learn_mode_ended`/`tutorial_mode_ended` normally.

**Overlay priority:**
1. `WatchDisconnectedDialog` (BT lost) - highest, blocking
2. `SessionPausedOverlay` (watch exited mode) - lower, non-blocking

## Files to Modify

| File | Changes |
|---|---|
| `WatchConnectionManager.kt` (phone) | Add `sessionPaused`/`watchOnModeScreen` StateFlows, handle exit/enter messages, add resume methods |
| `LearnModeSessionScreen.kt` (phone) | Observe paused state, show `SessionPausedOverlay` |
| `TutorialSessionScreen.kt` (phone) | Same as LearnModeSessionScreen |
| `PhoneCommunicationManager.kt` (watch) | Add exit/enter send methods, handle resume messages |
| `LearnModeScreen.kt` (watch) | Send exit/enter signals, add "Waiting for teacher..." state, handle resume |
| `TutorialModeScreen.kt` (watch) | Same as LearnModeScreen |

**New file:**
| File | Purpose |
|---|---|
| `SessionPausedOverlay.kt` (phone) | Non-blocking overlay composable |

## Relationship to Cross-Mode Sync Fixes

This feature is additive to the cross-mode sync fixes design. The state request mechanism (`/request_learn_mode_state`) from that design is replaced by the more explicit `watch_entered_screen` + teacher-controlled `resume` flow. The disconnect detection (PING/PONG during sessions) remains independent and complementary.
