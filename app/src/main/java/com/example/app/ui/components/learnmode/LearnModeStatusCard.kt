package com.example.app.ui.components.learnmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

@Composable
fun LearnModeStatusCard(
    title: String,
    status: String = "Not Started",
    completionPercentage: Int = 0,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFAE8EFB) else Color(0xFFF8F5FF))
            .padding(10.dp)
            .fillMaxWidth()
            .height(420.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconRes = if (status == "Completed") R.drawable.ic_star_completed else R.drawable.ic_star_notstarted
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Status Icon",
                modifier = Modifier.size(125.dp)
            )

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF181818),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            val isCompleted = status == "Completed"
            val fillFraction = (completionPercentage.coerceIn(0, 100)) / 100f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isCompleted && !isSelected) 0.dp else if (!isCompleted && completionPercentage == 0) 2.dp else 0.dp,
                        color = Color(0xFFBA9BFF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        if (isSelected) Color.White else Color(0xFFF0EAFF),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                // Fill layer (proportional)
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = fillFraction)
                            .background(
                                if (isSelected) Color(0xFFBA9BFF) else Color(0xFFAE8EFB),
                                RoundedCornerShape(12.dp)
                            )
                    )
                }

                // Text layer (centered)
                Text(
                    text = status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isSelected && isCompleted -> Color(0xFFAE8EFB)
                        isSelected -> Color(0xFFAE8EFB)
                        isCompleted -> Color.White
                        completionPercentage > 0 -> Color.White
                        else -> Color(0xFFBA9BFF)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(
    name = "LearnModeStatusCard",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
fun LearnModeStatusCardPreview() {
    MaterialTheme {
        Surface(color = Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LearnModeStatusCard(
                    title = "Short Vowels",
                    status = "Not Started",
                    completionPercentage = 0
                )
            }
        }
    }
}

@Preview(name = "LearnModeStatusCard In Progress", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LearnModeStatusCardInProgressPreview() {
    MaterialTheme {
        Surface(color = Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LearnModeStatusCard(
                    title = "Short Vowels",
                    status = "60% Progress",
                    completionPercentage = 60
                )
            }
        }
    }
}

@Preview(name = "LearnModeStatusCard Selected", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LearnModeStatusCardSelectedPreview() {
    MaterialTheme {
        Surface(color = Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LearnModeStatusCard(
                    title = "Short Vowels",
                    status = "Not Started",
                    completionPercentage = 0,
                    isSelected = true
                )
            }
        }
    }
}