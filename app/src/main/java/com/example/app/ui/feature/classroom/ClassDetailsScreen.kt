package com.example.app.ui.feature.classroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.components.classroom.RemoveStudentDialog
import com.example.app.ui.components.classroom.StudentCard

@Composable
fun ClassDetailsScreen(
    classId: String = "1",
    onNavigateBack: () -> Unit,
    onNavigateToAddStudent: (String) -> Unit = {},
    onNavigateToEditClass: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onNavigateToStudentDetails: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: ClassroomViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.classDetailsUiState.collectAsState()
    
    // Load class details when screen is first composed
    LaunchedEffect(classId) {
        viewModel.loadClassDetails(classId.toLongOrNull() ?: 0L)
    }
    // State for removal mode
    var isRemovalMode by remember { mutableStateOf(false) }
    var studentToRemove by remember { mutableStateOf<RosterStudent?>(null) }
    
    val classEntity = uiState.classEntity
    val students = uiState.students

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 140.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Back Button
            IconButton(
                onClick = { onNavigateBack() },
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF3FA9F8)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Show loading or error state
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3FA9F8))
                }
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    fontSize = 16.sp,
                    color = Color(0xFF49A9FF),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else if (classEntity != null) {
                // Class Banner - handle both drawable resources and custom file paths
                when {
                    classEntity.bannerPath?.startsWith("drawable://") == true -> {
                        val resName = classEntity.bannerPath.removePrefix("drawable://")
                        val drawableRes = when (resName) {
                            "ic_class_abc" -> R.drawable.ic_class_abc
                            "ic_class_stars" -> R.drawable.ic_class_stars
                            else -> R.drawable.ic_class_abc
                        }
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = "Class Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(169.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    classEntity.bannerPath != null -> {
                        AsyncImage(
                            model = java.io.File(classEntity.bannerPath),
                            contentDescription = "Class Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(169.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_class_abc)
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = R.drawable.ic_class_abc),
                            contentDescription = "Class Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(169.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Class Code
                Text(
                    text = classEntity.classCode,
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
                        text = classEntity.className,
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
                                onNavigateToEditClass(classId, classEntity.className, classEntity.classCode, classEntity.bannerPath)
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

                // Student Grid or Empty State
                if (students.isEmpty()) {
                    // Empty state
                    Spacer(Modifier.height(40.dp))
                    
                    Text(
                        text = "Your Class is Ready!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3FA9F8),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Add your first student to get started with tracking their progress.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        lineHeight = 24.sp
                    )
                    
                    Spacer(Modifier.height(40.dp))
                } else {
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
                                        studentName = student.fullName,
                                        profileImageRes = R.drawable.dis_default_pfp,
                                        profileImagePath = student.pfpPath,
                                        onClick = { 
                                            onNavigateToStudentDetails(
                                                student.studentId.toString(),
                                                student.fullName,
                                                classEntity.className
                                            )
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
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // Add A New Student Button - Transparent, overlaying at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            PrimaryButton(
                text = "Add A New Student",
                onClick = { 
                    classEntity?.let { 
                        onNavigateToAddStudent(it.className)
                    }
                },
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
            studentName = student.fullName,
            onConfirm = {
                viewModel.removeStudentFromClass(
                    studentId = student.studentId,
                    classId = classId.toLongOrNull() ?: 0L,
                    onSuccess = {
                        studentToRemove = null
                        isRemovalMode = false
                        // Reload class details to refresh the list
                        viewModel.loadClassDetails(classId.toLongOrNull() ?: 0L)
                    },
                    onError = { error ->
                        // TODO: Show error toast/snackbar
                        studentToRemove = null
                    }
                )
            },
            onDismiss = {
                studentToRemove = null
            }
        )
    }
}
