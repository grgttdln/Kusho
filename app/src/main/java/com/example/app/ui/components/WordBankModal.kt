package com.example.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import coil.compose.AsyncImage

/**
 * Reusable Word Bank Modal component.
 * Displays a dialog with upload media area, word input, and add button.
 *
 * @param isVisible Whether the modal is currently visible
 * @param wordInput Current text in the word input field
 * @param selectedImageUri URI of the selected image (optional)
 * @param inputError Error message to display, if any
 * @param imageError Error message for image upload, if any
 * @param isSubmitEnabled Whether the submit button is enabled
 * @param isLoading Whether the modal is in a loading state
 * @param onWordInputChanged Callback when word input changes
 * @param onMediaUploadClick Callback when upload media area is clicked
 * @param onRemoveImage Callback when remove image button is clicked
 * @param onAddClick Callback when "Add to Word Bank" button is clicked
 * @param onDismiss Callback when modal is dismissed
 */
@Composable
fun WordBankModal(
    isVisible: Boolean,
    wordInput: String,
    selectedImageUri: Uri?,
    inputError: String?,
    imageError: String?,
    isSubmitEnabled: Boolean,
    isLoading: Boolean,
    onWordInputChanged: (String) -> Unit,
    onMediaUploadClick: () -> Unit,
    onRemoveImage: () -> Unit,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val focusManager = LocalFocusManager.current

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
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Upload Media Area or Selected Image Preview
                if (selectedImageUri != null) {
                    SelectedImagePreview(
                        imageUri = selectedImageUri,
                        onRemoveClick = onRemoveImage,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                } else {
                    UploadMediaArea(
                        onClick = onMediaUploadClick,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }

                // Image error message
                if (imageError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = imageError,
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Word Label
                Text(
                    text = "Word",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0B0B0B),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Word Input Field
                OutlinedTextField(
                    value = wordInput,
                    onValueChange = onWordInputChanged,
                    placeholder = {
                        Text(
                            text = "E.g, dog",
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF49A9FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        errorBorderColor = Color.Red,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color(0xFF49A9FF),
                        disabledBorderColor = Color(0xFFE0E0E0),
                        disabledContainerColor = Color(0xFFF5F5F5)
                    ),
                    singleLine = true,
                    isError = inputError != null,
                    enabled = !isLoading,
                    supportingText = if (inputError != null) {
                        { Text(text = inputError, color = Color.Red) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (isSubmitEnabled && !isLoading) {
                                onAddClick()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Add to Word Bank Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onAddClick()
                    },
                    enabled = isSubmitEnabled && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
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
                    } else {
                        Text(
                            text = "Add to Word Bank",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Selected Image Preview component showing the selected image with a remove button.
 */
@Composable
fun SelectedImagePreview(
    imageUri: Uri,
    onRemoveClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F4FD))
    ) {
        // Image
        AsyncImage(
            model = imageUri,
            contentDescription = "Selected image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        // Remove button
        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onRemoveClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove image",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Upload Media Area component with dashed border.
 */
@Composable
fun UploadMediaArea(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dashColor = if (enabled) Color(0xFF49A9FF) else Color(0xFFB0D9FF)
    val backgroundColor = Color(0xFFE8F4FD)
    val contentColor = if (enabled) Color(0xFF49A9FF) else Color(0xFFB0D9FF)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .then(
                if (enabled) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Draw dashed border
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            val dashLength = 10.dp.toPx()
            val gapLength = 6.dp.toPx()

            drawRoundRect(
                color = dashColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashLength, gapLength),
                        0f
                    )
                )
            )
        }

        // Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Plus icon in circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        width = 2.dp,
                        color = contentColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add media",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Upload\nMedia",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WordBankModalPreview() {
    WordBankModal(
        isVisible = true,
        wordInput = "",
        selectedImageUri = null,
        inputError = null,
        imageError = null,
        isSubmitEnabled = false,
        isLoading = false,
        onWordInputChanged = {},
        onMediaUploadClick = {},
        onRemoveImage = {},
        onAddClick = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun WordBankModalWithInputPreview() {
    WordBankModal(
        isVisible = true,
        wordInput = "dog",
        selectedImageUri = null,
        inputError = null,
        imageError = null,
        isSubmitEnabled = true,
        isLoading = false,
        onWordInputChanged = {},
        onMediaUploadClick = {},
        onRemoveImage = {},
        onAddClick = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun UploadMediaAreaPreview() {
    UploadMediaArea(
        onClick = {},
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .padding(16.dp)
    )
}

