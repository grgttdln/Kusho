# Fill in the Blanks — Picture UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** When Fill in the Blank has a picture, render it using the Name the Picture layout (big standalone image card + word below) instead of the card-wrapped layout.

**Architecture:** Hoist `imageExists` above the layout branch point, expand the branch condition to route Fill in the Blank with image into the Name the Picture layout branch, and conditionally render `FillInTheBlankDisplay` vs `NameThePictureDisplay` in the word display section.

**Tech Stack:** Kotlin, Jetpack Compose

---

### Task 1: Hoist `imageExists` and update the branch condition

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1111-1114`

**Step 1: Add `imageExists` and `isFillInBlankWithImage` before the branch**

At line 1111, the current code is:

```kotlin
        val isNameThePicture = currentWord?.configurationType == "Name the Picture"

        // Large Content Card (purple border) - hidden for Name the Picture mode
        if (!isNameThePicture) {
```

Replace with:

```kotlin
        val isNameThePicture = currentWord?.configurationType == "Name the Picture"
        val isFillInTheBlank = currentWord?.configurationType == "Fill in the Blank"
        val imageExistsForBranch = currentWord?.imagePath?.let { File(it).exists() } ?: false
        val isFillInBlankWithImage = isFillInTheBlank == true && imageExistsForBranch

        // Large Content Card (purple border) - hidden for Name the Picture mode and Fill in the Blank with image
        if (!isNameThePicture && !isFillInBlankWithImage) {
```

Note: The existing `imageExists` variables inside each branch (line 1180 and line 1293) can stay — they are used locally and won't conflict. The new `imageExistsForBranch` is only used for the branch decision.

**Step 2: Update the else branch comment**

At line 1283, update the comment:

```kotlin
        } else {
            // Name the Picture / Fill in the Blank with image - no card wrapper, just the content with weight
```

---

### Task 2: Conditionally render word display in the else branch

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:1374-1386`

**Step 1: Replace the word display section**

Current code at lines 1374-1386:

```kotlin
                // Word display
                if (currentWord != null) {
                    NameThePictureDisplay(
                        word = currentWord.word,
                        completedIndices = completedLetterIndices,
                        currentIndex = currentLetterIndex,
                        hasImage = imageExists,
                        wrongLetterText = wrongLetterText,
                        wrongLetterAnimationActive = wrongLetterAnimationActive,
                        wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                        wrongLetterAlpha = wrongLetterAlpha.value
                    )
                }
```

Replace with:

```kotlin
                // Word display - use FillInTheBlankDisplay for Fill in the Blank, NameThePictureDisplay otherwise
                if (currentWord != null) {
                    val isFillInTheBlankHere = currentWord.configurationType == "Fill in the Blank"
                    if (isFillInTheBlankHere) {
                        FillInTheBlankDisplay(
                            word = currentWord.word,
                            maskedIndex = currentWord.selectedLetterIndex,
                            isCorrect = fillInBlankCorrect,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
                    } else {
                        NameThePictureDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
                    }
                }
```

---

### Task 3: Verify and commit

**Step 1: Build the project**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt
git commit -m "feat: use Name the Picture layout for Fill in the Blank with image"
```
