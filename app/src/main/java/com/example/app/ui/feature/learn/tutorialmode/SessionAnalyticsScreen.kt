package com.example.app.ui.feature.learn.tutorialmode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

private val YellowColor = Color(0xFFEDBB00)
private val LightYellowBg = Color(0xFFFFF9E6)
private val BlueButtonColor = Color(0xFF3FA9F8)

@Composable
fun SessionAnalyticsScreen(
    onPracticeAgain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    score: String = "9/10",
    gestureAccuracy: String = "78.6%",
    timeSpent: String = "2m 4s"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kusho Logo
        Image(
            painter = painterResource(id = R.drawable.ic_kusho),
            contentDescription = "Kusho Logo",
            modifier = Modifier
                .height(54.dp)
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(32.dp))

        // Congratulations Text
        Text(
            text = "Congratulations!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Tutorial Completed!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        // Champion Avatar Image
        // Note: Replace with R.drawable.dis_chamption when the image is added
        Image(
            painter = painterResource(id = R.drawable.dis_champion),
            contentDescription = "Champion",
            modifier = Modifier
                .size(300.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(24.dp))

        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = LightYellowBg
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score
                StatItem(
                    value = score,
                    label = "Score",
                    progress = 0.9f
                )

                // Gesture Accuracy
                StatItem(
                    value = gestureAccuracy,
                    label = "Gesture Accuracy",
                    progress = 0.786f
                )

                // Time Spent
                StatItem(
                    value = timeSpent,
                    label = "Time Spent",
                    progress = 1f
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Practice Again Button
        OutlinedButton(
            onClick = onPracticeAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, BlueButtonColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BlueButtonColor
            )
        ) {
            Text(
                text = "Practice Again",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueButtonColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(70.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular progress
            Canvas(modifier = Modifier.size(70.dp)) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background circle
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc
                drawArc(
                    color = YellowColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Value text
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = YellowColor
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = YellowColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionAnalyticsScreenPreview() {
    SessionAnalyticsScreen(
        score = "9/10",
        gestureAccuracy = "78.6%",
        timeSpent = "2m 4s",
        onPracticeAgain = {},
        onContinue = {}
    )
}

