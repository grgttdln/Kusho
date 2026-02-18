package com.example.kusho.presentation.tutorial

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.kusho.R
import com.example.kusho.ml.ClassifierLoadResult
import com.example.kusho.ml.ModelConfig
import com.example.kusho.ml.ModelLoader
import com.example.kusho.presentation.components.CircularModeBorder
import com.example.kusho.presentation.service.PhoneCommunicationManager
import com.example.kusho.presentation.theme.AppColors
import com.example.kusho.sensors.MotionSensorManager
import com.example.kusho.speech.TextToSpeechManager
import kotlinx.coroutines.launch

/**
 * Tutorial Mode screen - displays air writing practice with gesture input
 * 
 * States:
 * 1. Waiting for phone to start session
 * 2. Showing wait screen (dis_watch_wait.png) - tap to start countdown
 * 3. Practicing (gesture recognition active) - swipe left to skip
 * 4. Showing feedback (correct/incorrect)
 * 5. Session complete
 */
@Composable
fun TutorialModeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val phoneCommunicationManager = remember { PhoneCommunicationManager(context) }
    
    // Initialize TextToSpeech manager
    val ttsManager = remember { TextToSpeechManager(context) }
    
    // Cleanup TTS when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }
    
    val isPhoneInTutorialMode by phoneCommunicationManager.isPhoneInTutorialMode.collectAsState()
    val letterData by TutorialModeStateHolder.letterData.collectAsState()
    val sessionData by TutorialModeStateHolder.sessionData.collectAsState()
    val feedbackData by TutorialModeStateHolder.feedbackData.collectAsState()
    val isSessionComplete by TutorialModeStateHolder.isSessionComplete.collectAsState()
    val retryTrigger by TutorialModeStateHolder.retryTrigger.collectAsState()
    
    // Debouncing state for skip gesture
    var lastSkipTime by remember { mutableLongStateOf(0L) }
    
    // State for wait screen interaction
    var showWaitScreen by remember { mutableStateOf(true) }
    var isRecognizing by remember { mutableStateOf(false) }
    var needsReset by remember { mutableStateOf(false) }
    
    // Gesture recognition dependencies
    var sensorManager by remember { mutableStateOf<MotionSensorManager?>(null) }
    var classifierResult by remember { mutableStateOf<ClassifierLoadResult?>(null) }
    var isModelInitialized by remember { mutableStateOf(false) }
    var modelLoadError by remember { mutableStateOf<String?>(null) }
    var currentModelCase by remember { mutableStateOf("") }
    
    // Initialize sensor manager once
    LaunchedEffect(Unit) {
        try {
            sensorManager = MotionSensorManager(context)
            Log.d("TutorialMode", "✅ Sensor manager initialized")
        } catch (e: Exception) {
            modelLoadError = "Failed to initialize sensor: ${e.message}"
            Log.e("TutorialMode", "❌ Sensor initialization error", e)
        }
    }
    
    // Clean up resources when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            Log.d("TutorialMode", "🧹 Cleaning up resources")
            // Close the TFLite model to free native memory
            (classifierResult as? ClassifierLoadResult.Success)?.classifier?.close()
            classifierResult = null
            isModelInitialized = false
        }
    }
    
    // Load appropriate model when letterCase or dominantHand changes
    LaunchedEffect(letterData.letterCase, letterData.dominantHand) {
        val modelKey = "${letterData.letterCase}_${letterData.dominantHand}"
        if (letterData.letterCase.isNotEmpty() && modelKey != currentModelCase) {
            try {
                Log.d("TutorialMode", "🔄 Loading model for case: ${letterData.letterCase}, hand: ${letterData.dominantHand}")
                
                // CRITICAL: Close the old model before loading a new one to prevent memory leak
                val oldClassifier = (classifierResult as? ClassifierLoadResult.Success)?.classifier
                if (oldClassifier != null) {
                    Log.d("TutorialMode", "🧹 Closing old model: $currentModelCase")
                    oldClassifier.close()
                }
                
                val modelConfig = ModelConfig.getModelForSession(letterData.letterCase, letterData.dominantHand)
                val loadResult = ModelLoader.load(context, modelConfig)
                classifierResult = loadResult
                
                when (loadResult) {
                    is ClassifierLoadResult.Success -> {
                        isModelInitialized = true
                        currentModelCase = modelKey
                        modelLoadError = null
                        Log.d("TutorialMode", "✅ Model loaded: ${modelConfig.displayName}")
                    }
                    is ClassifierLoadResult.Error -> {
                        modelLoadError = loadResult.message
                        isModelInitialized = false
                        Log.e("TutorialMode", "❌ Model load failed: ${loadResult.message}")
                    }
                }
            } catch (e: Exception) {
                modelLoadError = "Failed to load model: ${e.message}"
                isModelInitialized = false
                Log.e("TutorialMode", "❌ Model loading error", e)
            }
        }
    }
    
    // Reset wait screen when letter data arrives
    LaunchedEffect(letterData.timestamp) {
        if (letterData.timestamp > 0) {
            showWaitScreen = true
            isRecognizing = false
            needsReset = false  // New letter, no reset needed
        }
    }
    
    // Listen for retry trigger from mobile (when user taps "Try Again" on phone)
    LaunchedEffect(retryTrigger) {
        if (retryTrigger > 0L) {
            Log.d("TutorialMode", "🔄 Retry triggered from mobile, setting needsReset")
            needsReset = true
            showWaitScreen = true
            isRecognizing = false
        }
    }
    
    CircularModeBorder(borderColor = AppColors.TutorialModeColor) {
        Scaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !isPhoneInTutorialMode || !sessionData.isActive -> {
                        // State 1: Waiting for phone to start session
                        WaitingContent()
                    }
                    isSessionComplete -> {
                        // State 5: Session complete
                        CompleteContent()
                    }
                    feedbackData.shouldShow -> {
                        // State 4: Showing feedback
                        FeedbackContent(
                            isCorrect = feedbackData.isCorrect,
                            onDismiss = {
                                // Notify mobile that watch dismissed feedback
                                scope.launch {
                                    phoneCommunicationManager.sendTutorialModeFeedbackDismissed()
                                }
                                TutorialModeStateHolder.clearFeedback()
                                // Reset to wait screen to allow retry or next letter
                                showWaitScreen = true
                                isRecognizing = false
                                // Mark that we need to reset on next start
                                needsReset = true
                            }
                        )
                    }
                    isRecognizing && isModelInitialized && sensorManager != null && classifierResult is ClassifierLoadResult.Success && letterData.letter.isNotEmpty() -> {
                        // State 3: Gesture recognition (countdown -> recording -> processing)
                        GestureRecognitionContent(
                            letter = letterData.letter,
                            letterCase = letterData.letterCase,
                            timestamp = letterData.timestamp,
                            sensorManager = sensorManager!!,
                            classifierResult = classifierResult as ClassifierLoadResult.Success,
                            ttsManager = ttsManager,
                            needsReset = needsReset,
                            onResetHandled = { needsReset = false },
                            onGestureResult = { isCorrect, predictedLetter ->
                                scope.launch {
                                    phoneCommunicationManager.sendTutorialModeGestureResult(isCorrect, predictedLetter)
                                }
                                TutorialModeStateHolder.showFeedback(isCorrect)
                                isRecognizing = false
                            },
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendTutorialModeSkipCommand()
                                    }
                                    isRecognizing = false
                                }
                            }
                        )
                    }
                    showWaitScreen -> {
                        // State 2: Wait screen - tap to start
                        if (modelLoadError != null) {
                            // Show error if model failed to load
                            ErrorContent(errorMessage = modelLoadError!!)
                        } else if (!isModelInitialized) {
                            // Show loading while model loads
                            LoadingContent()
                        } else {
                            WaitScreenContent(
                                onTap = {
                                    if (isModelInitialized && sensorManager != null && classifierResult is ClassifierLoadResult.Success) {
                                        showWaitScreen = false
                                        isRecognizing = true
                                    }
                                },
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendTutorialModeSkipCommand()
                                    }
                                }
                            }
                        )
                        }
                    }
                    else -> {
                        // Fallback
                        WaitingContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
            indicatorColor = AppColors.TutorialModeColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading...",
            color = AppColors.TutorialModeColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(errorMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            color = Color.Red,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            color = AppColors.TutorialModeColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please restart the app",
            color = AppColors.TutorialModeColor.copy(alpha = 0.5f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WaitingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Text at the top
        Text(
            text = "Waiting...",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Mascot below the text
        Image(
            painter = painterResource(id = R.drawable.dis_watch_learn_start),
            contentDescription = "Tutorial Mode waiting mascot",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun WaitScreenContent(
    onTap: () -> Unit,
    onSkip: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    // Only handle left swipe for skip
                    if (dragAmount < -50f) {
                        change.consume()
                        onSkip()
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Tap to begin!",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Image(
                painter = painterResource(id = R.drawable.dis_watch_wait),
                contentDescription = "Tap to start",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun PracticingContent(
    letter: String,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -50f) {
                        change.consume()
                        onSkip()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_kusho_hand),
            contentDescription = "Air write now",
            modifier = Modifier.size(70.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun FeedbackContent(
    isCorrect: Boolean,
    onDismiss: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = if (isCorrect) R.drawable.dis_watch_correct else R.drawable.dis_watch_wrong
            ),
            contentDescription = if (isCorrect) "Correct" else "Wrong",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun CompleteContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dis_watch_complete),
            contentDescription = "Session Complete",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun GestureRecognitionContent(
    letter: String,
    letterCase: String,
    timestamp: Long,
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult,
    ttsManager: TextToSpeechManager,
    needsReset: Boolean,
    onResetHandled: () -> Unit,
    onGestureResult: (Boolean, String) -> Unit,
    onSkip: () -> Unit
) {
    // Convert letter to appropriate case for display
    val displayLetter = when (letterCase.lowercase()) {
        "small" -> letter.lowercase()
        else -> letter.uppercase()
    }
    
    // Create ViewModel with gesture result callback
    // Include timestamp in key to force fresh ViewModel for each session
    val viewModel: TutorialModeViewModel = viewModel(
        factory = TutorialModeViewModelFactory(
            sensorManager = sensorManager,
            classifierResult = classifierResult,
            targetLetter = letter,
            letterCase = letterCase,
            onGestureResult = onGestureResult
        ),
        key = "$letter-$letterCase-$timestamp"
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Call reset directly when needed (after incorrect result)
    LaunchedEffect(needsReset) {
        if (needsReset) {
            viewModel.reset()
            onResetHandled()  // Clear the flag immediately
        }
    }
    
    // Speak the prediction when we enter RESULT state
    // Only speak the letter if correct; otherwise speak encouraging try-again message
    LaunchedEffect(uiState.state, uiState.prediction, uiState.isCorrect) {
        if (uiState.state == TutorialModeViewModel.State.RESULT && uiState.prediction != null) {
            if (uiState.isCorrect) {
                ttsManager.speakLetter(uiState.prediction!!)
            } else {
                ttsManager.speakTryAgain()
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelRecognition()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -50f) {
                        change.consume()
                        onSkip()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (uiState.state) {
            TutorialModeViewModel.State.COUNTDOWN -> {
                // Show countdown
                Text(
                    text = "${uiState.countdownSeconds}",
                    color = AppColors.TutorialModeColor,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            TutorialModeViewModel.State.RECORDING -> {
                // Show recording progress with hand icon
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = uiState.recordingProgress,
                        modifier = Modifier.fillMaxSize(0.95f),
                        strokeWidth = 8.dp,
                        indicatorColor = AppColors.TutorialModeColor
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_kusho_hand),
                        contentDescription = "Air write now",
                        modifier = Modifier.size(85.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            TutorialModeViewModel.State.PROCESSING -> {
                // Show processing indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        strokeWidth = 6.dp,
                        indicatorColor = AppColors.TutorialModeColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.statusMessage,
                        color = AppColors.TutorialModeColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            TutorialModeViewModel.State.RESULT -> {
                // Show result - only display predicted letter if correct
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isCorrect) {
                        // Show the correct prediction
                        Text(
                            text = uiState.prediction ?: "",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TutorialModeColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Correct!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Green,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Don't show the incorrect prediction (model forced a classification)
                        // Just show encouraging message
                        Text(
                            text = "😊",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try again!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TutorialModeColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            TutorialModeViewModel.State.COMPLETE -> {
                // Show brief status before transitioning
                Text(
                    text = uiState.statusMessage,
                    color = AppColors.TutorialModeColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
