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

    val context = LocalContext.current

    // Get WatchConnectionManager instance
    val watchConnectionManager = remember { WatchConnectionManager.getInstance(context) }
    
    // Notify watch when Learn Mode session starts
    LaunchedEffect(sessionKey) {
        watchConnectionManager.notifyLearnModeStarted()
    }
    
    // Notify watch when session ends (cleanup)
    androidx.compose.runtime.DisposableEffect(sessionKey) {
        onDispose {
            watchConnectionManager.notifyLearnModeEnded()
        }
    }

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
            watchConnectionManager.sendLearnModeWordData(
                word = currentWord.word,
                maskedIndex = currentWord.selectedLetterIndex,
                configurationType = currentWord.configurationType
            )
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
            // Session complete - navigate away
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
                Text(
                    text = when {
                        isNameThePicture -> currentWord.getBlankWord()
                        isWriteTheWord -> currentWord.getFullWord()
                        else -> currentWord.getMaskedWord()
                    },
                    fontSize = if (currentWord.imagePath != null) 48.sp else 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWriteTheWord) Color(0xFF808080) else Color.Black,
                    letterSpacing = if (currentWord.imagePath != null) 4.sp else 8.sp
                )
            }

            Spacer(Modifier.weight(0.7f))
        }

        Spacer(Modifier.height(24.dp))
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