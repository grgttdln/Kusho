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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.kusho.speech.TextToSpeechManager

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

    // Initialize TextToSpeech manager
    val ttsManager = remember { TextToSpeechManager(context) }

    // Cleanup TTS when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

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
                classifierResult = classifierResult!!,
                ttsManager = ttsManager
            )
        }
    }
}

@Composable
private fun PracticeModeContent(
    sensorManager: MotionSensorManager,
    classifierResult: ClassifierLoadResult,
    ttsManager: TextToSpeechManager
) {
    val viewModel: PracticeModeViewModel = viewModel(
        factory = PracticeModeViewModelFactory(sensorManager, classifierResult)
    )

    val uiState by viewModel.uiState.collectAsState()

    // Initialize word formation with hardcoded 3-letter CVC words
    LaunchedEffect(Unit) {
        // Enable word formation by default
        viewModel.setWordFormationEnabled(true)

        // Hardcoded 3-letter CVC (Consonant-Vowel-Consonant) word bank
        val cvcWords = listOf(
            // Common CVC words for kids
            "CAT", "BAT", "HAT", "MAT", "RAT", "SAT", "PAT", "FAT", "VAT",
            "BED", "FED", "LED", "RED", "WED",
            "BIG", "DIG", "FIG", "JIG", "PIG", "RIG", "WIG",
            "BIT", "FIT", "HIT", "KIT", "LIT", "PIT", "SIT", "WIT",
            "BOX", "FOX", "POX",
            "BUD", "CUD", "DUD", "MUD",
            "BUG", "DUG", "HUG", "JUG", "MUG", "PUG", "RUG", "TUG",
            "BUN", "FUN", "GUN", "NUN", "PUN", "RUN", "SUN",
            "BUS", "PUS",
            "BUT", "CUT", "GUT", "HUT", "JUT", "NUT", "PUT", "RUT",
            "CAB", "DAB", "FAB", "GAB", "JAB", "LAB", "NAB", "TAB",
            "CAD", "DAD", "FAD", "GAD", "HAD", "LAD", "MAD", "PAD", "SAD",
            "CAM", "DAM", "HAM", "JAM", "RAM", "YAM",
            "CAN", "BAN", "DAN", "FAN", "MAN", "PAN", "RAN", "TAN", "VAN",
            "CAP", "GAP", "LAP", "MAP", "NAP", "RAP", "SAP", "TAP", "ZAP",
            "CAR", "BAR", "FAR", "JAR", "TAR", "WAR",
            "COB", "BOB", "FOB", "GOB", "HOB", "JOB", "MOB", "NOB", "ROB", "SOB",
            "COD", "GOD", "HOD", "NOD", "POD", "ROD", "SOD",
            "COG", "BOG", "DOG", "FOG", "HOG", "JOG", "LOG", "TOG",
            "COP", "BOP", "HOP", "MOP", "POP", "SOP", "TOP",
            "COT", "BOT", "DOT", "GOT", "HOT", "JOT", "LOT", "NOT", "POT", "ROT", "TOT",
            "COW", "BOW", "HOW", "NOW", "POW", "ROW", "SOW", "VOW", "WOW",
            "CUB", "DUB", "HUB", "NUB", "PUB", "RUB", "SUB", "TUB",
            "CUP", "PUP", "SUP",
            "DEN", "BEN", "FEN", "HEN", "KEN", "MEN", "PEN", "TEN", "YEN", "ZEN",
            "DIM", "HIM", "RIM", "VIM",
            "DIN", "BIN", "FIN", "GIN", "KIN", "PIN", "SIN", "TIN", "WIN",
            "DIP", "HIP", "LIP", "NIP", "RIP", "SIP", "TIP", "ZIP",
            "GAL", "PAL",
            "GAS", "HAS", "WAS",
            "GEM", "HEM",
            "GET", "BET", "JET", "LET", "MET", "NET", "PET", "SET", "VET", "WET", "YET",
            "GUM", "BUM", "HUM", "MUM", "RUM", "SUM", "YUM",
            "GUT", "BUT", "CUT", "HUT", "JUT", "NUT", "PUT", "RUT",
            "HEX", "REX", "SEX", "VEX",
            "HID", "BID", "DID", "KID", "LID", "RID",
            "HOP", "BOP", "COP", "MOP", "POP", "SOP", "TOP",
            "LEG", "BEG", "KEG", "PEG",
            "LET", "BET", "GET", "JET", "MET", "NET", "PET", "SET", "VET", "WET",
            "MIX", "FIX", "SIX",
            "PEN", "BEN", "DEN", "HEN", "MEN", "TEN",
            "PET", "BET", "GET", "JET", "LET", "MET", "NET", "SET", "VET", "WET",
            "POT", "BOT", "COT", "DOT", "GOT", "HOT", "JOT", "LOT", "NOT", "ROT",
            "SIT", "BIT", "FIT", "HIT", "KIT", "LIT", "PIT", "WIT",
            "SUN", "BUN", "FUN", "GUN", "NUN", "PUN", "RUN",
            "TEN", "BEN", "DEN", "HEN", "MEN", "PEN",
            "TOP", "BOP", "COP", "HOP", "MOP", "POP", "SOP",
            "TUB", "CUB", "DUB", "HUB", "NUB", "PUB", "RUB", "SUB",
            "VAN", "BAN", "CAN", "DAN", "FAN", "MAN", "PAN", "RAN", "TAN",
            "WAX", "MAX", "TAX",
            "WEB",
            "WIG", "BIG", "DIG", "FIG", "JIG", "PIG", "RIG",
            "WIN", "BIN", "DIN", "FIN", "GIN", "KIN", "PIN", "SIN", "TIN",
            "YAK",
            "YAM", "CAM", "DAM", "HAM", "JAM", "RAM",
            "YAP", "CAP", "GAP", "LAP", "MAP", "NAP", "RAP", "SAP", "TAP", "ZAP",
            "ZAP", "CAP", "GAP", "LAP", "MAP", "NAP", "RAP", "SAP", "TAP", "YAP",
            "ZEN", "BEN", "DEN", "HEN", "KEN", "MEN", "PEN", "TEN", "YEN",
            "ZIP", "DIP", "HIP", "LIP", "NIP", "RIP", "SIP", "TIP"
        ).distinct() // Remove duplicates

        viewModel.loadWordBank(cvcWords)
        android.util.Log.d("PracticeModeScreen", "Loaded ${cvcWords.distinct().size} CVC words")
    }

    // Speak the prediction when we enter RESULT state
    LaunchedEffect(uiState.state, uiState.prediction) {
        if (uiState.state == PracticeModeViewModel.State.RESULT && uiState.prediction != null) {
            ttsManager.speakLetter(uiState.prediction!!)
        }
    }

    // Speak the formed word (just the word, no encouragement)
    // Triggered when wordJustFormed becomes true
    LaunchedEffect(uiState.wordJustFormed, uiState.lastFormedWord) {
        if (uiState.wordJustFormed && uiState.lastFormedWord != null) {
            // Wait for letter TTS to finish before speaking the word
            kotlinx.coroutines.delay(1000)
            ttsManager.speakWord(uiState.lastFormedWord!!)
            android.util.Log.d("PracticeModeScreen", "TTS speaking word: ${uiState.lastFormedWord}")
        }
    }


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
            // "Tap to Start" text
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
    // Track display phase: LETTER -> WORD (if formed)
    var showFormedWord by remember { mutableStateOf(false) }

    // Use lastFormedWord from uiState - this is reliable
    val formedWord = uiState.lastFormedWord
    val hasFormedWord = uiState.wordJustFormed && formedWord != null

    // Reset showFormedWord when prediction changes (new letter)
    LaunchedEffect(uiState.prediction) {
        showFormedWord = false
    }

    // Auto-transition logic
    LaunchedEffect(uiState.prediction, hasFormedWord) {
        if (hasFormedWord) {
            // Word formed: Show letter -> wait -> show word -> wait -> IDLE
            android.util.Log.d("ResultContent", "Word formed: $formedWord, showing letter first")
            kotlinx.coroutines.delay(1000) // Wait for letter TTS
            showFormedWord = true
            android.util.Log.d("ResultContent", "Now showing formed word: $formedWord")
            kotlinx.coroutines.delay(2000) // Wait for word TTS and display
            viewModel.acknowledgeFormedWord()
            viewModel.resetToIdle()
        } else {
            // No word formed: Show letter -> wait -> IDLE
            kotlinx.coroutines.delay(1500) // Wait for letter TTS and brief display
            viewModel.resetToIdle()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Show formed word after letter has been displayed and spoken
        if (showFormedWord && formedWord != null) {
            Text(
                text = formedWord,
                color = Color(0xFF4CAF50),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else {
            // Show predicted letter only
            Text(
                text = uiState.prediction ?: "?",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.display1,
                modifier = Modifier.wrapContentSize(Alignment.Center)
            )
        }
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
