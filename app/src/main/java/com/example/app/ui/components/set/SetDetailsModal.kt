package com.example.app.ui.components.set

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.data.repository.SetRepository

/**
 * Set Details Modal component for viewing set information with edit and delete options.
 * Displays a dialog with the set's title, description, words list, and action buttons.
 *
 * @param setDetails The set details to display
 * @param onDismiss Callback when modal is dismissed
 * @param onEdit Callback when "Edit" button is clicked
 * @param onDelete Callback when "Delete" button is clicked
 */
@Composable
fun SetDetailsModal(
    setDetails: SetRepository.SetDetails,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title Section
                SetTitleSection(
                    title = setDetails.set.title,
                    description = setDetails.set.description
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Words Section
                WordsSection(
                    words = setDetails.words
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                ActionButtonsRow(
                    onEditClick = onEdit,
                    onDeleteClick = { showDeleteConfirmation = true }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            setTitle = setDetails.set.title,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

/**
 * Title section displaying set name and description.
 */
@Composable
private fun SetTitleSection(
    title: String,
    description: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Set Title Label
        Text(
            text = "Set Title",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0B0B0B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0B0B0B)
            )
        }

        // Description if available
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Description",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0B0B0B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF808080)
                )
            }
        }
    }
}

/**
 * Words section displaying the list of words with their configuration types.
 */
@Composable
private fun WordsSection(
    words: List<SetRepository.WordWithConfig>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Words Label
        Text(
            text = "Words (${words.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0B0B0B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Words List Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8F4FD))
                .border(1.dp, Color(0xFFB8DDF8), RoundedCornerShape(12.dp))
        ) {
            if (words.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No words in this set",
                        fontSize = 14.sp,
                        color = Color(0xFF808080),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(words) { wordWithConfig ->
                        WordItem(
                            word = wordWithConfig.word,
                            configurationType = wordWithConfig.configurationType
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual word item displaying word text and configuration type.
 */
@Composable
private fun WordItem(
    word: String,
    configurationType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0B0B0B),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = configurationType,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF49A9FF)
            )
        }
    }
}

/**
 * Row containing Edit and Delete action buttons.
 */
@Composable
private fun ActionButtonsRow(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Edit button
        Button(
            onClick = onEditClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF49A9FF)
            )
        ) {
            Text(
                text = "Edit",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Delete button
        Button(
            onClick = onDeleteClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Text(
                text = "Delete",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * Delete confirmation dialog.
 */
@Composable
private fun DeleteConfirmationDialog(
    setTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Set",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Are you sure you want to delete \"$setTitle\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Delete",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF49A9FF)
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun SetDetailsModalPreview() {
    // Preview placeholder - cannot preview actual modal without SetDetails
}
