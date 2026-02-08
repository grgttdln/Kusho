package com.example.kusho.presentation.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.kusho.R
import com.example.kusho.ml.ClassifierLoadResult
import com.example.kusho.ml.ModelLoader
import com.example.kusho.presentation.components.CircularModeBorder
import com.example.kusho.presentation.service.PhoneCommunicationManager
import com.example.kusho.presentation.theme.AppColors
import com.example.kusho.sensors.MotionSensorManager
import com.example.kusho.speech.TextToSpeechManager
import kotlinx.coroutines.launch

/**
 * Learn Mode screen - displays fill-in-the-blanks with gesture input
 * For "Fill in the Blank" configuration, users can tap to start gesture recognition
 * Swipe Left: Skip to next word (sends command to phone)
 * Swipe Right: Native Wear OS back gesture (preserved)
 */
@Composable
fun LearnModeScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val phoneCommunicationManager = remember { PhoneCommunicationManager(context) }
    val isPhoneInLearnMode by phoneCommunicationManager.isPhoneInLearnMode.collectAsState()
    
    // Keep screen on during Learn Mode to prevent sleep during air writing
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Observe word data from LearnModeStateHolder
    val wordData by LearnModeStateHolder.wordData.collectAsState()
    val sessionData by LearnModeStateHolder.sessionData.collectAsState()

    // Debouncing state for skip gesture
    var lastSkipTime by remember { mutableLongStateOf(0L) }

    // Check if current word is Fill in the Blank type
    val isFillInTheBlank = wordData.configurationType == "Fill in the Blank"
    val isWriteTheWord = wordData.configurationType == "Write the Word"
    val isNameThePicture = wordData.configurationType == "Name the Picture"

    CircularModeBorder(borderColor = AppColors.LearnModeColor) {
        Scaffold {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    sessionData.isActivityComplete -> {
                        // Activity complete - show completion image
                        ActivityCompleteContent()
                    }
                    !isPhoneInLearnMode -> {
                        // Waiting state - phone hasn't started Learn Mode session yet
                        WaitingContent()
                    }
                    isFillInTheBlank && wordData.word.isNotEmpty() -> {
                        // Fill in the Blank mode - show gesture input
                        FillInTheBlankContent(
                            wordData = wordData,
                            phoneCommunicationManager = phoneCommunicationManager,
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendSkipCommand()
                                    }
                                }
                            }
                        )
                    }
                    isWriteTheWord && wordData.word.isNotEmpty() -> {
                        // Write the Word mode - show letter-by-letter gesture input
                        WriteTheWordContent(
                            wordData = wordData,
                            phoneCommunicationManager = phoneCommunicationManager,
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendSkipCommand()
                                    }
                                }
                            }
                        )
                    }
                    isNameThePicture && wordData.word.isNotEmpty() -> {
                        // Name the Picture mode - letter-by-letter gesture input without showing next letter
                        NameThePictureContent(
                            wordData = wordData,
                            phoneCommunicationManager = phoneCommunicationManager,
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendSkipCommand()
                                    }
                                }
                            }
                        )
                    }
                    else -> {
                        // Other modes or waiting for word data - show simple swipe to skip
                        DefaultLearnModeContent(
                            onSkip = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSkipTime >= 500) {
                                    lastSkipTime = currentTime
                                    scope.launch {
                                        phoneCommunicationManager.sendSkipCommand()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
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
            contentDescription = "Learn Mode waiting mascot",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ActivityCompleteContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dis_watch_complete),
            contentDescription = "Activity Complete",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DefaultLearnModeContent(onSkip: () -> Unit) {
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Learn Mode",
                color = AppColors.LearnModeColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Swipe left to skip",
                color = AppColors.LearnModeColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun FillInTheBlankContent(
    wordData: LearnModeStateHolder.WordData,
    phoneCommunicationManager: PhoneCommunicationManager,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    // Initialize dependencies
    var isInitialized by remember { mutableStateOf(false) }
    var sensorManager by remember { mutableStateOf<MotionSensorManager?>(null) }
    var classifierResult by remember { mutableStateOf<ClassifierLoadResult?>(null) }

    // Initialize TextToSpeech manager
    val ttsManager = remember { TextToSpeechManager(context) }

    // Cleanup TTS when disposed
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    // Initialize dependencies
    LaunchedEffect(Unit) {
        sensorManager = MotionSensorManager(context)
        classifierResult = try {
            ModelLoader.loadDefault(context)
        } catch (e: Exception) {
            ClassifierLoadResult.Error("Failed to load model: ${e.message}", e)
        }
        isInitialized = true
    }

    if (!isInitialized || sensorManager == null || classifierResult == null) {
        // Loading state
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
        FillInTheBlankMainContent(
            wordData = wordData,
            sensorManager = sensorManager!!,
            classifierResult = classifierResult!!,
            ttsManager = ttsManager,
            phoneCommunicationManager = phoneCommunicationManager,
            onSkip = onSkip
        )
    }
}

@Composable
private fun FillInTheBlankMainContent(
    wordData: LearnModeStateHolder.WordData,
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult,
    ttsManager: TextToSpeechManager,
    phoneCommunicationManager: PhoneCommunicationManager,
    onSkip: () -> Unit
) {
    val viewModel: LearnModeViewModel = viewModel(
        factory = LearnModeViewModelFactory(sensorManager, classifierResult)
    )

    val uiState by viewModel.uiState.collectAsState()

    // Track last spoken prediction to avoid double TTS
    var lastSpokenPrediction by remember { mutableStateOf<String?>(null) }

    // Speak the prediction and send to phone when prediction is shown
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                ttsManager.speakLetter(uiState.prediction!!)
            }

            // Send letter input to phone immediately for validation
            android.util.Log.d("LearnModeScreen", "📤 Sending letter input to phone: ${uiState.prediction} at index ${wordData.maskedIndex}")
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, wordData.maskedIndex)
        }
    }

    // Reset last spoken prediction when going back to idle
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.IDLE) {
            lastSpokenPrediction = null
        }
    }

    // Build masked word display
    val maskedWord = buildMaskedWordDisplay(wordData.word, wordData.maskedIndex)

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.state) {
                LearnModeViewModel.State.IDLE -> IdleContent(
                    maskedWord = maskedWord,
                    viewModel = viewModel
                )
                LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                LearnModeViewModel.State.RESULT -> ResultContent(
                    uiState = uiState,
                    maskedWord = maskedWord,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    maskedWord: String,
    viewModel: LearnModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.startRecording() }
    ) {
        // Mascot image fills the entire background
        Image(
            painter = painterResource(id = R.drawable.dis_watch_wait),
            contentDescription = "Learn mascot",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )


    }
}

