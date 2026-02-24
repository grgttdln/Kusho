# Countdown Voice Audio on Watch

## Context

The watch app displays a 3-2-1 countdown before air-writing recording in Tutorial Mode, Learn Mode, and Practice Mode. The audio files `voice_3.mp3`, `voice_2.mp3`, `voice_1.mp3`, and `voice_go.mp3` already exist in the phone app's `res/raw/` but are unused. This design adds voice playback to the watch countdown.

## Decisions

- **Audio plays on watch only** (not phone)
- **Fire-and-forget timing**: audio starts at each tick; the 1-second countdown rhythm is unchanged
- **voice_go.mp3 plays at RECORDING start**: when the screen transitions to "Write now!"
- **Approach: Screen/Composable layer** using `LaunchedEffect`, matching existing watch audio patterns (e.g. PracticeModeScreen feedback audio)

## Audio Files

Copy from `app/src/main/res/raw/` to `wear/src/main/res/raw/`:
- `voice_1.mp3`
- `voice_2.mp3`
- `voice_3.mp3`
- `voice_go.mp3`

## Implementation

Add two `LaunchedEffect` blocks in each mode's Screen composable:

### Countdown Voice

```kotlin
LaunchedEffect(uiState.countdownSeconds) {
    val resId = when (uiState.countdownSeconds) {
        3 -> R.raw.voice_3
        2 -> R.raw.voice_2
        1 -> R.raw.voice_1
        else -> null
    }
    if (resId != null && uiState.state == State.COUNTDOWN) {
        val mp = MediaPlayer.create(context, resId)
        mp?.start()
        mp?.setOnCompletionListener { it.release() }
    }
}
```

### Go Voice

```kotlin
LaunchedEffect(uiState.state) {
    if (uiState.state == State.RECORDING) {
        val mp = MediaPlayer.create(context, R.raw.voice_go)
        mp?.start()
        mp?.setOnCompletionListener { it.release() }
    }
}
```

## Files Modified

| File | Change |
|------|--------|
| `wear/src/main/res/raw/` | Add 4 audio files |
| `wear/.../tutorial/TutorialModeScreen.kt` | Add countdown + go LaunchedEffect |
| `wear/.../learn/LearnModeScreen.kt` | Add countdown + go LaunchedEffect |
| `wear/.../practice/PracticeModeScreen.kt` | Add countdown + go LaunchedEffect |
