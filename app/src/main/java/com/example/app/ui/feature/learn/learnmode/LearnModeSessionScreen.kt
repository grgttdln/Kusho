package com.example.app.ui.feature.learn.learnmode

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.app.R
import com.example.app.data.AppDatabase
import com.example.app.data.repository.SetRepository
import com.example.app.service.WatchConnectionManager
import com.example.app.ui.components.LearnerProfileAnnotationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val PurpleColor = Color(0xFFAE8EFB)
private val LightPurpleColor = Color(0xFFE7DDFE)
private val CompletedLetterColor = Color(0xFFAE8EFB)
private val PendingLetterColor = Color(0xFF808080)

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

@Composable
fun LearnModeSessionScreen(
    setId: Long = 0L,
    activityTitle: String = "",
    sessionKey: Int = 0,
    studentName: String = "",
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSessionComplete: () -> Unit = {}
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

    // State for ProgressCheckDialog
    var showProgressCheckDialog by remember(sessionKey) { mutableStateOf(false) }
    var isCorrectGesture by remember(sessionKey) { mutableStateOf(false) }
    var predictedLetter by remember(sessionKey) { mutableStateOf("") }
    var targetLetter by remember(sessionKey) { mutableStateOf("") }
    var targetCase by remember(sessionKey) { mutableStateOf("") }
    
    // Store pending state changes to apply after dialog dismissal
    var pendingCorrectAction by remember(sessionKey) { mutableStateOf<(() -> Unit)?>(null) }

    // State for Learner Profile Annotation Dialog
    var showAnnotationDialog by remember(sessionKey) { mutableStateOf(false) }

    val context = LocalContext.current

    // Get WatchConnectionManager instance
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    
    // Notify watch when Learn Mode session starts
    LaunchedEffect(sessionKey) {
        watchConnectionManager.notifyLearnModeStarted()
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
                val setRepository = SetRepository(
                    database.setDao(),
                    database.setWordDao(),
                    database.wordDao()
                )

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

    // Send current word data to watch whenever current word changes
    LaunchedEffect(currentWordIndex, words) {
        val currentWord = words.getOrNull(currentWordIndex)
        if (currentWord != null) {
            // Reset letter tracking state for new word
            completedLetterIndices = emptySet()
            currentLetterIndex = 0
            fillInBlankCorrect = false

            watchConnectionManager.sendLearnModeWordData(
                word = currentWord.word,
                maskedIndex = currentWord.selectedLetterIndex,
                configurationType = currentWord.configurationType
            )
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
                            predictedLetter = event.letter.toString()
                            isCorrectGesture = isCorrect
                            
                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())

                            android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Fill in the Blank")
                            showProgressCheckDialog = true

                            // Store pending action to execute on dialog dismiss
                            if (isCorrect) {
                                pendingCorrectAction = {
                                    // Mark Fill in the Blank as correct to reveal the letter
                                    fillInBlankCorrect = true
                                    
                                    // Correct answer - notify watch and move to next word
                                    watchConnectionManager.sendWordComplete()
                                    
                                    if (currentWordIndex < words.size - 1) {
                                        currentWordIndex++
                                    } else {
                                        // All items complete - notify watch before navigating away
                                        watchConnectionManager.notifyActivityComplete()
                                        onSessionComplete()
                                    }
                                }
                            } else {
                                pendingCorrectAction = {
                                    // Wrong letter - send incorrect feedback and resend word data for retry
                                    watchConnectionManager.sendLetterResult(false, currentWord.selectedLetterIndex, currentWord.word.length)
                                    // Resend word data so watch can prompt user to retry
                                    watchConnectionManager.sendLearnModeWordData(
                                        word = currentWord.word,
                                        maskedIndex = currentWord.selectedLetterIndex,
                                        configurationType = currentWord.configurationType
                                    )
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
                            predictedLetter = event.letter.toString()
                            isCorrectGesture = isCorrect
                            
                            // Send feedback to watch so it shows the same screen
                            watchConnectionManager.sendLearnModeFeedback(isCorrect, event.letter.toString())
                            
                            android.util.Log.d("LearnModeSession", "🎭 Setting showProgressCheckDialog = true for Write the Word/Name the Picture")
                            showProgressCheckDialog = true

                            // Store pending action to execute on dialog dismiss
                            if (isCorrect) {
                                val newLetterIndex = currentLetterIndex + 1
                                pendingCorrectAction = {
                                    // Mark letter as completed
                                    completedLetterIndices = completedLetterIndices + currentLetterIndex
                                    currentLetterIndex = newLetterIndex

                                    if (newLetterIndex >= currentWord.word.length) {
                                        // Word complete - notify watch and move to next word
                                        watchConnectionManager.sendWordComplete()

                                        if (currentWordIndex < words.size - 1) {
                                            currentWordIndex++
                                        } else {
                                            // All items complete - notify watch before navigating away
                                            watchConnectionManager.notifyActivityComplete()
                                            onSessionComplete()
                                        }
                                    } else {
                                        // Send correct result and move to next letter
                                        watchConnectionManager.sendLetterResult(true, newLetterIndex, currentWord.word.length)
                                    }
                                }
                            } else {
                                pendingCorrectAction = {
                                    // Wrong letter - send incorrect feedback but DON'T advance letter index
                                    // User must retry the same letter until correct (case-sensitive)
                                    watchConnectionManager.sendLetterResult(false, currentLetterIndex, currentWord.word.length)
                                    // Resend word data so watch can prompt user to retry
                                    watchConnectionManager.sendLearnModeWordData(
                                        word = currentWord.word,
                                        maskedIndex = currentWord.selectedLetterIndex,
                                        configurationType = currentWord.configurationType
                                    )
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
                onSkip()
                if (currentWordIndex < words.size - 1) {
                    currentWordIndex++
                } else {
                    // All items complete - notify watch before navigating away
                    watchConnectionManager.notifyActivityComplete()
                    onSessionComplete()
                }
            }
        }
    }

    // Progress Check Dialog - show before loading check to ensure it always renders
    if (showProgressCheckDialog) {
        ProgressCheckDialog(
            isCorrect = isCorrectGesture,
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
        LearnerProfileAnnotationDialog(
            studentName = studentName,
            onDismiss = { showAnnotationDialog = false },
            onAddNote = { levelOfProgress, strengthsObserved, strengthsNote, challenges, challengesNote ->
                // TODO: Save annotation data to database or send to analytics
                android.util.Log.d("LearnModeSession", "📝 Annotation saved: level=$levelOfProgress, strengths=$strengthsObserved, challenges=$challenges")
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
        if (currentWordIndex < words.size - 1) {
            currentWordIndex++
        } else {
            // Session complete - notify watch and navigate away
            watchConnectionManager.notifyActivityComplete()
            onSessionComplete()
        }
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
            totalSteps = totalWords,
            modifier = Modifier.fillMaxWidth(),
            activeColor = PurpleColor,
            inactiveColor = LightPurpleColor
        )

        Spacer(Modifier.height(12.dp))

        // Annotate and Skip Button Row
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
                    contentScale = ContentScale.Fit
                )
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
            if (currentWord?.imagePath != null) {
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
                            hasImage = currentWord.imagePath != null
                        )
                    }
                    isFillInTheBlank -> {
                        // Fill in the Blank mode - show masked word, reveal correct letter in violet when answered
                        FillInTheBlankDisplay(
                            word = currentWord.word,
                            maskedIndex = currentWord.selectedLetterIndex,
                            isCorrect = fillInBlankCorrect,
                            hasImage = currentWord.imagePath != null
                        )
                    }
                    isNameThePicture -> {
                        // Name the Picture mode - show blanks that reveal letters when correct
                        NameThePictureDisplay(
                            word = currentWord.word,
                            completedIndices = completedLetterIndices,
                            currentIndex = currentLetterIndex,
                            hasImage = currentWord.imagePath != null
                        )
                    }
                    else -> {
                        Text(
                            text = currentWord.getMaskedWord(),
                            fontSize = if (currentWord.imagePath != null) 48.sp else 96.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            letterSpacing = if (currentWord.imagePath != null) 4.sp else 8.sp
                        )
                    }
                }
            }

            Spacer(Modifier.weight(0.7f))
        }

        Spacer(Modifier.height(24.dp))
    }
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
    hasImage: Boolean
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
                Text(
                    text = letter.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        index in completedIndices -> CompletedLetterColor  // Completed - purple
                        else -> PendingLetterColor  // Pending letters - gray
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
    hasImage: Boolean
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
                Text(
                    text = if (index == maskedIndex && !isCorrect) " " else letter.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        index == maskedIndex && isCorrect -> CompletedLetterColor  // Revealed - purple
                        else -> Color.Black  // Other letters - black
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
    hasImage: Boolean
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
                Text(
                    text = if (index in completedIndices) letter.toString() else " ",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (index in completedIndices) CompletedLetterColor else Color.Transparent
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

@Composable
private fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = PurpleColor,
    inactiveColor: Color = LightPurpleColor
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
                        if (isActive) activeColor else inactiveColor
                    )
            )
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

