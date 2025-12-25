package com.example.kusho.presentation.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.kusho.ml.ModelLoader
import com.example.kusho.presentation.components.CircularModeBorder
import com.example.kusho.presentation.theme.AppColors
import com.example.kusho.sensors.MotionSensorManager

/**
 * Practice Mode screen: countdown -> record gesture -> classify -> show result
 */
@Composable
fun PracticeModeScreen() {
    val context = LocalContext.current

    // Load dependencies
    val sensorManager = remember { MotionSensorManager(context) }
    // Changed loadAnyAvailable to loadDefault
    val classifierResult = remember { ModelLoader.loadDefault(context) }

    val viewModel: PracticeModeViewModel = viewModel(
        factory = PracticeModeViewModelFactory(sensorManager, classifierResult)
    )

    val uiState by viewModel.uiState.collectAsState()

    CircularModeBorder(borderColor = AppColors.PracticeModeColor) {
        Scaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState.state) {
                    PracticeModeViewModel.State.IDLE -> IdleContent(uiState, viewModel)
                    PracticeModeViewModel.State.COUNTDOWN -> CountdownContent(uiState, viewModel)
                    PracticeModeViewModel.State.RECORDING -> RecordingContent(uiState, viewModel)
                    PracticeModeViewModel.State.PROCESSING -> ProcessingContent(uiState)
                    PracticeModeViewModel.State.RESULT -> ResultContent(uiState)
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    // Show last prediction if available
    Text(
        text = uiState.prediction ?: "-",
        color = if (uiState.errorMessage != null) Color.Gray else AppColors.TextPrimary,
        fontSize = 40.sp
    )

    // Show confidence if available
    uiState.confidence?.let { conf ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(conf * 100).toInt()}%",
            color = when {
                uiState.errorMessage != null -> Color.Red
                conf >= 0.6f -> Color.Green
                conf >= 0.3f -> Color.Yellow
                else -> Color.Red
            },
            fontSize = 16.sp
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Status message
    Text(
        text = uiState.statusMessage,
        color = AppColors.TextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Start button - always enabled, will show error if model not loaded
    Button(
        onClick = { viewModel.startRecording() }
    ) {
        Text("Start")
    }

    // Show error message if present
    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        ErrorBox(error)
    }

    // Show model info at bottom
    uiState.modelInfo?.let { info ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = info,
            color = Color.DarkGray,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CountdownContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(AppColors.PracticeModeColor.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${uiState.countdownSeconds}",
            color = AppColors.PracticeModeColor,
            fontSize = 60.sp,
            style = MaterialTheme.typography.display1
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = uiState.statusMessage,
        color = AppColors.TextPrimary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(onClick = { viewModel.cancelRecording() }) {
        Text("Cancel")
    }
}

@Composable
private fun RecordingContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = uiState.recordingProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            indicatorColor = Color.Green
        )
        Text(text = "✍️", fontSize = 48.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = uiState.statusMessage,
        color = Color.Green,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(onClick = { viewModel.cancelRecording() }) {
        Text("Cancel")
    }
}

@Composable
private fun ProcessingContent(uiState: PracticeModeViewModel.UiState) {
    CircularProgressIndicator(
        modifier = Modifier.size(60.dp),
        strokeWidth = 6.dp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = uiState.statusMessage,
        color = AppColors.TextPrimary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ResultContent(uiState: PracticeModeViewModel.UiState) {
    val confidence = uiState.confidence ?: 0f
    val resultColor = when {
        confidence >= 0.6f -> Color.Green
        confidence >= 0.3f -> Color.Yellow
        else -> Color.Red
    }

    Text(
        text = uiState.prediction ?: "?",
        color = resultColor,
        fontSize = 60.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "${(confidence * 100).toInt()}%",
        color = resultColor,
        fontSize = 20.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = uiState.statusMessage,
        color = AppColors.TextPrimary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ErrorBox(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(Color.DarkGray, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "⚠️ Error",
            color = Color.Red,
            fontSize = 10.sp
        )
        Text(
            text = message,
            color = Color.Yellow,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
