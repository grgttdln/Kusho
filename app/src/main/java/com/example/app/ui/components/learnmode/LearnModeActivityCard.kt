package com.example.app.ui.components.learnmode

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.example.app.R

@Composable
fun LearnModeActivityCard(
    title: String,
    iconRes: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = Color(0xFFB8B2F2) // Light purple
    val fillColor = Color(0xFFBA9BFF) // Purple
    val greenColor = Color(0xFF4CAF50)

    Box(
        modifier = modifier
            .width(325.dp)
            .height(175.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        // Background image for unselected state, solid color for selected
        if (!isSelected) {
            Image(
                painter = painterResource(id = R.drawable.bg_lesson),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fillColor)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                                color = borderColor,
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
                        .background(Color.White)
                        .drawBehind {
                            drawCircle(
                                color = borderColor,
                                style = Stroke(width = 24f)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else fillColor),
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
                    color = if (isSelected) Color.White else fillColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun LearnModeActivityCardPreview() {
        MaterialTheme {
            LearnModeActivityCard(
                title = "Vowels",
                iconRes = R.drawable.ic_apple,
                isSelected = false,
                onClick = {}
            )
        }
    }


    @Composable
    fun LearnModeActivityCardSelectedPreview() {
        MaterialTheme {
            LearnModeActivityCard(
                title = "Vowels",
                iconRes = R.drawable.ic_apple,
                isSelected = true,
                onClick = {}
            )
        }
    }
}
