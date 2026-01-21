package com.example.app.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BatteryIcon(
    percentage: Int?,
    modifier: Modifier = Modifier
) {
    val batteryColor = Color(0xFF14FF1E)

    Box(modifier = modifier.size(24.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bodyWidth = size.width * 0.75f
            val bodyHeight = size.height * 0.65f
            val strokeWidth = 1.dp.toPx()

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(0f, (size.height - bodyHeight) / 2),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(4.3.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = Offset(bodyWidth, size.height * 0.35f),
                size = Size(size.width * 0.25f, size.height * 0.3f)
            )

            percentage?.let {
                val fillWidth = (bodyWidth - strokeWidth * 2) * (it / 100f)
                val fillHeight = bodyHeight - strokeWidth * 2
                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(strokeWidth, (size.height - bodyHeight) / 2 + strokeWidth),
                    size = Size(fillWidth, fillHeight),
                    cornerRadius = CornerRadius(2.5.dp.toPx())
                )
            }
        }
    }
}