@Composable
private fun CountdownContent(uiState: LearnModeViewModel.UiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${uiState.countdownSeconds}",
            color = AppColors.LearnModeColor,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.display1
        )
    }
}

@Composable
private fun RecordingContent(uiState: LearnModeViewModel.UiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = uiState.recordingProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            indicatorColor = AppColors.LearnModeColor
        )
        Text(text = "✍️", fontSize = 52.sp)
    }
}

@Composable
private fun ProcessingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            strokeWidth = 6.dp,
            indicatorColor = AppColors.LearnModeColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Processing...",
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShowingPredictionContent(uiState: LearnModeViewModel.UiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Display the predicted letter as the user inputted it
        Text(
            text = uiState.prediction ?: "",
            color = Color.White,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.display1
        )
    }
}

@Composable
private fun ResultContent(
    uiState: LearnModeViewModel.UiState,
    maskedWord: String,
    viewModel: LearnModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.resetToIdle() },
        contentAlignment = Alignment.Center
    ) {
        // Show correct or wrong mascot image based on result
        Image(
            painter = painterResource(
                id = if (uiState.isCorrect == true) R.drawable.dis_watch_correct else R.drawable.dis_watch_wrong
            ),
            contentDescription = if (uiState.isCorrect == true) "Correct" else "Wrong",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Build a display string with the masked letter shown as underscore
 */
private fun buildMaskedWordDisplay(word: String, maskedIndex: Int): String {
    return word.mapIndexed { index, char ->
        if (index == maskedIndex) "_" else char.toString()
    }.joinToString(" ")
}

/**
 * Write the Word mode content - letter-by-letter gesture input
 * User inputs each letter of the word sequentially using gestures
 */
@Composable
private fun WriteTheWordContent(
    wordData: LearnModeStateHolder.WordData,
    phoneCommunicationManager: PhoneCommunicationManager,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observe Write the Word state
    val writeTheWordState by LearnModeStateHolder.writeTheWordState.collectAsState()

    // Initialize dependencies
    var isInitialized by remember { mutableStateOf(false) }
    var sensorManager by remember { mutableStateOf<MotionSensorManager?>(null) }
    var classifierResult by remember { mutableStateOf<ClassifierLoadResult?>(null) }

    // Initialize TextToSpeech manager
    val ttsManager = remember { TextToSpeechManager(context) }

    // Cleanup TTS when disposed
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    // Initialize dependencies
    LaunchedEffect(Unit) {
        sensorManager = MotionSensorManager(context)
        classifierResult = try {
            ModelLoader.loadDefault(context)
        } catch (e: Exception) {
            ClassifierLoadResult.Error("Failed to load model: ${e.message}", e)
        }
        isInitialized = true
    }

    if (!isInitialized || sensorManager == null || classifierResult == null) {
        // Loading state
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
        WriteTheWordMainContent(
            wordData = wordData,
            writeTheWordState = writeTheWordState,
            sensorManager = sensorManager!!,
            classifierResult = classifierResult!!,
            ttsManager = ttsManager,
            phoneCommunicationManager = phoneCommunicationManager,
            onSkip = onSkip
        )
    }
}

@Composable
private fun WriteTheWordMainContent(
    wordData: LearnModeStateHolder.WordData,
    writeTheWordState: LearnModeStateHolder.WriteTheWordState,
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult,
    ttsManager: TextToSpeechManager,
    phoneCommunicationManager: PhoneCommunicationManager,
    onSkip: () -> Unit
) {
    val viewModel: LearnModeViewModel = viewModel(
        factory = LearnModeViewModelFactory(sensorManager, classifierResult)
    )

    val uiState by viewModel.uiState.collectAsState()

    // Current letter to input - keep exact case (uppercase or lowercase)
    val currentLetterIndex = writeTheWordState.currentLetterIndex
    val expectedLetter = wordData.word.getOrNull(currentLetterIndex)

    // Track if we're showing feedback (correct/wrong image)
    var showingFeedback by remember { mutableStateOf(false) }
    var feedbackIsCorrect by remember { mutableStateOf(false) }

    // Track when this content was composed to filter stale events
    val contentStartTime = remember { System.currentTimeMillis() }

    // Track last spoken prediction to avoid double TTS
    var lastSpokenPrediction by remember { mutableStateOf<String?>(null) }

    // Send letter input to phone when prediction is made
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                ttsManager.speakLetter(uiState.prediction!!)
            }

            // Send letter input to phone for validation (preserve exact case)
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, currentLetterIndex)
        }
    }

    // Reset last spoken prediction when going back to idle
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.IDLE) {
            lastSpokenPrediction = null
        }
    }

    // Listen for letter result from phone
    LaunchedEffect(Unit) {
        phoneCommunicationManager.letterResultEvent.collect { result ->
            // Only process events that occurred after this content was composed
            if (result.timestamp > contentStartTime) {
                // Show feedback image
                feedbackIsCorrect = result.isCorrect
                showingFeedback = true

                // Auto-reset after showing feedback
                kotlinx.coroutines.delay(1500) // Show feedback for 1.5 seconds
                showingFeedback = false
                viewModel.resetToIdle()
            }
        }
    }

    // Check if prediction matches expected letter for local UI feedback (case-sensitive)
    val isCorrectPrediction = uiState.prediction?.firstOrNull() == expectedLetter

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show feedback image if we're in feedback state
            if (showingFeedback) {
                WriteTheWordFeedbackContent(isCorrect = feedbackIsCorrect)
            } else {
                when (uiState.state) {
                    LearnModeViewModel.State.IDLE -> WriteTheWordIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                    LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                    LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                    LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                    LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                    LearnModeViewModel.State.RESULT -> WriteTheWordIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun WriteTheWordIdleContent(
    wordData: LearnModeStateHolder.WordData,
    writeTheWordState: LearnModeStateHolder.WriteTheWordState,
    viewModel: LearnModeViewModel
) {
    val currentLetterIndex = writeTheWordState.currentLetterIndex
    val expectedLetter = wordData.word.getOrNull(currentLetterIndex)?.toString() ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.startRecording() },
        contentAlignment = Alignment.Center
    ) {
        // Display only the current letter to write - large and purple like in the UI image
        Text(
            text = expectedLetter,
            color = AppColors.LearnModeColor,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.display1
        )
    }
}

