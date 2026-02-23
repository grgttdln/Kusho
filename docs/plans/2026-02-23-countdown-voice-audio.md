# Countdown Voice Audio Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Play voice_3.mp3, voice_2.mp3, voice_1.mp3, and voice_go.mp3 on the watch during the 3-2-1 countdown in Tutorial Mode, Learn Mode, and Practice Mode.

**Architecture:** Add `LaunchedEffect` blocks in each mode's Screen composable that react to `uiState.countdownSeconds` changes and `uiState.state` transitions. Fire-and-forget `MediaPlayer` playback, matching the existing watch audio pattern in PracticeModeScreen.

**Tech Stack:** Android MediaPlayer, Jetpack Compose LaunchedEffect, Kotlin

---

### Task 1: Copy audio files to wear module

**Files:**
- Copy: `app/src/main/res/raw/voice_1.mp3` → `wear/src/main/res/raw/voice_1.mp3`
- Copy: `app/src/main/res/raw/voice_2.mp3` → `wear/src/main/res/raw/voice_2.mp3`
- Copy: `app/src/main/res/raw/voice_3.mp3` → `wear/src/main/res/raw/voice_3.mp3`
- Copy: `app/src/main/res/raw/voice_go.mp3` → `wear/src/main/res/raw/voice_go.mp3`

**Step 1: Copy the 4 audio files**

```bash
cp app/src/main/res/raw/voice_1.mp3 wear/src/main/res/raw/voice_1.mp3
cp app/src/main/res/raw/voice_2.mp3 wear/src/main/res/raw/voice_2.mp3
cp app/src/main/res/raw/voice_3.mp3 wear/src/main/res/raw/voice_3.mp3
cp app/src/main/res/raw/voice_go.mp3 wear/src/main/res/raw/voice_go.mp3
```

**Step 2: Verify files exist**

```bash
ls -la wear/src/main/res/raw/voice_*.mp3
```

Expected: 4 files listed (voice_1.mp3, voice_2.mp3, voice_3.mp3, voice_go.mp3)

---

