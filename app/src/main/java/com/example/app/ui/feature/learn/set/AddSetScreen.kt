package com.example.app.ui.feature.learn.set

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.repository.SetRepository
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.common.AlreadyExistsDialog
import kotlinx.coroutines.launch

@Composable
fun AddSetScreen(
    modifier: Modifier = Modifier,
    userId: Long = 0L,
    activityId: Long? = null,
    onBackClick: () -> Unit = {},
    onAddWordsClick: (existingWords: List<String>) -> Unit = {},
    onCreateSet: (setTitle: String, setDescription: String, words: List<SetRepository.SelectedWordConfig>) -> Unit = { _, _, _ -> },
    selectedWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    viewModel: AddSetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var setTitle by remember { mutableStateOf(uiState.setTitle) }
    var setDescription by remember { mutableStateOf(uiState.setDescription) }
    var internalWords by remember { mutableStateOf(selectedWords) }
    var showDuplicateError by remember { mutableStateOf(false) }

    // Update viewModel when local state changes
    LaunchedEffect(setTitle, setDescription) {
        viewModel.setTitle(setTitle)
        viewModel.setDescription(setDescription)
    }

    // Keep local copy in sync when external selectedWords changes
    LaunchedEffect(selectedWords) {
        internalWords = selectedWords
    }

    // Propagate any local changes to internalWords back to the ViewModel
    LaunchedEffect(internalWords) {
        viewModel.setSelectedWords(internalWords)
    }

    val coroutineScope = rememberCoroutineScope()

    // Show error dialog when duplicate set detected
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            if (message.contains("already exists", ignoreCase = true)) {
                showDuplicateError = true
            }
        }
    }

    // Error Dialog
    AlreadyExistsDialog(
        isVisible = showDuplicateError,
        itemType = "set",
        onDismiss = {
            showDuplicateError = false
            viewModel.clearErrorMessage()
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF3FA9F8)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_kusho),
                    contentDescription = "Kusho Logo",
                    modifier = Modifier
                        .height(54.dp)
                        .weight(1f)
                        .padding(horizontal = 30.dp)
                        .offset(x = 10.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title text "Add a New Set"
            Text(
                text = "Add a New Set",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0B0B0B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                // Set Title Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add a Set Title",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0B0B0B)
                    )
                    OutlinedTextField(
                        value = setTitle,
                        onValueChange = { setTitle = it },
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
                }

                // Description Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add Description",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0B0B0B)
                    )
                    OutlinedTextField(
                        value = setDescription,
                        onValueChange = { setDescription = it },
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
                }

                // Add Words Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (internalWords.isEmpty()) "Add Word/s" else "Words Added",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0B0B0B)
                    )

                    // Display added words with configuration
                    if (internalWords.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            internalWords.forEachIndexed { index, wordConfig ->
                                AddWordWithConfigItem(
                                    index = index + 1,
                                    word = wordConfig.wordName,
                                    configurationType = wordConfig.configurationType,
                                    selectedLetterIndex = wordConfig.selectedLetterIndex,
                                    onConfigurationChange = { newConfig ->
                                        internalWords = internalWords.toMutableList().apply {
                                            this[index] = wordConfig.copy(configurationType = newConfig)
                                        }
                                    },
                                    onLetterSelected = { letterIndex ->
                                        internalWords = internalWords.toMutableList().apply {
                                            this[index] = wordConfig.copy(selectedLetterIndex = letterIndex)
                                        }
                                    },
                                    onRemove = {
                                        internalWords = internalWords.filterIndexed { i, _ -> i != index }
                                    }
                                )
                            }
                        }
                    }

                    // Add Words Button
                    Button(
                        onClick = { onAddWordsClick(internalWords.map { it.wordName }) },
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
                            text = if (internalWords.isEmpty()) "Add Words" else "Add More Words",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF3FA9F8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Create Set Button at bottom
        Button(
            onClick = {
                if (setTitle.isNotBlank() && internalWords.isNotEmpty()) {
                    coroutineScope.launch {
                        val success = viewModel.createSet(
                            title = setTitle,
                            description = setDescription,
                            selectedWords = internalWords,
                            userId = userId,
                            activityId = activityId
                        )

                        if (success) {
                            onCreateSet(setTitle, setDescription, internalWords)
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .fillMaxWidth(0.86f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8),
                disabledContainerColor = Color(0xFFB3E5FC),
                disabledContentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp),
            enabled = setTitle.isNotBlank() && internalWords.isNotEmpty() && !uiState.isLoading
        ) {
            Text(
                text = if (uiState.isLoading) "Creating..." else "Create Set",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 2,
            onTabSelected = { },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AddWordWithConfigItem(
    index: Int,
    word: String,
    configurationType: String,
    selectedLetterIndex: Int,
    onConfigurationChange: (String) -> Unit,
    onLetterSelected: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    // Dropdown options
    val dropdownOptions = listOf("Fill in the Blank", "Name the Picture", "Write the Word")

    // Show letter selection only for "Fill in the Blank" mode
    val showLetterSelection = configurationType == "Fill in the Blank"

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
                    word.forEachIndexed { letterIndex, letter ->
                        AddLetterButton(
                            letter = letter,
                            isSelected = letterIndex == selectedLetterIndex,
                            onClick = { onLetterSelected(letterIndex) }
                        )
                    }
                } else {
                    // Show word as text for other modes
                    Text(
                        text = word,
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
                        text = configurationType,
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
                                    color = if (option == configurationType) Color(0xFF3FA9F8) else Color(0xFF0B0B0B),
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
                                    color = if (option == configurationType) Color(0xFFF0F9FF) else Color.Transparent,
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
private fun AddLetterButton(
    letter: Char,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(30.dp),
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