@Composable
private fun WriteTheWordLetterDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int
) {
    val completedColor = Color(0xFFAE8EFB) // Purple for completed
    val currentColor = Color.White // White for current letter
    val pendingColor = Color(0xFF808080) // Gray for pending

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        word.forEachIndexed { index, letter ->
            Text(
                text = letter.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    index in completedIndices -> completedColor
                    index == currentIndex -> currentColor
                    else -> pendingColor
                }
            )
        }
    }
}

@Composable
private fun WriteTheWordResultContent(
    uiState: LearnModeViewModel.UiState,
    wordData: LearnModeStateHolder.WordData,
    writeTheWordState: LearnModeStateHolder.WriteTheWordState,
    isCorrect: Boolean,
    viewModel: LearnModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.resetToIdle() },
        contentAlignment = Alignment.Center
    ) {
        // Show correct or wrong mascot image based on result
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

/**
 * Feedback content for Write the Word mode - shows correct/wrong image
 */
@Composable
private fun WriteTheWordFeedbackContent(isCorrect: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Show correct or wrong mascot image
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

/**
 * Name the Picture mode content - letter-by-letter gesture input
 * Similar to Write the Word but does NOT show the next letter to input
 */
@Composable
private fun NameThePictureContent(
    wordData: LearnModeStateHolder.WordData,
    phoneCommunicationManager: PhoneCommunicationManager,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observe Write the Word state (reuse the same state for letter tracking)
    val writeTheWordState by LearnModeStateHolder.writeTheWordState.collectAsState()

    // Initialize dependencies
    var isInitialized by remember { mutableStateOf(false) }
    var sensorManager by remember { mutableStateOf<MotionSensorManager?>(null) }
    var classifierResult by remember { mutableStateOf<ClassifierLoadResult?>(null) }

    // Initialize TextToSpeech manager
    val ttsManager = remember { TextToSpeechManager(context) }

    // Cleanup TTS when disposed
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    // Initialize dependencies
    LaunchedEffect(Unit) {
        sensorManager = MotionSensorManager(context)
        classifierResult = try {
            ModelLoader.loadDefault(context)
        } catch (e: Exception) {
            ClassifierLoadResult.Error("Failed to load model: ${e.message}", e)
        }
        isInitialized = true
    }

    if (!isInitialized || sensorManager == null || classifierResult == null) {
        // Loading state
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
        NameThePictureMainContent(
            wordData = wordData,
            writeTheWordState = writeTheWordState,
            sensorManager = sensorManager!!,
            classifierResult = classifierResult!!,
            ttsManager = ttsManager,
            phoneCommunicationManager = phoneCommunicationManager,
            onSkip = onSkip
        )
    }
}

@Composable
private fun NameThePictureMainContent(
    wordData: LearnModeStateHolder.WordData,
    writeTheWordState: LearnModeStateHolder.WriteTheWordState,
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult,
    ttsManager: TextToSpeechManager,
    phoneCommunicationManager: PhoneCommunicationManager,
    onSkip: () -> Unit
) {
    val viewModel: LearnModeViewModel = viewModel(
        factory = LearnModeViewModelFactory(sensorManager, classifierResult)
    )

    val uiState by viewModel.uiState.collectAsState()

    // Current letter to input - keep exact case (uppercase or lowercase)
    val currentLetterIndex = writeTheWordState.currentLetterIndex
    val expectedLetter = wordData.word.getOrNull(currentLetterIndex)

    // Track if we're showing feedback (correct/wrong image)
    var showingFeedback by remember { mutableStateOf(false) }
    var feedbackIsCorrect by remember { mutableStateOf(false) }

    // Track when this content was composed to filter stale events
    val contentStartTime = remember { System.currentTimeMillis() }

    // Track last spoken prediction to avoid double TTS
    var lastSpokenPrediction by remember { mutableStateOf<String?>(null) }

    // Send letter input to phone when prediction is made
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == LearnModeViewModel.State.SHOWING_PREDICTION && uiState.prediction != null) {
            // Only speak if we haven't already spoken this prediction
            if (lastSpokenPrediction != uiState.prediction) {
                lastSpokenPrediction = uiState.prediction
                ttsManager.speakLetter(uiState.prediction!!)
            }

            // Send letter input to phone for validation (preserve exact case)
            phoneCommunicationManager.sendLetterInput(uiState.prediction!!, currentLetterIndex)
        }
    }

    // Reset last spoken prediction when going back to idle
    LaunchedEffect(uiState.state) {
        if (uiState.state == LearnModeViewModel.State.IDLE) {
            lastSpokenPrediction = null
        }
    }

    // Listen for letter result from phone
    LaunchedEffect(Unit) {
        phoneCommunicationManager.letterResultEvent.collect { result ->
            // Only process events that occurred after this content was composed
            if (result.timestamp > contentStartTime) {
                // Show feedback image
                feedbackIsCorrect = result.isCorrect
                showingFeedback = true

                // Auto-reset after showing feedback
                kotlinx.coroutines.delay(1500) // Show feedback for 1.5 seconds
                showingFeedback = false
                viewModel.resetToIdle()
            }
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
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show feedback image if we're in feedback state
            if (showingFeedback) {
                WriteTheWordFeedbackContent(isCorrect = feedbackIsCorrect)
            } else {
                when (uiState.state) {
                    LearnModeViewModel.State.IDLE -> NameThePictureIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                    LearnModeViewModel.State.COUNTDOWN -> CountdownContent(uiState)
                    LearnModeViewModel.State.RECORDING -> RecordingContent(uiState)
                    LearnModeViewModel.State.PROCESSING -> ProcessingContent()
                    LearnModeViewModel.State.SHOWING_PREDICTION -> ShowingPredictionContent(uiState)
                    LearnModeViewModel.State.RESULT -> NameThePictureIdleContent(
                        wordData = wordData,
                        writeTheWordState = writeTheWordState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

/**
 * Idle content for Name the Picture mode - shows mascot image with letter progress
 * Completed letters are revealed in purple, pending letters show as underlines
 */
@Composable
private fun NameThePictureIdleContent(
    wordData: LearnModeStateHolder.WordData,
    writeTheWordState: LearnModeStateHolder.WriteTheWordState,
    viewModel: LearnModeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.startRecording() },
        contentAlignment = Alignment.Center
    ) {
        // Show mascot avatar image - user taps to start gesture recognition
        Image(
            painter = painterResource(id = R.drawable.dis_watch_wait),
            contentDescription = "Name the Picture mascot",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}


