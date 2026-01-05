package com.example.app.ui.feature.classroom

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import kotlin.random.Random

@Composable
fun StudentAddedSuccessScreen(
    studentName: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Confetti Animation
        ConfettiAnimationStudent()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Mascot
                Image(
                    painter = painterResource(id = R.drawable.dis_onb1),
                    contentDescription = "Success",
                    modifier = Modifier
                        .size(350.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(16.dp))

                // Success Title
                Text(
                    text = "Student Added!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0B0B0B),
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )

                Spacer(Modifier.height(16.dp))

                // Success Message
                Text(
                    text = "$studentName is now in your class.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF4A4A4A),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            // Great Button - At Bottom
            PrimaryButton(
                text = "Great!",
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ConfettiAnimationStudent() {
    val confettiColors = listOf(
        Color(0xFFFFC107), // Yellow
        Color(0xFFE91E63), // Pink
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF5722), // Red-Orange
        Color(0xFF9C27B0)  // Purple
    )

    // Create 20 confetti pieces
    for (i in 0..19) {
        ConfettiPieceStudent(
            color = confettiColors.random(),
            startX = Random.nextFloat(),
            delay = Random.nextInt(0, 500)
        )
    }
}

@Composable
fun ConfettiPieceStudent(
    color: Color,
    startX: Float,
    delay: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000 + Random.nextInt(-500, 500),
                easing = LinearEasing,
                delayMillis = delay
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000 + Random.nextInt(-200, 200),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiRotation"
    )

    val offsetX by infiniteTransition.animateFloat(
        initialValue = startX * 400f,
        targetValue = startX * 400f + Random.nextFloat() * 100f - 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confettiX"
    )

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(12.dp)
            .graphicsLayer {
                rotationZ = rotation
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawRect(color = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentAddedSuccessScreenPreview() {
    StudentAddedSuccessScreen(
        studentName = "John Doe",
        onContinue = {}
    )
}
