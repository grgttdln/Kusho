package com.example.app.ui.feature.learn.tutorialmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.tutorial.TutorialActivityCard
import com.example.app.ui.components.tutorial.LetterTypeSelectionDialog

@Composable
fun TutorialModeStudentScreen(
    studentId: Long,
    classId: Long,
    studentName: String = "",
    preselectedSection: String? = null,
    onBack: () -> Unit,
    onStartSession: (String, String, String) -> Unit, // (title, letterType, studentName)
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf<String?>(preselectedSection) }
    var showLetterTypeDialog by remember { mutableStateOf(preselectedSection != null) }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Header with back button and Kusho logo
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF3FA9F8)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_kusho),
                    contentDescription = "Kusho Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .offset(x = 10.dp)
                        .align(Alignment.Center),
                    alignment = Alignment.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Vowels Card
            TutorialActivityCard(
                title = "Vowels",
                iconRes = R.drawable.ic_apple,
                isSelected = selectedSection == "Vowels",
                onClick = { 
                    selectedSection = if (selectedSection == "Vowels") null else "Vowels"
                }
            )

            Spacer(Modifier.height(16.dp))

            // Consonants Card
            TutorialActivityCard(
                title = "Consonants",
                iconRes = R.drawable.ic_ball,
                isSelected = selectedSection == "Consonants",
                onClick = { 
                    selectedSection = if (selectedSection == "Consonants") null else "Consonants"
                }
            )

            Spacer(Modifier.height(100.dp))
        }

        // Start Session Button at the bottom
        Button(
            onClick = {
                if (selectedSection != null) {
                    showLetterTypeDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedSection != null
        ) {
            Text(
                text = "Start Session",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Letter Type Selection Dialog
    if (showLetterTypeDialog) {
        LetterTypeSelectionDialog(
            onDismiss = { showLetterTypeDialog = false },
            onSelectType = { letterType ->
                showLetterTypeDialog = false
                selectedSection?.let { section ->
                    onStartSession(section, letterType, studentName)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialModeStudentScreenPreview() {
    TutorialModeStudentScreen(
        studentId = 1L,
        classId = 1L,
        studentName = "Test Student",
        preselectedSection = null,
        onBack = {},
        onStartSession = { _, _, _ -> }
    )
}

