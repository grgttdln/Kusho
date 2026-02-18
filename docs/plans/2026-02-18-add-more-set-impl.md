# Add More Set Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the "Add More Set" button generate one additional set and append it to the existing sets without regenerating everything.

**Architecture:** Reuse the existing `regenerateSet` pipeline in LessonViewModel/GeminiRepository. Pass all current set titles+descriptions as avoidance context so the AI generates something different. Wire the result through MainNavigationContainer to parse, detect overlaps/similarity, and append to the editable sets list.

**Tech Stack:** Kotlin, Jetpack Compose, Gemini SDK, Gson

---

### Task 1: Add `addMoreSet()` to LessonViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonViewModel.kt:675` (after `regenerateSet()`)

**Step 1: Add the function**

Add this function right after `regenerateSet()` (before the closing `}` of the class at line 675):

```kotlin
/**
 * Generate one additional set to append to the existing generated sets.
 * Reuses the regenerateSet pipeline, passing all existing titles as avoidance context.
 */
fun addMoreSet(
    existingSetTitles: List<String>,
    existingSetDescriptions: List<String>,
    onResult: (String?) -> Unit
) {
    if (lastSelectedWords.isEmpty()) {
        onResult(null)
        return
    }

    // Use the first existing set's title/description as the "current" set to avoid,
    // and all existing titles feed into the existingSetTitles avoidance list
    val avoidTitle = existingSetTitles.joinToString(", ").take(50)
    val avoidDescription = existingSetDescriptions.joinToString("; ").take(100)

    viewModelScope.launch {
        val allTitlesToAvoid = (cachedExistingSetTitles + existingSetTitles).distinct()

        val result = geminiRepository.regenerateSet(
            prompt = lastGenerationPrompt,
            availableWords = lastSelectedWords,
            currentSetTitle = avoidTitle,
            currentSetDescription = avoidDescription,
            existingSetTitles = allTitlesToAvoid,
            onPhaseChange = { phase ->
                _generationPhase.value = phase
            }
        )

        when (result) {
            is AiGenerationResult.Success -> {
                _generationPhase.value = GenerationPhase.Idle
                val gson = com.google.gson.Gson()
                onResult(gson.toJson(result.data))
            }
            is AiGenerationResult.Error -> {
                _generationPhase.value = GenerationPhase.Idle
                onResult(null)
            }
        }
    }
}
```

**Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add `onAddMoreSet` Callback and Loading State to AISetReviewScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/generate/AISetReviewScreen.kt`

**Step 1: Add `onAddMoreSet` parameter to `AISetReviewScreen` function signature**

At line 60, after the `onRegenerateSet` parameter, add:

```kotlin
onAddMoreSet: (existingTitles: List<String>, existingDescriptions: List<String>, onResult: (String?) -> Unit) -> Unit = { _, _, _ -> },
```

The full parameter list section (lines 60-65) becomes:

```kotlin
    onRegenerateSet: (currentSetTitle: String, currentSetDescription: String, onResult: (String?) -> Unit) -> Unit = { _, _, _ -> },
    onAddMoreSet: (existingTitles: List<String>, existingDescriptions: List<String>, onResult: (String?) -> Unit) -> Unit = { _, _, _ -> },
    onDiscardSet: () -> Unit = {},
    onAddWordsClick: (existingWords: List<String>) -> Unit = {},
    additionalWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    existingSetTitleMap: Map<String, Long> = emptyMap(),
    modifier: Modifier = Modifier
