package com.example.app.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    completedIndices: Set<Int> = emptySet(),
    activeColor: Color = DefaultActiveColor,
    inactiveColor: Color = DefaultInactiveColor,
    completedColor: Color = activeColor,
    height: Dp = 8.dp,
    spacing: Dp = 8.dp,
    cornerRadius: Dp = 4.dp,
    onSegmentClick: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(totalSteps) { index ->
            val segmentColor = when {
                index in completedIndices -> completedColor
                index == currentStep - 1 -> activeColor
                else -> inactiveColor
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(segmentColor)
                    .then(
                        if (onSegmentClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSegmentClick(index) }
                        } else {
                            Modifier
                        }
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
