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
 * Tutorial Mode screen - displays air writing practice with gesture input.
 * Mirrors LearnModeScreen architecture: phone is single source of truth for feedback,
 * feedback is handled INSIDE the content composable (not as a top-level state branch).
 *
 * States:
 * 1. Waiting for phone to start session
 * 2. Gesture recognition active (with feedback overlay from phone)
 * 3. Session complete
 */
@Composable
fun TutorialModeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val phoneCommunicationManager = remember { PhoneCommunicationManager(context) }
    val isPhoneInTutorialMode by phoneCommunicationManager.isPhoneInTutorialMode.collectAsState()

    // Initialize TextToSpeech manager
    val ttsManager = remember { TextToSpeechManager(context) }

    // Cleanup when the composable is disposed
    DisposableEffect(Unit) {
        // Clear stale state from any previous session before setting screen flag
        TutorialModeStateHolder.resetSession()
        // Mark watch as on Tutorial Mode screen for handshake gating
        TutorialModeStateHolder.setWatchOnTutorialScreen(true)
        onDispose {
            TutorialModeStateHolder.setWatchOnTutorialScreen(false)
            ttsManager.shutdown()
            phoneCommunicationManager.cleanup()
        }
    }

    val letterData by TutorialModeStateHolder.letterData.collectAsState()
    val isSessionComplete by TutorialModeStateHolder.isSessionComplete.collectAsState()

    // Debouncing state for skip gesture
    var lastSkipTime by remember { mutableLongStateOf(0L) }

    // Phone-driven feedback state — persists across recompositions (mirrors Learn Mode)
    var showingFeedback by remember { mutableStateOf(false) }
    var feedbackIsCorrect by remember { mutableStateOf(false) }

    // "Tap to begin" wait screen — shown after handshake but before user taps
    var showWaitScreen by remember { mutableStateOf(true) }

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
        }
    }

    // Two-way handshake: send watch_ready when session becomes active.
    LaunchedEffect(isPhoneInTutorialMode) {
        if (isPhoneInTutorialMode) {
            scope.launch {
                phoneCommunicationManager.sendTutorialModeWatchReady()
            }
        }
    }

    // Heartbeat: keep sending watch_ready while no letter data yet
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            if (letterData.letter.isEmpty() && TutorialModeStateHolder.isWatchOnTutorialScreen.value) {
                scope.launch {
                    phoneCommunicationManager.sendTutorialModeWatchReady()
                }
            }
        }
    }

    // Listen for feedback from phone (phone is single source of truth)
    LaunchedEffect(Unit) {
        phoneCommunicationManager.tutorialModeFeedbackEvent.collect { event ->
            if (event.timestamp > 0L) {
                Log.d("TutorialMode", "📥 Feedback received from phone: correct=${event.isCorrect}")
                feedbackIsCorrect = event.isCorrect
                showingFeedback = true
            }
        }
    }

    // Listen for feedback dismissal from phone
    LaunchedEffect(Unit) {
        var lastDismissTime = 0L
        phoneCommunicationManager.tutorialModeFeedbackDismissed.collect { timestamp ->
            if (timestamp > lastDismissTime && timestamp > 0L && showingFeedback) {
                lastDismissTime = timestamp
                Log.d("TutorialMode", "👆 Phone dismissed feedback")
                showingFeedback = false
            }
        }
    }

    // Reset feedback when new letter data arrives
    LaunchedEffect(letterData.timestamp) {
        if (letterData.timestamp > 0L) {
            Log.d("TutorialMode", "🔄 Letter changed - resetting feedback state")
            showingFeedback = false
        }
    }

    CircularModeBorder(borderColor = AppColors.TutorialModeColor) {
        Scaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !isPhoneInTutorialMode -> {
                        // Waiting for phone to start session
                        WaitingContent()
                    }
                    isSessionComplete -> {
                        // Session complete
                        CompleteContent()
                    }
                    showWaitScreen && isModelInitialized && sensorManager != null && classifierResult is ClassifierLoadResult.Success && letterData.letter.isNotEmpty() -> {
                        // Tap to begin (model ready, letter received)
                        if (modelLoadError != null) {
                            ErrorContent(errorMessage = modelLoadError!!)
                        } else {
                            WaitScreenContent(
                                onTap = {
                                    showWaitScreen = false
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
                    !showWaitScreen && isModelInitialized && sensorManager != null && classifierResult is ClassifierLoadResult.Success && letterData.letter.isNotEmpty() -> {
                        // Gesture recognition + phone-driven feedback (content stays mounted)
                        GestureRecognitionContent(
                            letter = letterData.letter,
                            letterCase = letterData.letterCase,
                            timestamp = letterData.timestamp,
                            sensorManager = sensorManager!!,
                            classifierResult = classifierResult as ClassifierLoadResult.Success,
                            ttsManager = ttsManager,
                            phoneCommunicationManager = phoneCommunicationManager,
                            showingFeedback = showingFeedback,
                            feedbackIsCorrect = feedbackIsCorrect,
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendTutorialModeSkipCommand()
                                    }
                                    showWaitScreen = true
                                }
                            }
                        )
                    }
                    showWaitScreen -> {
                        // Wait screen (model still loading or no letter yet)
                        if (modelLoadError != null) {
                            ErrorContent(errorMessage = modelLoadError!!)
                        } else {
                            WaitScreenContent(
                                onTap = { /* Model not ready yet, ignore */ },
                                onSkip = { /* Nothing to skip yet */ }
                            )
                        }
                    }
                    else -> {
                        WaitingContent()
                    }
                }
            }
        }
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
        Text(
            text = "Waiting...",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
private fun CompleteContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dis_watch_complete),
            contentDescription = "Session Complete",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Gesture recognition content — stays mounted during feedback.
 * Mirrors LearnModeScreen's FillInTheBlankMainContent pattern:
 * - showingFeedback/feedbackIsCorrect passed in as params
 * - if (showingFeedback) shows feedback overlay, else shows ViewModel UI states
 * - When feedback dismisses (true → false), viewModel.resetToIdle() is called
 */
