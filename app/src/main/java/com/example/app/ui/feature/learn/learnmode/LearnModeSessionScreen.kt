package com.example.app.ui.feature.learn.learnmode

import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.app.R
import com.example.app.data.AppDatabase
import com.example.app.data.entity.LearnerProfileAnnotation
import com.example.app.data.repository.SetRepository
import com.example.app.service.WatchConnectionManager
import com.example.app.speech.DeepgramTTSManager
import com.example.app.speech.TextToSpeechManager
import com.example.app.ui.components.SessionPausedOverlay
import com.example.app.ui.components.WatchDisconnectedDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.app.service.SessionDisconnectReason
import com.example.app.ui.components.learnmode.AnnotationData
import com.example.app.ui.components.learnmode.LearnerProfileAnnotationDialog
import com.example.app.ui.components.common.ProgressIndicator
import com.example.app.ui.components.common.EndSessionDialog
import com.example.app.ui.components.common.ResumeSessionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val PurpleColor = Color(0xFFAE8EFB)
private val LightPurpleColor = Color(0xFFE7DDFE)
private val CompletedLetterColor = Color(0xFFAE8EFB)
private val PendingLetterColor = Color(0xFF808080)
private val BlueColor = Color(0xFF42A5F5)
private val WrongLetterColor = Color(0xFFFF6B6B)
private val YellowColor = Color(0xFFEDBB00)

private val wrongAffirmativeAudioResources = listOf(
    R.raw.wrong_youre_learning_so_keep_going,
    R.raw.wrong_think_carefully_and_try_one_more_time,
    R.raw.wrong_its_okay_to_make_mistakes_keep_trying,
    R.raw.wrong_believe_in_yourself_and_try_again,
    R.raw.wrong_stay_patient_and_keep_working,
    R.raw.wrong_give_it_another_try_and_do_your_best,
    R.raw.wrong_try_again_with_confidence,
    R.raw.wrong_youre_almost_there_keep_going,
    R.raw.wrong_keep_practicing_and_youll_get_it,
    R.raw.wrong_take_your_time_and_try_once_more,
    R.raw.wrong_you_can_do_better_on_the_next_try,
    R.raw.wrong_dont_worry_try_again
)

/**
 * Letters that have very similar writing structures between uppercase and lowercase
 * in air writing. These letters should be checked case-insensitively.
 */
private val similarCaseLetters = setOf('c', 'k', 'o', 'p', 's', 'u', 'v', 'w', 'x', 'z',
                                       'C', 'K', 'O', 'P', 'S', 'U', 'V', 'W', 'X', 'Z')

/**
 * Check if the input letter matches the expected letter.
 * For letters with similar writing structures (c, k, o, p, s, u, v, w, x, z),
 * the comparison is case-insensitive.
 * For other letters, the comparison is case-sensitive.
 */
private fun isLetterMatch(inputLetter: Char?, expectedLetter: Char?): Boolean {
    if (inputLetter == null || expectedLetter == null) return false

    // If the expected letter is one with similar case writing structure, compare case-insensitively
    return if (expectedLetter in similarCaseLetters) {
        inputLetter.lowercaseChar() == expectedLetter.lowercaseChar()
    } else {
        inputLetter == expectedLetter
    }
}

/**
 * Data class representing a word item in the learn mode session.
 */
