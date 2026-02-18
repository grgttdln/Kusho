package com.example.app.ui.components.wordbank

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.R
import com.example.app.data.entity.Word
import com.example.app.data.repository.GenerationPhase

/**
 * Activity Creation Modal component for creating activities with selected words.
 * Displays a dialog with activity input, word selection, and magic creation button.
 *
 * @param isVisible Whether the modal is currently visible
 * @param activityInput Current text in the activity input field
 * @param words List of all available words from Word Bank
 * @param selectedWordIds Set of selected word IDs
 * @param isLoading Whether the modal is in a loading state
 * @param onActivityInputChanged Callback when activity input changes
 * @param onWordSelectionChanged Callback when word selection changes (wordId, isSelected)
 * @param onSelectAll Callback when "Select all" is clicked
 * @param onCreateActivity Callback when "Do the magic!" button is clicked
 * @param onDismiss Callback when modal is dismissed
 */
@Composable
fun ActivityCreationModal(
    isVisible: Boolean,
    activityInput: String,
    words: List<Word>,
    selectedWordIds: Set<Long>,
    isLoading: Boolean,
    generationPhase: GenerationPhase = GenerationPhase.Idle,
    error: String? = null,
    onActivityInputChanged: (String) -> Unit,
    onWordSelectionChanged: (Long, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onCreateActivity: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val focusManager = LocalFocusManager.current
    val isSubmitEnabled = activityInput.isNotBlank() && selectedWordIds.isNotEmpty()

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Main card content (positioned below the mascot)
            Column(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                // Blue header section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF49A9FF))
                )

                // White content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Activity Creation with Kuu",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Activity input section
                    ActivityInputSection(
                        activityInput = activityInput,
                        isLoading = isLoading,
                        onActivityInputChanged = onActivityInputChanged,
                        onSubmit = {
                            focusManager.clearFocus()
                            if (isSubmitEnabled) {
                                onCreateActivity()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Word selection section
                    WordSelectionSection(
                        words = words,
                        selectedWordIds = selectedWordIds,
                        isLoading = isLoading,
                        onWordSelectionChanged = onWordSelectionChanged,
                        onSelectAll = onSelectAll
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Magic button
                    MagicButton(
                        isLoading = isLoading,
                        generationPhase = generationPhase,
                        isEnabled = isSubmitEnabled,
                        onClick = {
                            focusManager.clearFocus()
                            onCreateActivity()
                        }
                    )

                    // Error message
                    if (error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Mascot image overlapping the top
            Image(
                painter = painterResource(id = R.drawable.dis_wand_sit),
                contentDescription = "Kuu with wand",
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Activity input section with label and text field.
 */
@Composable
private fun ActivityInputSection(
    activityInput: String,
    isLoading: Boolean,
    onActivityInputChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Label
        Text(
            text = "What activity do you want to create?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0B0B0B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Field
        OutlinedTextField(
            value = activityInput,
            onValueChange = onActivityInputChanged,
            textStyle = TextStyle(
                color = Color(0xFF49A9FF),
                fontSize = 16.sp
            ),
            placeholder = {
                Text(
                    text = "Describe your activity...",
                    color = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF49A9FF),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF49A9FF),
                disabledBorderColor = Color(0xFFE0E0E0),
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedTextColor = Color(0xFF49A9FF),
                unfocusedTextColor = Color(0xFF49A9FF),
                disabledTextColor = Color(0xFF49A9FF).copy(alpha = 0.5f)
            ),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            maxLines = 4
        )
    }
}

/**
 * Word selection section with header, counter, and word chips.
 */
@Composable
private fun WordSelectionSection(
    words: List<Word>,
    selectedWordIds: Set<Long>,
    isLoading: Boolean,
    onWordSelectionChanged: (Long, Boolean) -> Unit,
    onSelectAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section label
        Text(
            text = "Select the words you want for the activity!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0B0B0B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Counter and Select all row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedWordIds.size} words selected",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            if (words.isNotEmpty()) {
                Text(
                    text = "Select all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF49A9FF),
                    modifier = Modifier.clickable(
                        enabled = !isLoading,
                        onClick = onSelectAll
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Word chips grid
        if (words.isEmpty()) {
            Text(
                text = "No words in your Word Bank yet. Add some words first!",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val scrollState = rememberScrollState()
            val row1Words = words.filterIndexed { index, _ -> index % 2 == 0 }
            val row2Words = words.filterIndexed { index, _ -> index % 2 != 0 }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1Words.forEach { word ->
                        val isSelected = selectedWordIds.contains(word.id)
                        WordChip(
                            word = word.word,
                            isSelected = isSelected,
                            isEnabled = !isLoading,
                            onClick = { onWordSelectionChanged(word.id, !isSelected) }
                        )
                    }
                }
                if (row2Words.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row2Words.forEach { word ->
                            val isSelected = selectedWordIds.contains(word.id)
                            WordChip(
                                word = word.word,
                                isSelected = isSelected,
                                isEnabled = !isLoading,
                                onClick = { onWordSelectionChanged(word.id, !isSelected) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual word chip component.
 */
@Composable
private fun WordChip(
    word: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF49A9FF) else Color.White
    val textColor = if (isSelected) Color.White else Color(0xFF49A9FF)
    val borderColor = Color(0xFF49A9FF)

    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                color = if (isEnabled) borderColor else borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(if (isEnabled) backgroundColor else backgroundColor.copy(alpha = 0.3f))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * "Do the magic!" button with wand icon.
 */
@Composable
private fun MagicButton(
    isLoading: Boolean,
    generationPhase: GenerationPhase = GenerationPhase.Idle,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF49A9FF),
            disabledContainerColor = Color(0xFFB0D9FF)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (generationPhase) {
                    is GenerationPhase.Filtering -> "Filtering words... (1/3)"
                    is GenerationPhase.Grouping -> "Grouping sets... (2/3)"
                    is GenerationPhase.Configuring -> "Configuring... (3/3)"
                    else -> "Generating..."
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        } else {
            // Wand icon
            Icon(
                painter = painterResource(id = R.drawable.ic_wand),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Do the magic!",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivityCreationModalPreview() {
    val sampleWords = listOf(
        Word(id = 1L, userId = 1L, word = "cat"),
        Word(id = 2L, userId = 1L, word = "dog"),
        Word(id = 3L, userId = 1L, word = "bird"),
        Word(id = 4L, userId = 1L, word = "fish"),
        Word(id = 5L, userId = 1L, word = "elephant"),
        Word(id = 6L, userId = 1L, word = "lion")
    )

    ActivityCreationModal(
        isVisible = true,
        activityInput = "",
        words = sampleWords,
        selectedWordIds = emptySet(),
        isLoading = false,
        onActivityInputChanged = {},
        onWordSelectionChanged = { _, _ -> },
        onSelectAll = {},
        onCreateActivity = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ActivityCreationModalWithSelectionPreview() {
    val sampleWords = listOf(
        Word(id = 1L, userId = 1L, word = "cat"),
        Word(id = 2L, userId = 1L, word = "dog"),
        Word(id = 3L, userId = 1L, word = "bird"),
        Word(id = 4L, userId = 1L, word = "fish"),
        Word(id = 5L, userId = 1L, word = "elephant"),
        Word(id = 6L, userId = 1L, word = "lion")
    )

    ActivityCreationModal(
        isVisible = true,
        activityInput = "Create a story about animals",
        words = sampleWords,
        selectedWordIds = setOf(1L, 2L),
        isLoading = false,
        onActivityInputChanged = {},
        onWordSelectionChanged = { _, _ -> },
        onSelectAll = {},
        onCreateActivity = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ActivityCreationModalLoadingPreview() {
    val sampleWords = listOf(
        Word(id = 1L, userId = 1L, word = "cat"),
        Word(id = 2L, userId = 1L, word = "dog"),
        Word(id = 3L, userId = 1L, word = "bird")
    )

    ActivityCreationModal(
        isVisible = true,
        activityInput = "Create a story",
        words = sampleWords,
        selectedWordIds = setOf(1L, 2L),
        isLoading = true,
        onActivityInputChanged = {},
        onWordSelectionChanged = { _, _ -> },
        onSelectAll = {},
        onCreateActivity = {},
        onDismiss = {}
    )
}
