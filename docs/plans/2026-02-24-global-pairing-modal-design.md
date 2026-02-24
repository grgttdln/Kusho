# Global Pairing Modal Design

## Problem

The Watch Pairing Request Modal and Pairing Success Modal currently live inside `DashboardScreen.kt`. When a watch sends a pairing request while the user is on any other screen, the modal is not visible.

## Solution

Extract both modals into a `GlobalPairingModals` composable placed at the Activity root, so they render on top of any screen.

## Approach

Top-level composable wrapper in `MainActivity`.

## Component

**New file:** `ui/components/pairing/GlobalPairingModals.kt`

A `@Composable fun GlobalPairingModals()` that:

- Gets `WatchConnectionManager.getInstance(context)` internally
- Collects `watchConnectionManager.pairingRequest` as state
- Manages local state for `showPairingSuccessModal` and `pairedWatchName`
- Renders the Pairing Request Dialog when `pairingRequest != null`
- Renders the Pairing Success Dialog after accept
- Calls `watchConnectionManager.acceptPairing()` / `declinePairing()` directly

Modal UI is moved as-is from `DashboardScreen.kt` (lines 246-471) with no visual changes.

## Integration Points

### MainActivity.kt

Add `GlobalPairingModals()` inside `setContent{}`, alongside the `Scaffold` inside a `Box`:

```kotlin
KushoTheme {
    Box {
        Scaffold { ... AppNavigation(...) }
        GlobalPairingModals()
    }
}
```

### DashboardScreen.kt

Remove:

- `pairingRequest` state collection (line 87)
- `showPairingSuccessModal` / `pairedWatchName` state variables (lines 85-86)
- Pairing Request Dialog block (lines 246-371)
- Pairing Success Modal block (lines 374-471)

Keep:

- Device Card's visual reaction to `pairingRequest` (amber tint, "Waiting to pair..." text) via `DashboardViewModel.pairingRequest`

### DashboardViewModel.kt

No changes. Keep `pairingRequest`, `acceptPairingRequest()`, `declinePairingRequest()` for Device Card use.

## Data Flow

```
Watch sends /pairing_request
        |
WatchConnectionManager._pairingRequest updated
        |
    +---------------------------+----------------------------+
    |                           |                            |
GlobalPairingModals()     DashboardViewModel            (future)
collects StateFlow        exposes StateFlow
    |                           |
Renders modal on          Device Card shows
ANY screen                amber "Waiting" state
    |
User taps Accept/Decline
    |
WatchConnectionManager.acceptPairing() / declinePairing()
    |
_pairingRequest set to null -> modal dismisses
    |
If accepted -> GlobalPairingModals shows success modal
```
