package com.example.app.ui.feature.learn.tutorialmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.classroom.StudentCard
import com.example.app.ui.components.tutorial.LetterTypeSelectionDialog
import com.example.app.ui.feature.classroom.ClassroomViewModel

@Composable
fun TutorialStudentSelectionScreen(
    onBack: () -> Unit,
    onSelectStudent: (studentId: Long, studentName: String, classId: Long, letterType: String, dominantHand: String) -> Unit,
    showLetterTypeDialog: Boolean = true,
    modifier: Modifier = Modifier,
    classroomViewModel: ClassroomViewModel = viewModel()
) {
    val students by classroomViewModel.allStudents.collectAsState()
    var selectedStudent by remember { mutableStateOf<Triple<Long, String, Long>?>(null) }
    var selectedStudentHand by remember { mutableStateOf("RIGHT") }
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrollable screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
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

            Text(
                text = "Select a Student",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(Modifier.height(24.dp))

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.offset(y = (-40).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dis_none),
                            contentDescription = "No students mascot",
                            modifier = Modifier.size(240.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "No Students Yet",
                            color = Color(0xFF4A4A4A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add students in the Class tab\nto start a tutorial session.",
                            color = Color(0xFF7A7A7A),
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // 2-column grid using chunked rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    students.chunked(2).forEach { rowStudents ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowStudents.forEach { student ->
                                StudentCard(
                                    studentName = student.fullName,
                                    profileImagePath = student.pfpPath,
                                    onClick = {
                                        // Store selected student
                                        selectedStudent = Triple(student.studentId, student.fullName, 0L)
                                        selectedStudentHand = student.dominantHand
                                        if (showLetterTypeDialog) {
                                            // Show letter type dialog for tutorial mode
                                            showDialog = true
                                        } else {
                                            // Skip dialog for learn mode, pass empty letterType
                                            onSelectStudent(student.studentId, student.fullName, 0L, "", student.dominantHand)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (rowStudents.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Letter Type Selection Dialog
    if (showDialog) {
        LetterTypeSelectionDialog(
            onDismiss = { 
                showDialog = false
                selectedStudent = null
            },
            onSelectType = { letterType ->
                showDialog = false
                selectedStudent?.let { (studentId, studentName, classId) ->
                    onSelectStudent(studentId, studentName, classId, letterType, selectedStudentHand)
                }
                selectedStudent = null
            }
        )
    }
}
