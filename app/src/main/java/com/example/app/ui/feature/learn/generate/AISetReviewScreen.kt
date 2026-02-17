package com.example.app.ui.feature.learn.generate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
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
    currentSetIndex: Int,
    onCurrentSetIndexChange: (Int) -> Unit,
    onRegenerateSet: (currentSetTitle: String, currentSetDescription: String, onResult: (String?) -> Unit) -> Unit = { _, _, _ -> },
    onAddWordsClick: (existingWords: List<String>) -> Unit = {},
    additionalWords: List<SetRepository.SelectedWordConfig> = emptyList(),
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
                                    selectedLetterIndex = wordConfig.selectedLetterIndex
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

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Progress Bar (Set indicators) - Always show even with one set
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalSets) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(
                                color = if (index <= currentSetIndex) Color(0xFF3FA9F8) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Bar with Previous/Next Set buttons (below progress indicator)
            if (totalSets > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Set Button (Icon only)
                    if (currentSetIndex > 0) {
                        IconButton(
                            onClick = { onCurrentSetIndexChange(currentSetIndex - 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Set",
                                tint = Color(0xFF3FA9F8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    // Set indicator text
                    Text(
                        text = "${currentSetIndex + 1} / $totalSets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )

                    // Next Set Button (Icon only)
                    if (currentSetIndex < totalSets - 1) {
                        IconButton(
                            onClick = { onCurrentSetIndexChange(currentSetIndex + 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Set",
                                tint = Color(0xFF3FA9F8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }



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
                                    newActivity?.sets?.firstOrNull()?.let { newSet ->
                                        onEditableSetsChange(editableSets.toMutableList().apply {
                                            this[currentSetIndex] = EditableSet(
                                                title = newSet.title,
                                                description = newSet.description,
                                                words = newSet.words.map { word ->
                                                    EditableWord(
                                                        word = word.word,
                                                        configurationType = mapAiConfigTypeToUi(word.configurationType),
                                                        selectedLetterIndex = word.selectedLetterIndex
                                                    )
                                                }
                                            )
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
                        showBottomButtons = currentSetIndex == totalSets - 1,
                        isSaving = isSaving,
                        onAddMoreSetClick = { 
                            // TODO: Add logic to generate an additional set
                        },
                        onProceedClick = {
                            isSaving = true
                            coroutineScope.launch {
                                val savedSetIds = mutableListOf<Long>()
                                
                                editableSets.forEach { set ->
                                    val selectedWords = set.words.map { word ->
                                        SetRepository.SelectedWordConfig(
                                            wordName = word.word,
                                            configurationType = word.configurationType,
                                            selectedLetterIndex = word.selectedLetterIndex
                                        )
                                    }
                                    
                                    when (val result = setRepository.addSetWithWords(
                                        title = set.title,
                                        description = set.description,
                                        userId = userId,
                                        selectedWords = selectedWords
                                    )) {
                                        is SetRepository.AddSetResult.Success -> {
                                            savedSetIds.add(result.setId)
                                        }
                                        is SetRepository.AddSetResult.Error -> {
                                            android.util.Log.e("AISetReviewScreen", "Failed to save set: ${result.message}")
                                        }
                                    }
                                }
                                
                                isSaving = false
                                
                                generatedActivity?.let { activity ->
                                    onFinish(
                                        activity.activity.title,
                                        activity.activity.description,
                                        savedSetIds
                                    )
                                }
                            }
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

        // Bottom Navigation
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SetReviewCard(
    set: EditableSet,
    onSetChange: (EditableSet) -> Unit,
    onWordRemove: (Int) -> Unit,
    isRegenerating: Boolean,
    onRegenerateSet: () -> Unit,
    onAddWordsClick: () -> Unit,
    showBottomButtons: Boolean = false,
    isSaving: Boolean = false,
    onAddMoreSetClick: () -> Unit = {},
    onProceedClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Add a Set Title Field
        Text(
            text = "Add a Set Title",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF0B0B0B),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = set.title,
            onValueChange = { onSetChange(set.copy(title = it)) },
            placeholder = { 
                Text(
                    text = "E.g, Tall letters",
                    fontSize = 16.sp,
                    color = Color(0xFFC5E5FD),
                    fontWeight = FontWeight.Normal
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add Description Field
        Text(
            text = "Add Description",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF0B0B0B),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = set.description,
            onValueChange = { onSetChange(set.copy(description = it)) },
            placeholder = {
                Text(
                    text = "E.g, Tall letters",
                    fontSize = 16.sp,
                    color = Color(0xFFC5E5FD),
                    fontWeight = FontWeight.Normal
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Words Added Label with Regenerate Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Words Added",
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
                        text = "Regenerate Set",
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

        // Bottom Buttons - Only show on the last set
        if (showBottomButtons) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                
                // Proceed Button (filled style) - saves all sets and finishes
                Button(
                    onClick = onProceedClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3FA9F8)
                    ),
                    enabled = !isSaving
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
    val dropdownOptions = listOf("Fill in the Blank", "Name the Picture", "Write the Word")
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

// Data classes for UI state
data class EditableSet(
    val title: String,
    val description: String,
    val words: List<EditableWord>,
    val overlapMatch: SetRepository.OverlapResult? = null,
    val mergeDecision: MergeDecision = MergeDecision.UNDECIDED
)

data class EditableWord(
    val word: String,
    val configurationType: String,
    val selectedLetterIndex: Int = 0
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
    var currentSetIndex by remember { mutableStateOf(0) }

    AISetReviewScreen(
        onNavigate = {},
        onBackClick = {},
        onFinish = { _, _, _ -> },
        generatedJson = sampleJson,
        userId = 1L,
        editableSets = editableSets,
        onEditableSetsChange = { editableSets = it },
        currentSetIndex = currentSetIndex,
        onCurrentSetIndexChange = { currentSetIndex = it },
        onRegenerateSet = { _, _, _ -> },
        onAddWordsClick = {}
    )
}
