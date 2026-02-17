package com.example.app.ui.feature.learn.tutorialmode

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.app.ui.feature.classroom.ConfettiAnimationStudent

private val BlueButtonColor = Color(0xFF3FA9F8)
private val ScreenBackgroundColor = Color(0xFFFDF8E5)

@Composable
fun SessionAnalyticsScreen(
    onPracticeAgain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    score: String = "9/10",
    gestureAccuracy: String = "78.6%",
    timeSpent: String = "2m 4s"
) {
    val context = LocalContext.current
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    
    // Play finish audio when screen appears
    val mediaPlayer = remember { MediaPlayer() }
    DisposableEffect(Unit) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.finish}"))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            mediaPlayer.release()
        }
    }
    
    // Wrapper functions to notify watch before navigating away
    val handlePracticeAgain: () -> Unit = {
        watchConnectionManager.notifyTutorialModeEnded()
        onPracticeAgain()
    }

    val handleContinue: () -> Unit = {
        watchConnectionManager.notifyTutorialModeEnded()
        onContinue()
    }

    Box(modifier = modifier.fillMaxSize().background(ScreenBackgroundColor)) {
        ConfettiAnimationStudent()

        Column(
            modifier = Modifier
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

            Spacer(Modifier.height(30.dp))

            // Great Job text at the top
            Text(
                text = "Great Job!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFEDBB00),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Tutorial Completed!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDBB00),
                textAlign = TextAlign.Center
            )



            // Avatar in the center
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_champion),
                    contentDescription = "Champion",
                    modifier = Modifier.size(400.dp),
                    contentScale = ContentScale.Fit
                )
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
                border = BorderStroke(2.dp, BlueButtonColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
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

                Button(
                    onClick = handleContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueButtonColor),
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
