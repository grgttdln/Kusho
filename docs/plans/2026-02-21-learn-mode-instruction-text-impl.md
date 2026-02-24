# Learn Mode Instruction Text Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a dynamic instruction subtitle to Learn Mode that displays the TTS phrase text with a pulsing animation during audio playback, matching Tutorial Mode's voice prompt pattern.

**Architecture:** Add state tracking (`isTtsPlaying`, `currentInstructionText`) to LearnModeSessionScreen. Generate the instruction phrase once per word change using the existing `getRandomPhrase()` from TTS managers, display it below the activity type subtitle, and pass the same phrase to TTS so text and audio match. Pulse the text with alpha/color animation during TTS playback.

**Tech Stack:** Jetpack Compose (animation APIs: `rememberInfiniteTransition`, `animateColorAsState`), existing DeepgramTTSManager/TextToSpeechManager

---

### Task 1: Add isTtsPlaying state and instruction text state

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:216-217`

**Step 1: Add imports for animation APIs**

Add these imports after the existing animation imports at line 9:

```kotlin
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
```

**Step 2: Add state variables**

After the `isWatchReady` state variable (line 217), add:

```kotlin
    // State for TTS instruction text and playback animation
    var isTtsPlaying by remember(sessionKey) { mutableStateOf(false) }
    var currentInstructionText by remember(sessionKey) { mutableStateOf("") }
```

**Step 3: Commit**

```
feat(learn-mode): add isTtsPlaying and instruction text state variables
```

---

### Task 2: Generate instruction phrase and wire TTS callbacks

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:586-605`

**Step 1: Replace the TTS launch block**

In the `LaunchedEffect(currentWordIndex, words, isWatchReady)` block (around line 586-605), replace the TTS section with phrase generation + callbacks:

Replace from `// Speak random phrase based on question type` through the closing `}` of the `launch { ... }` block (lines 586-605) with:

```kotlin
            // Generate instruction text and speak it with TTS
            val wordForPhrase = if (currentWord.configurationType == "Name the Picture") null else currentWord.word
            val phrase = if (useDeepgram) {
                deepgramTtsManager.getRandomPhrase(currentWord.configurationType, wordForPhrase)
            } else {
                nativeTtsManager.getRandomPhrase(currentWord.configurationType, wordForPhrase)
            }
            currentInstructionText = phrase

            launch {
                try {
                    isTtsPlaying = true
                    if (useDeepgram) {
                        Log.d("LearnModeSession", "Using Deepgram TTS")
                        deepgramTtsManager.speak(phrase) {
                            isTtsPlaying = false
                        }
                    } else {
                        Log.d("LearnModeSession", "Using Native TTS")
                        nativeTtsManager.speak(phrase) {
                            isTtsPlaying = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LearnModeSession", "Error playing TTS", e)
                    isTtsPlaying = false
                }
            }
```

Note: We call `getRandomPhrase()` first to capture the text, then call `speak()` directly (not `speakRandomPhrase()`) so we speak the exact same phrase we display. Both managers have a `speak(text, onComplete)` method.

**Step 2: Verify speak() method exists on both managers**

Confirm `DeepgramTTSManager.speak(text: String, onComplete: (() -> Unit)? = null)` exists (it does — `speakRandomPhrase` calls it internally at line 270).
Confirm `TextToSpeechManager.speak(text: String, onComplete: (() -> Unit)? = null)` exists (it does — `speakRandomPhrase` calls it at line 163).

**Step 3: Commit**

```
feat(learn-mode): generate instruction phrase and track TTS playback state
```

---

### Task 3: Add instruction subtitle UI with pulsing animation

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1117-1125`

**Step 1: Add instruction text with pulsing animation**

After the activity type subtitle Text (line 1122-1123) and before the Spacer (line 1125), insert:

```kotlin
        // Instruction text with pulsing animation during TTS playback
        Spacer(Modifier.height(4.dp))

        val subtitleAlpha = if (isTtsPlaying) {
            val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "subtitlePulse"
            ).value
        } else {
            1f
        }
        val subtitleColor = animateColorAsState(
            targetValue = if (isTtsPlaying) PurpleColor else Color.Gray,
            animationSpec = tween(300),
            label = "subtitleColor"
        ).value

        Text(
            text = currentInstructionText,
            fontSize = 16.sp,
            fontWeight = if (isTtsPlaying) FontWeight.SemiBold else FontWeight.Medium,
            color = subtitleColor.copy(alpha = subtitleAlpha),
            textAlign = TextAlign.Center
        )
```

This goes between line 1123 (`color = PurpleColor`) closing paren and line 1125 (`Spacer(Modifier.height(24.dp))`).

**Step 2: Commit**

```
feat(learn-mode): add instruction subtitle with pulsing animation during TTS
```

---

### Task 4: Verify and handle edge case — reset isTtsPlaying on word skip

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

**Step 1: Ensure TTS stops on word change**

The existing `LaunchedEffect(currentWordIndex, ...)` already re-runs when `currentWordIndex` changes (e.g., on skip). Since we set `isTtsPlaying = true` at the start and the new TTS call will replace the old one, verify that:
- `deepgramTtsManager.stop()` or `nativeTtsManager.stop()` is called before speaking the new phrase, OR
- The TTS managers naturally handle overlapping calls

Check if TTS managers handle this. If `speak()` doesn't stop previous playback, add a stop call before starting new TTS. Add at the beginning of the TTS section (before the `val wordForPhrase` line):

```kotlin
            // Stop any ongoing TTS before starting new phrase
            if (useDeepgram) {
                deepgramTtsManager.stop()
            } else {
                nativeTtsManager.stop()
            }
            isTtsPlaying = false
```

**Step 2: Commit**

```
fix(learn-mode): stop previous TTS before starting new instruction phrase
```

---

## Summary of All Changes

**Single file modified:** `LearnModeSessionScreen.kt`

1. **Imports** — 4 new animation imports
2. **State** — 2 new state variables (`isTtsPlaying`, `currentInstructionText`)
3. **TTS block** — Replaced `speakRandomPhrase()` call with `getRandomPhrase()` + `speak()` + callbacks
4. **UI** — ~25 lines of instruction subtitle + pulsing animation inserted after activity type text
5. **Edge case** — TTS stop on word change
