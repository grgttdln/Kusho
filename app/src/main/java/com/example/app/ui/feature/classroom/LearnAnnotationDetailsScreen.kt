package com.example.app.ui.feature.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.AppDatabase
import com.example.app.data.entity.LearnerProfileAnnotation
import com.example.app.data.entity.SetWord
import com.example.app.data.entity.Word

@Composable
fun LearnAnnotationDetailsScreen(
    lessonName: String,
    setId: Long,
    activityId: Long,
    studentId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    
    var setWords by remember { mutableStateOf(listOf<SetWord>()) }
    var wordsMap by remember { mutableStateOf(mapOf<Long, Word>()) }
    var annotationsMap by remember { mutableStateOf(mapOf<Long, LearnerProfileAnnotation?>()) }
    
    // Load set words and their corresponding word details
    LaunchedEffect(setId, studentId) {
        if (setId > 0 && studentId.isNotEmpty()) {
            val setWordDao = database.setWordDao()
            val wordDao = database.wordDao()
            val annotationDao = database.learnerProfileAnnotationDao()
            
            val loadedSetWords = setWordDao.getSetWords(setId)
            setWords = loadedSetWords
            
            // Load word details and annotations for each set word
            val wordMap = mutableMapOf<Long, Word>()
            val annotationMap = mutableMapOf<Long, LearnerProfileAnnotation?>()
            
            loadedSetWords.forEachIndexed { index, setWord ->
                wordDao.getWordById(setWord.wordId)?.let { word ->
                    wordMap[setWord.id] = word
                    
                    // Load annotation for this word in this set
                    val annotation = annotationDao.getAnnotation(
                        studentId = studentId,
                        setId = setId,
                        itemId = index,
                        sessionMode = LearnerProfileAnnotation.MODE_LEARN
                    )
                    annotationMap[setWord.id] = annotation
                }
            }
            wordsMap = wordMap
            annotationsMap = annotationMap
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))

        // Header with back button - no extra padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            IconButton(
                onClick = { onNavigateBack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF3FA9F8)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Content with extra side padding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            // Title Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Learn Mode subtitle
                Text(
                    text = "Learn Mode",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFAE8EFB)
                )

                Spacer(Modifier.height(8.dp))

                // Main title
                Text(
                    text = lessonName,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(32.dp))

            // Set Items List
            setWords.forEachIndexed { index, setWord ->
                val word = wordsMap[setWord.id]
                val annotation = annotationsMap[setWord.id]
                if (word != null) {
                    SetItemRow(
                        word = word,
                        setWord = setWord,
                        annotation = annotation,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SetItemRow(
    word: Word,
    setWord: SetWord,
    annotation: LearnerProfileAnnotation?,
    modifier: Modifier = Modifier
) {
    val category = when (setWord.configurationType.lowercase()) {
        "fill in the blank" -> "Fill in the Blanks"
        "identification", "air writing" -> "Write the Word"
        "name the picture" -> "Name the Picture"
        else -> setWord.configurationType.replaceFirstChar { it.uppercase() }
    }
    
    val isFullWordPurple = setWord.configurationType.lowercase() in listOf("identification", "air writing", "name the picture", "write the word")
    val isFillInTheBlank = setWord.configurationType.lowercase() == "fill in the blank"
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Word display with masked letters
            if (isFillInTheBlank) {
                // For Fill in the Blanks: show each character separately with the selected one in purple
                val letterIndex = setWord.selectedLetterIndex.coerceIn(0, word.word.length - 1)
                Row {
                    word.word.forEachIndexed { index, char ->
                        Text(
                            text = char.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (index == letterIndex) Color(0xFFAE8EFB) else Color.Black
                        )
                    }
                }
            } else {
                // For other categories: show the full word in purple
                Text(
                    text = word.word,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFullWordPurple) Color(0xFFAE8EFB) else Color.Black
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level of Progress badge with star icon
                annotation?.levelOfProgress?.let { level ->
                    if (level.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFAE8EFB),
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_star),
                                    contentDescription = "Star",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = level,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Category badge
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFAE8EFB),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        
        // Show annotation cards if exists
        annotation?.let { ann ->
            // Strengths card
            if (ann.getStrengthsList().isNotEmpty() || ann.strengthsNote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                StrengthsCard(annotation = ann)
            }
            
            // Challenges card
            if (ann.getChallengesList().isNotEmpty() || ann.challengesNote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ChallengesCard(annotation = ann)
            }
        }
    }
}

@Composable
private fun StrengthsCard(
    annotation: LearnerProfileAnnotation,
    modifier: Modifier = Modifier
) {
    val strengths = annotation.getStrengthsList()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF5F0FF),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = Color(0xFFAE8EFB),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Strength tags
        if (strengths.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                strengths.forEach { strength ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFAE8EFB),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strength,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        
        // Strengths note
        if (annotation.strengthsNote.isNotBlank()) {
            Text(
                text = annotation.strengthsNote,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun ChallengesCard(
    annotation: LearnerProfileAnnotation,
    modifier: Modifier = Modifier
) {
    val challenges = annotation.getChallengesList()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF0F7FF),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = Color(0xFF5B9BD5),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Challenges tags
        if (challenges.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                challenges.forEach { challenge ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF5B9BD5),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = challenge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        
        // Challenges note
        if (annotation.challengesNote.isNotBlank()) {
            Text(
                text = annotation.challengesNote,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                lineHeight = 24.sp
            )
        }
    }
}
