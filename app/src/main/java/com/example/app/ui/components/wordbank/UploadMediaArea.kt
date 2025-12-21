package com.example.app.ui.components.wordbank

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Upload Media Area component with dashed border.
 * Displays a clickable area for users to upload images.
 *
 * @param onClick Callback when the area is clicked
 * @param modifier Modifier for the component
 * @param enabled Whether the component is interactive
 */
@Composable
fun UploadMediaArea(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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
fun UploadMediaAreaPreview() {
    UploadMediaArea(
        onClick = {},
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun UploadMediaAreaDisabledPreview() {
    UploadMediaArea(
        onClick = {},
        enabled = false,
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .padding(16.dp)
    )
}

