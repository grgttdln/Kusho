# LessonScreen Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Convert LessonScreen into an "Air Writing Activities" navigation hub with 3 full-width tiles, and extract all Word Bank functionality into a new standalone WordBankScreen.

**Architecture:** LessonScreen becomes a stateless hub (no ViewModel) with 3 ModeCard-style tiles navigating to WordBankScreen (new, screen 52), YourActivitiesScreen (screen 6), and YourSetsScreen (screen 7). WordBankScreen inherits all word bank UI and modals from the old LessonScreen, reusing the existing LessonViewModel which is already hoisted in MainNavigationContainer.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, existing LessonViewModel

---

### Task 1: Create WordBankScreen.kt

**Files:**
- Create: `app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt`

**Step 1: Create the WordBankScreen composable**

Create the file with all Word Bank functionality extracted from LessonScreen. The layout follows the YourActivitiesScreen pattern (back button + centered logo header, no edit button).

```kotlin
package com.example.app.ui.feature.learn

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.wordbank.ActivityCreationModal
import com.example.app.ui.components.wordbank.WordAddedConfirmationModal
import com.example.app.ui.components.wordbank.WordBankEditModal
import com.example.app.ui.components.wordbank.WordBankModal

@Composable
fun WordBankScreen(
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToAIGenerate: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LessonViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val generationPhase by viewModel.generationPhase.collectAsState()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onMediaSelected(uri)
    }

    // Image picker launcher for edit modal
    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onEditMediaSelected(uri)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header: Back Button + Kusho Logo (centered)
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Button (left)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF3FA9F8)
                    )
                }

                // Kusho Logo (centered)
                Image(
                    painter = painterResource(id = R.drawable.ic_kusho),
                    contentDescription = "Kusho Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .offset(x = 10.dp)
                        .align(Alignment.Center),
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Word Bank",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Word Bank List (scrollable grid)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WordBankList(
                    words = uiState.words,
                    onWordClick = { word ->
                        viewModel.onWordClick(word)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Action Buttons: "+ Word Bank" and Magic Wand
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddWordBankButton(
                    onClick = {
                        viewModel.showWordBankModal()
                    }
                )

                Spacer(Modifier.width(12.dp))

                // Magic Wand Button
                IconButton(
                    onClick = { viewModel.showActivityCreationModal() },
                    modifier = Modifier
                        .size(75.dp)
                        .background(Color(0xFF3FA9F8), RoundedCornerShape(37.5.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_wand),
                        contentDescription = "Magic Wand",
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Word Bank Modal
        WordBankModal(
            isVisible = uiState.isModalVisible,
            wordInput = uiState.wordInput,
            selectedImageUri = uiState.selectedMediaUri,
            inputError = uiState.inputError,
            imageError = uiState.imageError,
            isSubmitEnabled = viewModel.isSubmitEnabled(),
            isLoading = uiState.isLoading,
            onWordInputChanged = { viewModel.onWordInputChanged(it) },
            onMediaUploadClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = {
                viewModel.onRemoveMedia()
            },
            onAddClick = {
                viewModel.addWordToBank()
            },
            onDismiss = {
                viewModel.hideWordBankModal()
            }
        )

        // Word Added Confirmation Modal
        WordAddedConfirmationModal(
            isVisible = uiState.isConfirmationVisible,
            addedWord = uiState.confirmedWord,
            onDismiss = {
                viewModel.dismissConfirmation()
            }
        )

        // Word Bank Edit Modal
        WordBankEditModal(
            isVisible = uiState.isEditModalVisible,
            wordInput = uiState.editWordInput,
            existingImagePath = uiState.editingWord?.imagePath,
            selectedImageUri = uiState.editSelectedMediaUri,
            inputError = uiState.editInputError,
            imageError = uiState.editImageError,
            isSaveEnabled = viewModel.isEditSaveEnabled(),
            isLoading = uiState.isEditLoading,
            onWordInputChanged = { viewModel.onEditWordInputChanged(it) },
            onMediaUploadClick = {
                editImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = {
                viewModel.onEditRemoveMedia()
            },
            onSaveClick = {
                viewModel.saveEditedWord()
            },
            onDeleteClick = {
                viewModel.deleteEditingWord()
            },
            onDismiss = {
                viewModel.hideEditModal()
            }
        )

        // Activity Creation Modal
        ActivityCreationModal(
            isVisible = uiState.isActivityCreationModalVisible,
            activityInput = uiState.activityInput,
            words = uiState.words,
            selectedWordIds = uiState.selectedActivityWordIds,
            isLoading = uiState.isActivityCreationLoading,
            generationPhase = generationPhase,
            error = uiState.activityError,
            onActivityInputChanged = { viewModel.onActivityInputChanged(it) },
            onWordSelectionChanged = { wordId, isSelected ->
                viewModel.onActivityWordSelectionChanged(wordId, isSelected)
            },
            onSelectAll = { viewModel.onSelectAllActivityWords() },
            onCreateActivity = {
                viewModel.createActivity { jsonResult ->
                    onNavigateToAIGenerate(jsonResult)
                }
            },
            onDismiss = { viewModel.hideActivityCreationModal() }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WordBankScreenPreview() {
    WordBankScreen(
        onNavigate = {},
        onBackClick = {}
    )
}
```