@Composable
private fun GestureRecognitionContent(
    letter: String,
    letterCase: String,
    timestamp: Long,
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult,
    ttsManager: TextToSpeechManager,
    phoneCommunicationManager: PhoneCommunicationManager,
    showingFeedback: Boolean,
    feedbackIsCorrect: Boolean,
    onSkip: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Create ViewModel — includes timestamp in key to force fresh ViewModel per letter
    val viewModel: TutorialModeViewModel = viewModel(
        factory = TutorialModeViewModelFactory(
            sensorManager = sensorManager,
            classifierResult = classifierResult,
            targetLetter = letter,
            letterCase = letterCase,
            onGestureResult = { isCorrect, predictedLetter ->
                // Send result to phone — phone will send feedback back
                scope.launch {
                    phoneCommunicationManager.sendTutorialModeGestureResult(isCorrect, predictedLetter)
                }
            },
            onRecordingStarted = {
                scope.launch {
                    phoneCommunicationManager.sendTutorialModeGestureRecording()
                }
            }
        ),
        key = "$letter-$letterCase-$timestamp"
    )

    val uiState by viewModel.uiState.collectAsState()

    // Speak the prediction when we enter SHOWING_PREDICTION state
    LaunchedEffect(uiState.state, uiState.prediction, uiState.isCorrect) {
        if (uiState.state == TutorialModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            if (uiState.prediction == "?") {
                ttsManager.speak("Oops, you did not air write!")
            } else if (uiState.isCorrect) {
                ttsManager.speakLetter(uiState.prediction!!)
            } else {
                ttsManager.speakTryAgain()
            }
        }
    }

    // Reset ViewModel only when feedback is actually dismissed (true -> false),
    // not on initial composition when showingFeedback starts as false.
    // Mirrors LearnModeScreen's pattern exactly.
    var previousShowingFeedback by remember { mutableStateOf(false) }
    LaunchedEffect(showingFeedback) {
        if (previousShowingFeedback && !showingFeedback) {
            viewModel.resetToIdle()
        }
        previousShowingFeedback = showingFeedback
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
        // Phone-driven feedback overlay (mirrors Learn Mode pattern)
        // Skip wrong avatar when prediction was "?" (no air-writing) — go straight to waiting
        if (showingFeedback) {
            if (uiState.prediction == "?") {
                TutorialModeFeedbackContent(
                    isCorrect = feedbackIsCorrect,
                    skipAvatar = true
                )
            } else {
                TutorialModeFeedbackContent(
                    isCorrect = feedbackIsCorrect
                )
            }
        } else {
            when (uiState.state) {
                TutorialModeViewModel.State.IDLE -> {
                    // Idle state — tap to start recording
                    // Auto-start on first composition (user already tapped "Tap to begin!")
                    LaunchedEffect(Unit) {
                        viewModel.startRecording()
                    }
                    // Show brief "Get ready..." while starting
                    Text(
                        text = uiState.statusMessage,
                        color = AppColors.TutorialModeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                TutorialModeViewModel.State.COUNTDOWN -> {
                    Text(
                        text = "${uiState.countdownSeconds}",
                        color = AppColors.TutorialModeColor,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TutorialModeViewModel.State.RECORDING -> {
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
                TutorialModeViewModel.State.SHOWING_PREDICTION -> {
                    // Show the predicted letter — stays here until phone sends feedback
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.prediction ?: "",
                            color = Color.White,
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Feedback content for Tutorial Mode - briefly shows correct/wrong image,
 * then transitions to a waiting state until the teacher continues on the phone.
 * Mirrors LearnModeFeedbackContent for consistent behavior.
 */
@Composable
private fun TutorialModeFeedbackContent(isCorrect: Boolean, skipAvatar: Boolean = false) {
    var showWaiting by remember { mutableStateOf(skipAvatar) }

    LaunchedEffect(Unit) {
        if (!skipAvatar) {
            kotlinx.coroutines.delay(2000L)
            showWaiting = true
        }
    }

    if (showWaiting) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Waiting for teacher...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(id = R.drawable.dis_watch_wait),
                    contentDescription = "Waiting for teacher",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = if (isCorrect) R.drawable.dis_watch_correct else R.drawable.dis_watch_wrong
                ),
                contentDescription = if (isCorrect) "Correct" else "Wrong",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
