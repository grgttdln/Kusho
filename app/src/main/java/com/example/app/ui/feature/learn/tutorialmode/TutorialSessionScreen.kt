package com.example.app.ui.feature.learn.tutorialmode

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PanToolAlt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ColorFilter
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
import com.example.app.R
import com.example.app.data.AppDatabase
import com.example.app.service.WatchConnectionManager
import com.example.app.ui.components.SessionPausedOverlay
import com.example.app.ui.components.WatchDisconnectedDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.app.service.SessionDisconnectReason
import com.example.app.ui.components.common.ProgressIndicator
import com.example.app.ui.components.common.EndSessionDialog
import com.example.app.ui.components.common.ResumeSessionDialog
import com.example.app.ui.components.learnmode.AnnotationData
import com.example.app.ui.components.learnmode.LearnerProfileAnnotationDialog
import com.example.app.ui.components.tutorial.AnimatedLetterView
import com.example.app.data.entity.getCompletedIndices
import com.example.app.data.entity.serializeCompletedIndices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val YellowColor = Color(0xFFEDBB00)
private val LightYellowColor = Color(0xFFFFF3C4)
private val BlueButtonColor = Color(0xFF3FA9F8)
private val YellowIconColor = Color(0xFFFFC700) // #FFC700 for tutorial mode icons
private val OrangeButtonColor = Color(0xFFFF8C42) // Orange for tutorial mode button
private val GreenCompletedColor = Color(0xFF4CAF50) // Green for completed letters
private val PendingYellowBorder = Color(0xFFFFC107) // Yellow border for pending tiles
private val LightYellowTooltipColor = Color(0xFFFFF9E6) // Light yellow for tooltip
private val BlueAnnotationColor = Color(0xFF42A5F5) // Blue for annotation dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialSessionScreen(
    title: String,
    letterType: String = "capital",
    studentName: String = "",
    studentId: Long = 0L,
    dominantHand: String = "RIGHT",
    onEndSession: () -> Unit,
    onEarlyExit: () -> Unit = onEndSession,
    modifier: Modifier = Modifier,
    initialStep: Int = 1,
    totalSteps: Int = 0, // Will be calculated from letters
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    val isConnectionLost by watchConnectionManager.sessionConnectionLost.collectAsState()
    val disconnectReason by watchConnectionManager.sessionDisconnectReason.collectAsState()
    val isSessionPaused by watchConnectionManager.sessionPaused.collectAsState()
    val isWatchOnModeScreen by watchConnectionManager.watchOnModeScreen.collectAsState()

    // Bluetooth enable launcher - same pattern as DashboardScreen
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* BroadcastReceiver in WatchConnectionManager handles STATE_ON transition */ }

    // Pre-recorded voice prompt MediaPlayer for letter announcements
    val voiceMediaPlayer = remember { MediaPlayer() }

    // Map of letter (uppercase) -> resource ID for capital voice files
    val capitalVoiceMap = remember {
        mapOf(
            "A" to R.raw.capital_a, "B" to R.raw.capital_b, "C" to R.raw.capital_c,
            "D" to R.raw.capital_d, "E" to R.raw.capital_e, "F" to R.raw.capital_f,
            "G" to R.raw.capital_g, "H" to R.raw.capital_h, "I" to R.raw.capital_i,
            "J" to R.raw.capital_j, "K" to R.raw.capital_k, "L" to R.raw.capital_l,
            "M" to R.raw.capital_m, "N" to R.raw.capital_n, "O" to R.raw.capital_o,
            "P" to R.raw.capital_p, "Q" to R.raw.capital_q, "R" to R.raw.capital_r,
            "S" to R.raw.capital_s, "T" to R.raw.capital_t, "U" to R.raw.capital_u,
            "V" to R.raw.capital_v, "W" to R.raw.capital_w, "X" to R.raw.capital_x,
            "Y" to R.raw.capital_y, "Z" to R.raw.capital_z
        )
    }

    // Map of letter (uppercase) -> resource ID for small voice files
    val smallVoiceMap = remember {
        mapOf(
            "A" to R.raw.small_a, "B" to R.raw.small_b, "C" to R.raw.small_c,
            "D" to R.raw.small_d, "E" to R.raw.small_e, "F" to R.raw.small_f,
            "G" to R.raw.small_g, "H" to R.raw.small_h, "I" to R.raw.small_i,
            "J" to R.raw.small_j, "K" to R.raw.small_k, "L" to R.raw.small_l,
            "M" to R.raw.small_m, "N" to R.raw.small_n, "O" to R.raw.small_o,
            "P" to R.raw.small_p, "Q" to R.raw.small_q, "R" to R.raw.small_r,
            "S" to R.raw.small_s, "T" to R.raw.small_t, "U" to R.raw.small_u,
            "V" to R.raw.small_v, "W" to R.raw.small_w, "X" to R.raw.small_x,
            "Y" to R.raw.small_y, "Z" to R.raw.small_z
        )
    }

    // Cleanup voice MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceMediaPlayer.release()
        }
    }

    var currentStep by remember { mutableIntStateOf(initialStep) }
    var showProgressCheck by remember { mutableStateOf(false) }
    var isCorrectGesture by remember { mutableStateOf(false) }
    var predictedLetter by remember { mutableStateOf("") }
    var showAnimation by remember { mutableStateOf(false) } // Toggle for animation vs static image
    
    // Phase 1: Watch ready state and end session confirmation
    var isWatchReady by remember { mutableStateOf(false) }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var resumeChecked by remember { mutableStateOf(false) }
    
    // Phase 2: Track completed letter indices (for green progress bar segments)
    var completedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // Track which items the student has attempted air-writing (for revisit auto-advance)
    var attemptedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // Phase 2: Card stack grid navigation overlay
    var showCardStackGrid by remember { mutableStateOf(false) }
    // Voice playback highlight state
    var isVoicePlaying by remember { mutableStateOf(false) }
    // Restart confirmation (second layer after ResumeSessionDialog)
    var showRestartConfirmation by remember { mutableStateOf(false) }
    
    // Phase 3: "Student is writing" indicator + UI lock
    var isStudentWriting by remember { mutableStateOf(false) }
    
    // Annotation dialog state
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var annotationsMap by remember { mutableStateOf<Map<Int, AnnotationData>>(emptyMap()) }
    
    // Tooltip state for grid/card-stack icon
    var showAnnotationTooltip by remember { mutableStateOf(true) }
    val tooltipState = rememberTooltipState(isPersistent = false)
    // Tooltip state for annotation icon in progress check overlay
    var showOverlayAnnotationTooltip by remember { mutableStateOf(true) }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    
    // Get database instance for annotation persistence
    val database = remember { AppDatabase.getInstance(context) }
    val annotationDao = remember { database.learnerProfileAnnotationDao() }
    val annotationSummaryDao = remember { database.annotationSummaryDao() }
    val tutorialCompletionDao = remember { database.tutorialCompletionDao() }
    val kuuRecommendationDao = remember { database.kuuRecommendationDao() }
    val geminiRepository = remember { com.example.app.data.repository.GeminiRepository() }

    // Pre-compute total letter count (needed by lambdas defined before the full letter lists)
    val letterCount = remember(title, letterType) {
        when (title.lowercase()) {
            "vowels" -> 5
            "consonants" -> 21
            else -> 0
        }
    }

    // Generate a unique setId based on tutorial session (title + letterType)
    // This ensures annotations are saved per tutorial section
    // Using negative IDs to avoid collision with actual database setIds
    val tutorialSetId = remember(title, letterType) {
        when {
            title.equals("Vowels", ignoreCase = true) && letterType.equals("capital", ignoreCase = true) -> -1L
            title.equals("Vowels", ignoreCase = true) && letterType.equals("small", ignoreCase = true) -> -2L
            title.equals("Consonants", ignoreCase = true) && letterType.equals("capital", ignoreCase = true) -> -3L
            title.equals("Consonants", ignoreCase = true) && letterType.equals("small", ignoreCase = true) -> -4L
            else -> -(kotlin.math.abs("$title-$letterType".hashCode().toLong()) % 1000 + 5)
        }
    }

    // Save completion record and end session
    val completeTutorialAndEnd: () -> Unit = {
        // Notify watch immediately so it shows the completion screen (dis_watch_complete)
        // while the phone transitions to the analytics screen — matches Learn Mode behavior
        watchConnectionManager.notifyTutorialModeSessionComplete()
        coroutineScope.launch {
            if (studentId > 0L) {
                withContext(Dispatchers.IO) {
                    // Delete any in-progress row first, then insert a completed one
                    tutorialCompletionDao.deleteInProgress(studentId, tutorialSetId)
                    val allIndices = (0 until letterCount).toSet()
                    tutorialCompletionDao.insertIfNotExists(
                        com.example.app.data.entity.TutorialCompletion(
                            studentId = studentId,
                            tutorialSetId = tutorialSetId,
                            lastCompletedStep = letterCount,
                            totalSteps = letterCount,
                            completedAt = System.currentTimeMillis(),
                            completedIndicesJson = serializeCompletedIndices(allIndices)
                        )
                    )
                    kuuRecommendationDao.incrementCompletions(studentId)
                }
            }
            onEndSession()
        }
    }

    // Save partial progress and exit early (non-linear safe: saves exact completed indices)
    val saveProgressAndExit: () -> Unit = {
        coroutineScope.launch {
            if (studentId > 0L && completedIndices.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    // Upsert in-progress record (completedAt = null)
                    val existingProgress = tutorialCompletionDao.getInProgressSession(studentId, tutorialSetId)
                    tutorialCompletionDao.upsertProgress(
                        com.example.app.data.entity.TutorialCompletion(
                            id = existingProgress?.id ?: 0,
                            studentId = studentId,
                            tutorialSetId = tutorialSetId,
                            lastCompletedStep = completedIndices.size, // Actual count of completed letters
                            totalSteps = letterCount,
                            completedAt = null, // null = in-progress
                            completedIndicesJson = serializeCompletedIndices(completedIndices)
                        )
                    )
                }
            }
            watchConnectionManager.notifyTutorialModeEnded()
            onEarlyExit()
        }
    }

    // Load existing annotations for this student and tutorial session when the screen loads
    LaunchedEffect(studentId, tutorialSetId) {
        if (studentId > 0L) {
            withContext(Dispatchers.IO) {
                val studentIdString = studentId.toString()
                val existingAnnotations = annotationDao.getAnnotationsForStudentInSet(
                    studentIdString, 
                    tutorialSetId,
                    com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL
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

    // Check for existing in-progress session and show resume dialog
    // Stores parsed completed indices for use in the resume dialog
    var resumedCompletedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(studentId, tutorialSetId) {
        if (studentId > 0L && !resumeChecked) {
            val inProgress = withContext(Dispatchers.IO) {
                tutorialCompletionDao.getInProgressSession(studentId, tutorialSetId)
            }
            if (inProgress != null && inProgress.lastCompletedStep > 0) {
                // Parse the specific completed indices from JSON
                resumedCompletedIndices = inProgress.getCompletedIndices()
                // Find the first uncompleted letter to position the cursor
                val maxSteps = letterCount
                val firstIncomplete = (0 until maxSteps).firstOrNull { it !in resumedCompletedIndices } ?: 0
                currentStep = firstIncomplete + 1
                showResumeDialog = true
            }
            resumeChecked = true
        }
    }
    
    // Define all letters based on the section
    val allLetters = remember(title) {
        when (title.lowercase()) {
            "vowels" -> listOf(
                R.drawable.ic_letter_a_upper, R.drawable.ic_letter_a_lower,
                R.drawable.ic_letter_e_upper, R.drawable.ic_letter_e_lower,
                R.drawable.ic_letter_i_upper, R.drawable.ic_letter_i_lower,
                R.drawable.ic_letter_o_upper, R.drawable.ic_letter_o_lower,
                R.drawable.ic_letter_u_upper, R.drawable.ic_letter_u_lower
            )
            "consonants" -> listOf(
                R.drawable.ic_letter_b_upper, R.drawable.ic_letter_b_lower,
                R.drawable.ic_letter_c_upper, R.drawable.ic_letter_c_lower,
                R.drawable.ic_letter_d_upper, R.drawable.ic_letter_d_lower,
                R.drawable.ic_letter_f_upper, R.drawable.ic_letter_f_lower,
                R.drawable.ic_letter_g_upper, R.drawable.ic_letter_g_lower,
                R.drawable.ic_letter_h_upper, R.drawable.ic_letter_h_lower,
                R.drawable.ic_letter_j_upper, R.drawable.ic_letter_j_lower,
                R.drawable.ic_letter_k_upper, R.drawable.ic_letter_k_lower,
                R.drawable.ic_letter_l_upper, R.drawable.ic_letter_l_lower,
                R.drawable.ic_letter_m_upper, R.drawable.ic_letter_m_lower,
                R.drawable.ic_letter_n_upper, R.drawable.ic_letter_n_lower,
                R.drawable.ic_letter_p_upper, R.drawable.ic_letter_p_lower,
                R.drawable.ic_letter_q_upper, R.drawable.ic_letter_q_lower,
                R.drawable.ic_letter_r_upper, R.drawable.ic_letter_r_lower,
                R.drawable.ic_letter_s_upper, R.drawable.ic_letter_s_lower,
                R.drawable.ic_letter_t_upper, R.drawable.ic_letter_t_lower,
                R.drawable.ic_letter_v_upper, R.drawable.ic_letter_v_lower,
                R.drawable.ic_letter_w_upper, R.drawable.ic_letter_w_lower,
                R.drawable.ic_letter_x_upper, R.drawable.ic_letter_x_lower,
                R.drawable.ic_letter_y_upper, R.drawable.ic_letter_y_lower,
                R.drawable.ic_letter_z_upper, R.drawable.ic_letter_z_lower
            )
            else -> emptyList()
        }
    }
    
    // Filter letters based on type (capital or small)
    val letters = remember(allLetters, letterType) {
        when (letterType.lowercase()) {
            "capital" -> allLetters.filterIndexed { index, _ -> index % 2 == 0 }
            "small" -> allLetters.filterIndexed { index, _ -> index % 2 == 1 }
            else -> allLetters
        }
    }
    
    // Get current letter based on step
    val currentLetterRes = remember(currentStep, letters) {
        if (letters.isNotEmpty() && currentStep > 0 && currentStep <= letters.size) {
            letters[currentStep - 1]
        } else {
            null
        }
    }
    
    // Calculate total steps from letters
    val calculatedTotalSteps = remember(letters) { letters.size }
    
    // Get letter names for watch sync
    val letterNames = remember(title, letterType) {
        when (title.lowercase()) {
            "vowels" -> listOf("A", "E", "I", "O", "U")
            "consonants" -> listOf("B", "C", "D", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "V", "W", "X", "Y", "Z")
            else -> emptyList()
        }
    }
    
    val currentLetter = remember(currentStep, letterNames) {
        if (letterNames.isNotEmpty() && currentStep > 0 && currentStep <= letterNames.size) {
            letterNames[currentStep - 1]
        } else ""
    }

    // Track active session state for watch re-entry sync
    DisposableEffect(Unit) {
        watchConnectionManager.setTutorialModeSessionActive(true)
        onDispose {
            watchConnectionManager.setTutorialModeSessionActive(false)
        }
    }

    // Start session health monitoring for disconnect detection
    DisposableEffect(Unit) {
        watchConnectionManager.startSessionMonitoring()
        onDispose {
            watchConnectionManager.stopSessionMonitoring()
        }
    }

    // Notify watch when Tutorial Mode session starts
    LaunchedEffect(Unit) {
        watchConnectionManager.notifyTutorialModeStarted(studentName, title)
    }

    // Two-way handshake: send phone_ready immediately and heartbeat every 2s until watch replies
    LaunchedEffect(Unit) {
        // Send initial phone_ready signal
        watchConnectionManager.sendTutorialModePhoneReady()
        // Heartbeat: keep pinging every 2 seconds until watch responds
        while (!isWatchReady) {
            kotlinx.coroutines.delay(2000)
            if (!isWatchReady) {
                watchConnectionManager.sendTutorialModePhoneReady()
            }
        }
    }

    // Listen for watch ready signal (watch replies to our phone_ready or sends its own on entry)
    LaunchedEffect(Unit) {
        watchConnectionManager.tutorialModeWatchReady.collect { timestamp ->
            if (timestamp > 0L) {
                isWatchReady = true
            }
        }
    }

    // Auto-show and auto-dismiss annotation tooltip after 5 seconds
    LaunchedEffect(showAnnotationTooltip) {
        if (showAnnotationTooltip) {
            tooltipState.show()
            kotlinx.coroutines.delay(5000)
            showAnnotationTooltip = false
        }
    }

    // Note: We don't call notifyTutorialModeEnded() here on dispose anymore
    // because we want the watch to keep showing the completion screen
    // while the phone shows the analytics screen.
    // notifyTutorialModeEnded() is now called from SessionAnalyticsScreen
    // when the user leaves (Practice Again or Continue).
    
    // Send current letter data to watch whenever it changes AND play pre-recorded voice prompt
    // Gated behind isWatchReady so we don't send data before the watch is listening
    LaunchedEffect(currentStep, currentLetter, isWatchReady, showResumeDialog) {
        if (currentLetter.isNotEmpty() && isWatchReady && !showResumeDialog) {
            watchConnectionManager.sendTutorialModeLetterData(
                letter = currentLetter,
                letterCase = letterType,
                currentIndex = currentStep,
                totalLetters = calculatedTotalSteps,
                dominantHand = dominantHand
            )
            watchConnectionManager.updateCurrentTutorialModeLetterData(
                letter = currentLetter,
                letterCase = letterType,
                currentIndex = currentStep,
                totalLetters = calculatedTotalSteps,
                dominantHand = dominantHand
            )

            // Play pre-recorded voice prompt for the current letter
            val voiceMap = if (letterType.lowercase() == "capital") capitalVoiceMap else smallVoiceMap
            val voiceResId = voiceMap[currentLetter.uppercase()]
            if (voiceResId != null) {
                try {
                    voiceMediaPlayer.reset()
                    val afd = context.resources.openRawResourceFd(voiceResId)
                    voiceMediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    voiceMediaPlayer.prepare()
                    isVoicePlaying = true
                    voiceMediaPlayer.setOnCompletionListener { isVoicePlaying = false }
                    voiceMediaPlayer.start()
                    Log.d("TutorialSession", "Playing pre-recorded voice for: $currentLetter ($letterType)")
                } catch (e: Exception) {
                    isVoicePlaying = false
                    Log.e("TutorialSession", "Error playing voice prompt", e)
                }
            }
        }
    }
    
    // Listen for skip commands from watch with debouncing
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastSkipTime = 0L
        watchConnectionManager.tutorialModeSkipTrigger.collect { skipTime ->
            val timeSinceLastSkip = skipTime - lastSkipTime
            if (skipTime > sessionStartTime && skipTime > lastSkipTime && timeSinceLastSkip >= 500) {
                lastSkipTime = skipTime
                val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                // Wrap around to first unanswered item instead of stopping at end
                val nextIncomplete = ((currentStep) until maxSteps).firstOrNull { it !in completedIndices }
                    ?: (0 until currentStep - 1).firstOrNull { it !in completedIndices }
                if (nextIncomplete != null) {
                    currentStep = nextIncomplete + 1
                }
                onSkip()
            }
        }
    }
    
    // Listen for gesture results from watch
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastGestureTime = 0L
        watchConnectionManager.tutorialModeGestureResult.collect { result ->
            val timestamp = result["timestamp"] as? Long ?: 0L
            if (timestamp > sessionStartTime && timestamp > lastGestureTime && result.isNotEmpty()) {
                lastGestureTime = timestamp
                val gestureCorrect = result["isCorrect"] as? Boolean ?: false
                val gesturePredicted = result["predictedLetter"] as? String ?: ""

                // Ignore spurious results with empty predictedLetter — these come from
                // error paths (e.g., cancelled recording during letter transition), not
                // real gesture attempts. Don't let them poison attemptedIndices.
                if (gesturePredicted.isEmpty()) {
                    Log.d("TutorialSession", "Ignoring gesture result with empty predictedLetter")
                    return@collect
                }

                isStudentWriting = false // Recording finished, student is no longer writing

                // Send feedback to watch (phone is single source of truth)
                watchConnectionManager.sendTutorialModeFeedback(gestureCorrect, gesturePredicted)

                val itemIndex = currentStep - 1
                attemptedIndices = attemptedIndices + itemIndex

                // Always show teacher-gated ProgressCheckDialog
                isCorrectGesture = gestureCorrect
                predictedLetter = gesturePredicted
                showProgressCheck = true
            }
        }
    }
    
    // Phase 3: Listen for gesture recording signal from watch (student started writing)
    LaunchedEffect(Unit) {
        val sessionStartTime = System.currentTimeMillis()
        var lastRecordingTime = 0L
        watchConnectionManager.tutorialModeGestureRecording.collect { timestamp ->
            if (timestamp > sessionStartTime && timestamp > lastRecordingTime && timestamp > 0L) {
                lastRecordingTime = timestamp
                isStudentWriting = true
                showAnimation = true // Auto-play the letter trace animation
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
                watchConnectionManager.notifyTutorialModeEnded()
                onEarlyExit()
            }
        )
    }

    // Progress Check Dialog (teacher-gated: only Continue button advances)
    if (showProgressCheck) {
        ProgressCheckDialog(
            isCorrect = isCorrectGesture,
            studentName = studentName,
            targetLetter = currentLetter,
            targetCase = letterType,
            predictedLetter = predictedLetter,
            showAnnotationTooltip = showOverlayAnnotationTooltip,
            onAnnotateClick = {
                showAnnotationDialog = true
            },
            onAnnotationTooltipDismissed = {
                showOverlayAnnotationTooltip = false
            },
            onContinue = {
                showProgressCheck = false
                // Reset Phase 3 air-writing state so next letter starts idle
                isStudentWriting = false
                showAnimation = false
                // Notify watch to dismiss feedback (phone-driven SSOT)
                watchConnectionManager.notifyTutorialModeFeedbackDismissed()
                if (isCorrectGesture) {
                    // Mark current step as completed (green in progress bar)
                    val newCompletedIndices = completedIndices + (currentStep - 1)
                    completedIndices = newCompletedIndices
                    val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                    // Check if ALL letters are now completed (non-linear safe)
                    if (newCompletedIndices.size >= maxSteps) {
                        completeTutorialAndEnd()
                    } else {
                        // Find the next incomplete letter, starting from the next sequential position
                        val nextIncomplete = ((currentStep) until maxSteps).firstOrNull { it !in newCompletedIndices }
                            ?: (0 until currentStep - 1).firstOrNull { it !in newCompletedIndices }
                        if (nextIncomplete != null) {
                            currentStep = nextIncomplete + 1
                        }
                        // sendTutorialModeLetterData is triggered by LaunchedEffect on currentStep change
                    }
                } else {
                    // On incorrect, notify watch to retry and resend letter data
                    isStudentWriting = false
                    showAnimation = false
                    watchConnectionManager.notifyTutorialModeRetry()
                    if (currentLetter.isNotEmpty()) {
                        watchConnectionManager.sendTutorialModeLetterData(
                            letter = currentLetter,
                            letterCase = letterType,
                            currentIndex = currentStep,
                            totalLetters = calculatedTotalSteps,
                            dominantHand = dominantHand
                        )
                        watchConnectionManager.updateCurrentTutorialModeLetterData(
                            letter = currentLetter,
                            letterCase = letterType,
                            currentIndex = currentStep,
                            totalLetters = calculatedTotalSteps,
                            dominantHand = dominantHand
                        )
                    }
                }
            }
        )
    }
    
    // Learner Profile Annotation Dialog
    if (showAnnotationDialog) {
        val existingAnnotation = annotationsMap[currentStep - 1] ?: AnnotationData.empty()
        
        LearnerProfileAnnotationDialog(
            studentName = studentName,
            existingData = existingAnnotation,
            onDismiss = { showAnnotationDialog = false },
            accentColor = BlueAnnotationColor,
            buttonColor = BlueAnnotationColor,
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
                annotationsMap = annotationsMap + ((currentStep - 1) to newAnnotationData)
                
                // Close dialog
                showAnnotationDialog = false
                
                // Save to database asynchronously
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        if (studentId > 0L) {
                            val studentIdString = studentId.toString()
                            val annotation = com.example.app.data.entity.LearnerProfileAnnotation.create(
                                studentId = studentIdString,
                                setId = tutorialSetId,
                                itemId = currentStep - 1,
                                sessionMode = com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL,
                                levelOfProgress = levelOfProgress,
                                strengthsObserved = strengthsObserved,
                                strengthsNote = strengthsNote,
                                challenges = challenges,
                                challengesNote = challengesNote
                            )
                            annotationDao.insertOrUpdate(annotation)

                            // Generate AI summary for this tutorial's annotations
                            try {
                                val allAnnotations = annotationDao.getAnnotationsForStudentInSet(
                                    studentIdString,
                                    tutorialSetId,
                                    com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL
                                )
                                if (allAnnotations.isNotEmpty()) {
                                    val letterNameMap = letterNames.mapIndexed { i, name -> i to name }.toMap()
                                    val summaryText = geminiRepository.generateAnnotationSummary(
                                        allAnnotations, com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL, letterNameMap
                                    )
                                    if (summaryText != null) {
                                        annotationSummaryDao.insertOrUpdate(
                                            com.example.app.data.entity.AnnotationSummary(
                                                studentId = studentIdString,
                                                setId = tutorialSetId,
                                                sessionMode = com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL,
                                                summaryText = summaryText
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TutorialSession", "AI summary generation failed: ${e.message}")
                            }
                        }
                    }
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phase 2.2: Interactive Progress Bar — green for completed, yellow for current, clickable to navigate
        // Grayed out and non-interactive when student is air writing
        ProgressIndicator(
            currentStep = currentStep,
            totalSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
            completedIndices = completedIndices,
            activeColor = YellowColor,
            inactiveColor = LightYellowColor,
            completedColor = GreenCompletedColor,
            onSegmentClick = { clickedIndex ->
                if (!isStudentWriting) {
                    // Navigate to the clicked letter (1-based step)
                    val newStep = clickedIndex + 1
                    val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                    if (newStep in 1..maxSteps) {
                        currentStep = newStep
                        // Watch sync is handled automatically by LaunchedEffect(currentStep, ...)
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        // Phase 2.3: Card Stack icon (left) and Skip icon (right) row
        // Grayed out when student is air writing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card Stack / Grid icon (left) - opens full-screen grid navigation
            if (showAnnotationTooltip) {
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
                                containerColor = LightYellowTooltipColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "View all letters",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    },
                    state = tooltipState
                ) {
                    IconButton(
                        onClick = {
                            if (!isStudentWriting) {
                                showCardStackGrid = true
                                showAnnotationTooltip = false
                            }
                        },
                        enabled = !isStudentWriting
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewModule,
                            contentDescription = "Card Stack",
                            modifier = Modifier.size(28.dp),
                            tint = YellowIconColor.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = {
                        if (!isStudentWriting) {
                            showCardStackGrid = true
                        }
                    },
                    enabled = !isStudentWriting
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewModule,
                        contentDescription = "Card Stack",
                        modifier = Modifier.size(28.dp),
                        tint = YellowIconColor.copy(alpha = 0.5f)
                    )
                }
            }

            // Skip button (right) - icon instead of text
            IconButton(
                onClick = {
                    if (!isStudentWriting) {
                        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                        // Wrap around to first unanswered item instead of stopping at end
                        val nextIncomplete = ((currentStep) until maxSteps).firstOrNull { it !in completedIndices }
                            ?: (0 until currentStep - 1).firstOrNull { it !in completedIndices }
                        if (nextIncomplete != null) {
                            currentStep = nextIncomplete + 1
                        }
                        onSkip()
                    }
                },
                enabled = !isStudentWriting
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_skip),
                    contentDescription = "Skip",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(YellowIconColor.copy(alpha = 0.5f))
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

        // Phase 2.1: Dynamic Voice Subtitle — per-letter prompt with voice highlight
        Spacer(Modifier.height(4.dp))

        // Pulsing alpha animation while voice is playing
        val subtitleAlpha = if (isVoicePlaying) {
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
            targetValue = if (isVoicePlaying) YellowColor else Color.Gray,
            animationSpec = tween(300),
            label = "subtitleColor"
        ).value

        Text(
            text = getVoicePromptText(currentLetter, letterType),
            fontSize = 16.sp,
            fontWeight = if (isVoicePlaying) FontWeight.SemiBold else FontWeight.Medium,
            color = subtitleColor.copy(alpha = subtitleAlpha),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Large Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(3.dp, YellowColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Phase 2.4: Replay (left) and Hand/Animation toggle (right) icons in top-right row
                // Grayed out when student is air writing
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .then(if (isStudentWriting) Modifier.alpha(0.35f) else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Replay voice prompt icon
                    IconButton(
                        enabled = !isStudentWriting,
                        onClick = {
                            // Replay the pre-recorded voice prompt for the current letter
                            val voiceMap = if (letterType.lowercase() == "capital") capitalVoiceMap else smallVoiceMap
                            val voiceResId = voiceMap[currentLetter.uppercase()]
                            if (voiceResId != null) {
                                try {
                                    voiceMediaPlayer.reset()
                                    val afd = context.resources.openRawResourceFd(voiceResId)
                                    voiceMediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                    afd.close()
                                    voiceMediaPlayer.prepare()
                                    isVoicePlaying = true
                                    voiceMediaPlayer.setOnCompletionListener { isVoicePlaying = false }
                                    voiceMediaPlayer.start()
                                } catch (e: Exception) {
                                    isVoicePlaying = false
                                    Log.e("TutorialSession", "Error replaying voice prompt", e)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay voice prompt",
                            modifier = Modifier.size(24.dp),
                            tint = YellowIconColor
                        )
                    }
                    // Hand (pan_tool_alt) icon to toggle animation
                    IconButton(
                        onClick = { if (!isStudentWriting) showAnimation = !showAnimation },
                        modifier = Modifier.size(40.dp),
                        enabled = !isStudentWriting
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanToolAlt,
                            contentDescription = if (showAnimation) "Show static letter" else "Show animation",
                            modifier = Modifier.size(24.dp),
                            tint = if (showAnimation) YellowIconColor else Color.LightGray
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showAnimation) {
                        // Display animated letter for current step
                        if (currentLetter.isNotEmpty()) {
                            AnimatedLetterView(
                                letter = currentLetter.first(),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(1f),
                                isUpperCase = letterType.lowercase() == "capital",
                                strokeColor = Color.Black,
                                numberColor = Color.White,
                                circleColor = Color.Black,
                                loopAnimation = true,
                                loopDelay = 2000
                            )
                        }
                    } else {
                        // Display static drawable image
                        currentLetterRes?.let { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "Letter $currentLetter",
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Phase 3: "Student is air writing..." indicator — replaces End Session button while active
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
                    color = YellowColor.copy(alpha = writingAlpha),
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
                    containerColor = BlueButtonColor
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

    // Phase 1: Waiting for watch overlay
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
                    painter = painterResource(id = R.drawable.dis_pairing_tutorial),
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
                    color = YellowColor,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Please open Tutorial Mode\non the watch to connect.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.weight(1f))

                // Cancel Session button (same position as End Session)
                Button(
                    onClick = {
                        watchConnectionManager.notifyTutorialModeEnded()
                        onEarlyExit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueButtonColor
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

    // Phase 1: End Session confirmation dialog
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

    // Resume dialog — shown when there's saved progress from a previous session
    if (showResumeDialog) {
        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
        ResumeSessionDialog(
            mascotDrawable = R.drawable.dis_pairing_tutorial,
            studentName = studentName,
            completedCount = resumedCompletedIndices.size,
            totalCount = maxSteps,
            unitLabel = "letters",
            onResume = {
                showResumeDialog = false
                // Reset Phase 3 air-writing state to idle on resume
                isStudentWriting = false
                showAnimation = false
                // Restore the exact completed indices (non-linear safe)
                completedIndices = resumedCompletedIndices
                // currentStep is already set to the first incomplete letter
            },
            onRestart = {
                // Show a second confirmation before wiping progress
                showRestartConfirmation = true
            }
        )
    }

    // Restart confirmation dialog (second layer)
    if (showRestartConfirmation) {
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
                            text = "This will erase all progress (${resumedCompletedIndices.size} completed letter${if (resumedCompletedIndices.size != 1) "s" else ""}). Are you sure you want to start over?",
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
                                    // Reset Phase 3 air-writing state to idle on restart
                                    isStudentWriting = false
                                    showAnimation = false
                                    currentStep = 1
                                    completedIndices = emptySet()
                                    attemptedIndices = emptySet()
                                    resumedCompletedIndices = emptySet()
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            tutorialCompletionDao.deleteInProgress(studentId, tutorialSetId)
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

    // Phase 2.3: Card Stack Grid Navigation overlay
    if (showCardStackGrid) {
        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
        val completedCount = completedIndices.size
        CardStackGridDialog(
            title = title,
            letterNames = letterNames,
            letterResources = letters,
            completedIndices = completedIndices,
            currentStep = currentStep,
            totalSteps = maxSteps,
            completedCount = completedCount,
            onLetterSelected = { selectedIndex ->
                currentStep = selectedIndex + 1
                showCardStackGrid = false
            },
            onDismiss = { showCardStackGrid = false }
        )
    }

    // Session Paused Overlay - non-blocking, drawn on top of content inside Box
    // Only show if not also showing the disconnect dialog (disconnect takes priority)
    if (isSessionPaused && !isConnectionLost) {
        SessionPausedOverlay(
            watchOnScreen = isWatchOnModeScreen,
            onContinue = {
                watchConnectionManager.resumeTutorialModeSession()
            },
            onEndSession = {
                watchConnectionManager.stopSessionMonitoring()
                watchConnectionManager.notifyTutorialModeEnded()
                onEarlyExit()
            }
        )
    }

    } // End of outer Box
}

/**
 * Phase 2.3: Full-screen grid dialog showing all letter tiles for the session.
 * Tiles are styled green (completed) or yellow (pending). Matches the reference UI:
 * back arrow + title + "Select a Letter to navigate" + completion count + 2-column grid of letter tiles.
 */
@Composable
private fun CardStackGridDialog(
    title: String,
    letterNames: List<String>,
    letterResources: List<Int>,
    completedIndices: Set<Int>,
    currentStep: Int,
    totalSteps: Int,
    completedCount: Int,
    onLetterSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back button + Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = YellowColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.weight(1f))
                    // Invisible spacer to balance the back button
                    Spacer(Modifier.size(48.dp))
                }

                Spacer(Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Select a Letter to navigate",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                // Completion count
                Text(
                    text = "$completedCount/$totalSteps Completed",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // 2-column grid of letter tiles
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(letterResources) { index, resId ->
                        val isCompleted = index in completedIndices
                        val borderColor = if (isCompleted) GreenCompletedColor else PendingYellowBorder

                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onLetterSelected(index) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            border = BorderStroke(3.dp, borderColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = if (index < letterNames.size) "Letter ${letterNames[index]}" else "Letter",
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns the specific voice prompt subtitle text for a given letter and case.
 * Each letter has a unique, varied phrase.
 */
private fun getVoicePromptText(letter: String, letterType: String): String {
    if (letter.isEmpty()) return ""
    val isCapital = letterType.lowercase() == "capital"
    val key = letter.uppercase()
    return if (isCapital) {
        when (key) {
            // Uppercase Vowels
            "A" -> "Let's write a big A!"
            "E" -> "Time for a capital E!"
            "I" -> "Get ready for a big I!"
            "O" -> "Let's draw a capital O!"
            "U" -> "Here comes a big U!"
            // Uppercase Consonants
            "B" -> "Let's write a capital B!"
            "C" -> "Time for a big C!"
            "D" -> "Get ready for a capital D!"
            "F" -> "Let's draw a big F!"
            "G" -> "Here comes a capital G!"
            "H" -> "Let's write a big H!"
            "J" -> "Time for a capital J!"
            "K" -> "Get ready for a big K!"
            "L" -> "Let's draw a capital L!"
            "M" -> "Here comes a big M!"
            "N" -> "Let's write a capital N!"
            "P" -> "Time for a big P!"
            "Q" -> "Get ready for a capital Q!"
            "R" -> "Let's draw a big R!"
            "S" -> "Here comes a capital S!"
            "T" -> "Let's write a big T!"
            "V" -> "Time for a capital V!"
            "W" -> "Get ready for a big W!"
            "X" -> "Let's draw a big X!"
            "Y" -> "Here comes a big Y!"
            "Z" -> "Let's write a capital Z!"
            else -> "Let's write a big $key!"
        }
    } else {
        val lower = key.lowercase()
        when (key) {
            // Lowercase Vowels
            "A" -> "Let's write a small a!"
            "E" -> "Time for a lowercase e!"
            "I" -> "Get ready for a small i!"
            "O" -> "Let's draw a lowercase o!"
            "U" -> "Here comes a small u!"
            // Lowercase Consonants
            "B" -> "Let's write a lowercase b!"
            "C" -> "Time for a small c!"
            "D" -> "Get ready for a lowercase d!"
            "F" -> "Let's draw a small f!"
            "G" -> "Here comes a lowercase g!"
            "H" -> "Let's write a small h!"
            "J" -> "Time for a lowercase j!"
            "K" -> "Get ready for a small k!"
            "L" -> "Let's draw a lowercase l!"
            "M" -> "Here comes a small m!"
            "N" -> "Let's write a lowercase n!"
            "P" -> "Time for a small p!"
            "Q" -> "Get ready for a lowercase q!"
            "R" -> "Let's draw a small r!"
            "S" -> "Here comes a lowercase s!"
            "T" -> "Let's write a small t!"
            "V" -> "Time for a lowercase v!"
            "W" -> "Get ready for a small w!"
            "X" -> "Let's draw a lowercase x!"
            "Y" -> "Here comes a small y!"
            "Z" -> "Let's write a lowercase z!"
            else -> "Let's write a small $lower!"
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialSessionScreenPreview() {
    TutorialSessionScreen(
        title = "Vowels",
        initialStep = 1,
        totalSteps = 5,
        studentName = "Test Student",
        onEndSession = {}
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressCheckDialog(
    isCorrect: Boolean,
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    showAnnotationTooltip: Boolean,
    onAnnotateClick: () -> Unit,
    onAnnotationTooltipDismissed: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current

    // Lists of specific affirmative audio files (same as Learn Mode)
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

    val wrongAffirmatives = listOf(
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

    // Play two-phase audio when dialog appears (base sound → random affirmative)
    DisposableEffect(isCorrect) {
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

        // Play base sound first, then random affirmative
        val baseResId = if (isCorrect) R.raw.correct else R.raw.wrong
        val affirmativeList = if (isCorrect) correctAffirmatives else wrongAffirmatives
        val randomAffirmative = affirmativeList.random()

        playAudio(baseResId) {
            // Play random affirmative after base sound finishes
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
    val hasCaseMismatch = isCorrect && isSimilarShape && 
                         predictedLetter.isNotEmpty() && 
                         !predictedLetter.equals(expectedCase, ignoreCase = false)
    
    // Compute display letter with correct case for dynamic feedback text
    val displayLetter = when (targetCase.lowercase()) {
        "small", "lowercase" -> targetLetter.lowercase()
        else -> targetLetter.uppercase()
    }

    Dialog(
        onDismissRequest = { /* Teacher-gated: no dismiss on back or outside tap */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        // Full-screen dark overlay (non-dismissable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            // Annotation icon - top left (yellow for tutorial mode)
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
                                    containerColor = YellowIconColor
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
                                colorFilter = ColorFilter.tint(YellowIconColor),
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
                            colorFilter = ColorFilter.tint(YellowIconColor),
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
                    painter = painterResource(
                        id = if (isCorrect) R.drawable.dis_mobile_correct else R.drawable.dis_mobile_incorrect
                    ),
                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Main label: student name greeting
                Text(
                    text = if (isCorrect) {
                        "Great job${if (firstName.isNotEmpty()) ", $firstName" else ""}!"
                    } else {
                        "Not quite${if (firstName.isNotEmpty()) ", $firstName" else ""}."
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFFCCDB00) else Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Sub label: letter-specific message
                Text(
                    text = if (isCorrect) {
                        "You traced the letter $displayLetter!"
                    } else {
                        "Let's try the letter $displayLetter again!"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Sub label: encouragement text
                if (isCorrect) {
                    Text(
                        text = "Keep up the amazing work!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Let's give it another go!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Case mismatch disclaimer (if applicable)
                if (isCorrect && hasCaseMismatch) {
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = "Psst... you wrote ${if (predictedLetter.first().isUpperCase()) "uppercase" else "lowercase"} ${predictedLetter.uppercase()}, " +
                              "but we're practicing ${if (targetCase.lowercase() in listOf("small", "lowercase")) "lowercase" else "uppercase"} letters! " +
                              "They look similar, so that's still great! \uD83D\uDE0A",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

            }

            // Teacher-led Continue button at the bottom
            Button(
                onClick = onContinue,
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
