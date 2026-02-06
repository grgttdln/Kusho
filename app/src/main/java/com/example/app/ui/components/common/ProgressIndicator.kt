package com.example.app.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultActiveColor = Color(0xFFEDBB00)
private val DefaultInactiveColor = Color(0xFFFFF3C4)

@Composable
fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = DefaultActiveColor,
    inactiveColor: Color = DefaultInactiveColor,
    height: Dp = 8.dp,
    spacing: Dp = 8.dp,
    cornerRadius: Dp = 4.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        if (isActive) activeColor else inactiveColor
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressIndicatorPreview() {
    ProgressIndicator(
        currentStep = 3,
        totalSteps = 5,
        modifier = Modifier
    )
}

@Preview(showBackground = true)
@Composable
fun ProgressIndicatorCustomColorsPreview() {
    ProgressIndicator(
        currentStep = 2,
        totalSteps = 4,
        activeColor = Color(0xFF4CAF50),
        inactiveColor = Color(0xFFE8F5E9)
    )
}