private data class WordItem(
    val word: String,
    val selectedLetterIndex: Int,
    val imagePath: String?,
    val configurationType: String
) {
    fun getMaskedWord(): String {
        return word.mapIndexed { index, char ->
            if (index == selectedLetterIndex) "_" else char
        }.joinToString(" ")
    }

    fun getFullWord(): String {
        return word.toList().joinToString(" ")
    }

    fun getBlankWord(): String {
        return word.map { "▬" }.joinToString("  ")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnModeSessionScreen(
    setId: Long = 0L,
    activityId: Long = 0L,
    activityTitle: String = "",
    sessionKey: Int = 0,
    studentId: String = "",
    studentName: String = "",
    dominantHand: String = "RIGHT",
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSessionComplete: () -> Unit = {},
    onEarlyExit: () -> Unit = {}
) {
    // Local state - fresh on each recomposition with new sessionKey
    var isLoading by remember(sessionKey) { mutableStateOf(true) }
    var words by remember(sessionKey) { mutableStateOf<List<WordItem>>(emptyList()) }
    var currentWordIndex by remember(sessionKey) { mutableIntStateOf(0) }
    var title by remember(sessionKey) { mutableStateOf(activityTitle) }

    // State for Write the Word mode - tracks which letters have been correctly input
    var completedLetterIndices by remember(sessionKey) { mutableStateOf<Set<Int>>(emptySet()) }
    var currentLetterIndex by remember(sessionKey) { mutableIntStateOf(0) }

    // State for Fill in the Blank mode - tracks if the masked letter has been correctly answered
    var fillInBlankCorrect by remember(sessionKey) { mutableStateOf(false) }

    // Track which word indices were correctly answered (for green progress segments)
    var correctlyAnsweredWords by remember(sessionKey) { mutableStateOf<Set<Int>>(emptySet()) }

    // State for ProgressCheckDialog (only shown for correct answers)
    var showProgressCheckDialog by remember(sessionKey) { mutableStateOf(false) }
    var predictedLetter by remember(sessionKey) { mutableStateOf("") }
    var targetLetter by remember(sessionKey) { mutableStateOf("") }
    var targetCase by remember(sessionKey) { mutableStateOf("") }
    
    // Store pending state changes to apply after dialog dismissal
    var pendingCorrectAction by remember(sessionKey) { mutableStateOf<(() -> Unit)?>(null) }

    // State for wrong letter inline animation (replaces ProgressCheckDialog for incorrect answers)
    var wrongLetterText by remember(sessionKey) { mutableStateOf("") }
    var wrongLetterAnimationActive by remember(sessionKey) { mutableStateOf(false) }
    var wrongLetterAnimationTrigger by remember(sessionKey) { mutableIntStateOf(0) }
    val wrongLetterShakeOffset = remember(sessionKey) { Animatable(0f) }
    val wrongLetterAlpha = remember(sessionKey) { Animatable(1f) }

    // State for Learner Profile Annotation Dialog
    var showAnnotationDialog by remember(sessionKey) { mutableStateOf(false) }
    
    // State for annotation tooltip - show once when starting
    var showAnnotationTooltip by remember(sessionKey) { mutableStateOf(true) }

    // State for storing annotations per item (itemIndex -> AnnotationData)
    // This is a map keyed by item index within the current set
    var annotationsMap by remember(sessionKey) { mutableStateOf<Map<Int, AnnotationData>>(emptyMap()) }

    // Current annotation data for the dialog (loaded when dialog opens)
    var currentAnnotationData by remember(sessionKey) { mutableStateOf(AnnotationData.empty()) }

    // State to trigger save-and-complete when session ends
    var shouldSaveAndComplete by remember(sessionKey) { mutableStateOf(false) }

    // State for End Session confirmation dialog
    var showEndSessionConfirmation by remember(sessionKey) { mutableStateOf(false) }

    // State for Resume dialog
    var showResumeDialog by remember(sessionKey) { mutableStateOf(false) }
    // Restart confirmation (second layer after ResumeSessionDialog)
    var showRestartConfirmation by remember(sessionKey) { mutableStateOf(false) }

    // State for watch handshake - blocks session until watch is ready
    var isWatchReady by remember(sessionKey) { mutableStateOf(false) }

    // State for "student is writing" indicator + UI lock
    var isStudentWriting by remember(sessionKey) { mutableStateOf(false) }

    // State for TTS instruction text and playback animation
    var isTtsPlaying by remember(sessionKey) { mutableStateOf(false) }
    var currentInstructionText by remember(sessionKey) { mutableStateOf("") }

    val context = LocalContext.current

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Get database instance for annotation persistence
    val database = remember { AppDatabase.getInstance(context) }
    val annotationDao = remember { database.learnerProfileAnnotationDao() }
    val studentSetProgressDao = remember { database.studentSetProgressDao() }
    val annotationSummaryDao = remember { database.annotationSummaryDao() }
    val kuuRecommendationDao = remember { database.kuuRecommendationDao() }
    val geminiRepository = remember { com.example.app.data.repository.GeminiRepository() }

    // Get WatchConnectionManager instance
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    val isConnectionLost by watchConnectionManager.sessionConnectionLost.collectAsState()
    val disconnectReason by watchConnectionManager.sessionDisconnectReason.collectAsState()
    val isSessionPaused by watchConnectionManager.sessionPaused.collectAsState()
    val isWatchOnModeScreen by watchConnectionManager.watchOnModeScreen.collectAsState()

    // Bluetooth enable launcher - same pattern as DashboardScreen
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* BroadcastReceiver in WatchConnectionManager handles STATE_ON transition */ }

    // Initialize TTS managers
    val deepgramTtsManager = remember { DeepgramTTSManager(context) }
    val nativeTtsManager = remember { TextToSpeechManager(context) }

    // Check internet connectivity
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    val isNetworkAvailable = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    // Determine which TTS to use (Deepgram needs both API key AND internet)
    val useDeepgram = deepgramTtsManager.isConfigured() && isNetworkAvailable
    Log.d("LearnModeSession", "TTS Configuration - Deepgram configured: ${deepgramTtsManager.isConfigured()}, Internet available: $isNetworkAvailable, Using Deepgram: $useDeepgram")

    // Cleanup TTS on dispose
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            deepgramTtsManager.stop()
            nativeTtsManager.shutdown()
        }
    }

    // Load existing annotations for this student and set when the screen loads
    LaunchedEffect(setId, studentId, sessionKey) {
        if (setId > 0 && studentId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val existingAnnotations = annotationDao.getAnnotationsForStudentInSet(
                    studentId,
                    setId,
                    com.example.app.data.entity.LearnerProfileAnnotation.MODE_LEARN,
                    activityId
                )
                val loadedMap = existingAnnotations.associate { annotation ->
                    annotation.itemId to AnnotationData(
                        levelOfProgress = annotation.levelOfProgress,
                        strengthsObserved = annotation.getStrengthsList().toSet(),
                        strengthsNote = annotation.strengthsNote,
                        challenges = annotation.getChallengesList().toSet(),
                        challengesNote = annotation.challengesNote
                    )
                }
                annotationsMap = loadedMap
            }
        }
    }

    // Check for in-progress session to offer resume
    LaunchedEffect(setId, studentId, activityId, sessionKey) {
        val studentIdLong = studentId.toLongOrNull()
        if (studentIdLong != null && studentIdLong > 0 && activityId > 0 && setId > 0) {
            val inProgress = withContext(Dispatchers.IO) {
                studentSetProgressDao.getInProgressSession(studentIdLong, activityId, setId)
            }
            if (inProgress != null && inProgress.lastCompletedWordIndex > 0) {
                // Restore state from saved progress
                currentWordIndex = inProgress.lastCompletedWordIndex
                // Restore correctly answered words from JSON
                val savedWords = inProgress.correctlyAnsweredWordsJson
                if (!savedWords.isNullOrBlank()) {
                    try {
                        val indices = org.json.JSONArray(savedWords)
                        val restoredSet = mutableSetOf<Int>()
                        for (i in 0 until indices.length()) {
                            restoredSet.add(indices.getInt(i))
                        }
                        correctlyAnsweredWords = restoredSet
                    } catch (e: Exception) {
                        Log.e("LearnModeSession", "Failed to parse correctlyAnsweredWordsJson", e)
                    }
                }
                showResumeDialog = true
            }
        }
    }

    // Track active session state for watch re-entry sync
    DisposableEffect(sessionKey) {
        watchConnectionManager.setLearnModeSessionActive(true)
        onDispose {
            watchConnectionManager.setLearnModeSessionActive(false)
        }
    }

    // Start session health monitoring for disconnect detection
    DisposableEffect(Unit) {
        watchConnectionManager.startSessionMonitoring()
        onDispose {
            watchConnectionManager.stopSessionMonitoring()
        }
    }

    // Notify watch when Learn Mode session starts and begin handshake
    LaunchedEffect(sessionKey) {
        watchConnectionManager.notifyLearnModeStarted()
        // Send initial phone_ready signal
        watchConnectionManager.sendLearnModePhoneReady()
        // Heartbeat: keep pinging every 2 seconds until watch responds
        while (!isWatchReady) {
            kotlinx.coroutines.delay(2000)
            if (!isWatchReady) {
                watchConnectionManager.sendLearnModePhoneReady()
            }
        }
    }

    // Listen for watch ready signal
    LaunchedEffect(sessionKey) {
        watchConnectionManager.learnModeWatchReady.collect { timestamp ->
            if (timestamp > 0L) {
                isWatchReady = true
            }
        }
    }
    
    // Auto-dismiss annotation tooltip after 5 seconds (only when overlay is visible)
    LaunchedEffect(showAnnotationTooltip, showProgressCheckDialog) {
        if (showAnnotationTooltip && showProgressCheckDialog) {
            kotlinx.coroutines.delay(5000)
            showAnnotationTooltip = false
        }
    }
    
    // Handle save-and-complete when session ends
    LaunchedEffect(shouldSaveAndComplete) {
        if (shouldSaveAndComplete) {
            Log.d("LearnModeSession", "📝 Session ending - saving annotations and marking set as completed. studentId=$studentId, setId=$setId, annotationsCount=${annotationsMap.size}")
            withContext(Dispatchers.IO) {
                if (studentId.isNotBlank() && setId > 0) {
                    // Save all annotations in the map to database
                    annotationsMap.forEach { (itemId, annotationData) ->
                        if (annotationData.hasData()) {
                            val annotation = LearnerProfileAnnotation.create(
                                studentId = studentId,
                                setId = setId,
                                itemId = itemId,
                                sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                activityId = activityId,
                                levelOfProgress = annotationData.levelOfProgress,
                                strengthsObserved = annotationData.strengthsObserved.toList(),
                                strengthsNote = annotationData.strengthsNote,
                                challenges = annotationData.challenges.toList(),
                                challengesNote = annotationData.challengesNote
                            )
                            annotationDao.insertOrUpdate(annotation)
                            Log.d("LearnModeSession", "📝 Saved annotation for itemId=$itemId: level=${annotationData.levelOfProgress}")
                        }
                    }
                    Log.d("LearnModeSession", "📝 All annotations saved to DB on session complete: ${annotationsMap.size} items for studentId=$studentId, setId=$setId")
                    
                    // Mark the set as completed in StudentSetProgress
                    val studentIdLong = studentId.toLongOrNull()
                    if (studentIdLong != null && activityId > 0) {
                        // Clear any in-progress session data
                        studentSetProgressDao.clearInProgressSession(studentIdLong, activityId, setId)
                        // Check if progress record exists
                        val existingProgress = studentSetProgressDao.getProgress(studentIdLong, activityId, setId)
                        if (existingProgress != null) {
                            // Update existing record to mark as completed
                            studentSetProgressDao.markSetAsCompleted(
                                studentId = studentIdLong,
                                activityId = activityId,
                                setId = setId,
                                completedAt = System.currentTimeMillis()
                            )
                        } else {
                            // Create new progress record marked as completed
                            val progress = com.example.app.data.entity.StudentSetProgress(
                                studentId = studentIdLong,
                                activityId = activityId,
                                setId = setId,
                                isCompleted = true,
                                completionPercentage = 100,
                                completedAt = System.currentTimeMillis()
                            )
                            studentSetProgressDao.upsertProgress(progress)
                        }
                        kuuRecommendationDao.incrementCompletions(studentIdLong)
                        Log.d("LearnModeSession", "✅ Set marked as completed in StudentSetProgress: studentId=$studentId, activityId=$activityId, setId=$setId")

                        // Generate AI summary for this set's annotations
                        try {
                            val allAnnotations = annotationDao.getAnnotationsForStudentInSet(
                                studentId = studentId,
                                setId = setId,
                                sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                activityId = activityId
                            )
                            if (allAnnotations.isNotEmpty()) {
                                val wordNames = words.mapIndexed { i, w -> i to w.word }.toMap()
                                val summaryText = geminiRepository.generateAnnotationSummary(
                                    allAnnotations, LearnerProfileAnnotation.MODE_LEARN, wordNames
                                )
                                if (summaryText != null) {
                                    annotationSummaryDao.insertOrUpdate(
                                        com.example.app.data.entity.AnnotationSummary(
                                            studentId = studentId,
                                            setId = setId,
                                            sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                            activityId = activityId,
                                            summaryText = summaryText
                                        )
                                    )
                                    Log.d("LearnModeSession", "AI summary generated and saved for setId=$setId")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("LearnModeSession", "AI summary generation failed: ${e.message}")
                        }
                    } else {
                        Log.w("LearnModeSession", "⚠️ Cannot mark set as completed - studentId=$studentId, activityId=$activityId, setId=$setId")
                    }
                } else {
                    Log.w("LearnModeSession", "⚠️ Cannot save annotations - studentId='$studentId' (blank=${studentId.isBlank()}), setId=$setId")
                }
            }
            // Navigate after saving is complete
            onSessionComplete()
        }
    }

    // Save partial progress and exit early
    val saveProgressAndExit: () -> Unit = {
        coroutineScope.launch {
            val studentIdLong = studentId.toLongOrNull()
            if (studentIdLong != null && studentIdLong > 0 && setId > 0 && activityId > 0) {
                withContext(Dispatchers.IO) {
                    // Save all annotations accumulated so far
                    annotationsMap.forEach { (itemId, annotationData) ->
                        if (annotationData.hasData()) {
                            val annotation = LearnerProfileAnnotation.create(
                                studentId = studentId,
                                setId = setId,
                                itemId = itemId,
                                sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                activityId = activityId,
                                levelOfProgress = annotationData.levelOfProgress,
                                strengthsObserved = annotationData.strengthsObserved.toList(),
                                strengthsNote = annotationData.strengthsNote,
                                challenges = annotationData.challenges.toList(),
                                challengesNote = annotationData.challengesNote
                            )
                            annotationDao.insertOrUpdate(annotation)
                        }
                    }

                    // Generate AI summary if annotations exist
                    try {
                        val allAnnotations = annotationDao.getAnnotationsForStudentInSet(
                            studentId = studentId,
                            setId = setId,
                            sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                            activityId = activityId
                        )
                        if (allAnnotations.isNotEmpty()) {
                            val wordNames = words.mapIndexed { i, w -> i to w.word }.toMap()
                            val summaryText = geminiRepository.generateAnnotationSummary(
                                allAnnotations, LearnerProfileAnnotation.MODE_LEARN, wordNames
                            )
                            if (summaryText != null) {
                                annotationSummaryDao.insertOrUpdate(
                                    com.example.app.data.entity.AnnotationSummary(
                                        studentId = studentId,
                                        setId = setId,
                                        sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                        activityId = activityId,
                                        summaryText = summaryText
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LearnModeSession", "AI summary generation failed on early exit: ${e.message}")
                    }

                    // Save partial progress
                    val completedWords = correctlyAnsweredWords.size
                    val totalWords = words.size.coerceAtLeast(1)
                    val percentage = (completedWords * 100) / totalWords
                    val correctWordsJson = org.json.JSONArray(correctlyAnsweredWords.toList()).toString()

                    val existingProgress = studentSetProgressDao.getProgress(studentIdLong, activityId, setId)
                    if (existingProgress != null) {
                        studentSetProgressDao.upsertProgress(
                            existingProgress.copy(
                                lastCompletedWordIndex = currentWordIndex,
                                correctlyAnsweredWordsJson = correctWordsJson,
                                completionPercentage = percentage,
                                isCompleted = false,
                                completedAt = null,
                                lastAccessedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        studentSetProgressDao.upsertProgress(
                            com.example.app.data.entity.StudentSetProgress(
                                studentId = studentIdLong,
                                activityId = activityId,
                                setId = setId,
                                isCompleted = false,
                                completionPercentage = percentage,
                                lastCompletedWordIndex = currentWordIndex,
                                correctlyAnsweredWordsJson = correctWordsJson,
                                lastAccessedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    Log.d("LearnModeSession", "Partial progress saved: wordIndex=$currentWordIndex, correctWords=$correctWordsJson, percentage=$percentage%")
                }
            }
            watchConnectionManager.notifyLearnModeEnded()
            onEarlyExit()
        }
    }

    // Note: We don't call notifyLearnModeEnded() here on dispose anymore
    // because we want the watch to keep showing the completion screen
    // while the phone shows the analytics screen.
    // notifyLearnModeEnded() is now called from LearnModeSessionAnalyticsScreen
    // when the user leaves (Practice Again or Continue).

    // Load data when screen opens - using sessionKey as key ensures this runs fresh each time
    LaunchedEffect(setId, sessionKey) {
        if (setId > 0) {
            isLoading = true
            try {
                val database = AppDatabase.getInstance(context)
                val setRepository = SetRepository(database)

                val setDetails = withContext(Dispatchers.IO) {
                    setRepository.getSetDetails(setId)
                }

                if (setDetails != null) {
                    words = setDetails.words.map { wordConfig ->
                        WordItem(
                            word = wordConfig.word,
                            selectedLetterIndex = wordConfig.selectedLetterIndex,
                            imagePath = wordConfig.imagePath,
                            configurationType = wordConfig.configurationType
                        )
                    }
                    title = activityTitle
                }
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Send current word data to watch whenever current word changes (only after handshake)
    LaunchedEffect(currentWordIndex, words, isWatchReady, showResumeDialog) {
        if (!isWatchReady || showResumeDialog) return@LaunchedEffect
        val currentWord = words.getOrNull(currentWordIndex)
        if (currentWord != null) {
            // Reset letter tracking state for new word
            completedLetterIndices = emptySet()
            currentLetterIndex = 0
            fillInBlankCorrect = false

            watchConnectionManager.sendLearnModeWordData(
                word = currentWord.word,
                maskedIndex = currentWord.selectedLetterIndex,
                configurationType = currentWord.configurationType,
                dominantHand = dominantHand
            )
            watchConnectionManager.updateCurrentLearnModeWordData(
                word = currentWord.word,
                maskedIndex = currentWord.selectedLetterIndex,
                configurationType = currentWord.configurationType,
                dominantHand = dominantHand
            )

            // Stop any ongoing TTS before starting new phrase
            if (useDeepgram) {
                deepgramTtsManager.stop()
            } else {
                nativeTtsManager.stop()
            }
            isTtsPlaying = false

            // Generate instruction text and speak it with TTS
            val wordForPhrase = if (currentWord.configurationType == "Name the Picture") null else currentWord.word
            val phrase = getInstructionText(currentWord.configurationType, wordForPhrase)
            currentInstructionText = phrase

            launch {
                try {
                    isTtsPlaying = true
                    if (useDeepgram) {
                        Log.d("LearnModeSession", "Using Deepgram TTS")
                        deepgramTtsManager.speak(phrase) {
                            isTtsPlaying = false
                        }
                    } else {
                        Log.d("LearnModeSession", "Using Native TTS")
                        nativeTtsManager.speak(phrase) {
                            coroutineScope.launch(Dispatchers.Main.immediate) {
                                isTtsPlaying = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LearnModeSession", "Error playing TTS", e)
                    isTtsPlaying = false
                }
            }
        }
    }

    // Listen for letter input events from watch (for Write the Word, Name the Picture, and Fill in the Blank modes)
    LaunchedEffect(Unit) {
        var lastEventTime = 0L
        watchConnectionManager.letterInputEvent.collect { event ->
            android.util.Log.d("LearnModeSession", "📥 Received letterInputEvent: letter=${event.letter}, timestamp=${event.timestamp}, lastEventTime=$lastEventTime")

            // Process event only if timestamp is valid (> 0) and newer than last processed event
            if (event.timestamp > 0L && event.timestamp > lastEventTime) {
                lastEventTime = event.timestamp
                isStudentWriting = false  // Student finished writing, reset indicator

                // Block input while wrong letter animation is playing
                if (wrongLetterAnimationActive) {
                    android.util.Log.d("LearnModeSession", "🚫 Input BLOCKED - animation active (trigger=$wrongLetterAnimationTrigger)")
                    return@collect
                }

                val currentWord = words.getOrNull(currentWordIndex)
                android.util.Log.d("LearnModeSession", "📥 Processing event: currentWord=${currentWord?.word}, type=${currentWord?.configurationType}, currentWordIndex=$currentWordIndex")
                if (currentWord != null) {
                    when (currentWord.configurationType) {
                        "Fill in the Blank" -> {
                            // For Fill in the Blank, check if the masked letter is correct
                            val expectedLetter = currentWord.word.getOrNull(currentWord.selectedLetterIndex)
                            // Use case-insensitive matching for similar letters (c, k, o, p, s, u, v, w, x, z)
                            val isCorrect = isLetterMatch(event.letter, expectedLetter)
                            android.util.Log.d("LearnModeSession", "📝 Fill in the Blank: input=${event.letter}, expected=$expectedLetter, isCorrect=$isCorrect")

                            // Set up dialog state
                            targetLetter = expectedLetter?.toString() ?: ""
                            targetCase = if (expectedLetter?.isUpperCase() == true) "capital" else "small"

                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            if (isCorrect) {
                                predictedLetter = event.letter.toString()
                                android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Fill in the Blank (correct)")
                                showProgressCheckDialog = true
                            } else {
                                android.util.Log.d("LearnModeSession", "🎭 Starting wrong letter animation for Fill in the Blank")
                                wrongLetterText = event.letter.toString()
                                wrongLetterAnimationActive = true
                                wrongLetterAnimationTrigger++
                            }

                            // Store pending action to execute on dialog dismiss
                            if (isCorrect) {
                                pendingCorrectAction = {
                                    // Mark Fill in the Blank as correct to reveal the letter
                                    fillInBlankCorrect = true

                                    // Flag this word as correctly answered
                                    correctlyAnsweredWords = correctlyAnsweredWords + currentWordIndex

                                    // Correct answer - notify watch and move to next word
                                    watchConnectionManager.sendWordComplete()

                                    if (correctlyAnsweredWords.size >= words.size) {
                                        // All words answered correctly - complete the activity
                                        watchConnectionManager.notifyActivityComplete()
                                        shouldSaveAndComplete = true
                                    } else {
                                        // Navigate to next unanswered word (wrap around)
                                        val nextUnanswered = ((currentWordIndex + 1) until words.size).firstOrNull { it !in correctlyAnsweredWords }
                                            ?: (0 until currentWordIndex).firstOrNull { it !in correctlyAnsweredWords }
                                        if (nextUnanswered != null) {
                                            currentWordIndex = nextUnanswered
                                        }
                                    }
                                }
                            } else {
                                pendingCorrectAction = {
                                    // Wrong letter - send incorrect feedback so watch stays on same letter
                                    watchConnectionManager.sendLetterResult(false, currentWord.selectedLetterIndex, currentWord.word.length)
                                }
                            }
                        }
                        "Write the Word", "Name the Picture" -> {
                            // Get expected letter
                            val expectedLetter = currentWord.word.getOrNull(currentLetterIndex)

                            // Check if input letter matches expected letter
                            // Uses case-insensitive matching for similar letters (c, k, o, p, s, u, v, w, x, z)
                            val isCorrect = isLetterMatch(event.letter, expectedLetter)
                            android.util.Log.d("LearnModeSession", "📝 Write the Word/Name the Picture: input=${event.letter}, expected=$expectedLetter, isCorrect=$isCorrect")

                            // Set up dialog state
                            targetLetter = expectedLetter?.toString() ?: ""
                            targetCase = if (expectedLetter?.isUpperCase() == true) "capital" else "small"

                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            if (isCorrect) {
                                predictedLetter = event.letter.toString()
                                val newLetterIndex = currentLetterIndex + 1
                                val isWordComplete = newLetterIndex >= currentWord.word.length

                                // Mark letter as completed immediately (turns purple)
                                completedLetterIndices = completedLetterIndices + currentLetterIndex
                                currentLetterIndex = newLetterIndex

                                if (isWordComplete) {
                                    // Word complete - show the full correct overlay
                                    android.util.Log.d("LearnModeSession", "🎭 Word complete! Showing overlay for Write the Word/Name the Picture")
                                    showProgressCheckDialog = true
                                    pendingCorrectAction = {
                                        // Flag this word as correctly answered
                                        correctlyAnsweredWords = correctlyAnsweredWords + currentWordIndex

                                        // Notify watch and move to next word
                                        watchConnectionManager.sendWordComplete()

                                        if (correctlyAnsweredWords.size >= words.size) {
                                            // All words answered correctly - complete the activity
                                            watchConnectionManager.notifyActivityComplete()
                                            shouldSaveAndComplete = true
                                        } else {
                                            // Navigate to next unanswered word (wrap around)
                                            val nextUnanswered = ((currentWordIndex + 1) until words.size).firstOrNull { it !in correctlyAnsweredWords }
                                                ?: (0 until currentWordIndex).firstOrNull { it !in correctlyAnsweredWords }
                                            if (nextUnanswered != null) {
                                                currentWordIndex = nextUnanswered
                                            }
                                        }
                                    }
                                } else {
                                    // Letter correct but word not done - play correct sound, no overlay
                                    android.util.Log.d("LearnModeSession", "📝 Letter correct, advancing to next letter (no overlay)")
                                    try {
                                        val mp = MediaPlayer()
                                        val afd = context.resources.openRawResourceFd(R.raw.correct)
                                        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                        mp.prepare()
                                        mp.setOnCompletionListener { mp.release() }
                                        mp.start()
                                    } catch (e: Exception) {
                                        android.util.Log.e("LearnModeSession", "Error playing correct sound", e)
                                    }
                                    // Notify watch and move to next letter
                                    watchConnectionManager.sendLetterResult(true, newLetterIndex, currentWord.word.length)
                                    watchConnectionManager.notifyLearnModeFeedbackDismissed()
                                }
                            } else {
                                android.util.Log.d("LearnModeSession", "🎭 Starting wrong letter animation for Write the Word/Name the Picture")
                                wrongLetterText = event.letter.toString()
                                wrongLetterAnimationActive = true
                                wrongLetterAnimationTrigger++
                                pendingCorrectAction = {
                                    // Wrong letter - send incorrect feedback so watch stays on same letter
                                    watchConnectionManager.sendLetterResult(false, currentLetterIndex, currentWord.word.length)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Listen for gesture recording signal from watch (student started writing)
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastRecordingTime = 0L
        watchConnectionManager.learnModeGestureRecording.collect { timestamp ->
            if (timestamp > sessionStartTime && timestamp > lastRecordingTime && timestamp > 0L) {
                lastRecordingTime = timestamp
                isStudentWriting = true
            }
        }
    }

    // Wrong letter inline animation sequence (~3 seconds)
    // Key is a counter that increments per trigger — never changes mid-execution,
    // so the coroutine always runs to completion (no cancellation issues).
    LaunchedEffect(wrongLetterAnimationTrigger) {
        if (wrongLetterAnimationTrigger > 0 && wrongLetterAnimationActive) {
            android.util.Log.d("LearnModeSession", "🎬 Animation START: trigger=$wrongLetterAnimationTrigger, letter=$wrongLetterText")
            // Reset animation values
            wrongLetterShakeOffset.snapTo(0f)
            wrongLetterAlpha.snapTo(1f)

            // Play wrong audio concurrently — MediaPlayer self-releases when all audio finishes,
            // so the animation ending won't cut off audio that's still playing.
            try {
                val mediaPlayer = MediaPlayer()
                fun playAudio(resId: Int, onComplete: (() -> Unit)? = null) {
                    try {
                        mediaPlayer.reset()
                        val afd = context.resources.openRawResourceFd(resId)
                        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        mediaPlayer.prepare()
                        mediaPlayer.setOnCompletionListener {
                            onComplete?.invoke()
                        }
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onComplete?.invoke()
                    }
                }

                val randomAffirmative = wrongAffirmativeAudioResources.random()
                playAudio(R.raw.wrong) {
                    playAudio(randomAffirmative) {
                        // Release after all audio finishes
                        try { mediaPlayer.release() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e("LearnModeSession", "Error playing wrong audio", e)
            }

            // Phase 1: Hold
            kotlinx.coroutines.delay(500L)

            // Phase 2: Shake
            val shakeDurations = listOf(300, 300, 300, 300, 300)
            val shakeAmplitudes = listOf(12f, 10f, 8f, 5f, 3f)
            for (i in shakeDurations.indices) {
                wrongLetterShakeOffset.animateTo(
                    targetValue = shakeAmplitudes[i],
                    animationSpec = tween(durationMillis = shakeDurations[i] / 2)
                )
                wrongLetterShakeOffset.animateTo(
                    targetValue = -shakeAmplitudes[i],
                    animationSpec = tween(durationMillis = shakeDurations[i] / 2)
                )
            }
            wrongLetterShakeOffset.snapTo(0f)

            // Phase 3: Fade out
            wrongLetterAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = EaseOut)
            )

            // Reset animation values
            wrongLetterShakeOffset.snapTo(0f)
            wrongLetterAlpha.snapTo(1f)

            // Only do cleanup if animation wasn't already cancelled by watch dismiss or skip
            if (wrongLetterAnimationActive) {
                wrongLetterText = ""
                wrongLetterAnimationActive = false

                android.util.Log.d("LearnModeSession", "🎬 Animation COMPLETE: trigger=$wrongLetterAnimationTrigger, notifying watch & running pending action")
                watchConnectionManager.notifyLearnModeFeedbackDismissed()
                pendingCorrectAction?.invoke()
                pendingCorrectAction = null
            } else {
                android.util.Log.d("LearnModeSession", "🎬 Animation already cancelled, skipping duplicate cleanup")
            }
        }
    }

    // Watch Disconnected Dialog - blocks session when watch is unreachable
    if (isConnectionLost) {
        WatchDisconnectedDialog(
            isBluetoothOff = disconnectReason == SessionDisconnectReason.BLUETOOTH_OFF,
            onTurnOnBluetooth = {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyLearnModeEnded()
                onEarlyExit()
            }
        )
    }

    // Progress Check Dialog - show before loading check to ensure it always renders
    if (showProgressCheckDialog) {
        ProgressCheckDialog(
            studentName = studentName,
            targetLetter = targetLetter,
            targetCase = targetCase,
            predictedLetter = predictedLetter,
            showAnnotationTooltip = showAnnotationTooltip,
            onAnnotateClick = {
                showAnnotationDialog = true
            },
            onAnnotationTooltipDismissed = {
                showAnnotationTooltip = false
            },
            onDismiss = {
                showProgressCheckDialog = false
                // Notify watch that mobile dismissed feedback
                watchConnectionManager.notifyLearnModeFeedbackDismissed()
                // Execute pending action after dialog is dismissed
                pendingCorrectAction?.invoke()
                pendingCorrectAction = null
            }
        )
    }

    // Learner Profile Annotation Dialog
    if (showAnnotationDialog) {
        // Load current annotation data when dialog opens
        val existingAnnotation = annotationsMap[currentWordIndex] ?: AnnotationData.empty()

        LearnerProfileAnnotationDialog(
            studentName = studentName,
            existingData = existingAnnotation,
            onDismiss = { showAnnotationDialog = false },
            accentColor = BlueColor,
            buttonColor = BlueColor,
            onAddNote = { levelOfProgress, strengthsObserved, strengthsNote, challenges, challengesNote ->
                // Create new annotation data
                val newAnnotationData = AnnotationData(
                    levelOfProgress = levelOfProgress,
                    strengthsObserved = strengthsObserved.toSet(),
                    strengthsNote = strengthsNote,
                    challenges = challenges.toSet(),
                    challengesNote = challengesNote
                )

                // Update local state immediately
                annotationsMap = annotationsMap + (currentWordIndex to newAnnotationData)

                // Save to database asynchronously
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        if (studentId.isNotBlank() && setId > 0) {
                            val annotation = LearnerProfileAnnotation.create(
                                studentId = studentId,
                                setId = setId,
                                itemId = currentWordIndex,
                                sessionMode = LearnerProfileAnnotation.MODE_LEARN,
                                activityId = activityId,
                                levelOfProgress = levelOfProgress,
                                strengthsObserved = strengthsObserved,
                                strengthsNote = strengthsNote,
                                challenges = challenges,
                                challengesNote = challengesNote
                            )
                            annotationDao.insertOrUpdate(annotation)
                            Log.d("LearnModeSession", "📝 Annotation saved to DB: studentId=$studentId, setId=$setId, activityId=$activityId, itemId=$currentWordIndex")
                        }
                    }
                }

                Log.d("LearnModeSession", "📝 Annotation saved: level=$levelOfProgress, strengths=$strengthsObserved, challenges=$challenges")
            }
        )
    }

    // Show loading
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PurpleColor)
        }
        return
    }

    val currentWord = words.getOrNull(currentWordIndex)
    val totalWords = words.size.coerceAtLeast(1)
    val currentStep = currentWordIndex + 1

    // Function to handle skip/next
    fun handleSkipOrNext() {
        isStudentWriting = false
        // Cancel any ongoing wrong letter animation
        if (wrongLetterAnimationActive) {
            wrongLetterAnimationActive = false
            wrongLetterText = ""
            pendingCorrectAction = null
        }
        // Navigate to next unanswered word (wrap around instead of completing)
        val nextUnanswered = ((currentWordIndex + 1) until words.size).firstOrNull { it !in correctlyAnsweredWords }
            ?: (0 until currentWordIndex).firstOrNull { it !in correctlyAnsweredWords }
        if (nextUnanswered != null) {
            currentWordIndex = nextUnanswered
        }
        // Note: activity only completes when all words are answered correctly (not via skip)
    }


    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Bar
        ProgressIndicator(
            currentStep = currentStep,
            totalSteps = totalWords,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
            completedIndices = correctlyAnsweredWords,
            activeColor = PurpleColor,
            inactiveColor = LightPurpleColor,
            completedColor = Color(0xFF4CAF50),
            onSegmentClick = { index ->
                if (!isStudentWriting) {
                    // Cancel any ongoing wrong letter animation
                    if (wrongLetterAnimationActive) {
                        wrongLetterAnimationActive = false
                        wrongLetterText = ""
                        pendingCorrectAction = null
                    }
                    // Navigate to the clicked item
                    currentWordIndex = index
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        // Skip Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip button
            IconButton(
                onClick = {
                    if (!isStudentWriting) {
                        onSkip()
                        handleSkipOrNext()
                    }
                },
                enabled = !isStudentWriting
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_skip),
                    contentDescription = "Skip",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // Instruction text with pulsing animation during TTS playback
        Spacer(Modifier.height(4.dp))

        val subtitleAlpha = if (isTtsPlaying) {
            val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "subtitlePulse"
            ).value
        } else {
            1f
        }
        val subtitleColor = animateColorAsState(
            targetValue = if (isTtsPlaying) PurpleColor else Color.Gray,
            animationSpec = tween(300),
            label = "subtitleColor"
        ).value

        Text(
            text = currentInstructionText,
            fontSize = 16.sp,
            fontWeight = if (isTtsPlaying) FontWeight.SemiBold else FontWeight.Medium,
            color = subtitleColor.copy(alpha = subtitleAlpha),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        val isNameThePicture = currentWord?.configurationType == "Name the Picture"
        val isFillInTheBlank = currentWord?.configurationType == "Fill in the Blank"
        val imageExistsForBranch = currentWord?.imagePath?.let { File(it).exists() } ?: false
        val isFillInBlankWithImage = isFillInTheBlank && imageExistsForBranch

        // Large Content Card (purple border) - hidden for Name the Picture mode and Fill in the Blank with image
        if (!isNameThePicture && !isFillInBlankWithImage) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(3.dp, Color(0xFFAE8EFB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Replay question button - top end
                IconButton(
                    onClick = {
                        if (currentInstructionText.isNotEmpty()) {
                            coroutineScope.launch {
                                try {
                                    if (useDeepgram) {
                                        deepgramTtsManager.stop()
                                    } else {
                                        nativeTtsManager.stop()
                                    }
                                    isTtsPlaying = true
                                    if (useDeepgram) {
                                        deepgramTtsManager.speak(currentInstructionText) {
                                            isTtsPlaying = false
                                        }
                                    } else {
                                        nativeTtsManager.speak(currentInstructionText) {
                                            coroutineScope.launch(Dispatchers.Main.immediate) {
                                                isTtsPlaying = false
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("LearnModeSession", "Error replaying TTS", e)
                                    isTtsPlaying = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay question",
                        modifier = Modifier.size(24.dp),
                        tint = PurpleColor
                    )
                }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large Content Card with Image (Square) - only show if there's an image
            val imageExists = currentWord?.imagePath?.let { File(it).exists() } ?: false
            if (imageExists) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFBF9FF)
                    ),
                    border = BorderStroke(4.dp, Color(0xFFAE8EFB))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(File(currentWord.imagePath))
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Word image",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Extra spacing for cards to push text/dashes lower
                val isNameThePicture = currentWord.configurationType == "Name the Picture"
                val isFillInTheBlanks = currentWord.configurationType == "Fill in the Blank"
                if (isNameThePicture || isFillInTheBlanks) {
                    Spacer(Modifier.height(48.dp))
                } else {
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Masked Word Display (e.g., "D _ G") or Full Word Display for "Write the Word" or Blank for "Name the Picture"
            if (currentWord != null) {
                val isWriteTheWord = currentWord.configurationType == "Write the Word"
                val isNameThePicture = currentWord.configurationType == "Name the Picture"
                val isFillInTheBlank = currentWord.configurationType == "Fill in the Blank"

                when {
                    isWriteTheWord -> {
                        // Write the Word mode - show letters with color based on completion
                        WriteTheWordDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
                    }
                    isFillInTheBlank -> {
                        // Fill in the Blank mode - show masked word, reveal correct letter in violet when answered
                        FillInTheBlankDisplay(
                            word = currentWord.word,
                            maskedIndex = currentWord.selectedLetterIndex,
                            isCorrect = fillInBlankCorrect,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
                    }
                    isNameThePicture -> {
                        // Name the Picture mode - show blanks that reveal letters when correct
                        NameThePictureDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = imageExists,
                            wrongLetterText = wrongLetterText,
                            wrongLetterAnimationActive = wrongLetterAnimationActive,
                            wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                            wrongLetterAlpha = wrongLetterAlpha.value
                        )
                    }
                    else -> {
                        Text(
                            text = currentWord.getMaskedWord(),
                            fontSize = if (imageExists) 48.sp else 96.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            letterSpacing = if (imageExists) 4.sp else 8.sp
                        )
                    }
                }
            }

        }
            }
        }
        } else {
            // Name the Picture / Fill in the Blank with image - no card wrapper, just the content with weight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                val imageExists = currentWord?.imagePath?.let { File(it).exists() } ?: false

                // Replay question button row (same alignment as skip button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentInstructionText.isNotEmpty()) {
                                coroutineScope.launch {
                                    try {
                                        if (useDeepgram) {
                                            deepgramTtsManager.stop()
                                        } else {
                                            nativeTtsManager.stop()
                                        }
                                        isTtsPlaying = true
                                        if (useDeepgram) {
                                            deepgramTtsManager.speak(currentInstructionText) {
                                                isTtsPlaying = false
                                            }
                                        } else {
                                            nativeTtsManager.speak(currentInstructionText) {
                                                coroutineScope.launch(Dispatchers.Main.immediate) {
                                                    isTtsPlaying = false
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LearnModeSession", "Error replaying TTS", e)
                                        isTtsPlaying = false
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay question",
                            modifier = Modifier.size(28.dp),
                            tint = PurpleColor
                        )
                    }
                }

                // Image card for Name the Picture (bigger)
                if (imageExists) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFBF9FF)
                        ),
                        border = BorderStroke(4.dp, Color(0xFFAE8EFB))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(File(currentWord.imagePath))
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Word image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Word display - use FillInTheBlankDisplay for Fill in the Blank, NameThePictureDisplay otherwise
                if (currentWord != null) {
                    when (currentWord.configurationType) {
                        "Fill in the Blank" -> {
                            FillInTheBlankDisplay(
                                word = currentWord.word,
                                maskedIndex = currentWord.selectedLetterIndex,
                                isCorrect = fillInBlankCorrect,
                                hasImage = imageExists,
                                wrongLetterText = wrongLetterText,
                                wrongLetterAnimationActive = wrongLetterAnimationActive,
                                wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                                wrongLetterAlpha = wrongLetterAlpha.value
                            )
                        }
                        else -> {
                            NameThePictureDisplay(
                                word = currentWord.word,
                                completedIndices = completedLetterIndices,
                                currentIndex = currentLetterIndex,
                                hasImage = imageExists,
                                wrongLetterText = wrongLetterText,
                                wrongLetterAnimationActive = wrongLetterAnimationActive,
                                wrongLetterShakeOffset = wrongLetterShakeOffset.value,
                                wrongLetterAlpha = wrongLetterAlpha.value
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // "Student is air writing..." indicator or End Session button
        if (isStudentWriting) {
            val writingPulse = rememberInfiniteTransition(label = "writingPulse")
            val writingAlpha by writingPulse.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "writingAlpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${studentName.ifEmpty { "Student" }} is air writing...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleColor.copy(alpha = writingAlpha),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // End Session Button
            Button(
                onClick = { showEndSessionConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "End Session",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Waiting for watch overlay - drawn on top of content inside Box
    if (!isWatchReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Block clicks through overlay
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Spacer(Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.dis_pairing_learn),
                    contentDescription = "Waiting for watch",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Waiting for $studentName...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Please open Learn Mode\non the watch to connect.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.weight(1f))

                // Cancel Session button
                Button(
                    onClick = {
                        watchConnectionManager.notifyLearnModeEnded()
                        onEarlyExit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel Session",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // End Session confirmation dialog
    if (showEndSessionConfirmation) {
        EndSessionDialog(
            mascotDrawable = R.drawable.dis_end_session,
            onEndSession = {
                showEndSessionConfirmation = false
                saveProgressAndExit()
            },
            onCancel = { showEndSessionConfirmation = false }
        )
    }

    // Resume dialog - shown when there's saved progress from a previous session
    if (showResumeDialog) {
        val savedWords = correctlyAnsweredWords.size
        val totalWords = words.size.coerceAtLeast(1)
        ResumeSessionDialog(
            mascotDrawable = R.drawable.dis_pairing_learn,
            studentName = studentName,
            completedCount = savedWords,
            totalCount = totalWords,
            unitLabel = "words",
            onResume = {
                showResumeDialog = false
                // currentWordIndex and correctlyAnsweredWords already restored in LaunchedEffect
            },
            onRestart = {
                // Show a second confirmation before wiping progress
                showRestartConfirmation = true
            }
        )
    }

    // Restart confirmation dialog (second layer)
    if (showRestartConfirmation) {
        val savedWords = correctlyAnsweredWords.size
        Dialog(
            onDismissRequest = { showRestartConfirmation = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Restart Session?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "This will erase all progress ($savedWords completed word${if (savedWords != 1) "s" else ""}). Are you sure you want to start over?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Go Back button
                            Button(
                                onClick = { showRestartConfirmation = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.LightGray.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Go Back",
                                    color = Color.DarkGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Confirm Restart button
                            Button(
                                onClick = {
                                    showRestartConfirmation = false
                                    showResumeDialog = false
                                    currentWordIndex = 0
                                    correctlyAnsweredWords = emptySet()
                                    coroutineScope.launch {
                                        val studentIdLong = studentId.toLongOrNull()
                                        if (studentIdLong != null && studentIdLong > 0) {
                                            withContext(Dispatchers.IO) {
                                                studentSetProgressDao.clearInProgressSession(studentIdLong, activityId, setId)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Restart",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Session Paused Overlay - non-blocking, drawn on top of content inside Box
    // Only show if not also showing the disconnect dialog (disconnect takes priority)
    if (isSessionPaused && !isConnectionLost) {
        SessionPausedOverlay(
            watchOnScreen = isWatchOnModeScreen,
            onContinue = {
                watchConnectionManager.resumeLearnModeSession()
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyLearnModeEnded()
                onEarlyExit()
            }
        )
    }

    } // End of outer Box
}

/**
 * Bouncing hand pointer that appears below the current letter to indicate where to write next.
 */
@Composable
private fun BouncingHandPointer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "handBounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handBounceOffset"
    )

    Image(
        painter = painterResource(id = R.drawable.ic_kusho_hand),
        contentDescription = "Write here",
        modifier = modifier
            .size(40.dp)
            .offset(y = bounceOffset.dp),
        contentScale = ContentScale.Fit
    )
}

/**
 * Display for Write the Word mode showing each letter with color based on completion status.
 * Current letter has an underline. Completed letters turn purple (#AE8EFB), pending letters are gray.
 * A bouncing hand pointer indicates the current letter to write.
 */
@Composable
private fun WriteTheWordDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int,
    hasImage: Boolean,
    wrongLetterText: String = "",
    wrongLetterAnimationActive: Boolean = false,
    wrongLetterShakeOffset: Float = 0f,
    wrongLetterAlpha: Float = 1f
) {
    val fontSize = if (hasImage) 48.sp else 96.sp
    val letterSpacing = if (hasImage) 12.dp else 16.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(letterSpacing),
        verticalAlignment = Alignment.Top
    ) {
        word.forEachIndexed { index, letter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isCurrentAndWrong = index == currentIndex && wrongLetterAnimationActive
                Text(
                    text = if (isCurrentAndWrong) wrongLetterText else letter.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentAndWrong -> WrongLetterColor
                        index in completedIndices -> CompletedLetterColor
                        else -> PendingLetterColor
                    },
                    modifier = if (isCurrentAndWrong) {
                        Modifier
                            .offset(x = wrongLetterShakeOffset.dp)
                            .alpha(wrongLetterAlpha)
                    } else {
                        Modifier
                    }
                )

                // Add underline for current letter being input
                if (index == currentIndex) {
                    Box(
                        modifier = Modifier
                            .width(fontSize.value.dp * 0.7f)
                            .height(4.dp)
                            .background(Color.Black, RoundedCornerShape(2.dp))
                    )
                }

                // Bouncing hand below current letter
                if (index == currentIndex) {
                    Spacer(Modifier.height(4.dp))
                    BouncingHandPointer()
                } else {
                    Spacer(Modifier.height(44.dp))
                }
            }
        }
    }
}

/**
 * Display for Fill in the Blank mode showing the word with a masked letter.
 * When the correct letter is written, it's revealed in violet/purple color.
 * A bouncing hand pointer indicates the blank letter to fill in.
 */
@Composable
private fun FillInTheBlankDisplay(
    word: String,
    maskedIndex: Int,
    isCorrect: Boolean,
    hasImage: Boolean,
    wrongLetterText: String = "",
    wrongLetterAnimationActive: Boolean = false,
    wrongLetterShakeOffset: Float = 0f,
    wrongLetterAlpha: Float = 1f
) {
    val fontSize = if (hasImage) 48.sp else 96.sp
    val letterSpacing = if (hasImage) 12.dp else 16.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(letterSpacing),
        verticalAlignment = Alignment.Top
    ) {
        word.forEachIndexed { index, letter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isMaskedAndWrong = index == maskedIndex && !isCorrect && wrongLetterAnimationActive
                Text(
                    text = when {
                        isMaskedAndWrong -> wrongLetterText
                        index == maskedIndex && !isCorrect -> " "
                        else -> letter.toString()
                    },
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isMaskedAndWrong -> WrongLetterColor
                        index == maskedIndex && isCorrect -> CompletedLetterColor
                        else -> Color.Black
                    },
                    modifier = if (isMaskedAndWrong) {
                        Modifier
                            .offset(x = wrongLetterShakeOffset.dp)
                            .alpha(wrongLetterAlpha)
                    } else {
                        Modifier
                    }
                )

                // Add underline for the masked letter (current letter being input)
                if (index == maskedIndex && !isCorrect) {
                    Box(
                        modifier = Modifier
                            .width(fontSize.value.dp * 0.7f)
                            .height(4.dp)
                            .background(Color.Black, RoundedCornerShape(2.dp))
                    )
                }

                // Bouncing hand below the masked letter
                if (index == maskedIndex && !isCorrect) {
                    Spacer(Modifier.height(4.dp))
                    BouncingHandPointer()
                } else {
                    Spacer(Modifier.height(44.dp))
                }
            }
        }
    }
}

/**
 * Display for Name the Picture mode showing blanks initially.
 * Completed letters are revealed in purple, pending letters show as underlines.
 * A bouncing hand pointer indicates the current letter to write.
 */
@Composable
private fun NameThePictureDisplay(
    word: String,
    completedIndices: Set<Int>,
    currentIndex: Int,
    hasImage: Boolean,
    wrongLetterText: String = "",
    wrongLetterAnimationActive: Boolean = false,
    wrongLetterShakeOffset: Float = 0f,
    wrongLetterAlpha: Float = 1f
) {
    val fontSize = if (hasImage) 48.sp else 96.sp
    val letterSpacing = if (hasImage) 12.dp else 16.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(letterSpacing),
        verticalAlignment = Alignment.Top
    ) {
        word.forEachIndexed { index, letter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isCurrentAndWrong = index == currentIndex && index !in completedIndices && wrongLetterAnimationActive
                Text(
                    text = when {
                        isCurrentAndWrong -> wrongLetterText
                        index in completedIndices -> letter.toString()
                        else -> " "
                    },
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentAndWrong -> WrongLetterColor
                        index in completedIndices -> CompletedLetterColor
                        else -> Color.Transparent
                    },
                    modifier = if (isCurrentAndWrong) {
                        Modifier
                            .offset(x = wrongLetterShakeOffset.dp)
                            .alpha(wrongLetterAlpha)
                    } else {
                        Modifier
                    }
                )

                // Add underline for letters not yet completed
                if (index !in completedIndices) {
                    Box(
                        modifier = Modifier
                            .width(fontSize.value.dp * 0.7f)
                            .height(4.dp)
                            .background(Color.Black, RoundedCornerShape(2.dp))
                    )
                }

                // Bouncing hand below current letter
                if (index == currentIndex && index !in completedIndices) {
                    Spacer(Modifier.height(4.dp))
                    BouncingHandPointer()
                } else {
                    Spacer(Modifier.height(44.dp))
                }
            }
        }
    }
}


private fun getInstructionText(configurationType: String, word: String?): String {
    return when (configurationType) {
        "Write the Word" -> {
            val phrases = listOf(
                "Can you write the word %s?",
                "Let's write the word %s!",
                "Try writing the word %s.",
                "Can you spell the word %s?",
                "Trace and write the word %s."
            )
            word?.let { phrases.random().format(it) } ?: phrases.random()
        }
        "Fill in the Blank" -> {
            val phrases = listOf(
                "Fill in the missing letter to make the word %s!",
                "What letter is missing in the word %s?",
                "Can you complete the word %s?",
                "Trace the right letter to finish the word %s."
            )
            word?.let { phrases.random().format(it) } ?: phrases.random()
        }
        "Name the Picture" -> {
            listOf(
                "What is this picture? Trace and write its name!",
                "Look at the picture. What is it? Trace the word!",
                "What do you see? Trace and write the name!",
                "Can you name this picture? Let's trace it!",
                "What is shown in the picture? Trace its name!",
                "Name the picture and trace the word.",
                "Do you know what this is? Trace and write it!"
            ).random()
        }
        else -> word?.let { "Can you write the word $it?" } ?: "What should you write?"
    }
}

@Preview(showBackground = true)
@Composable
fun LearnModeSessionScreenPreview() {
    LearnModeSessionScreen(
        setId = 0L,
        activityTitle = "Vowels",
        sessionKey = 0
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressCheckDialog(
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    showAnnotationTooltip: Boolean,
    onAnnotateClick: () -> Unit,
    onAnnotationTooltipDismissed: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val correctAffirmatives = listOf(
        R.raw.correct_great_effort_on_that_task,
        R.raw.correct_you_completed_that_perfectly,
        R.raw.correct_your_focus_is_impressive,
        R.raw.correct_youre_making_a_fantastic_progress,
        R.raw.correct_that_was_an_excellent_attempt,
        R.raw.correct_you_handled_that_very_well,
        R.raw.correct_your_hard_work_is_paying_off,
        R.raw.correct_keep_up_the_good_work,
        R.raw.correct_you_are_doing_really_well,
        R.raw.correct_you_did_a_great_job
    )

    // Play correct audio when dialog appears
    DisposableEffect(Unit) {
        val mediaPlayer = MediaPlayer()

        fun playAudio(resId: Int, onComplete: (() -> Unit)? = null) {
            try {
                mediaPlayer.reset()
                val afd = context.resources.openRawResourceFd(resId)
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer.prepare()
                mediaPlayer.setOnCompletionListener {
                    onComplete?.invoke()
                }
                mediaPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke()
            }
        }

        val randomAffirmative = correctAffirmatives.random()
        playAudio(R.raw.correct) {
            playAudio(randomAffirmative)
        }

        onDispose {
            mediaPlayer.release()
        }
    }

    // Extract first name only for more friendly tone
    val firstName = studentName.split(" ").firstOrNull()?.takeIf { it.isNotEmpty() } ?: ""

    // Define similar shape letters (same as watch side)
    val similarShapeLetters = setOf('C', 'K', 'O', 'P', 'S', 'V', 'W', 'X', 'Z')

    // Check if there's a case mismatch for similar letters
    val targetUppercase = targetLetter.uppercase()
    val isSimilarShape = targetUppercase.firstOrNull() in similarShapeLetters
    val expectedCase = when (targetCase.lowercase()) {
        "small", "lowercase" -> targetLetter.lowercase()
        else -> targetLetter.uppercase()
    }
    val hasCaseMismatch = isSimilarShape &&
                         predictedLetter.isNotEmpty() &&
                         !predictedLetter.equals(expectedCase, ignoreCase = false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Full-screen dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Annotation icon - top left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 48.dp)
            ) {
                if (showAnnotationTooltip) {
                    val tooltipState = rememberTooltipState(isPersistent = false)

                    LaunchedEffect(Unit) {
                        tooltipState.show()
                    }

                    TooltipBox(
                        positionProvider = object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: androidx.compose.ui.unit.IntRect,
                                windowSize: androidx.compose.ui.unit.IntSize,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                popupContentSize: androidx.compose.ui.unit.IntSize
                            ): androidx.compose.ui.unit.IntOffset {
                                val x = anchorBounds.left + (anchorBounds.width / 2) - 15
                                val y = anchorBounds.bottom + 16
                                return androidx.compose.ui.unit.IntOffset(x, y)
                            }
                        },
                        tooltip = {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE7DDFE)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Click here to Add Note",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        state = tooltipState
                    ) {
                        IconButton(onClick = {
                            onAnnotateClick()
                            onAnnotationTooltipDismissed()
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_annotate),
                                contentDescription = "Annotate",
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { onAnnotateClick() }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_annotate),
                            contentDescription = "Annotate",
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mascot Image
                Image(
                    painter = painterResource(id = R.drawable.dis_mobile_correct),
                    contentDescription = "Correct",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Great Job${if (firstName.isNotEmpty()) ", $firstName" else ""}!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCCDB00),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "You're doing super!\nKeep up the amazing work!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                // Case mismatch disclaimer for similar-shaped letters
                if (hasCaseMismatch) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Psst... you wrote ${if (predictedLetter.first().isUpperCase()) "uppercase" else "lowercase"} ${predictedLetter.uppercase()}, " +
                              "but we're practicing ${if (targetCase.lowercase() in listOf("small", "lowercase")) "lowercase" else "uppercase"} letters! " +
                              "They look similar, so that's still great! 😊",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            // Continue button at the bottom
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3FA9F8)
                ),
                shape = RoundedCornerShape(16.dp)
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

