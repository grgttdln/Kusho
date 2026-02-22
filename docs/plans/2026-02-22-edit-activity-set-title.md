# Edit Activity Set Title - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Allow users to edit the Activity Set title inline on the ActivitySetsScreen via an always-visible OutlinedTextField.

**Architecture:** The ActivitySetsViewModel loads the Activity entity, owns the editable title state, and saves via the existing `ActivityRepository.updateActivity()`. The ActivitySetsScreen replaces static title text with an OutlinedTextField and a conditional Save button. A callback propagates the updated title to MainNavigationContainer.

**Tech Stack:** Kotlin, Jetpack Compose, Room, AndroidViewModel, StateFlow

---

### Task 1: Expand ActivitySetsUiState with title fields

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/activities/ActivitySetsViewModel.kt:19-23`

**Step 1: Add title-related fields to the data class**

Replace the existing `ActivitySetsUiState` (lines 19-23) with:

```kotlin
data class ActivitySetsUiState(
    val sets: List<Set> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val editableTitle: String = "",
    val originalTitle: String = "",
    val titleError: String? = null,
    val isSaving: Boolean = false,
    val titleSaved: Boolean = false
)
```

**Step 2: Verify the file compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/activities/ActivitySetsViewModel.kt
git commit -m "feat: add title editing fields to ActivitySetsUiState"
```

---

### Task 2: Add ActivityRepository and title methods to ActivitySetsViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/activities/ActivitySetsViewModel.kt`

**Step 1: Add the ActivityRepository import and field**

Add import after line 8 (`import com.example.app.data.repository.SetRepository`):

```kotlin
import com.example.app.data.repository.ActivityRepository
```

Add a new field after line 33 (`private val setRepository = SetRepository(database)`):

```kotlin
private val activityRepository = ActivityRepository(database.activityDao())
```

**Step 2: Add `loadActivity()` method**

Add after the `currentActivityId` field (after line 39):

```kotlin
fun loadActivity(activityId: Long) {
    viewModelScope.launch {
        try {
            val result = activityRepository.getActivityById(activityId)
            result.onSuccess { activity ->
                _uiState.update {
                    it.copy(
                        editableTitle = activity.title,
                        originalTitle = activity.title,
                        titleError = null,
                        titleSaved = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to load activity")
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = e.message ?: "Failed to load activity")
            }
        }
    }
}
```

**Step 3: Add `onTitleChanged()` method**

Add after the `loadActivity` method:

```kotlin
fun onTitleChanged(newTitle: String) {
    if (newTitle.length <= 30) {
        _uiState.update {
            it.copy(
                editableTitle = newTitle,
                titleError = null,
                titleSaved = false
            )
        }
    }
}
```

**Step 4: Add `saveTitle()` method**

Add after the `onTitleChanged` method:

```kotlin
fun saveTitle(onTitleUpdated: (String) -> Unit) {
    val state = _uiState.value
    val newTitle = state.editableTitle.trim()

    if (newTitle.isBlank()) {
        _uiState.update { it.copy(titleError = "Title cannot be empty") }
        return
    }

    if (newTitle == state.originalTitle) {
        return
    }

    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true, titleError = null) }
        val activityId = currentActivityId ?: return@launch
        val result = activityRepository.updateActivity(
            activityId = activityId,
            title = newTitle
        )
        result.onSuccess {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    originalTitle = newTitle,
                    titleSaved = true,
                    titleError = null
                )
            }
            onTitleUpdated(newTitle)
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isSaving = false,
                    titleError = e.message ?: "Failed to update title"
                )
            }
        }
    }
}
```

**Step 5: Verify the file compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/activities/ActivitySetsViewModel.kt
git commit -m "feat: add loadActivity, onTitleChanged, saveTitle to ActivitySetsViewModel"
```

---

### Task 3: Update ActivitySetsScreen UI with editable title

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/activities/ActivitySetsScreen.kt`

