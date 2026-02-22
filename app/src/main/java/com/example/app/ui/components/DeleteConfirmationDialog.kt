package com.example.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Type of deletion action for the confirmation dialog.
 */
enum class DeleteType {
    SET,
    ACTIVITY,
    SET_FROM_ACTIVITY,
    WORD,
    DISCARD_ACTIVITY
}

/**
 * Reusable confirmation dialog for deletion actions.
 * Supports deleting sets, activities, and removing sets from activities.
 *
 * @param isVisible Whether the dialog is currently visible
 * @param deleteType The type of deletion being performed
 * @param onConfirm Callback when the user confirms deletion
 * @param onDismiss Callback when the dialog is dismissed or cancelled
 */
@Composable
fun DeleteConfirmationDialog(
    isVisible: Boolean,
    deleteType: DeleteType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val message = when (deleteType) {
        DeleteType.SET -> "Delete this Activity?"
        DeleteType.ACTIVITY -> "Delete this Activity Set?"
        DeleteType.SET_FROM_ACTIVITY -> "Remove Activity From this Activity Set?"
        DeleteType.WORD -> "Delete this Word?"
        DeleteType.DISCARD_ACTIVITY -> "Discard this Activity?"
    }

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
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Confirmation message
                    Text(
                        text = message,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Confirm button
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49A9FF)
                            )
                        ) {
                            Text(
                                text = "Confirm",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Cancel button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = 0.dp,
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(28.dp)
                                ),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD6EDFF)
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF49A9FF)
                            )
                        }
                    }
                }
            }

            // Mascot image overlapping the top
            Image(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Delete confirmation",
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEEEE)
@Composable
fun DeleteConfirmationDialogPreview_Set() {
    DeleteConfirmationDialog(
        isVisible = true,
        deleteType = DeleteType.SET,
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEEEE)
@Composable
fun DeleteConfirmationDialogPreview_Activity() {
    DeleteConfirmationDialog(
        isVisible = true,
        deleteType = DeleteType.ACTIVITY,
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEEEE)
@Composable
fun DeleteConfirmationDialogPreview_SetFromActivity() {
    DeleteConfirmationDialog(
        isVisible = true,
        deleteType = DeleteType.SET_FROM_ACTIVITY,
        onConfirm = {},
        onDismiss = {}
    )
}

