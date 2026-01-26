// filepath: /Users/georgette/AndroidStudioProjects/Kusho/app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt
package com.example.app.ui.feature.learn.tutorialmode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

private val YellowColor = Color(0xFFEDBB00)
private val LightYellowColor = Color(0xFFFFF3C4)
private val BlueButtonColor = Color(0xFF3FA9F8)

@Composable
fun TutorialSessionScreen(
    title: String,
    letterType: String = "capital",
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 1,
    totalSteps: Int = 0, // Will be calculated from letters
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(initialStep) }
    
    // Define all letters based on the section
    val allLetters = remember(title) {
        when (title.lowercase()) {
            "vowels" -> listOf(
                R.drawable.ic_letter_a_upper, R.drawable.ic_letter_a_lower,
                R.drawable.ic_letter_e_upper, R.drawable.ic_letter_e_lower,
                R.drawable.ic_letter_i_upper, R.drawable.ic_letter_i_lower,
                R.drawable.ic_letter_o_upper, R.drawable.ic_letter_o_lower,
                R.drawable.ic_letter_u_upper, R.drawable.ic_letter_u_lower
            )
            "consonants" -> listOf(
                R.drawable.ic_letter_b_upper, R.drawable.ic_letter_b_lower,
                R.drawable.ic_letter_c_upper, R.drawable.ic_letter_c_lower,
                R.drawable.ic_letter_d_upper, R.drawable.ic_letter_d_lower,
                R.drawable.ic_letter_f_upper, R.drawable.ic_letter_f_lower,
                R.drawable.ic_letter_g_upper, R.drawable.ic_letter_g_lower,
                R.drawable.ic_letter_h_upper, R.drawable.ic_letter_h_lower,
                R.drawable.ic_letter_j_upper, R.drawable.ic_letter_j_lower,
                R.drawable.ic_letter_k_upper, R.drawable.ic_letter_k_lower,
                R.drawable.ic_letter_l_upper, R.drawable.ic_letter_l_lower,
                R.drawable.ic_letter_m_upper, R.drawable.ic_letter_m_lower,
                R.drawable.ic_letter_n_upper, R.drawable.ic_letter_n_lower,
                R.drawable.ic_letter_p_upper, R.drawable.ic_letter_p_lower,
                R.drawable.ic_letter_q_upper, R.drawable.ic_letter_q_lower,
                R.drawable.ic_letter_r_upper, R.drawable.ic_letter_r_lower,
                R.drawable.ic_letter_s_upper, R.drawable.ic_letter_s_lower,
                R.drawable.ic_letter_t_upper, R.drawable.ic_letter_t_lower,
                R.drawable.ic_letter_v_upper, R.drawable.ic_letter_v_lower,
                R.drawable.ic_letter_w_upper, R.drawable.ic_letter_w_lower,
                R.drawable.ic_letter_x_upper, R.drawable.ic_letter_x_lower,
                R.drawable.ic_letter_y_upper, R.drawable.ic_letter_y_lower,
                R.drawable.ic_letter_z_upper, R.drawable.ic_letter_z_lower
            )
            else -> emptyList()
        }
    }
    
    // Filter letters based on type (capital or small)
    val letters = remember(allLetters, letterType) {
        when (letterType.lowercase()) {
            "capital" -> allLetters.filterIndexed { index, _ -> index % 2 == 0 }
            "small" -> allLetters.filterIndexed { index, _ -> index % 2 == 1 }
            else -> allLetters
        }
    }
    
    // Get current letter based on step
    val currentLetterRes = remember(currentStep, letters) {
        if (letters.isNotEmpty() && currentStep > 0 && currentStep <= letters.size) {
            letters[currentStep - 1]
        } else {
            null
        }
    }
    
    // Calculate total steps from letters
    val calculatedTotalSteps = remember(letters) { letters.size }

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
            totalSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps,
            modifier = Modifier.fillMaxWidth()
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
                    tint = YellowColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            TextButton(onClick = {
                val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                if (currentStep < maxSteps) {
                    currentStep++
                }
                onSkip()
            }) {
                Text(
                    text = "Skip",
                    color = YellowColor,
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
            border = BorderStroke(3.dp, YellowColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Display single letter for current step
                currentLetterRes?.let { letterRes ->
                    Image(
                        painter = painterResource(id = letterRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // End Session Button
        Button(
            onClick = onEndSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueButtonColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "End Session",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
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
                        if (isActive) YellowColor else LightYellowColor
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialSessionScreenPreview() {
    TutorialSessionScreen(
        title = "Vowels",
        initialStep = 1,
        totalSteps = 5,
        onEndSession = {}
    )
}

