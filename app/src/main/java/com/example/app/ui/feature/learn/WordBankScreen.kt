package com.example.app.ui.feature.learn

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.app.ui.components.DeleteConfirmationDialog
import com.example.app.ui.components.DeleteType
import com.example.app.ui.components.wordbank.WordAddedConfirmationModal
import com.example.app.ui.components.wordbank.WordBankEditModal
import com.example.app.ui.components.wordbank.WordBankGenerationModal
import com.example.app.ui.components.wordbank.WordBankModal
import androidx.compose.runtime.LaunchedEffect

@Composable
fun WordBankScreen(
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonViewModel = viewModel()
) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Local state for WordBankGenerationModal
    var isGenerationModalVisible by remember { mutableStateOf(false) }
    var generationPrompt by remember { mutableStateOf("") }
    var generationWordCount by remember { mutableIntStateOf(5) }

    // Local state for batch delete confirmation
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Load suggested prompts when generation modal becomes visible
    LaunchedEffect(isGenerationModalVisible) {
        if (isGenerationModalVisible) {
            viewModel.loadSuggestedPrompts()
        }
    }

    // Image picker launcher for add modal
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
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header: Back Button and Kusho Logo (centered)
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

                // Kusho Logo (centered with 10dp x offset)
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

                // Edit Mode Button (right) - switches between Edit and Delete icons
                IconButton(
                    onClick = {
                        if (uiState.isWordBankEditMode && uiState.selectedWordIds.isNotEmpty()) {
                            showBatchDeleteDialog = true
                        } else {
                            viewModel.toggleWordBankEditMode()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (uiState.isWordBankEditMode) Icons.Default.Delete else Icons.Default.Edit,
                        contentDescription = if (uiState.isWordBankEditMode) "Delete Selected" else "Edit Mode",
                        tint = if (uiState.isWordBankEditMode) Color(0xFFFF6B6B) else Color(0xFF3FA9F8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title with Select All checkbox
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "My Word Bank",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0B0B),
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Select All checkbox (always present to maintain layout, but visibility controlled)
                IconButton(
                    onClick = { 
                        if (uiState.isWordBankEditMode) {
                            if (uiState.selectedWordIds.size == uiState.words.size) {
                                viewModel.deselectAllWords()
                            } else {
                                viewModel.selectAllWords()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .alpha(if (uiState.isWordBankEditMode) 1f else 0f),
                    enabled = uiState.isWordBankEditMode
                ) {
                    Icon(
                        imageVector = if (uiState.selectedWordIds.size == uiState.words.size) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Select All",
                        tint = Color(0xFF3FA9F8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Word Bank List
            Box(
                modifier = Modifier
                    .height(420.dp)
                    .fillMaxWidth()
            ) {
                WordBankList(
                    words = uiState.words,
                    onWordClick = { word ->
                        if (uiState.isWordBankEditMode) {
                            viewModel.toggleWordSelection(word.id)
                        } else {
                            viewModel.onWordClick(word)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    isEditMode = uiState.isWordBankEditMode,
                    selectedWordIds = uiState.selectedWordIds
                )
            }
        }

        // Action Buttons: "+ Word Bank" and Magic Wand (grayed out in edit mode)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .then(if (uiState.isWordBankEditMode) Modifier.alpha(0.4f) else Modifier),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddWordBankButton(
                onClick = {
                    if (!uiState.isWordBankEditMode) {
                        viewModel.showWordBankModal()
                    }
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Magic Wand Button
            IconButton(
                onClick = { 
                    if (!uiState.isWordBankEditMode) {
                        isGenerationModalVisible = true
                    }
                },
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

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        // Word Bank Modal (add word)
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
            },
        )

        // Word Added Confirmation Modal
        WordAddedConfirmationModal(
            isVisible = uiState.isConfirmationVisible,
            addedWord = uiState.confirmedWord,
            onDismiss = {
                viewModel.dismissConfirmation()
            }
        )

        // Word Bank Generation Modal (AI CVC word generation)
        WordBankGenerationModal(
            isVisible = isGenerationModalVisible,
            promptInput = generationPrompt,
            isLoading = uiState.isWordGenerationLoading,
            error = uiState.wordGenerationError,
            wordCount = generationWordCount,
            onWordCountChanged = { generationWordCount = it },
            suggestedPrompts = uiState.suggestedPrompts,
            isSuggestionsLoading = uiState.isSuggestionsLoading,
            onSuggestionClick = { suggestion ->
                generationPrompt = suggestion
            },
            onPromptInputChanged = { generationPrompt = it },
            onGenerate = {
                viewModel.generateWords(generationPrompt, generationWordCount)
            },
            onDismiss = {
                isGenerationModalVisible = false
                generationPrompt = ""
                generationWordCount = 5
                viewModel.clearWordGenerationState()
            },
            onBackClick = {
                isGenerationModalVisible = false
                generationPrompt = ""
                generationWordCount = 5
                viewModel.clearWordGenerationState()
            },
            generatedWords = uiState.generatedWords,
            requestedCount = uiState.wordGenerationRequestedCount,
            onDone = {
                isGenerationModalVisible = false
                generationPrompt = ""
                generationWordCount = 5
                viewModel.clearWordGenerationState()
            }
        )

        // Word Bank Edit Modal (edit/delete word)
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
            },
        )

        // Batch Delete Confirmation Dialog
        DeleteConfirmationDialog(
            isVisible = showBatchDeleteDialog,
            deleteType = DeleteType.WORDS,
            itemCount = uiState.selectedWordIds.size,
            onConfirm = {
                viewModel.deleteSelectedWords()
                showBatchDeleteDialog = false
            },
            onDismiss = {
                showBatchDeleteDialog = false
            }
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
