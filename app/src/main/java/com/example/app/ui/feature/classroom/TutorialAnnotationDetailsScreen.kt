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

@Composable
fun TutorialAnnotationDetailsScreen(
    tutorialName: String,
    setId: Long,
    studentId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }

    // Determine if it's Capital or Small based on tutorialName
    val isSmall = tutorialName.contains("Small", ignoreCase = true)
    
    // Determine if it's Vowels or Consonants based on tutorialName
    val isConsonants = tutorialName.contains("Consonants", ignoreCase = true)
    
    // Format the main title
    val displayTitle = when {
        isConsonants && isSmall -> "Lowercase Consonants"
        isConsonants && !isSmall -> "Uppercase Consonants"
        !isConsonants && isSmall -> "Lowercase Vowels"
        else -> "Uppercase Vowels"
    }
    
    // Letters to display: Vowels or Consonants
    val letters = when {
        isConsonants && isSmall -> listOf("b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x", "y", "z")
        isConsonants && !isSmall -> listOf("B", "C", "D", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "V", "W", "X", "Y", "Z")
        !isConsonants && isSmall -> listOf("a", "e", "i", "o", "u")
        else -> listOf("A", "E", "I", "O", "U")
    }
    
    var annotationsMap by remember { mutableStateOf(mapOf<Int, LearnerProfileAnnotation?>()) }

    // Load tutorial annotations for each letter
    LaunchedEffect(setId, studentId) {
        if (setId != 0L && studentId.isNotEmpty()) {
            val annotationDao = database.learnerProfileAnnotationDao()
            val annotationMap = mutableMapOf<Int, LearnerProfileAnnotation?>()

            letters.forEachIndexed { index, _ ->
                val annotation = annotationDao.getAnnotation(
                    studentId = studentId,
                    setId = setId,
                    itemId = index,
                    sessionMode = LearnerProfileAnnotation.MODE_TUTORIAL
                )
                annotationMap[index] = annotation
            }
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
                // Tutorial Mode subtitle
                Text(
                    text = "Tutorial Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEDBB00)
                )

                Spacer(Modifier.height(4.dp))

                // Main title
                Text(
                    text = displayTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(32.dp))

            // Letters List with Annotations
            letters.forEachIndexed { index, letter ->
                val annotation = annotationsMap[index]
                LetterItem(
                    letter = letter,
                    annotation = annotation
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LetterItem(
    letter: String,
    annotation: LearnerProfileAnnotation?,
    modifier: Modifier = Modifier
) {
    val strengths = annotation?.getStrengthsList() ?: emptyList()
    val challenges = annotation?.getChallengesList() ?: emptyList()

    Column(modifier = modifier) {
        // Row with vowel letter and badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter display
            Text(
                text = letter,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDBB00)
            )

            // Badges on the right
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
                                    color = Color(0xFFEDBB00),
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

                // Tracing category badge
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFEDBB00),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Tracing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Strengths card
        if (annotation != null && (strengths.isNotEmpty() || annotation.strengthsNote.isNotBlank())) {
            TutorialStrengthsCard(annotation = annotation)
            Spacer(Modifier.height(12.dp))
        }

        // Challenges card
        if (annotation != null && (challenges.isNotEmpty() || annotation.challengesNote.isNotBlank())) {
            TutorialChallengesCard(annotation = annotation)
        }
    }
}

@Composable
private fun TutorialStrengthsCard(
    annotation: LearnerProfileAnnotation,
    modifier: Modifier = Modifier
) {
    val strengths = annotation.getStrengthsList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFEFBF2),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = Color(0xFFEDBB00),
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
                                color = Color(0xFFEDBB00),
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
private fun TutorialChallengesCard(
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
