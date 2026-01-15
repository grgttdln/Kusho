package com.example.app.ui.feature.learn.learnmode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

private val PurpleColor = Color(0xFFAE8EFB)
private val LightPurpleColor = Color(0xFFE7DDFE)
private val BlueButtonColor = Color(0xFF3FA9F8)

@Composable
fun LearnModeSessionScreen(
    title: String,
    modifier: Modifier = Modifier,
    initialStep: Int = 1,
    totalSteps: Int = 5,
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(initialStep) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Bar
        ProgressIndicator(
            currentStep = currentStep,
            totalSteps = totalSteps,
            modifier = Modifier.fillMaxWidth(),
            activeColor = PurpleColor,
            inactiveColor = LightPurpleColor
        )

        Spacer(Modifier.height(12.dp))

        // Audio Icon and Skip Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAudioClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_volume),
                    contentDescription = "Audio",
                    tint = PurpleColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            TextButton(onClick = {
                if (currentStep < totalSteps) {
                    currentStep++
                }
                onSkip()
            }) {
                Text(
                    text = "Skip",
                    color = PurpleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(24.dp))

        // Large Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(3.dp, PurpleColor)
        ) {
            // Empty content area - can be customized later
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Content will go here
            }
        }

        Spacer(Modifier.height(24.dp))
        // No End Session Button
    }
}

@Composable
private fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = PurpleColor,
    inactiveColor: Color = LightPurpleColor
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) activeColor else inactiveColor
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LearnModeSessionScreenPreview() {
    LearnModeSessionScreen(
        title = "Vowels",
        initialStep = 1,
        totalSteps = 5
    )
}