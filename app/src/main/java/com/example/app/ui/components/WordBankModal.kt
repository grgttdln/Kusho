package com.example.app.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Reusable Word Bank Modal component.
 * Displays a dialog with upload media area, word input, and add button.
 *
 * @param isVisible Whether the modal is currently visible
 * @param wordInput Current text in the word input field
 * @param inputError Error message to display, if any
 * @param isSubmitEnabled Whether the submit button is enabled
 * @param onWordInputChanged Callback when word input changes
 * @param onMediaUploadClick Callback when upload media area is clicked
 * @param onAddClick Callback when "Add to Word Bank" button is clicked
 * @param onDismiss Callback when modal is dismissed
 */
@Composable
fun WordBankModal(
    isVisible: Boolean,
    wordInput: String,
    inputError: String?,
    isSubmitEnabled: Boolean,
    onWordInputChanged: (String) -> Unit,
    onMediaUploadClick: () -> Unit,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
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
                // Upload Media Area
                UploadMediaArea(
                    onClick = onMediaUploadClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

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
                        errorBorderColor = Color(0xFF49A9FF),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color(0xFF49A9FF)
                    ),
                    singleLine = true,
                    isError = inputError != null,
                    supportingText = if (inputError != null) {
                        { Text(text = inputError, color = Color(0xFF49A9FF)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (isSubmitEnabled) {
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
                    enabled = isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF49A9FF),
                        disabledContainerColor = Color(0xFFB0D9FF)
                    )
                ) {
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

/**
 * Upload Media Area component with dashed border.
 */
@Composable
fun UploadMediaArea(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashColor = Color(0xFF49A9FF)
    val backgroundColor = Color(0xFFE8F4FD)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() },
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
                        color = Color(0xFF49A9FF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add media",
                    tint = Color(0xFF49A9FF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Upload\nMedia",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF49A9FF),
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
        inputError = null,
        isSubmitEnabled = false,
        onWordInputChanged = {},
        onMediaUploadClick = {},
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
        inputError = null,
        isSubmitEnabled = true,
        onWordInputChanged = {},
        onMediaUploadClick = {},
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

