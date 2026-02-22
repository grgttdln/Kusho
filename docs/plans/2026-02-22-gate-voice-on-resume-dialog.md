# Gate Voice Playback on Resume Dialog — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Prevent voice playback from starting until the user confirms Resume or Restart in the session dialog.

**Architecture:** Add `showResumeDialog` as a `LaunchedEffect` key and early-return guard in both Learn Mode and Tutorial Mode voice playback blocks. When the dialog is dismissed, the state change re-triggers the effect and voice plays normally.

**Tech Stack:** Kotlin, Jetpack Compose (`LaunchedEffect`), Android TTS / MediaPlayer

---

### Task 1: Fix Learn Mode voice playback gating

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:584-585`

**Step 1: Edit the voice playback LaunchedEffect**

Change line 584-585 from:

```kotlin
    LaunchedEffect(currentWordIndex, words, isWatchReady) {
        if (!isWatchReady) return@LaunchedEffect
```

To:

```kotlin
    LaunchedEffect(currentWordIndex, words, isWatchReady, showResumeDialog) {
        if (!isWatchReady || showResumeDialog) return@LaunchedEffect
```

**Step 2: Verify the build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Manual verification**

1. Open Learn Mode with a student that has prior progress
2. Confirm the resume/restart dialog appears WITHOUT voice playing
3. Tap Resume → voice plays for the resumed word
4. Repeat and tap Restart → voice plays for the first word
5. Start a fresh session (no prior progress) → voice plays normally on first word

---

### Task 2: Fix Tutorial Mode voice playback gating

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt:429-430`

**Step 1: Edit the voice playback LaunchedEffect**

Change line 429-430 from:

```kotlin
    LaunchedEffect(currentStep, currentLetter, isWatchReady) {
        if (currentLetter.isNotEmpty() && isWatchReady) {
```

To:

```kotlin
    LaunchedEffect(currentStep, currentLetter, isWatchReady, showResumeDialog) {
        if (currentLetter.isNotEmpty() && isWatchReady && !showResumeDialog) {
```

**Step 2: Verify the build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Manual verification**

1. Open Tutorial Mode with a student that has prior progress
2. Confirm the resume/restart dialog appears WITHOUT voice playing
3. Tap Resume → voice plays for the resumed letter
4. Repeat and tap Restart → voice plays for the first letter
5. Start a fresh tutorial (no prior progress) → voice plays normally on first letter

---

### Task 3: Commit

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt
git commit -m "fix: gate voice playback on resume dialog in learn and tutorial modes"
```
