package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.components.classroom.RemoveStudentDialog
import com.example.app.ui.components.classroom.StudentCard

data class Student(
    val id: String,
    val name: String,
    val profileImageRes: Int = R.drawable.dis_default_pfp
)

@Composable
fun ClassDetailsScreen(
    classId: String = "1",
    onNavigateBack: () -> Unit,
    onNavigateToAddStudent: (String) -> Unit = {},
    onNavigateToEditClass: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToStudentDetails: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // State for removal mode
    var isRemovalMode by remember { mutableStateOf(false) }
    var studentToRemove by remember { mutableStateOf<Student?>(null) }
    
    // Mock data - will be replaced with database data later
    val classCode = "G1-YB"
    val className = "Grade 1 Young Builders"
    val classBannerRes = R.drawable.ic_class_abc
    
    val students = listOf(
        Student("1", "David, Kim"),
        Student("2", "Johnson, Alex"),
        Student("3", "Rose, Sofia"),
        Student("4", "Garcia, Maria")
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 140.dp)
        ) {
            Spacer(Modifier.height(40.dp))

                // Back Button - positioned like Kusho logo
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color(0xFF3FA9F8),
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 10.dp)
                        .clickable { onNavigateBack() }
                )

                Spacer(Modifier.height(28.dp))

                // Class Banner
                Image(
                    painter = painterResource(id = classBannerRes),
                    contentDescription = "Class Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(169.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(16.dp))

                // Class Code
                Text(
                    text = classCode,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF3FA9F8)
                )

                Spacer(Modifier.height(8.dp))

                // Class Name with Edit Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = className,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Class Name",
                        tint = Color(0xFF3FA9F8),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { 
                                onNavigateToEditClass(classId, className, classCode)
                            }
                    )
                }

                Spacer(Modifier.height(36.dp))

                // Class Roster Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Class Roster",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isRemovalMode) Icons.Default.EditOff else Icons.Default.Edit,
                        contentDescription = if (isRemovalMode) "Cancel Removal Mode" else "Edit Roster",
                        tint = Color(0xFF3FA9F8),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { isRemovalMode = !isRemovalMode }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Student Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    students.chunked(2).forEach { rowStudents ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowStudents.forEach { student ->
                                StudentCard(
                                    studentName = student.name,
                                    profileImageRes = student.profileImageRes,
                                    onClick = { 
                                        onNavigateToStudentDetails(student.id, student.name, className)
                                    },
                                    isRemovalMode = isRemovalMode,
                                    onRemove = { studentToRemove = student },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Add spacer if odd number of students in last row
                            if (rowStudents.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
        }

        // Add A New Student Button - Transparent, overlaying at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            PrimaryButton(
                text = "Add A New Student",
                onClick = { onNavigateToAddStudent(className) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
    
    // Remove Student Confirmation Dialog
    studentToRemove?.let { student ->
        RemoveStudentDialog(
            studentName = student.name,
            onConfirm = {
                // TODO: Remove student from database
                studentToRemove = null
                isRemovalMode = false
            },
            onDismiss = {
                studentToRemove = null
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ClassDetailsScreenPreview() {
    ClassDetailsScreen(
        onNavigateBack = {}
    )
}
