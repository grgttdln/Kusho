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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.app.R
import com.example.app.ui.components.tutorial.TutorialActivityCard

@Composable
fun TutorialModeStudentScreen(
    studentId: Long,
    classId: Long,
    onBack: () -> Unit,
    onStartSession: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var showLetterTypeDialog by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 25.dp)
                .zIndex(1f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2196F3)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_kusho),
                contentDescription = "Kusho Logo",
                modifier = Modifier
                    .height(54.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .offset(x = 10.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )

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
                    onStartSession(section, letterType)
                }
            }
        )
    }
}

@Composable
fun LetterTypeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Main card content (positioned below the mascot)
            Column(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                // Blue header section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF49A9FF))
                )

                // White content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Question text
                    Text(
                        text = "Which one should we practice?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Capital button
                        Button(
                            onClick = { onSelectType("capital") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49A9FF)
                            )
                        ) {
                            Text(
                                text = "CAPITAL",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Small button
                        Button(
                            onClick = { onSelectType("small") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49A9FF)
                            )
                        ) {
                            Text(
                                text = "small",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Question mascot image overlapping the top
            Image(
                painter = painterResource(id = R.drawable.dis_question),
                contentDescription = "Question",
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
}

@Preview(showBackground = true)
@Composable
fun TutorialModeStudentScreenPreview() {
    TutorialModeStudentScreen(
        studentId = 1L,
        classId = 1L,
        onBack = {},
        onStartSession = { _, _ -> }
    )
}

