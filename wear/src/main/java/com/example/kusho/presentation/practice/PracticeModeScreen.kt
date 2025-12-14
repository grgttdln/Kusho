package com.example.kusho.presentation.practice

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.kusho.ml.Preprocessor
import com.example.kusho.ml.TFLiteAirWritingClassifier
import com.example.kusho.presentation.theme.AppColors
import com.example.kusho.sensors.MotionSensorManager

/**
 * Practice Mode screen: shows only the predicted letter and a Start/Stop button.
 * After each prediction, waits 2 seconds and then plays a sound, and continues
 * in a loop until the user presses Stop.
 */
@Composable
fun PracticeModeScreen() {
    val context = LocalContext.current

    // Dependencies
    val motionSensorManager = remember { MotionSensorManager(context) }
    val preprocessor = remember { Preprocessor() }

    val classifierResult = remember {
        try {
            Result.success(TFLiteAirWritingClassifier(context))
        } catch (e: Exception) {
            Result.failure<TFLiteAirWritingClassifier>(e)
        }
    }
    val classifier = classifierResult.getOrNull()

    val viewModel: PracticeModeViewModel = viewModel(
        factory = PracticeModeViewModelFactory(
            motionSensorManager = motionSensorManager,
            preprocessor = preprocessor,
            classifier = classifier
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // Sound cue with 2-second cooldown after each prediction
    val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val ringtone: Ringtone? = remember { RingtoneManager.getRingtone(context, notificationUri) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Predicted letter (or '-' before first prediction)
            Text(
                text = uiState.currentPrediction ?: "-",
                color = AppColors.TextPrimary,
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Start/Stop button controlling continuous practice
            Button(
                onClick = {
                    if (!uiState.isCollecting) {
                        viewModel.startPractice(onPrediction = {
                            // After each prediction, wait 2 seconds, then play sound
                            handler.postDelayed({ ringtone?.play() }, 2000L)
                        })
                    } else {
                        viewModel.stopPractice()
                    }
                },
                enabled = classifier != null
            ) {
                Text(if (uiState.isCollecting) "Stop" else "Start")
            }
        }
    }
}