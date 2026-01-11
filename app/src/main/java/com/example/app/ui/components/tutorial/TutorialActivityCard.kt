package com.example.app.ui.components.tutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextAlign
import com.example.app.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/**
 * Component for displaying a tutorial activity card with icon and title.
 * Uses yellow/orange color scheme with dashed border.
 *
 * @param title The activity title
 * @param iconRes The resource ID of the icon image
 * @param isSelected Whether the card is selected
 * @param onClick Callback when card is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun TutorialActivityCard(
    title: String,
    iconRes: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val yellowColor = Color(0xFFEDBB00)
    val lightYellowColor = Color(0x80EDBB00)
    val greenColor = Color(0xFF4CAF50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) yellowColor else Color.Transparent)
            .clickable { onClick() }
            .then(
                if (!isSelected) {
                    Modifier.drawBehind {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
                        drawRoundRect(
                            color = yellowColor,
                            style = Stroke(width = 12f, pathEffect = pathEffect),
                            cornerRadius = CornerRadius(28.dp.toPx())
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(12.dp)
    ) {
        // Selection circle at top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.Transparent)
                .drawBehind {
                    if (!isSelected) {
                        drawCircle(
                            color = yellowColor,
                            style = Stroke(width = 4f)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = greenColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon Circle with Border
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .drawBehind {
                        drawCircle(
                            color = if (isSelected) Color(0x40FFFFFF) else lightYellowColor,
                            style = Stroke(width = 16f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else yellowColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = if (isSelected) Color.White else yellowColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(
    name = "TutorialActivityCard Unselected",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun TutorialActivityCardPreview() {
    MaterialTheme {
        TutorialActivityCard(
            title = "Vowels",
            iconRes = R.drawable.ic_apple,
            isSelected = false,
            onClick = {}
        )
    }
}

@Preview(
    name = "TutorialActivityCard Selected",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun TutorialActivityCardSelectedPreview() {
    MaterialTheme {
        TutorialActivityCard(
            title = "Vowels",
            iconRes = R.drawable.ic_apple,
            isSelected = true,
            onClick = {}
        )
    }
}

