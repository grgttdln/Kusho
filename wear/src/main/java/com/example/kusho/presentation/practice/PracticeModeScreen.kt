package com.example.kusho.presentation.practice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.kusho.R
import com.example.kusho.ml.ClassifierLoadResult
import com.example.kusho.ml.ModelLoader
import com.example.kusho.presentation.theme.AppColors
import com.example.kusho.sensors.MotionSensorManager

/**
 * Practice Mode screen: countdown -> record gesture -> classify -> show result
 */
@Composable
fun PracticeModeScreen() {
    val context = LocalContext.current

    // Use state to track if initialization is complete
    var isInitialized by remember { mutableStateOf(false) }
    var sensorManager by remember { mutableStateOf<MotionSensorManager?>(null) }
    var classifierResult by remember { mutableStateOf<ClassifierLoadResult?>(null) }

    // Initialize dependencies in LaunchedEffect to avoid blocking composition
    LaunchedEffect(Unit) {
        sensorManager = MotionSensorManager(context)
        classifierResult = try {
            ModelLoader.loadDefault(context)
        } catch (e: Exception) {
            ClassifierLoadResult.Error("Failed to load model: ${e.message}", e)
        }
        isInitialized = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(8.dp, AppColors.PracticeModeColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!isInitialized || sensorManager == null || classifierResult == null) {
            // Show loading while initializing
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading...",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            // Show main content once initialized
            PracticeModeContent(
                sensorManager = sensorManager!!,
                classifierResult = classifierResult!!
            )
        }
    }
}

@Composable
private fun PracticeModeContent(
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult
) {
    val viewModel: PracticeModeViewModel = viewModel(
        factory = PracticeModeViewModelFactory(sensorManager, classifierResult)
    )

    val uiState by viewModel.uiState.collectAsState()

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
            PracticeModeViewModel.State.RESULT -> ResultContent(uiState, viewModel)
        }
    }
}

@Composable
private fun IdleContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { viewModel.startRecording() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "Tap to Start" text below image
            Text(
                text = "Tap to Start",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp)
            )

            // Practice mascot image
            Image(
                painter = painterResource(id = R.drawable.dis_practice),
                contentDescription = "Practice mascot",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

    // Show error message if present
    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        ErrorBox(error)
    }
}

@Composable
private fun CountdownContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${uiState.countdownSeconds}",
            color = AppColors.PracticeModeColor,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.display1
        )
    }
}

@Composable
private fun RecordingContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = uiState.recordingProgress,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                indicatorColor = Color.Green
            )
            Text(text = "✍️", fontSize = 52.sp)
        }
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
private fun ResultContent(
    uiState: PracticeModeViewModel.UiState,
    viewModel: PracticeModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { viewModel.resetToIdle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = uiState.prediction ?: "?",
            color = Color.White,
            fontSize = 120.sp,
            fontWeight = FontWeight.Normal
        )
    }
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
