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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.classroom.StudentCard
import com.example.app.ui.feature.classroom.ClassroomViewModel

@Composable
fun TutorialModeScreen(
    onBack: () -> Unit,
    onStudentSelected: (studentId: Long, classId: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    classroomViewModel: ClassroomViewModel = viewModel()
) {
    val students by classroomViewModel.allStudents.collectAsState()

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
                                        // No class context in this mode, pass 0L
                                        onStudentSelected(student.studentId, 0L)
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

        // Back button on top of content
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 25.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2196F3)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialModeScreenPreview() {
    TutorialModeScreen(onBack = {})
}
