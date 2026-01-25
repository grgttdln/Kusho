package com.example.app.ui.feature.learn.set

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.entity.Word
import com.example.app.data.repository.SetRepository
import com.example.app.ui.components.BottomNavBar

@Composable
fun SelectWordsScreen(
    availableWords: List<Word> = emptyList(),
    onBackClick: () -> Unit = {},
    onWordsSelected: (selectedWords: List<SetRepository.SelectedWordConfig>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedWords by remember { mutableStateOf(setOf<String>()) }
    var wordConfigurations by remember { mutableStateOf(mapOf<String, String>()) }
    var selectedLetterIndices by remember { mutableStateOf(mapOf<String, Int>()) }

    // Optimize filtering with memoization
    val filteredWords = remember(searchQuery, availableWords) {
        if (searchQuery.isBlank()) {
            availableWords
        } else {
            val query = searchQuery.lowercase()
            availableWords.filter { it.word.lowercase().contains(query) }
        }
    }

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

            // Header with back button
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

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title "Select Word/s"
            Text(
                text = "Select Word/s",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0B0B0B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search for Words",
                        fontSize = 16.sp,
                        color = Color(0xFF3FA9F8),
                        fontWeight = FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF3FA9F8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 15.dp),
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "Select Word/s" label with counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically

                
            ) {
                Text(
                    text = "Select Words",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0B0B0B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
                
                // Word count indicator
                Text(
                    text = "${selectedWords.size} selected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedWords.size >= 3) Color(0xFF4CAF50) else Color(0xFF999999)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Word Selection Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
            ) {
                val rows = filteredWords.chunked(2)
                rows.forEachIndexed { rowIndex, rowWords ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        rowWords.forEach { wordObj ->
                            WordButton(
                                word = wordObj.word,
                                isSelected = selectedWords.contains(wordObj.word),
                                onClick = {
                                    selectedWords = if (selectedWords.contains(wordObj.word)) {
                                        selectedWords - wordObj.word
                                    } else {
                                        selectedWords + wordObj.word
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if only one item in last row
                        if (rowWords.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (rowIndex < rows.size - 1) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "Configure" label and items
            if (selectedWords.isNotEmpty()) {
                Text(
                    text = "Assign Question Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0B0B0B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                Spacer(modifier = Modifier.height(16.dp))

                selectedWords.toList().forEachIndexed { index, word ->
                    val wordObj = availableWords.find { it.word == word }
                    ConfigureWordItem(
                        index = index + 1,
                        word = word,
                        hasImage = wordObj?.imagePath != null,
                        configurationType = wordConfigurations[word] ?: "Fill in the Blank",
                        selectedLetterIndex = selectedLetterIndices[word] ?: 0,
                        onConfigurationChange = { newConfig ->
                            wordConfigurations = wordConfigurations.toMutableMap().apply {
                                this[word] = newConfig
                            }
                        },
                        onLetterSelected = { letterIndex ->
                            selectedLetterIndices = selectedLetterIndices.toMutableMap().apply {
                                this[word] = letterIndex
                            }
                        },
                        modifier = Modifier.padding(horizontal = 15.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        // Add Words Button at bottom
        Button(
            onClick = {
                val configuredWords = selectedWords.map { word ->
                    SetRepository.SelectedWordConfig(
                        wordName = word,
                        configurationType = wordConfigurations[word] ?: "Fill in the Blank",
                        selectedLetterIndex = selectedLetterIndices[word] ?: 0
                    )
                }
                onWordsSelected(configuredWords)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .fillMaxWidth(0.86f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8),
                disabledContainerColor = Color(0xFFB3E5FC),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp),
            enabled = selectedWords.size >= 3
        ) {
            Text(
                text = if (selectedWords.size >= 3) {
                    "Add ${selectedWords.size} Words"
                } else {
                    "Select at least 3 words"
                },
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
private fun WordButton(
    word: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF3FA9F8) else Color.White
        ),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color(0xFFC5E5FD)
            )
        } else {
            null
        },
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = word,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color(0xFF3FA9F8)
        )
    }
}

@Composable
private fun ConfigureWordItem(
    index: Int,
    word: String,
    hasImage: Boolean,
    configurationType: String,
    selectedLetterIndex: Int,
    onConfigurationChange: (String) -> Unit,
    onLetterSelected: (Int) -> Unit,
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
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Main row with index, word letters, and dropdown
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
            // Left side: Index and letter buttons
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

                // Letter buttons
                if (showLetterSelection) {
                    word.forEachIndexed { letterIndex, letter ->
                        LetterButton(
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

            // Dropdown button on the right
            Box {
            Button(
                onClick = { expandedDropdown = !expandedDropdown },
                modifier = Modifier
                    .height(36.dp)
                    .width(140.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3FA9F8)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = configurationType,
                    fontSize = 12.sp,
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
                    modifier = Modifier.size(18.dp)
                )
            }

            // Dropdown Menu with improved styling
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
                    .width(IntrinsicSize.Max) // Allow dropdown to size itself based on content
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Visible // Ensure text is not truncated
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
                            .fillMaxWidth() // Ensure full width usage
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
            .size(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF3FA9F8) else Color.White
        ),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(
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
fun SelectWordsScreenPreview() {
    SelectWordsScreen()
}
