package com.example.app.ui.feature.learn.generate

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.AppDatabase
import com.example.app.data.model.AiGeneratedActivity
import com.example.app.data.model.AiWordConfig
import com.example.app.data.repository.SetRepository
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.DeleteConfirmationDialog
import com.example.app.ui.components.DeleteType
import com.google.gson.Gson
import kotlinx.coroutines.launch

/**
 * Screen for reviewing AI-generated sets before saving.
 * Shows one set at a time with progress indicator.
 * Saves sets directly to database when Finish is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISetReviewScreen(
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onFinish: (activityTitle: String, activityDescription: String, setIds: List<Long>) -> Unit,
    generatedJson: String,
    userId: Long,
    editableSets: List<EditableSet>,
    onEditableSetsChange: (List<EditableSet>) -> Unit,
    onRegenerateSet: (currentSetTitle: String, currentSetDescription: String, onResult: (String?) -> Unit) -> Unit = { _, _, _ -> },
    onDiscardSet: () -> Unit = {},
    onAddWordsClick: (existingWords: List<String>) -> Unit = {},
    additionalWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    existingSetTitleMap: Map<String, Long> = emptyMap(),
    previouslySavedSetIds: List<Long> = emptyList(),
    onPreviouslySavedSetIdsCleaned: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val setRepository = remember { SetRepository(database) }
    val coroutineScope = rememberCoroutineScope()

    // Parse the JSON for activity info (title, description) used in onFinish
    val generatedActivity = remember(generatedJson) {
        try {
            Gson().fromJson(generatedJson, AiGeneratedActivity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    val currentSetIndex = 0
    val currentSet = editableSets.getOrNull(currentSetIndex)
    val totalSets = editableSets.size

    // Handle additional words being added from SelectWordsScreen
    LaunchedEffect(additionalWords, currentSetIndex) {
        if (additionalWords.isNotEmpty()) {
            val current = editableSets.getOrNull(currentSetIndex)
            current?.let { set ->
                val existingWordNames = set.words.map { it.word }.toSet()
                val newWords = additionalWords.filter { it.wordName !in existingWordNames }
                if (newWords.isNotEmpty()) {
                    onEditableSetsChange(editableSets.toMutableList().apply {
                        this[currentSetIndex] = set.copy(
                            words = set.words + newWords.map { wordConfig ->
                                EditableWord(
                                    word = wordConfig.wordName,
                                    configurationType = wordConfig.configurationType,
                                    selectedLetterIndex = wordConfig.selectedLetterIndex,
                                    hasImage = !wordConfig.imagePath.isNullOrBlank()
                                )
                            }
                        )
                    })
                }
            }
        }
    }

    // Loading states
    var isSaving by remember { mutableStateOf(false) }
    var isRegenerating by remember { mutableStateOf(false) }
    var regenerationError by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    DeleteConfirmationDialog(
        isVisible = showDiscardDialog,
        deleteType = DeleteType.DISCARD_ACTIVITY,
        onConfirm = {
            showDiscardDialog = false
            onDiscardSet()
        },
        onDismiss = { showDiscardDialog = false }
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header with back button and logo
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Button (left)
                IconButton(
                    onClick = { showDiscardDialog = true },
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


            // Current Set Card
            currentSet?.let { set ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SetReviewCard(
                        set = set,
                        isRegenerating = isRegenerating,
                        onSetChange = { updatedSet ->
                            onEditableSetsChange(editableSets.toMutableList().apply {
                                this[currentSetIndex] = updatedSet
                            })
                        },
                        onWordRemove = { wordIndex ->
                            onEditableSetsChange(editableSets.toMutableList().apply {
                                this[currentSetIndex] = set.copy(
                                    words = set.words.filterIndexed { i, _ -> i != wordIndex }
                                )
                            })
                        },
                        onExistingWordRemove = { wordIndex ->
                            onEditableSetsChange(editableSets.toMutableList().apply {
                                this[currentSetIndex] = set.copy(
                                    existingWords = set.existingWords.filterIndexed { i, _ -> i != wordIndex }
                                )
                            })
                        },
                        onRegenerateSet = {
                            isRegenerating = true
                            regenerationError = null
                            onRegenerateSet(set.title, set.description) { newJson ->
                                isRegenerating = false
                                if (newJson == null) {
                                    regenerationError = "Failed to regenerate set. Please try again."
                                    return@onRegenerateSet
                                }
                                try {
                                    val newActivity = Gson().fromJson(newJson, AiGeneratedActivity::class.java)
                                    // Build image lookup from existing editable words
                                    val wordsWithImages = editableSets
                                        .flatMap { it.words }
                                        .filter { it.hasImage }
                                        .map { it.word }
                                        .toSet()
                                    newActivity?.sets?.firstOrNull()?.let { newSet ->
                                        // Resolve title similarity from the regenerated set
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

                                        val regeneratedWords = newSet.words.map { word ->
                                            EditableWord(
                                                word = word.word,
                                                configurationType = mapAiConfigTypeToUi(word.configurationType),
                                                selectedLetterIndex = word.selectedLetterIndex,
                                                hasImage = word.word in wordsWithImages
                                            )
                                        }

                                        onEditableSetsChange(editableSets.toMutableList().apply {
                                            val currentEditableSet = this[currentSetIndex]
                                            if (currentEditableSet.mergeDecision == MergeDecision.MERGE) {
                                                // Merged mode: only replace new words, preserve merge state
                                                this[currentSetIndex] = currentEditableSet.copy(
                                                    words = regeneratedWords
                                                )
                                            } else {
                                                // Normal mode: replace entire set
                                                this[currentSetIndex] = EditableSet(
                                                    title = newSet.title,
                                                    description = newSet.description,
                                                    words = regeneratedWords,
                                                    titleSimilarityMatch = newTitleSimilarityMatch
                                                )
                                            }
                                        })
                                    }
                                } catch (e: Exception) {
                                    regenerationError = "Failed to parse regenerated set"
                                }
                            }
                        },
                        onAddWordsClick = {
                            onAddWordsClick(set.words.map { it.word })
                        },
                        showBottomButtons = true,
                        isSaving = isSaving,
                        hasUndecidedSets = editableSets.any {
                            (it.overlapMatch != null && it.mergeDecision == MergeDecision.UNDECIDED) ||
                            (it.titleSimilarityMatch != null && it.titleSimilarityDecision == TitleSimilarityDecision.UNDECIDED)
                        },
                        onProceedClick = {
                            isSaving = true
                            coroutineScope.launch {
                                // Delete previously saved sets from a prior Proceed
                                // (user went back, edited, and is re-saving)
                                if (previouslySavedSetIds.isNotEmpty()) {
                                    previouslySavedSetIds.forEach { oldSetId ->
                                        try {
                                            setRepository.deleteSet(oldSetId)
                                        } catch (e: Exception) {
                                            android.util.Log.e("AISetReview", "Failed to delete old set $oldSetId", e)
                                        }
                                    }
                                    onPreviouslySavedSetIdsCleaned()
                                }

                                val savedSetIds = mutableListOf<Long>()
                                val errors = mutableListOf<String>()

                                // Filter out sets that were skipped via title similarity
                                editableSets.filter {
                                    it.titleSimilarityDecision != TitleSimilarityDecision.SKIP
                                }.forEach { set ->
                                    val selectedNewWords = set.words.map { word ->
                                        SetRepository.SelectedWordConfig(
                                            wordName = word.word,
                                            configurationType = word.configurationType,
                                            selectedLetterIndex = word.selectedLetterIndex
                                        )
                                    }

                                    val result = if (set.mergeDecision == MergeDecision.MERGE && set.overlapMatch != null) {
                                        // Merge path: full sync of existing + new words
                                        val selectedExistingWords = set.existingWords.map { word ->
                                            SetRepository.SelectedWordConfig(
                                                wordName = word.word,
                                                configurationType = word.configurationType,
                                                selectedLetterIndex = word.selectedLetterIndex
                                            )
                                        }
                                        setRepository.updateExistingSetWithMerge(
                                            setId = set.overlapMatch.matchedSetId,
                                            userId = userId,
                                            title = set.title,
                                            description = set.description,
                                            existingWords = selectedExistingWords,
                                            newWords = selectedNewWords
                                        )
                                    } else {
                                        // Create path: save as new set (existing behavior)
                                        setRepository.addSetWithWords(
                                            title = set.title,
                                            description = set.description,
                                            userId = userId,
                                            selectedWords = selectedNewWords
                                        )
                                    }

                                    when (result) {
                                        is SetRepository.AddSetResult.Success -> {
                                            savedSetIds.add(result.setId)
                                        }
                                        is SetRepository.AddSetResult.Error -> {
                                            android.util.Log.e("AISetReviewScreen", "Failed to save set '${set.title}': ${result.message}")
                                            errors.add("\"${set.title}\": ${result.message}")
                                        }
                                    }
                                }

                                isSaving = false

                                if (savedSetIds.isEmpty()) {
                                    // All sets failed to save — don't navigate, show error
                                    regenerationError = "Failed to save sets: ${errors.joinToString("; ")}"
                                    return@launch
                                }

                                onFinish("", "", savedSetIds)
                            }
                        },
                        onMergeIntoExisting = {
                            val match = set.overlapMatch ?: return@SetReviewCard
                            val newWordNames = match.newWords.map { it.lowercase() }.toSet()
                            val newWordsOnly = set.words.filter { it.word.lowercase() in newWordNames }
                            // Immediately show merge state with new words
                            val updatedSets = editableSets.toMutableList().apply {
                                this[currentSetIndex] = set.copy(
                                    title = match.matchedSetTitle,
                                    words = newWordsOnly,
                                    mergeDecision = MergeDecision.MERGE,
                                    preMergeTitle = set.title,
                                    preMergeWords = set.words
                                )
                            }.toList()
                            onEditableSetsChange(updatedSets)
                            // Fetch existing set words from DB asynchronously
                            coroutineScope.launch {
                                val existingSetDetails = setRepository.getSetDetails(match.matchedSetId)
                                if (existingSetDetails != null) {
                                    val existingEditableWords = setRepository.getExistingSetWordsWithDetails(match.matchedSetId)
                                    if (existingEditableWords != null) {
                                        onEditableSetsChange(updatedSets.toMutableList().apply {
                                            val current = this[currentSetIndex]
                                            this[currentSetIndex] = current.copy(
                                                description = existingSetDetails.set.description ?: current.description,
                                                existingWords = existingEditableWords.map { w ->
                                                    EditableWord(
                                                        word = w.word,
                                                        configurationType = mapAiConfigTypeToUi(w.configurationType),
                                                        selectedLetterIndex = w.selectedLetterIndex,
                                                        hasImage = w.hasImage
                                                    )
                                                },
                                                existingDescription = existingSetDetails.set.description
                                            )
                                        })
                                    }
                                }
                            }
                        },
                        onCreateAsNew = {
                            onEditableSetsChange(editableSets.toMutableList().apply {
                                this[currentSetIndex] = set.copy(
                                    mergeDecision = MergeDecision.CREATE_NEW
                                )
                            })
                        },
                        onKeepTitle = {
                            onEditableSetsChange(editableSets.toMutableList().apply {
                                this[currentSetIndex] = set.copy(
                                    titleSimilarityDecision = TitleSimilarityDecision.KEEP
                                )
                            })
                        },
                        onSkipSet = {
                            // Same as discard — remove the set
                            onDiscardSet()
                        },
                        onUndoTitleDecision = {
                            onEditableSetsChange(editableSets.toMutableList().apply {
                                this[currentSetIndex] = set.copy(
                                    titleSimilarityDecision = TitleSimilarityDecision.UNDECIDED
                                )
                            })
                        }
                    )
                    
                    // Show error if regeneration failed
                    regenerationError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun SetReviewCard(
    set: EditableSet,
    onSetChange: (EditableSet) -> Unit,
    onWordRemove: (Int) -> Unit,
    onExistingWordRemove: (Int) -> Unit = {},
    isRegenerating: Boolean,
    onRegenerateSet: () -> Unit,
    onAddWordsClick: () -> Unit,
    showBottomButtons: Boolean = false,
    isSaving: Boolean = false,
    hasUndecidedSets: Boolean = false,
    onProceedClick: () -> Unit = {},
    onMergeIntoExisting: () -> Unit = {},
    onCreateAsNew: () -> Unit = {},
    onKeepTitle: () -> Unit = {},
    onSkipSet: () -> Unit = {},
    onUndoTitleDecision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
    ) {
        // Overlap detection banner
        if (set.overlapMatch != null) {
            when (set.mergeDecision) {
                MergeDecision.UNDECIDED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF0F9FF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFC5E5FD),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Similar to \"${set.overlapMatch.matchedSetTitle}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3FA9F8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${set.overlapMatch.overlappingWords.size} words already exist in this set",
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onMergeIntoExisting,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3FA9F8)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Merge into existing",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = onCreateAsNew,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF3FA9F8)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Create as new",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF3FA9F8)
                                )
                            }
                        }
                    }
                }
                MergeDecision.MERGE -> {
                    // Persistent green header bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFA5D6A7),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "\u270F\uFE0F",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Editing \"${set.overlapMatch.matchedSetTitle}\"",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Changes will update this existing set",
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                MergeDecision.CREATE_NEW -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF0F9FF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFC5E5FD),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Creating as new set",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3FA9F8)
                            )
                            Text(
                                text = "Similar to \"${set.overlapMatch.matchedSetTitle}\"",
                                fontSize = 13.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Title similarity banner (orange-themed, distinct from blue overlap banner)
        if (set.titleSimilarityMatch != null) {
            when (set.titleSimilarityDecision) {
                TitleSimilarityDecision.UNDECIDED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFFFCC80),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Similar title: \"${set.titleSimilarityMatch.existingTitle}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = set.titleSimilarityMatch.reason,
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onKeepTitle,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Keep Title",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = onSkipSet,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Skip Set",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }
                TitleSimilarityDecision.KEEP -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFA5D6A7),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keeping as new set",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onUndoTitleDecision) {
                            Text(
                                text = "Undo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                TitleSimilarityDecision.SKIP -> {
                    // This state shouldn't be visible since the set is removed,
                    // but handle gracefully
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add a Set Title Field
        Text(
            text = "Add an Activity Title",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF0B0B0B),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = set.title,
            onValueChange = { if (it.length <= 15) onSetChange(set.copy(title = it)) },
            placeholder = {
                Text(
                    text = "E.g, Tall letters",
                    fontSize = 16.sp,
                    color = Color(0xFFC5E5FD),
                    fontWeight = FontWeight.Normal
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3FA9F8),
                unfocusedBorderColor = Color(0xFFC5E5FD),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF3FA9F8),
                focusedTextColor = Color(0xFF0B0B0B),
                unfocusedTextColor = Color(0xFF0B0B0B)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            singleLine = true,
            supportingText = {
                Text(
                    text = "${set.title.length}/15",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Add Description Field
        Text(
            text = "Add a short description",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF0B0B0B),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = set.description,
            onValueChange = { if (it.length <= 30) onSetChange(set.copy(description = it)) },
            placeholder = {
                Text(
                    text = "E.g, Words ending in -at",
                    fontSize = 16.sp,
                    color = Color(0xFFC5E5FD),
                    fontWeight = FontWeight.Normal
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3FA9F8),
                unfocusedBorderColor = Color(0xFFC5E5FD),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF3FA9F8),
                focusedTextColor = Color(0xFF0B0B0B),
                unfocusedTextColor = Color(0xFF0B0B0B)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            singleLine = true,
            maxLines = 1,
            supportingText = {
                Text(
                    text = "${set.description.length}/30",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // === MERGED STATE: Two-section layout ===
        if (set.mergeDecision == MergeDecision.MERGE && set.existingWords.isNotEmpty()) {
            // Collapsible "Already in set" section
            var existingSectionExpanded by remember { mutableStateOf(true) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { existingSectionExpanded = !existingSectionExpanded }
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (existingSectionExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (existingSectionExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Already in set (${set.existingWords.size} word${if (set.existingWords.size != 1) "s" else ""})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0B0B0B)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (existingSectionExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        set.existingWords.forEachIndexed { wordIndex, word ->
                            WordReviewItem(
                                index = wordIndex + 1,
                                word = word,
                                onConfigurationChange = { newConfig ->
                                    onSetChange(
                                        set.copy(
                                            existingWords = set.existingWords.toMutableList().apply {
                                                this[wordIndex] = word.copy(configurationType = newConfig)
                                            }
                                        )
                                    )
                                },
                                onLetterSelected = { letterIndex ->
                                    onSetChange(
                                        set.copy(
                                            existingWords = set.existingWords.toMutableList().apply {
                                                this[wordIndex] = word.copy(selectedLetterIndex = letterIndex)
                                            }
                                        )
                                    )
                                },
                                onRemove = { onExistingWordRemove(wordIndex) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "Adding to set" section header with Regenerate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adding to set (${set.words.size} new word${if (set.words.size != 1) "s" else ""})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0B0B0B)
                )

                TextButton(
                    onClick = onRegenerateSet,
                    enabled = !isRegenerating,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF3FA9F8)
                    )
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF3FA9F8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Regenerating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Regenerate",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // New words list (continuous numbering from existing)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val existingCount = set.existingWords.size
                set.words.forEachIndexed { wordIndex, word ->
                    WordReviewItem(
                        index = existingCount + wordIndex + 1,
                        word = word,
                        onConfigurationChange = { newConfig ->
                            onSetChange(
                                set.copy(
                                    words = set.words.toMutableList().apply {
                                        this[wordIndex] = word.copy(configurationType = newConfig)
                                    }
                                )
                            )
                        },
                        onLetterSelected = { letterIndex ->
                            onSetChange(
                                set.copy(
                                    words = set.words.toMutableList().apply {
                                        this[wordIndex] = word.copy(selectedLetterIndex = letterIndex)
                                    }
                                )
                            )
                        },
                        onRemove = { onWordRemove(wordIndex) }
                    )
                }
            }
        } else {
            // === NORMAL STATE: Single word list ===
            // Words Added Label with Regenerate Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Words in Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0B0B0B)
                )

                TextButton(
                    onClick = onRegenerateSet,
                    enabled = !isRegenerating,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF3FA9F8)
                    )
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF3FA9F8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Regenerating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Regenerate Activity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Words List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                set.words.forEachIndexed { wordIndex, word ->
                    WordReviewItem(
                        index = wordIndex + 1,
                        word = word,
                        onConfigurationChange = { newConfig ->
                            onSetChange(
                                set.copy(
                                    words = set.words.toMutableList().apply {
                                        this[wordIndex] = word.copy(configurationType = newConfig)
                                    }
                                )
                            )
                        },
                        onLetterSelected = { letterIndex ->
                            onSetChange(
                                set.copy(
                                    words = set.words.toMutableList().apply {
                                        this[wordIndex] = word.copy(selectedLetterIndex = letterIndex)
                                    }
                                )
                            )
                        },
                        onRemove = { onWordRemove(wordIndex) }
                    )
                }
            }
        }

        // Add More Words Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddWordsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(
                    width = 1.3.dp,
                    color = Color(0xFF3FA9F8),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Word",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (set.words.isEmpty()) "Add Words" else "Add More Words",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF3FA9F8)
            )
        }

        // Bottom Button
        if (showBottomButtons) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onProceedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3FA9F8),
                    disabledContainerColor = Color(0xFFB0BEC5)
                ),
                enabled = !isSaving && !hasUndecidedSets
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Proceed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Warning message when proceed is blocked by undecided banners
            if (hasUndecidedSets) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resolve all overlap and similarity decisions before proceeding",
                    fontSize = 13.sp,
                    color = Color(0xFFE65100),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    }
}

@Composable
private fun WordReviewItem(
    index: Int,
    word: EditableWord,
    onConfigurationChange: (String) -> Unit,
    onLetterSelected: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val dropdownOptions = remember(word.hasImage) {
        if (word.hasImage) {
            listOf("Fill in the Blank", "Name the Picture", "Write the Word")
        } else {
            listOf("Fill in the Blank", "Write the Word")
        }
    }
    val showLetterSelection = word.configurationType == "Fill in the Blank"

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Main row with index, word letters/text, dropdown, and remove button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(
                    width = 1.dp,
                    color = Color(0xFF3FA9F8),
                    shape = RoundedCornerShape(15.dp)
                )
                .background(Color.White, RoundedCornerShape(15.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Index and letter buttons or word text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF3FA9F8)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Letter buttons for "Fill in the Blank" mode
                if (showLetterSelection) {
                    word.word.forEachIndexed { letterIndex, letter ->
                        LetterButton(
                            letter = letter,
                            isSelected = letterIndex == word.selectedLetterIndex,
                            onClick = { onLetterSelected(letterIndex) }
                        )
                    }
                } else {
                    // Show word as text for other modes
                    Text(
                        text = word.word,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3FA9F8)
                    )
                }
            }

            // Dropdown button
            Box {
                Button(
                    onClick = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier
                        .height(40.dp)
                        .width(120.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3FA9F8)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = word.configurationType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFC5E5FD),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 8.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    dropdownOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (option == word.configurationType) Color(0xFF3FA9F8) else Color(0xFF0B0B0B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible
                                )
                            },
                            onClick = {
                                onConfigurationChange(option)
                                expandedDropdown = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(40.dp)
                                .background(
                                    color = if (option == word.configurationType) Color(0xFFF0F9FF) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .fillMaxWidth()
                        )
                        if (option != dropdownOptions.last()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(Color(0xFFF5F5F5))
                            )
                        }
                    }
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Text(
                    text = "✕",
                    fontSize = 14.sp,
                    color = Color(0xFF3FA9F8),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LetterButton(
    letter: Char,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .size(30.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF3FA9F8) else Color.White
        ),
        border = if (!isSelected) {
            BorderStroke(
                width = 1.dp,
                color = Color(0xFFC5E5FD)
            )
        } else {
            null
        },
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = letter.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color(0xFF3FA9F8)
        )
    }
}

// Merge decision state for overlap detection
enum class MergeDecision {
    UNDECIDED,   // Banner visible, user hasn't chosen
    MERGE,       // User chose to merge into existing set
    CREATE_NEW   // User chose to create as new set
}

// Title similarity decision state
enum class TitleSimilarityDecision {
    UNDECIDED,   // Banner visible, user hasn't chosen
    KEEP,        // User chose to keep the title as new set
    SKIP         // User chose to skip/discard the set
}

// Info about a matched existing title
data class TitleSimilarityInfo(
    val existingTitle: String,
    val existingId: Long,
    val reason: String
)

// Data classes for UI state
data class EditableSet(
    val title: String,
    val description: String,
    val words: List<EditableWord>,
    val overlapMatch: SetRepository.OverlapResult? = null,
    val mergeDecision: MergeDecision = MergeDecision.UNDECIDED,
    val preMergeTitle: String? = null,
    val preMergeWords: List<EditableWord>? = null,
    val existingWords: List<EditableWord> = emptyList(),
    val existingDescription: String? = null,
    val titleSimilarityMatch: TitleSimilarityInfo? = null,
    val titleSimilarityDecision: TitleSimilarityDecision = TitleSimilarityDecision.UNDECIDED
)

data class EditableWord(
    val word: String,
    val configurationType: String,
    val selectedLetterIndex: Int = 0,
    val hasImage: Boolean = false
)

// Helper functions to map between AI and UI config types
fun mapAiConfigTypeToUi(aiType: String): String {
    return when (aiType.lowercase()) {
        "fill in the blanks", "fill in the blank" -> "Fill in the Blank"
        "name the picture", "identification" -> "Name the Picture"
        "write the word", "air writing" -> "Write the Word"
        else -> "Write the Word"
    }
}

private fun mapUiConfigTypeToAi(uiType: String): String {
    return when (uiType) {
        "Fill in the Blank" -> "fill in the blanks"
        "Name the Picture" -> "name the picture"
        "Write the Word" -> "write the word"
        else -> "write the word"
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AISetReviewScreenPreview() {
    val sampleJson = """
    {
      "activity": {
        "title": "Animal Word Practice",
        "description": "Practice writing and identifying animal-related words"
      },
      "sets": [
        {
          "title": "Basic Animals",
          "description": "Focus on common animal words",
          "words": [
            {
              "word": "cat",
              "configurationType": "fill in the blanks",
              "selectedLetterIndex": 1
            },
            {
              "word": "dog",
              "configurationType": "name the picture",
              "selectedLetterIndex": 0
            },
            {
              "word": "bird",
              "configurationType": "write the word",
              "selectedLetterIndex": 0
            }
          ]
        },
        {
          "title": "Farm Animals",
          "description": "Words for farm animals",
          "words": [
            {
              "word": "pig",
              "configurationType": "fill in the blanks",
              "selectedLetterIndex": 0
            },
            {
              "word": "cow",
              "configurationType": "name the picture",
              "selectedLetterIndex": 0
            }
          ]
        }
      ]
    }
    """.trimIndent()

    val sampleSets = listOf(
        EditableSet(
            title = "Basic Animals",
            description = "Focus on common animal words",
            words = listOf(
                EditableWord("cat", "Fill in the Blank", 1),
                EditableWord("dog", "Name the Picture", 0),
                EditableWord("bird", "Write the Word", 0)
            )
        ),
        EditableSet(
            title = "Farm Animals",
            description = "Words for farm animals",
            words = listOf(
                EditableWord("pig", "Fill in the Blank", 0),
                EditableWord("cow", "Name the Picture", 0)
            )
        )
    )
    var editableSets by remember { mutableStateOf(sampleSets) }

    AISetReviewScreen(
        onNavigate = {},
        onBackClick = {},
        onFinish = { _, _, _ -> },
        generatedJson = sampleJson,
        userId = 1L,
        editableSets = editableSets,
        onEditableSetsChange = { editableSets = it },
        onRegenerateSet = { _, _, _ -> },
        onDiscardSet = {},
        onAddWordsClick = {}
    )
}
