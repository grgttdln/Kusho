package com.example.app.ui.feature.learn.learnmode

import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
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

    // State for watch handshake - blocks session until watch is ready
    var isWatchReady by remember(sessionKey) { mutableStateOf(false) }

    val context = LocalContext.current

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Get database instance for annotation persistence
    val database = remember { AppDatabase.getInstance(context) }
    val annotationDao = remember { database.learnerProfileAnnotationDao() }
    val studentSetProgressDao = remember { database.studentSetProgressDao() }
    val annotationSummaryDao = remember { database.annotationSummaryDao() }
    val geminiRepository = remember { com.example.app.data.repository.GeminiRepository() }

    // Get WatchConnectionManager instance
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }

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
    
    // Auto-dismiss annotation tooltip after 5 seconds
    LaunchedEffect(showAnnotationTooltip) {
        if (showAnnotationTooltip) {
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
    LaunchedEffect(currentWordIndex, words, isWatchReady) {
        if (!isWatchReady) return@LaunchedEffect
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

            // Speak random phrase based on question type
            launch {
                try {
                    if (useDeepgram) {
                        Log.d("LearnModeSession", "Using Deepgram TTS")
                        deepgramTtsManager.speakRandomPhrase(
                            currentWord.configurationType,
                            if (currentWord.configurationType == "Name the Picture") null else currentWord.word
                        )
                    } else {
                        Log.d("LearnModeSession", "Using Native TTS")
                        nativeTtsManager.speakRandomPhrase(
                            currentWord.configurationType,
                            if (currentWord.configurationType == "Name the Picture") null else currentWord.word
                        )
                    }
                } catch (e: Exception) {
                    Log.e("LearnModeSession", "Error playing TTS", e)
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

                                    if (currentWordIndex < words.size - 1) {
                                        currentWordIndex++
                                    } else {
                                        // All items complete - save annotations, notify watch and navigate away
                                        watchConnectionManager.notifyActivityComplete()
                                        shouldSaveAndComplete = true
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
                                android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Write the Word/Name the Picture (correct)")
                                showProgressCheckDialog = true
                            } else {
                                android.util.Log.d("LearnModeSession", "🎭 Starting wrong letter animation for Write the Word/Name the Picture")
                                wrongLetterText = event.letter.toString()
                                wrongLetterAnimationActive = true
                                wrongLetterAnimationTrigger++
                            }

                            // Store pending action to execute on dialog dismiss
                            if (isCorrect) {
                                val newLetterIndex = currentLetterIndex + 1
                                pendingCorrectAction = {
                                    // Mark letter as completed
                                    completedLetterIndices = completedLetterIndices + currentLetterIndex
                                    currentLetterIndex = newLetterIndex

                                    if (newLetterIndex >= currentWord.word.length) {
                                        // Flag this word as correctly answered
                                        correctlyAnsweredWords = correctlyAnsweredWords + currentWordIndex

                                        // Word complete - notify watch and move to next word
                                        watchConnectionManager.sendWordComplete()

                                        if (currentWordIndex < words.size - 1) {
                                            currentWordIndex++
                                        } else {
                                            // All items complete - save annotations, notify watch and navigate away
                                            watchConnectionManager.notifyActivityComplete()
                                            shouldSaveAndComplete = true
                                        }
                                    } else {
                                        // Send correct result and move to next letter
                                        watchConnectionManager.sendLetterResult(true, newLetterIndex, currentWord.word.length)
                                    }
                                }
                            } else {
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

    // Listen for feedback dismissal from watch
    LaunchedEffect(Unit) {
        var lastDismissTime = 0L
        watchConnectionManager.learnModeFeedbackDismissed.collect { timestamp ->
            if (timestamp > lastDismissTime && timestamp > 0L) {
                lastDismissTime = timestamp
                if (showProgressCheckDialog) {
                    android.util.Log.d("LearnModeSession", "👆 Watch dismissed feedback - dismissing mobile dialog")
                    showProgressCheckDialog = false
                    // Execute pending action
                    pendingCorrectAction?.invoke()
                    pendingCorrectAction = null
                } else if (wrongLetterAnimationActive) {
                    android.util.Log.d("LearnModeSession", "👆 Watch dismissed feedback - cancelling wrong letter animation")
                    wrongLetterAnimationActive = false
                    wrongLetterText = ""
                    pendingCorrectAction?.invoke()
                    pendingCorrectAction = null
                }
            }
        }
    }

    // Listen for skip commands from watch with debouncing
    LaunchedEffect(sessionKey) {
        val sessionStartTime = System.currentTimeMillis()
        var lastSkipTime = 0L
        watchConnectionManager.learnModeSkipTrigger.collect { skipTime ->
            // Only process skip events that happened AFTER this session started (prevents stale events)
            // and at least 500ms since last skip (debouncing)
            val timeSinceLastSkip = skipTime - lastSkipTime
            if (skipTime > sessionStartTime && skipTime > lastSkipTime && timeSinceLastSkip >= 500) {
                lastSkipTime = skipTime
                // Cancel any ongoing wrong letter animation
                if (wrongLetterAnimationActive) {
                    wrongLetterAnimationActive = false
                    wrongLetterText = ""
                    pendingCorrectAction = null
                }
                onSkip()
                if (currentWordIndex < words.size - 1) {
                    currentWordIndex++
                } else {
                    // All items complete - save annotations, notify watch and navigate away
                    watchConnectionManager.notifyActivityComplete()
                    shouldSaveAndComplete = true
                }
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

    // Progress Check Dialog - show before loading check to ensure it always renders
    if (showProgressCheckDialog) {
        ProgressCheckDialog(
            studentName = studentName,
            targetLetter = targetLetter,
            targetCase = targetCase,
            predictedLetter = predictedLetter,
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
        // Cancel any ongoing wrong letter animation
        if (wrongLetterAnimationActive) {
            wrongLetterAnimationActive = false
            wrongLetterText = ""
            pendingCorrectAction = null
        }
        if (currentWordIndex < words.size - 1) {
            currentWordIndex++
        } else {
            // Session complete - save annotations, notify watch and navigate away
            watchConnectionManager.notifyActivityComplete()
            shouldSaveAndComplete = true
        }
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
            modifier = Modifier.fillMaxWidth(),
            completedIndices = correctlyAnsweredWords,
            activeColor = PurpleColor,
            inactiveColor = LightPurpleColor,
            completedColor = Color(0xFF4CAF50),
            onSegmentClick = { index ->
                // Cancel any ongoing wrong letter animation
                if (wrongLetterAnimationActive) {
                    wrongLetterAnimationActive = false
                    wrongLetterText = ""
                    pendingCorrectAction = null
                }
                // Navigate to the clicked item
                currentWordIndex = index
            }
        )

        Spacer(Modifier.height(12.dp))

        // Tooltip state for annotation button
        val tooltipState = rememberTooltipState(isPersistent = false)

        // Show tooltip on first load
        LaunchedEffect(showAnnotationTooltip) {
            if (showAnnotationTooltip) {
                tooltipState.show()
            }
        }

        // Annotate and Skip Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Annotate button (left) - opens learner profile annotation dialog
            if (showAnnotationTooltip) {
                TooltipBox(
                    positionProvider = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: androidx.compose.ui.unit.IntRect,
                            windowSize: androidx.compose.ui.unit.IntSize,
                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                            popupContentSize: androidx.compose.ui.unit.IntSize
                        ): androidx.compose.ui.unit.IntOffset {
                            // Position tooltip below the icon with a right offset
                            val x = anchorBounds.left + (anchorBounds.width / 2) - 15
                            val y = anchorBounds.bottom + 16 // spacing below
                            return androidx.compose.ui.unit.IntOffset(x, y)
                        }
                    },
                    tooltip = {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE7DDFE) // Light purple color
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
                        showAnnotationDialog = true
                        showAnnotationTooltip = false
                        onAudioClick()
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
                IconButton(onClick = {
                    showAnnotationDialog = true
                    onAudioClick()
                }) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_annotate),
                        contentDescription = "Annotate",
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Skip button (right)
            IconButton(onClick = {
                onSkip()
                handleSkipOrNext()
            }) {
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

        // Activity Type Subtitle (dynamic from database)
        Text(
            text = currentWord?.configurationType ?: "",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = PurpleColor
        )

        Spacer(Modifier.height(24.dp))

        // Centered content area (slightly raised)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.weight(0.3f))
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

            Spacer(Modifier.weight(0.7f))
        }

        Spacer(Modifier.height(24.dp))

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
            }
        )
    }

    } // End of outer Box
}

/**
 * Display for Write the Word mode showing each letter with color based on completion status.
 * Current letter has an underline. Completed letters turn purple (#AE8EFB), pending letters are gray.
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
        verticalAlignment = Alignment.CenterVertically
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
            }
        }
    }
}

/**
 * Display for Fill in the Blank mode showing the word with a masked letter.
 * When the correct letter is written, it's revealed in violet/purple color.
 * Uses the same underline style as Write the Word for consistency.
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
        verticalAlignment = Alignment.CenterVertically
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
            }
        }
    }
}

/**
 * Display for Name the Picture mode showing blanks initially.
 * Completed letters are revealed in purple, pending letters show as underlines.
 * Current letter has an underline (same style as Write the Word).
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
        verticalAlignment = Alignment.CenterVertically
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
            }
        }
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


@Composable
private fun ProgressCheckDialog(
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
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
        }
    }
}

