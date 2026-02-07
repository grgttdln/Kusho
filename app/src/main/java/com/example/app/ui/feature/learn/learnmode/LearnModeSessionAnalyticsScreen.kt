package com.example.app.ui.feature.learn.learnmode

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.service.WatchConnectionManager

private val PurpleColor = Color(0xFFAE8EFB)
private val LightPurpleBg = Color(0xFFE7DDFE)

@Composable
fun LearnModeSessionAnalyticsScreen(
    onPracticeAgain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    score: String = "9/10",
    gestureAccuracy: String = "78.6%",
    timeSpent: String = "2m 4s"
) {
    val context = LocalContext.current
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }

    // Wrapper functions to notify watch before navigating away
    val handlePracticeAgain: () -> Unit = {
        watchConnectionManager.notifyLearnModeEnded()
        onPracticeAgain()
    }

    val handleContinue: () -> Unit = {
        watchConnectionManager.notifyLearnModeEnded()
        onContinue()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 10.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_kusho),
            contentDescription = "Kusho Logo",
            modifier = Modifier
                .height(54.dp)
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .offset(x = 10.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Great Job!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Learn Mode Completed!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.dis_champion),
                contentDescription = "Champion",
                modifier = Modifier.size(300.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightPurpleBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItemPurple(
                        value = score,
                        label = "Score",
                        progress = 0.9f
                    )

                    StatItemPurple(
                        value = gestureAccuracy,
                        label = "Gesture Accuracy",
                        progress = 0.786f
                    )

                    StatItemPurple(
                        value = timeSpent,
                        label = "Time Spent",
                        progress = 1f
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = handlePracticeAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFF3FA9F8)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3FA9F8))
            ) {
                Text(
                    text = "Learn Again",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = handleContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FA9F8)),
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
}

@Composable
private fun StatItemPurple(
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
            Canvas(modifier = Modifier.size(70.dp)) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                drawArc(
                    color = PurpleColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleColor
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = PurpleColor,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LearnModeSessionAnalyticsScreenPreview() {
    LearnModeSessionAnalyticsScreen(
        score = "9/10",
        gestureAccuracy = "78.6%",
        timeSpent = "2m 4s",
        onPracticeAgain = {},
        onContinue = {}
    )
}