### Task 2: Add countdown voice to PracticeModeScreen

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/practice/PracticeModeScreen.kt`

This file already imports `android.media.MediaPlayer` and has `val context = LocalContext.current` at line 133 and `val uiState` at line 138.

**Step 1: Add countdown voice LaunchedEffect**

Insert after line 138 (`val uiState by viewModel.uiState.collectAsState()`), before the existing `LaunchedEffect(uiState.state, uiState.currentQuestion)` at line 141:

```kotlin
    // Play countdown voice audio (3, 2, 1)
    LaunchedEffect(uiState.countdownSeconds) {
        val resId = when (uiState.countdownSeconds) {
            3 -> R.raw.voice_3
            2 -> R.raw.voice_2
            1 -> R.raw.voice_1
            else -> null
        }
        if (resId != null && uiState.state == PracticeModeViewModel.State.COUNTDOWN) {
            val mp = MediaPlayer.create(context, resId)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }

    // Play "go" voice when recording starts
    LaunchedEffect(uiState.state) {
        if (uiState.state == PracticeModeViewModel.State.RECORDING) {
            val mp = MediaPlayer.create(context, R.raw.voice_go)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }
```

**Step 2: Verify no compile errors**

Build the wear module or check that the `R.raw.voice_*` references resolve (they will once Task 1 is done).

---

### Task 3: Add countdown voice to LearnModeScreen (FillInTheBlankContent)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`

This file does NOT import `android.media.MediaPlayer` yet.

**Step 1: Add MediaPlayer import**

Add at the top of the file, after the package declaration (line 1) and before the existing imports:

```kotlin
import android.media.MediaPlayer
```

**Step 2: Add countdown voice to FillInTheBlankContent**

`FillInTheBlankContent` has `val context` at line 382 and `val uiState` at line 482. Insert after line 482 (`val uiState by viewModel.uiState.collectAsState()`), before the existing `LaunchedEffect(Unit)` at line 486:

```kotlin
    // Play countdown voice audio (3, 2, 1)
    LaunchedEffect(uiState.countdownSeconds) {
        val resId = when (uiState.countdownSeconds) {
            3 -> R.raw.voice_3
            2 -> R.raw.voice_2
            1 -> R.raw.voice_1
            else -> null
        }
        if (resId != null && uiState.state == LearnModeViewModel.State.COUNTDOWN) {
            val mp = MediaPlayer.create(context, resId)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }

    // Play "go" voice when recording starts
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.RECORDING) {
            val mp = MediaPlayer.create(context, R.raw.voice_go)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }
```

---

### Task 4: Add countdown voice to LearnModeScreen (WriteTheWordContent)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`

`WriteTheWordContent` has `val context` at line 725 and `val uiState` at line 839.

**Step 1: Add countdown voice LaunchedEffect**

Insert after line 839 (`val uiState by viewModel.uiState.collectAsState()`), before the existing `var lastSpokenPrediction` at line 842:

```kotlin
    // Play countdown voice audio (3, 2, 1)
    LaunchedEffect(uiState.countdownSeconds) {
        val resId = when (uiState.countdownSeconds) {
            3 -> R.raw.voice_3
            2 -> R.raw.voice_2
            1 -> R.raw.voice_1
            else -> null
        }
        if (resId != null && uiState.state == LearnModeViewModel.State.COUNTDOWN) {
            val mp = MediaPlayer.create(context, resId)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }

    // Play "go" voice when recording starts
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.RECORDING) {
            val mp = MediaPlayer.create(context, R.raw.voice_go)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }
```

---

### Task 5: Add countdown voice to LearnModeScreen (NameThePictureContent)

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt`

`NameThePictureContent` has `val context` at line 1158 and `val uiState` at line 1272.

**Step 1: Add countdown voice LaunchedEffect**

Insert after line 1272 (`val uiState by viewModel.uiState.collectAsState()`), before the existing `var lastSpokenPrediction` at line 1275:

```kotlin
    // Play countdown voice audio (3, 2, 1)
    LaunchedEffect(uiState.countdownSeconds) {
        val resId = when (uiState.countdownSeconds) {
            3 -> R.raw.voice_3
            2 -> R.raw.voice_2
            1 -> R.raw.voice_1
            else -> null
        }
        if (resId != null && uiState.state == LearnModeViewModel.State.COUNTDOWN) {
            val mp = MediaPlayer.create(context, resId)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }

    // Play "go" voice when recording starts
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.RECORDING) {
            val mp = MediaPlayer.create(context, R.raw.voice_go)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }
```

---

### Task 6: Add countdown voice to TutorialModeScreen

**Files:**
- Modify: `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeScreen.kt`

This file does NOT import `android.media.MediaPlayer`. The `GestureRecognitionContent` composable (line 418) has `val uiState` at line 449 but no local `context` variable — `context` is only at line 51 in the top-level `TutorialModeScreen`.

**Step 1: Add MediaPlayer import**

Add after `import android.util.Log` (line 3):

```kotlin
import android.media.MediaPlayer
```

**Step 2: Add context to GestureRecognitionContent**

Insert after line 449 (`val uiState by viewModel.uiState.collectAsState()`):

```kotlin
    val context = LocalContext.current
```

**Step 3: Add countdown voice LaunchedEffect**

Insert after the new `val context` line, before the existing `LaunchedEffect(uiState.state, uiState.prediction, uiState.isCorrect)` at line 452:

```kotlin
    // Play countdown voice audio (3, 2, 1)
    LaunchedEffect(uiState.countdownSeconds) {
        val resId = when (uiState.countdownSeconds) {
            3 -> R.raw.voice_3
            2 -> R.raw.voice_2
            1 -> R.raw.voice_1
            else -> null
        }
        if (resId != null && uiState.state == TutorialModeViewModel.State.COUNTDOWN) {
            val mp = MediaPlayer.create(context, resId)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }

    // Play "go" voice when recording starts
    LaunchedEffect(uiState.state) {
        if (uiState.state == TutorialModeViewModel.State.RECORDING) {
            val mp = MediaPlayer.create(context, R.raw.voice_go)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }
```

---

### Task 7: Manual test on device

**Test each mode on the watch:**

1. **Practice Mode**: Start a practice question → tap to answer → verify you hear "three", "two", "one", "go!" during countdown
2. **Learn Mode (Fill in the Blank)**: Start a session → tap to begin → verify countdown audio plays
3. **Learn Mode (Write the Word)**: Start a write-the-word session → verify countdown audio plays for each letter
4. **Learn Mode (Name the Picture)**: Start a name-the-picture session → verify countdown audio plays
5. **Tutorial Mode**: Start a tutorial session → tap to begin → verify countdown audio plays before each air-writing attempt

**Verify:**
- Audio plays at the right time (3 with number 3, etc.)
- Audio does not interrupt or delay the visual countdown
- "Go" plays when recording starts
- No audio plays during non-countdown states
