package com.example.app.ui.feature.learn.tutorialmode

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.app.ui.components.common.ProgressCheckDialog
import com.example.app.ui.components.common.ProgressIndicator
import com.example.app.ui.components.learnmode.AnnotationData
import com.example.app.ui.components.learnmode.LearnerProfileAnnotationDialog
import com.example.app.ui.components.tutorial.AnimatedLetterView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val YellowColor = Color(0xFFEDBB00)
private val LightYellowColor = Color(0xFFFFF3C4)
private val BlueButtonColor = Color(0xFF3FA9F8)
private val YellowIconColor = Color(0xFFFFC700) // #FFC700 for tutorial mode icons
private val OrangeButtonColor = Color(0xFFFF8C42) // Orange for tutorial mode button
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
    modifier: Modifier = Modifier,
    initialStep: Int = 1,
    totalSteps: Int = 0, // Will be calculated from letters
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }

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
    
    // Annotation dialog state
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var annotationsMap by remember { mutableStateOf<Map<Int, AnnotationData>>(emptyMap()) }
    
    // Tooltip state for annotation button
    var showAnnotationTooltip by remember { mutableStateOf(true) }
    val tooltipState = rememberTooltipState(isPersistent = false)

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    
    // Get database instance for annotation persistence
    val database = remember { AppDatabase.getInstance(context) }
    val annotationDao = remember { database.learnerProfileAnnotationDao() }
    val annotationSummaryDao = remember { database.annotationSummaryDao() }
    val tutorialCompletionDao = remember { database.tutorialCompletionDao() }
    val geminiRepository = remember { com.example.app.data.repository.GeminiRepository() }

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
        coroutineScope.launch {
            if (studentId > 0L) {
                withContext(Dispatchers.IO) {
                    tutorialCompletionDao.insertIfNotExists(
                        com.example.app.data.entity.TutorialCompletion(
                            studentId = studentId,
                            tutorialSetId = tutorialSetId
                        )
                    )
                }
            }
            onEndSession()
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

    // Notify watch when Tutorial Mode session starts
    LaunchedEffect(Unit) {
        watchConnectionManager.notifyTutorialModeStarted(studentName, title)
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
    LaunchedEffect(currentStep, currentLetter) {
        if (currentLetter.isNotEmpty()) {
            watchConnectionManager.sendTutorialModeLetterData(
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
                    voiceMediaPlayer.start()
                    Log.d("TutorialSession", "Playing pre-recorded voice for: $currentLetter ($letterType)")
                } catch (e: Exception) {
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
                if (currentStep < maxSteps) {
                    currentStep++
                }
                onSkip()
            }
        }
    }
    
    // Listen for gesture results from watch
    LaunchedEffect(Unit) {
        var lastGestureTime = 0L
        watchConnectionManager.tutorialModeGestureResult.collect { result ->
            val timestamp = result["timestamp"] as? Long ?: 0L
            if (timestamp > lastGestureTime && result.isNotEmpty()) {
                lastGestureTime = timestamp
                isCorrectGesture = result["isCorrect"] as? Boolean ?: false
                predictedLetter = result["predictedLetter"] as? String ?: ""
                showProgressCheck = true
            }
        }
    }
    
    // Progress Check Dialog (teacher-gated: only Continue button advances)
    if (showProgressCheck) {
        ProgressCheckDialog(
            isCorrect = isCorrectGesture,
            studentName = studentName,
            targetLetter = currentLetter,
            targetCase = letterType,
            predictedLetter = predictedLetter,
            onContinue = {
                showProgressCheck = false
                if (isCorrectGesture) {
                    // Move to next letter on correct gesture
                    val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                    if (currentStep < maxSteps) {
                        currentStep++
                        // sendTutorialModeLetterData is triggered by LaunchedEffect on currentStep change
                    } else {
                        completeTutorialAndEnd()
                    }
                } else {
                    // On incorrect, notify watch to retry and resend letter data
                    watchConnectionManager.notifyTutorialModeRetry()
                    if (currentLetter.isNotEmpty()) {
                        watchConnectionManager.sendTutorialModeLetterData(
                            letter = currentLetter,
                            letterCase = letterType,
                            currentIndex = currentStep,
                            totalLetters = calculatedTotalSteps,
                            dominantHand = dominantHand
                        )
                    }
                }
            },
            onAddAnnotation = {
                showAnnotationDialog = true
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Bar
        ProgressIndicator(
            currentStep = currentStep,
            totalSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps,
            modifier = Modifier.fillMaxWidth(),
            activeColor = YellowColor,
            inactiveColor = LightYellowColor
        )

        Spacer(Modifier.height(12.dp))

        // Annotate and Skip Button Row (matching Learn Mode layout)
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
                                containerColor = LightYellowTooltipColor // Light yellow color
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
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(YellowIconColor.copy(alpha = 0.5f)) // #FFC700 @ 50% opacity
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
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(YellowIconColor.copy(alpha = 0.5f)) // #FFC700 @ 50% opacity
                    )
                }
            }

            // Skip button (right) - icon instead of text
            IconButton(onClick = {
                val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                if (currentStep < maxSteps) {
                    currentStep++
                }
                onSkip()
            }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_skip),
                    contentDescription = "Skip",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(YellowIconColor.copy(alpha = 0.5f)) // #FFC700 @ 50% opacity
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
                // Toggle switch in top-right corner
                Switch(
                    checked = showAnimation,
                    onCheckedChange = { showAnimation = it },
                    modifier = Modifier
                        .align(Alignment.TopEnd),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BlueButtonColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )

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

        Spacer(Modifier.height(24.dp))

        // End Session Button
        Button(
            onClick = onEndSession,
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

@Composable
private fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) YellowColor else LightYellowColor
                    )
            )
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


@Composable
private fun ProgressCheckDialog(
    isCorrect: Boolean,
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    onContinue: () -> Unit,
    onAddAnnotation: () -> Unit = {}
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

                Spacer(Modifier.height(20.dp))

                // Add Annotations clickable text
                Text(
                    text = "Add Annotations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueAnnotationColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onAddAnnotation() }
                        .padding(vertical = 4.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Teacher-led Continue button
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeButtonColor
                    ),
                    shape = RoundedCornerShape(12.dp)
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
}
