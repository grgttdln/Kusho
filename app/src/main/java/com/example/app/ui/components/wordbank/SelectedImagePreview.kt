package com.example.app.ui.components.wordbank

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Selected Image Preview component showing the selected image with a remove button.
 * Displays an image thumbnail with an overlay button to remove the selection.
 *
 * @param imageUri The URI of the selected image
 * @param onRemoveClick Callback when the remove button is clicked
 * @param modifier Modifier for the component
 * @param enabled Whether the remove button is interactive
 */
@Composable
fun SelectedImagePreview(
    imageUri: Uri,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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

@Preview(showBackground = true)
@Composable
fun SelectedImagePreviewPlaceholder() {
    // Preview with a placeholder background (actual image requires URI)
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F4FD)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Image Preview\n(requires URI)",
            color = Color.Gray
        )
    }
}

