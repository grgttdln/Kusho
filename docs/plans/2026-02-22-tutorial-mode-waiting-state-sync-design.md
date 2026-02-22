# Fix Tutorial Mode "Waiting" State Sync Bug

**Date**: 2026-02-22
**Status**: Approved

## Problem

The watch shows "Waiting..." in Tutorial Mode even when the phone session is already active. This happens because:

1. Phone sends `/tutorial_mode_started` → `TutorialModeStateHolder.startSession()` sets `sessionData.isActive = true`
2. Watch user opens TutorialModeScreen → `DisposableEffect` calls `resetSession()` → clears `isActive` back to `false`
3. A new `PhoneCommunicationManager` instance starts with `_isPhoneInTutorialMode = false`
4. Unlike Learn Mode, the `phone_ready` handler does NOT set `_isPhoneInTutorialMode = true`
5. Watch is stuck on "Waiting..." even though the phone session is active

## Solution

Mirror Learn Mode's pattern — use `PhoneCommunicationManager.isPhoneInTutorialMode` as the gate for the waiting screen instead of `sessionData.isActive`.

## Changes

### 1. `wear/.../tutorial/TutorialModeScreen.kt`

- Collect `phoneCommunicationManager.isPhoneInTutorialMode` as state
- Replace `!sessionData.isActive` with `!isPhoneInTutorialMode` in the `when` block
- Update the handshake `LaunchedEffect` to trigger on `isPhoneInTutorialMode` instead of `sessionData.isActive`

### 2. `wear/.../service/PhoneCommunicationManager.kt`

- In the `MESSAGE_PATH_TUTORIAL_MODE_PHONE_READY` handler, add `_isPhoneInTutorialMode.value = true` before the screen check — matching the Learn Mode `phone_ready` handler pattern

## Reference

Learn Mode equivalent:
- `LearnModeScreen.kt` line 69: `val isPhoneInLearnMode by phoneCommunicationManager.isPhoneInLearnMode.collectAsState()`
- `LearnModeScreen.kt` line 201: `!isPhoneInLearnMode -> { WaitingContent() }`
- `PhoneCommunicationManager.kt` line 332: `_isPhoneInLearnMode.value = true` (in phone_ready handler)
