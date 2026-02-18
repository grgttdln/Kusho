package com.example.app.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.R

/**
 * Reusable error dialog component that can be used throughout the app.
 * Shows an error mascot sitting on top of the dialog with an error message.
 * 
 * Use cases:
 * - Duplicate set names
 * - Duplicate activity names
 * - Duplicate class names
 * - Validation errors
 * - Any other error scenarios
 *
 * @param isVisible Whether the dialog is currently visible
 * @param title The error title (e.g., "Already Exists!", "Error!", "Oops!")
 * @param message The error message to display
 * @param buttonText The text for the dismiss button (default: "OK")
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun ErrorDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    buttonText: String = "OK",
    onDismiss: () -> Unit
) {
    if (!isVisible) return

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
                .fillMaxWidth(0.85f)
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
                        .padding(horizontal = 32.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error title
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error message
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dismiss button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF49A9FF)
                        )
                    ) {
                        Text(
                            text = buttonText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Error mascot image sitting on top (overlapping the container)
            Image(
                painter = painterResource(id = R.drawable.dis_remove),
                contentDescription = "Error",
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Convenience composable for "Already Exists" error
 */
@Composable
fun AlreadyExistsDialog(
    isVisible: Boolean,
    itemType: String, // "Set", "Activity", "Class", etc.
    onDismiss: () -> Unit
) {
    ErrorDialog(
        isVisible = isVisible,
        title = "Already Exists!",
        message = "A $itemType with this name already exists. Please choose a different name.",
        buttonText = "OK",
        onDismiss = onDismiss
    )
}

/**
 * Dialog shown when the AI-generated activity title is similar to an existing one.
 * Offers "View Existing Activity" and "OK" buttons.
 */
@Composable
fun SimilarTitleDialog(
    isVisible: Boolean,
    existingTitle: String,
    reason: String,
    onViewExisting: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

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
                .fillMaxWidth(0.85f)
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
                // Orange header section (distinct from blue error dialog)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFFFF9800))
                )

                // White content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Similar Activity Exists!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "An activity similar to this already exists: \"$existingTitle\". $reason",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // View Existing Activity button
                    Button(
                        onClick = onViewExisting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text(
                            text = "View Existing Activity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OK / Dismiss button
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFF9800))
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // Mascot image sitting on top (overlapping the container)
            Image(
                painter = painterResource(id = R.drawable.dis_remove),
                contentDescription = "Warning",
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorDialogPreview() {
    ErrorDialog(
        isVisible = true,
        title = "Already Exists!",
        message = "A set with this name already exists. Please choose a different name.",
        buttonText = "OK",
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AlreadyExistsDialogPreview() {
    AlreadyExistsDialog(
        isVisible = true,
        itemType = "Activity",
        onDismiss = {}
    )
}