```

**Step 2: Add `isAddingMoreSet` state variable**

After the existing `var isRegenerating` state (around line 82), add:

```kotlin
var isAddingMoreSet by remember { mutableStateOf(false) }
var addMoreSetError by remember { mutableStateOf<String?>(null) }
```

**Step 3: Wire the `onAddMoreSetClick` callback**

Replace the TODO block at line 278-280:

```kotlin
onAddMoreSetClick = {
    // TODO: Add logic to generate an additional set
},
```

With:

```kotlin
onAddMoreSetClick = {
    isAddingMoreSet = true
    addMoreSetError = null
    val titles = editableSets.map { it.title }
    val descriptions = editableSets.map { it.description }
    onAddMoreSet(titles, descriptions) { newJson ->
        isAddingMoreSet = false
        if (newJson == null) {
            addMoreSetError = "Failed to generate set. Please try again."
            return@onAddMoreSet
        }
        try {
            val newActivity = Gson().fromJson(newJson, AiGeneratedActivity::class.java)
            val wordsWithImages = editableSets
                .flatMap { it.words }
                .filter { it.hasImage }
                .map { it.word }
                .toSet()
            newActivity?.sets?.firstOrNull()?.let { newSet ->
                // Resolve title similarity
                val newTitleSimilarityMatch = newSet.titleSimilarity?.let { sim ->
                    val matchKey = existingSetTitleMap.keys.firstOrNull {
                        it.equals(sim.similarToExisting, ignoreCase = true)
                    }
                    if (matchKey != null) {
                        TitleSimilarityInfo(
                            existingTitle = matchKey,
                            existingId = existingSetTitleMap[matchKey] ?: 0L,
                            reason = sim.reason
                        )
                    } else null
                }

                val newEditableSet = EditableSet(
                    title = newSet.title,
                    description = newSet.description,
                    words = newSet.words.map { word ->
                        EditableWord(
                            word = word.word,
                            configurationType = mapAiConfigTypeToUi(word.configurationType),
                            selectedLetterIndex = word.selectedLetterIndex,
                            hasImage = word.word in wordsWithImages
                        )
                    },
                    titleSimilarityMatch = newTitleSimilarityMatch
                )

                // Append the new set
                onEditableSetsChange(editableSets + newEditableSet)
            }
        } catch (e: Exception) {
            addMoreSetError = "Failed to parse generated set"
        }
    }
},
```

**Step 4: Update the "Add More Set" button to show loading state and disable while generating**

At line 969 in `SetReviewCard`, the Add More Set button. Change:

```kotlin
// Add More Set Button (outline style)
Button(
    onClick = onAddMoreSetClick,
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.White
    ),
    border = BorderStroke(1.5.dp, Color(0xFF3FA9F8))
) {
    Text(
        text = "Add More Set",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF3FA9F8)
    )
}
```

To:

```kotlin
// Add More Set Button (outline style)
Button(
    onClick = onAddMoreSetClick,
    enabled = !isAddingMoreSet && !isSaving,
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        disabledContainerColor = Color.White
    ),
    border = BorderStroke(1.5.dp, if (isAddingMoreSet || isSaving) Color(0xFFB0BEC5) else Color(0xFF3FA9F8))
) {
    if (isAddingMoreSet) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color(0xFF3FA9F8),
            strokeWidth = 2.dp
        )
    } else {
        Text(
            text = "Add More Set",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSaving) Color(0xFFB0BEC5) else Color(0xFF3FA9F8)
        )
    }
}
```

Note: The `isAddingMoreSet` and `isRegenerating` states live in `AISetReviewScreen` (the parent composable), not in `SetReviewCard`. You need to pass `isAddingMoreSet` down to `SetReviewCard` as a new parameter:

Add to `SetReviewCard` function signature (around line 438):

```kotlin
isAddingMoreSet: Boolean = false,
```

And pass it in the call site (around line 204):

```kotlin
isAddingMoreSet = isAddingMoreSet,
```

**Step 5: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 3: Wire the Callback in MainNavigationContainer

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt`

**Step 1: Add `onAddMoreSet` to the AISetReviewScreen call**

In the screen 48 block (around line 808), after the `onRegenerateSet` callback, add:

```kotlin
onAddMoreSet = { existingTitles, existingDescriptions, onResult ->
    lessonViewModel.addMoreSet(existingTitles, existingDescriptions, onResult)
},
```

The section should look like:

```kotlin
onRegenerateSet = { setTitle, setDescription, onResult ->
    lessonViewModel.regenerateSet(setTitle, setDescription, onResult)
},
onAddMoreSet = { existingTitles, existingDescriptions, onResult ->
    lessonViewModel.addMoreSet(existingTitles, existingDescriptions, onResult)
},
onDiscardSet = {
```

**Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 4: Run Overlap Detection on New Set

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/generate/AISetReviewScreen.kt`

The overlap detection currently only runs at initial parse time in MainNavigationContainer. For the "Add More Set" flow, we need to check the new set for overlaps too.

**Step 1: Add overlap detection in the `onAddMoreSetClick` callback**

In the `onAddMoreSetClick` handler (added in Task 2 Step 3), after building `newEditableSet` and before appending it, add overlap detection:

Replace:

```kotlin
// Append the new set
onEditableSetsChange(editableSets + newEditableSet)
```

With:

```kotlin
// Run overlap detection on just the new set
coroutineScope.launch {
    var finalSet = newEditableSet
    try {
        val newSetWords = listOf(newEditableSet.words.map { it.word })
        val overlaps = setRepository.findOverlappingSets(userId, newSetWords)
        val match = overlaps[0]
        if (match != null) {
            finalSet = newEditableSet.copy(overlapMatch = match)
        }
    } catch (e: Exception) {
        android.util.Log.e("AISetReview", "Overlap detection failed for new set", e)
    }
    onEditableSetsChange(editableSets + finalSet)
}
```

Note: The `coroutineScope`, `setRepository`, and `userId` are already available in the `AISetReviewScreen` composable scope (lines 69-70 and parameter line 55).

However, since we're inside the `SetReviewCard` callback which is called from the composable, we need to make sure the coroutine launch happens in the right scope. The `onAddMoreSetClick` callback in the `AISetReviewScreen` composable (NOT `SetReviewCard`) already has access to `coroutineScope`. Move the overlap detection to be inside the `onAddMoreSet` result callback in the `AISetReviewScreen` composable, not in `SetReviewCard`.

**Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 5: Final Build and Smoke Test

**Step 1: Clean build**

Run: `./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Verification checklist**

- [ ] `LessonViewModel.addMoreSet()` exists and calls `geminiRepository.regenerateSet()` with combined avoidance titles
- [ ] `AISetReviewScreen` has `onAddMoreSet` parameter
- [ ] `isAddingMoreSet` state controls button loading/disabled state
- [ ] "Add More Set" button shows spinner while generating
- [ ] Both "Add More Set" and "Proceed" buttons disabled during generation
- [ ] New set is appended to `editableSets` (not replacing existing)
- [ ] Overlap detection runs on the new set
- [ ] Title similarity is resolved on the new set
- [ ] `MainNavigationContainer` wires `lessonViewModel.addMoreSet()` to the callback
- [ ] User stays on current set index after adding (doesn't auto-navigate)
