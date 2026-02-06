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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.repository.SetRepository
import com.example.app.ui.components.BottomNavBar
import kotlinx.coroutines.launch

@Composable
fun EditSetScreen(
    setId: Long,
    userId: Long = 0L,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddWordsClick: (existingWords: List<String>) -> Unit = {},
    onUpdateSuccess: () -> Unit = {},
    onDeleteSuccess: () -> Unit = {},
    selectedWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    viewModel: EditSetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Create stable callback references
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    val currentOnAddWordsClick by rememberUpdatedState(onAddWordsClick)
    val currentOnUpdateSuccess by rememberUpdatedState(onUpdateSuccess)
    val currentOnDeleteSuccess by rememberUpdatedState(onDeleteSuccess)

    // Load the set when the screen is first displayed or when setId changes
    // The ViewModel will only reload if it's a different set
    LaunchedEffect(setId) {
        if (setId > 0L) {
            viewModel.loadSet(setId)
        }
    }

    // Handle adding new words from SelectWordsScreen
    // Use derivedStateOf to get current words to avoid stale closure
    LaunchedEffect(selectedWords) {
        if (selectedWords.isNotEmpty()) {
            val currentWords = viewModel.uiState.value.selectedWords
            val existingWordNames = currentWords.map { it.wordName }.toSet()
            val newWords = selectedWords.filter { it.wordName !in existingWordNames }
            if (newWords.isNotEmpty()) {
                viewModel.setSelectedWords(currentWords + newWords)
            }
        }
    }

    // Handle navigation events (delete success, update success)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditSetEvent.DeleteSuccess -> currentOnDeleteSuccess()
                is EditSetEvent.UpdateSuccess -> currentOnUpdateSuccess()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (uiState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF3FA9F8)
                )
            }
        } else {
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
                        onClick = { currentOnBackClick() },
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

                // Title text "Edit Set"
                Text(
                    text = "Edit Set",
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
                            text = "Set Title",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0B0B0B)
                        )
                        OutlinedTextField(
                            value = uiState.setTitle,
                            onValueChange = { viewModel.setTitle(it) },
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
                            text = "Description",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0B0B0B)
                        )
                        OutlinedTextField(
                            value = uiState.setDescription,
                            onValueChange = { viewModel.setDescription(it) },
                            placeholder = {
                                Text(
                                    text = "E.g, Practice letters with tall strokes",
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

                    // Words Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (uiState.selectedWords.isEmpty()) "Words" else "Words in Set (${uiState.selectedWords.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0B0B0B)
                        )

                        // Display added words with configuration
                        if (uiState.selectedWords.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.selectedWords.forEachIndexed { index, wordConfig ->
                                    EditWordWithConfigItem(
                                        index = index + 1,
                                        word = wordConfig.wordName,
                                        hasImage = wordConfig.imagePath != null,
                                        configurationType = wordConfig.configurationType,
                                        selectedLetterIndex = wordConfig.selectedLetterIndex,
                                        onConfigurationChange = { newConfig ->
                                            viewModel.updateWordConfiguration(index, newConfig)
                                        },
                                        onLetterSelected = { letterIndex ->
                                            viewModel.updateWordLetterIndex(index, letterIndex)
                                        },
                                        onRemove = {
                                            viewModel.removeWord(index)
                                        }
                                    )
                                }
                            }
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFC5E5FD),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No words in this set",
                                    fontSize = 14.sp,
                                    color = Color(0xFF808080)
                                )
                            }
                        }

                        // Add Words Button
                        Button(
                            onClick = {
                                currentOnAddWordsClick(uiState.selectedWords.map { it.wordName })
                            },
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
                                text = if (uiState.selectedWords.isEmpty()) "Add Words" else "Add More Words",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF3FA9F8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Update Set Button at bottom
            Button(
                onClick = {
                    if (uiState.setTitle.isNotBlank() && uiState.selectedWords.isNotEmpty()) {
                        coroutineScope.launch {
                            viewModel.updateSet(userId)
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
                enabled = uiState.setTitle.isNotBlank() && uiState.selectedWords.isNotEmpty() && !uiState.isSaving
            ) {
                Text(
                    text = if (uiState.isSaving) "Updating..." else "Update Set",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom Navigation Bar
            BottomNavBar(
                selectedTab = 3,
                onTabSelected = { },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Error Snackbar
        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp)
                    .padding(horizontal = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                containerColor = Color(0xFFE53935)
            ) {
                Text(uiState.errorMessage ?: "", color = Color.White)
            }
        }
    }
}

@Composable
private fun EditWordWithConfigItem(
    index: Int,
    word: String,
    hasImage: Boolean,
    configurationType: String,
    selectedLetterIndex: Int,
    onConfigurationChange: (String) -> Unit,
    onLetterSelected: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    // Filter dropdown options based on whether word has an image
    val dropdownOptions = remember(hasImage) {
        if (hasImage) {
            listOf("Fill in the Blank", "Name the Picture", "Write the Word")
        } else {
            listOf("Fill in the Blank", "Write the Word")
        }
    }

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
                        EditLetterButton(
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible
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
private fun EditLetterButton(
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditSetScreenPreview() {
    EditSetScreen(setId = 1L)
}

