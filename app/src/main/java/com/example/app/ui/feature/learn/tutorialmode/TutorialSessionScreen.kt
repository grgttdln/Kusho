package com.example.app.ui.feature.learn.tutorialmode

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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

@Composable
fun TutorialSessionScreen(
    title: String,
    letterType: String = "capital",
    studentName: String = "",
    studentId: Long = 0L,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 1,
    totalSteps: Int = 0, // Will be calculated from letters
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    
    var currentStep by remember { mutableIntStateOf(initialStep) }
    var showProgressCheck by remember { mutableStateOf(false) }
    var isCorrectGesture by remember { mutableStateOf(false) }
    var predictedLetter by remember { mutableStateOf("") }
    var showAnimation by remember { mutableStateOf(false) } // Toggle for animation vs static image
    
    // Annotation dialog state
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var annotationsMap by remember { mutableStateOf<Map<Int, AnnotationData>>(emptyMap()) }
    
    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    
    // Get database instance for annotation persistence
    val database = remember { AppDatabase.getInstance(context) }
    val annotationDao = remember { database.learnerProfileAnnotationDao() }
    
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
    
    // Notify watch when session ends (cleanup)
    DisposableEffect(Unit) {
        onDispose {
            watchConnectionManager.notifyTutorialModeEnded()
        }
    }
    
    // Send current letter data to watch whenever it changes
    LaunchedEffect(currentStep, currentLetter) {
        if (currentLetter.isNotEmpty()) {
            watchConnectionManager.sendTutorialModeLetterData(
                letter = currentLetter,
                letterCase = letterType,
                currentIndex = currentStep,
                totalLetters = calculatedTotalSteps
            )
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
    
    // Listen for feedback dismissal from watch
    LaunchedEffect(Unit) {
        var lastDismissTime = 0L
        watchConnectionManager.tutorialModeFeedbackDismissed.collect { timestamp ->
            if (timestamp > lastDismissTime && timestamp > 0L) {
                lastDismissTime = timestamp
                if (showProgressCheck) {
                    showProgressCheck = false
                    // If correct, move to next letter
                    if (isCorrectGesture) {
                        val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                        if (currentStep < maxSteps) {
                            currentStep++
                        } else {
                            onEndSession()
                        }
                    }
                }
            }
        }
    }

    // Progress Check Dialog
    if (showProgressCheck) {
        ProgressCheckDialog(
            isCorrect = isCorrectGesture,
            studentName = studentName,
            targetLetter = currentLetter,
            targetCase = letterType,
            predictedLetter = predictedLetter,
            onDismiss = {
                showProgressCheck = false
                // Notify watch that mobile dismissed feedback
                watchConnectionManager.notifyTutorialModeFeedbackDismissed()
                if (isCorrectGesture) {
                    // Move to next letter on correct gesture
                    val maxSteps = if (totalSteps > 0) totalSteps else calculatedTotalSteps
                    if (currentStep < maxSteps) {
                        currentStep++
                    } else {
                        onEndSession()
                    }
                } else {
                    // On incorrect, notify watch to retry and resend letter data
                    watchConnectionManager.notifyTutorialModeRetry()
                    if (currentLetter.isNotEmpty()) {
                        watchConnectionManager.sendTutorialModeLetterData(
                            letter = currentLetter,
                            letterCase = letterType,
                            currentIndex = currentStep,
                            totalLetters = calculatedTotalSteps
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
            accentColor = YellowIconColor,
            buttonColor = OrangeButtonColor,
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
    onDismiss: () -> Unit
) {
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
                
                // Title with first name only
                Text(
                    text = if (isCorrect) {
                        "Great Job${if (firstName.isNotEmpty()) ", $firstName" else ""}!"
                    } else {
                        "Not quite${if (firstName.isNotEmpty()) ", $firstName" else ""}!"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFFCCDB00) else Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Body text with optional case mismatch disclaimer
                if (isCorrect && hasCaseMismatch) {
                    Text(
                        text = "You're doing super!\nKeep up the amazing work!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
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
                } else if (isCorrect) {
                    Text(
                        text = "You're doing super!\nKeep up the amazing work!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                } else {
                    Text(
                        text = "Let's give it another go!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}
