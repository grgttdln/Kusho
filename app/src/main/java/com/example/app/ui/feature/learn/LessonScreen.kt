package com.example.app.ui.feature.learn

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.entity.Word
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.wordbank.ActivityCreationModal
import com.example.app.ui.components.wordbank.WordAddedConfirmationModal
import com.example.app.ui.components.wordbank.WordBankEditModal
import com.example.app.ui.components.wordbank.WordBankItem
import com.example.app.ui.components.wordbank.WordBankModal

@Composable
fun LessonScreen(
    onNavigate: (Int) -> Unit,
    onNavigateToActivities: () -> Unit = {},
    onNavigateToSets: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LessonViewModel = viewModel()
) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
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

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Customize Activities",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0B0B)
                )

                Spacer(Modifier.height(28.dp))

                // Activity Cards Row - Fixed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActivityCard(
                        title = "Your\nActivities",
                        imageRes = R.drawable.ic_book,
                        backgroundColor = Color(0xFF5DB7FF),
                        onClick = { onNavigateToActivities() },
                        modifier = Modifier.weight(1f)
                    )

                    ActivityCard(
                        title = "Your\nSets",
                        imageRes = R.drawable.ic_pen,
                        backgroundColor = Color(0xFF5DB7FF),
                        onClick = { onNavigateToSets() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Word Bank Title
                Text(
                    text = "Word Bank",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0B0B),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(16.dp))
            }

            // Scrollable Word Bank List - Only this section scrolls
            Box(
                modifier = Modifier
                    .weight(1f) // Takes remaining space
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

            // Fixed "+ Word Bank" Button and Magic Wand Button above bottom nav
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

        // Bottom Navigation Bar - Fixed at bottom
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
            onActivityInputChanged = { viewModel.onActivityInputChanged(it) },
            onWordSelectionChanged = { wordId, isSelected ->
                viewModel.onActivityWordSelectionChanged(wordId, isSelected)
            },
            onSelectAll = { viewModel.onSelectAllActivityWords() },
            onCreateActivity = { viewModel.createActivity() },
            onDismiss = { viewModel.hideActivityCreationModal() }
        )
    }
}

/**
 * Reusable Word Bank List component with scrollable grid layout.
 * Only this component scrolls, keeping the rest of the UI fixed.
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

@Composable
private fun ActivityCard(
    title: String,
    imageRes: Int,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Start),
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigate",
                        tint = backgroundColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LessonScreenPreview() {
    LessonScreen(onNavigate = {})
}
