package com.example.app.ui.feature.learn.set

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
fun AddSetScreen(
    userId: Long = 0L,
    onBackClick: () -> Unit = {},
    onAddWordsClick: () -> Unit = {},
    onCreateSet: (setTitle: String, setDescription: String, words: List<SetRepository.SelectedWordConfig>) -> Unit = { _, _, _ -> },
    selectedWords: List<SetRepository.SelectedWordConfig> = emptyList(),
    modifier: Modifier = Modifier,
    viewModel: AddSetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var setTitle by remember { mutableStateOf(uiState.setTitle) }
    var setDescription by remember { mutableStateOf(uiState.setDescription) }
    var internalWords by remember { mutableStateOf(selectedWords) }

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
                                WordWithConfigItem(
                                    index = index + 1,
                                    word = wordConfig.wordName,
                                    configurationType = wordConfig.configurationType,
                                    onRemove = {
                                        internalWords = internalWords.filterIndexed { i, _ -> i != index }
                                    }
                                )
                            }
                        }
                    }

                    // Add Words Button
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
                            text = if (internalWords.isEmpty()) "Add Word/s" else "Add More Words",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF3FA9F8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                            userId = userId
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
            if (uiState.isLoading) {
                Text(
                    text = "Creating...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Create Set",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
private fun WordWithConfigItem(
    index: Int,
    word: String,
    configurationType: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
        // Word info on the left
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = index.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF3FA9F8)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = word,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B0B0B)
                )
                Text(
                    text = configurationType,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF3FA9F8)
                )
            }
        }

        // Remove button on the right
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Text(
                text = "✕",
                fontSize = 16.sp,
                color = Color(0xFF3FA9F8),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddSetScreenPreview() {
    AddSetScreen()
}