**Step 2: Verify the file compiles**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL (or warnings only, no errors)

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/WordBankScreen.kt
git commit -m "feat: create standalone WordBankScreen with all word bank functionality"
```

---

### Task 2: Rewrite LessonScreen as navigation hub

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonScreen.kt`

**Step 1: Replace LessonScreen with hub layout**

Rewrite the entire `LessonScreen` composable. The new version is a simple navigation hub with 3 ModeCard-style tiles (matching LearnScreen's pattern). Remove all ViewModel/modal code. Keep the `WordBankList`, `AddWordBankButton`, and `ActivityCard` composables in the file since `WordBankScreen` imports `WordBankList` and `AddWordBankButton` from this file (they are package-level functions).

Replace the `LessonScreen` composable and add a new `NavigationTile` composable. The file should become:

```kotlin
package com.example.app.ui.feature.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.data.entity.Word
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.wordbank.WordBankItem

@Composable
fun LessonScreen(
    onNavigate: (Int) -> Unit,
    onNavigateToWordBank: () -> Unit = {},
    onNavigateToActivities: () -> Unit = {},
    onNavigateToSets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_kusho),
                contentDescription = "Kusho Logo",
                modifier = Modifier
                    .height(54.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .offset(x = 10.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Air Writing Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(Modifier.height(32.dp))

            NavigationTile(
                title = "Word Bank",
                backgroundColor = Color(0xFF5DB7FF),
                imageRes = R.drawable.ic_tutorial,
                onClick = { onNavigateToWordBank() }
            )

            Spacer(Modifier.height(20.dp))

            NavigationTile(
                title = "Your Activities",
                backgroundColor = Color(0xFF5DB7FF),
                imageRes = R.drawable.ic_learn,
                onClick = { onNavigateToActivities() }
            )

            Spacer(Modifier.height(20.dp))

            NavigationTile(
                title = "Your Sets",
                backgroundColor = Color(0xFF5DB7FF),
                imageRes = R.drawable.ic_tutorial,
                onClick = { onNavigateToSets() }
            )
        }

        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun NavigationTile(
    title: String,
    backgroundColor: Color,
    imageRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "$title image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Reusable Word Bank List component with scrollable grid layout.
 * Used by WordBankScreen.
 */
@Composable
fun WordBankList(
    words: List<Word>,
    onWordClick: (Word) -> Unit,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_none),
                    contentDescription = "No words mascot",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "No Words Yet",
                    color = Color(0xFF4A4A4A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap the button below to add\nyour first word to the bank.",
                    color = Color(0xFF7A7A7A),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(words, key = { it.id }) { word ->
                WordBankItem(
                    word = word.word,
                    onClick = { onWordClick(word) }
                )
            }
        }
    }
}

/**
 * Add Word Bank button component.
 * Used by WordBankScreen.
 */
@Composable
fun AddWordBankButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(75.dp)
            .width(207.dp)
            .widthIn(min = 200.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3FA9F8)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Word Bank",
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LessonScreenPreview() {
    LessonScreen(onNavigate = {})
}
```

Note: The old `ActivityCard` composable is removed (no longer needed). `WordBankList` and `AddWordBankButton` remain as public composables since `WordBankScreen` uses them.

**Step 2: Verify compilation**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: Compilation errors in `MainNavigationContainer.kt` because LessonScreen's signature changed (this is expected and fixed in Task 3).

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonScreen.kt
git commit -m "feat: rewrite LessonScreen as Air Writing Activities hub with 3 tiles"
```

---

### Task 3: Wire navigation in MainNavigationContainer

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt`

**Step 1: Add WordBankScreen import**

At the top of the file, add this import alongside the existing learn imports (around line 14):

```kotlin
import com.example.app.ui.feature.learn.WordBankScreen
```

**Step 2: Update LessonScreen call site (screen 3)**

Replace the entire `3 -> LessonScreen(...)` block (lines 187-293) with:

```kotlin
        3 -> LessonScreen(
            onNavigate = { currentScreen = it },
            onNavigateToWordBank = { currentScreen = 52 },
            onNavigateToActivities = { currentScreen = 6 },
            onNavigateToSets = { currentScreen = 7 },
            modifier = modifier
        )
```

**Step 3: Add WordBankScreen entry (screen 52)**

Add the following new `when` branch after the screen 51 block (after line 892, before the closing `}`):

```kotlin
        // --- WORD BANK SCREEN ---
        52 -> WordBankScreen(
            onNavigate = { currentScreen = it },
            onBackClick = { currentScreen = 3 },
            onNavigateToAIGenerate = { jsonResult ->
                aiGeneratedJsonResult = jsonResult
                // Parse JSON, resolve image availability, and initialize editable sets
                coroutineScope.launch {
                    try {
                        val activity = Gson().fromJson(jsonResult, AiGeneratedActivity::class.java)
                        val allWords = wordRepository.getWordsForUserOnce(userId)
                        val wordsWithImages = allWords
                            .filter { !it.imagePath.isNullOrBlank() }
                            .map { it.word }
                            .toSet()

                        // Build existingSetTitleMap for title similarity resolution
                        val setDao = database.setDao()
                        val setWordRows = setDao.getSetsWithWordNames(userId)
                        existingSetTitleMap = setWordRows
                            .groupBy { it.setTitle }
                            .mapValues { (_, rows) -> rows.first().setId }

                        val parsedSets = activity?.sets?.map { set ->
                            // Resolve set-level title similarity
                            val titleSimMatch = set.titleSimilarity?.let { sim ->
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

                            EditableSet(
                                title = set.title,
                                description = set.description,
                                words = set.words.map { word ->
                                    EditableWord(
                                        word = word.word,
                                        configurationType = mapAiConfigTypeToUi(word.configurationType),
                                        selectedLetterIndex = word.selectedLetterIndex,
                                        hasImage = word.word in wordsWithImages
                                    )
                                },
                                titleSimilarityMatch = titleSimMatch
                            )
                        } ?: emptyList()

                        // Run overlap detection before showing review screen
                        try {
                            val generatedWordLists = parsedSets.map { set ->
                                set.words.map { it.word }
                            }
                            val overlaps = setRepository.findOverlappingSets(userId, generatedWordLists)

                            aiEditableSets = parsedSets.mapIndexed { index, set ->
                                val match = overlaps[index]
                                if (match != null) {
                                    set.copy(overlapMatch = match)
                                } else {
                                    set
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainNavigation", "Overlap detection failed", e)
                            aiEditableSets = parsedSets
                        }

                        // Check activity title similarity
                        val activitySim = activity?.activity?.titleSimilarity
                        if (activitySim != null) {
                            val activityDao = database.activityDao()
                            val existingActivities = activityDao.getActivitiesByUserIdOnce(userId)
                            val matchingActivity = existingActivities.firstOrNull {
                                it.title.equals(activitySim.similarToExisting, ignoreCase = true)
                            }
                            if (matchingActivity != null) {
                                activityTitleSimilarityExistingTitle = matchingActivity.title
                                activityTitleSimilarityExistingId = matchingActivity.id
                                activityTitleSimilarityReason = activitySim.reason
                                showActivityTitleSimilarityDialog = true
                                currentAiSetIndex = 0
                                return@launch
                            } else {
                                android.util.Log.d("MainNavigation", "AI reported similarity to '${activitySim.similarToExisting}' but no match found in DB")
                            }
                        }

                        currentAiSetIndex = 0
                        currentScreen = 48
                    } catch (e: Exception) {
                        aiEditableSets = emptyList()
                        currentAiSetIndex = 0
                        currentScreen = 48
                    }
                }
            },
            modifier = modifier,
            viewModel = lessonViewModel
        )
```

**Step 4: Verify full build compiles**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt
git commit -m "feat: wire WordBankScreen (screen 52) and update LessonScreen navigation"
```

---

### Task 4: Clean up unused imports in LessonScreen.kt

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/LessonScreen.kt`

**Step 1: Remove unused imports**

After the rewrite, several imports are no longer needed. The file should only have imports that are actually used. Remove these unused imports:
- `androidx.activity.compose.rememberLauncherForActivityResult`
- `androidx.activity.result.PickVisualMediaRequest`
- `androidx.activity.result.contract.ActivityResultContracts`
- `androidx.compose.material.icons.automirrored.filled.ArrowForward`
- `androidx.compose.material3.IconButton`
- `androidx.lifecycle.viewmodel.compose.viewModel`
- `com.example.app.ui.components.wordbank.ActivityCreationModal`
- `com.example.app.ui.components.wordbank.WordAddedConfirmationModal`
- `com.example.app.ui.components.wordbank.WordBankEditModal`
- `com.example.app.ui.components.wordbank.WordBankModal`

Note: The exact imports needed are already specified in the Task 2 code. If you wrote the file exactly as Task 2 specifies, this step is already done. Verify by checking for compiler warnings about unused imports.

**Step 2: Verify build**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL with no unused import warnings

**Step 3: Commit (if any changes)**

```bash
git add app/src/main/java/com/example/app/ui/feature/learn/LessonScreen.kt
git commit -m "chore: clean up unused imports in LessonScreen"
```

---

### Task 5: Final build verification and smoke test

**Step 1: Full clean build**

Run: `cd /Users/wincelarcen/Documents/AndroidDevStuff/Kusho && ./gradlew clean assembleDebug 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL

**Step 2: Verify no regressions in navigation references**

Search for any remaining references to the old LessonScreen parameters that might break:

Run: `grep -rn "onNavigateToAIGenerate" app/src/main/java/ --include="*.kt" | grep -v "MainNavigationContainer\|WordBankScreen"`

Expected: No results (onNavigateToAIGenerate should only appear in MainNavigationContainer and WordBankScreen now)

**Step 3: Commit all changes**

If any files were missed or need final touches:

```bash
git add -A
git commit -m "feat: complete LessonScreen redesign as Air Writing Activities hub"
```

---

## Summary of Changes

| File | Action | Description |
|------|--------|-------------|
| `LessonScreen.kt` | Rewrite | Hub with 3 NavigationTiles, keeps WordBankList/AddWordBankButton as shared composables |
| `WordBankScreen.kt` | Create | Standalone screen with all word bank functionality + modals |
| `MainNavigationContainer.kt` | Modify | Update screen 3 wiring, add screen 52 for WordBankScreen |

## Navigation Map After Changes

```
LessonScreen (3) ─── "Word Bank" ──────→ WordBankScreen (52)
                 ├── "Your Activities" ─→ YourActivitiesScreen (6)
                 └── "Your Sets" ──────→ YourSetsScreen (7)

WordBankScreen (52) ── back ───────────→ LessonScreen (3)
                    ── magic wand ─────→ AISetReviewScreen (48)
```
