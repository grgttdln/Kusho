package com.example.app.ui.feature.learn.learnmode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.app.R
import com.example.app.data.AppDatabase
import com.example.app.data.repository.SetRepository
import com.example.app.service.WatchConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val PurpleColor = Color(0xFFAE8EFB)
private val LightPurpleColor = Color(0xFFE7DDFE)
private val CompletedLetterColor = Color(0xFFAE8EFB)
private val PendingLetterColor = Color(0xFF808080)

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

            watchConnectionManager.sendLearnModeWordData(
                word = currentWord.word,
                maskedIndex = currentWord.selectedLetterIndex,
                configurationType = currentWord.configurationType
            )
        }
    }

    // Listen for letter input events from watch (for Write the Word, Name the Picture, and Fill in the Blank modes)
    LaunchedEffect(sessionKey) {
        val sessionStartTime = System.currentTimeMillis()
        watchConnectionManager.letterInputEvent.collect { event ->
            if (event.timestamp > sessionStartTime) {
                val currentWord = words.getOrNull(currentWordIndex)
                if (currentWord != null) {
                    when (currentWord.configurationType) {
                        "Fill in the Blank" -> {
                            // For Fill in the Blank, check if the masked letter is correct
                            val expectedLetter = currentWord.word.getOrNull(currentWord.selectedLetterIndex)
                            val isCorrect = event.letter == expectedLetter

                            if (isCorrect) {
                                // Correct answer - notify watch and move to next word
                                watchConnectionManager.sendWordComplete()

                                // Small delay before moving to next word
                                kotlinx.coroutines.delay(500)

                                if (currentWordIndex < words.size - 1) {
                                    currentWordIndex++
                                } else {
                                    // All items complete - notify watch before navigating away
                                    watchConnectionManager.notifyActivityComplete()
                                    onSessionComplete()
                                }
                            }
                            // For Fill in the Blank, wrong answers are already handled on the watch
                        }
                        "Write the Word", "Name the Picture" -> {
                            // Get expected letter - match exact case (uppercase vs lowercase)
                            val expectedLetter = currentWord.word.getOrNull(currentLetterIndex)

                            // Check if input letter matches expected letter exactly (case-sensitive)
                            // If expected is uppercase, input must be uppercase
                            // If expected is lowercase, input must be lowercase
                            val isCorrect = event.letter == expectedLetter

                            if (isCorrect) {
                                // Mark letter as completed
                                completedLetterIndices = completedLetterIndices + currentLetterIndex
                                currentLetterIndex++

                                if (currentLetterIndex >= currentWord.word.length) {
                                    // Word complete - notify watch and move to next word
                                    watchConnectionManager.sendWordComplete()

                                    // Small delay before moving to next word
                                    kotlinx.coroutines.delay(500)

                                    if (currentWordIndex < words.size - 1) {
                                        currentWordIndex++
                                    } else {
                                        // All items complete - notify watch before navigating away
                                        watchConnectionManager.notifyActivityComplete()
                                        onSessionComplete()
                                    }
                                } else {
                                    // Send correct result and move to next letter
                                    watchConnectionManager.sendLetterResult(true, currentLetterIndex, currentWord.word.length)
                                }
                            } else {
                                // Wrong letter - send incorrect feedback but DON'T advance letter index
                                // User must retry the same letter until correct (case-sensitive)
                                watchConnectionManager.sendLetterResult(false, currentLetterIndex, currentWord.word.length)
                            }
                        }
                    }
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

        // Audio Icon and Skip Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAudioClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_volume),
                    contentDescription = "Audio",
                    tint = PurpleColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            TextButton(onClick = {
                onSkip()
                handleSkipOrNext()
            }) {
                Text(
                    text = "Skip",
                    color = PurpleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
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

                if (isWriteTheWord) {
                    // Write the Word mode - show letters with color based on completion
                    WriteTheWordDisplay(
                        word = currentWord.word,
                        completedIndices = completedLetterIndices,
                        currentIndex = currentLetterIndex,
                        hasImage = currentWord.imagePath != null
                    )
                } else {
                    Text(
                        text = when {
                            isNameThePicture -> currentWord.getBlankWord()
                            else -> currentWord.getMaskedWord()
                        },
                        fontSize = if (currentWord.imagePath != null) 48.sp else 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = if (currentWord.imagePath != null) 4.sp else 8.sp
                    )
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
                        else -> Color.Black  // All other letters - black
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