**Step 1: Add new imports**

Add these imports after the existing imports (after line 33):

```kotlin
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
```

**Step 2: Add `onTitleUpdated` callback parameter**

In the `ActivitySetsScreen` composable signature (lines 41-49), add a new parameter after `onViewSetClick`:

```kotlin
onTitleUpdated: (String) -> Unit = {},
```

So the full signature becomes:

```kotlin
@Composable
fun ActivitySetsScreen(
    activityId: Long,
    activityTitle: String,
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onAddSetClick: () -> Unit = {},
    onViewSetClick: (Long) -> Unit = {},
    onTitleUpdated: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ActivitySetsViewModel = viewModel()
)
```

**Step 3: Add `loadActivity` call to LaunchedEffect**

Update the LaunchedEffect block (lines 54-56) to also load the activity:

```kotlin
LaunchedEffect(activityId) {
    viewModel.loadActivity(activityId)
    viewModel.loadSetsForActivity(activityId)
}
```

**Step 4: Replace static title Text with editable OutlinedTextField**

Replace lines 119-125 (the title section):

```kotlin
            // Title - Activity Set name + "Activities"
            Text(
                text = "$activityTitle Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )
```

With:

```kotlin
            // Editable Activity Set title
            OutlinedTextField(
                value = uiState.editableTitle,
                onValueChange = { viewModel.onTitleChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0B0B)
                ),
                placeholder = {
                    Text(
                        text = "Activity Set Title",
                        fontSize = 22.sp,
                        color = Color(0xFFC5E5FD),
                        fontWeight = FontWeight.Bold
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color(0x803FA9F8),
                    focusedIndicatorColor = Color(0xFF3FA9F8),
                    focusedTextColor = Color(0xFF000000),
                    unfocusedTextColor = Color(0xFF000000)
                ),
                singleLine = true,
                isError = uiState.titleError != null
            )

            // Error message
            if (uiState.titleError != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.titleError!!,
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // "Activities" label
            Text(
                text = "Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )

            // Save button - only visible when title has changed
            if (uiState.editableTitle.trim() != uiState.originalTitle && uiState.editableTitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveTitle { newTitle -> onTitleUpdated(newTitle) } },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .padding(horizontal = 15.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3FA9F8)
                    )
                ) {
                    Text(
                        text = if (uiState.isSaving) "Saving..." else "Save Title",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
```

**Step 5: Verify the file compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/activities/ActivitySetsScreen.kt
git commit -m "feat: replace static title with editable OutlinedTextField on ActivitySetsScreen"
```

---

### Task 4: Wire onTitleUpdated callback in MainNavigationContainer

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt:392-406`

**Step 1: Add `onTitleUpdated` callback to the ActivitySetsScreen call**

In the `MainNavigationContainer`, find the `ActivitySetsScreen` call site (around lines 394-405) and add the `onTitleUpdated` parameter:

```kotlin
        16 -> {
            key(yourSetsScreenKey) {
                ActivitySetsScreen(
                    activityId = selectedActivityId,
                    activityTitle = selectedActivityTitle,
                    onNavigate = { currentScreen = it },
                    onBackClick = { currentScreen = 6 },
                    onAddSetClick = { currentScreen = 17 },
                    onViewSetClick = { setId ->
                        selectedSetId = setId
                        currentScreen = 14
                    },
                    onTitleUpdated = { newTitle ->
                        selectedActivityTitle = newTitle
                    },
                    modifier = modifier
                )
            }
        }
```

**Step 2: Verify the file compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt
git commit -m "feat: wire onTitleUpdated callback in MainNavigationContainer"
```

---

### Task 5: Build and verify end-to-end

**Step 1: Full project build**

Run: `./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 2: Commit (if any fixups needed)**

If compilation fixes were needed:

```bash
git add -A
git commit -m "fix: resolve compilation issues from title editing feature"
```
