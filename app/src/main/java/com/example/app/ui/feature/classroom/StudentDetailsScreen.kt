package com.example.app.ui.feature.classroom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.util.ImageUtil
import com.example.app.ui.components.classroom.AnalyticsCard
import com.example.app.ui.components.classroom.ProgressItemCard
import com.example.app.ui.components.classroom.ProgressStatus
import com.example.app.ui.components.classroom.TipCard

@Composable
fun StudentDetailsScreen(
    studentId: String,
    studentName: String,
    className: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    classId: String = "",
    onEditStudent: () -> Unit = {},
    viewModel: ClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.studentDetailsUiState.collectAsState()
    
    // State for edit dialogs
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPfpDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Save image to internal storage
            val savedPath = ImageUtil.saveImageToInternalStorage(context, it, "profile")
            // Save immediately after selecting
            viewModel.updateStudent(
                studentId = studentId.toLongOrNull() ?: 0L,
                fullName = uiState.studentName,
                gradeLevel = uiState.gradeLevel,
                birthday = uiState.birthday,
                pfpPath = savedPath,
                onSuccess = {
                    // Reload student details
                    viewModel.loadStudentDetails(
                        studentId = studentId.toLongOrNull() ?: 0L,
                        classId = classId.toLongOrNull()
                    )
                    showEditPfpDialog = false
                },
                onError = { error ->
                    // TODO: Show error toast/snackbar
                }
            )
        }
    }
    
    // Load student details when screen is first composed
    LaunchedEffect(studentId, classId) {
        // classId may be empty when navigating from the top-level ClassScreen; pass null in that case
        viewModel.loadStudentDetails(
            studentId = studentId.toLongOrNull() ?: 0L,
            classId = classId.toLongOrNull()
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
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
                color = Color.Red,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            // Student Name with Edit Icon - Centered in Blue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.studentName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3FA9F8)
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Student Name",
                    tint = Color(0xFF3FA9F8),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { 
                            editedName = uiState.studentName
                            showEditNameDialog = true 
                        }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Class Name - Centered
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.className,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0B0B0B)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Profile Picture with Edit Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    uiState.pfpPath?.let { pfp ->
                        AsyncImage(
                            model = java.io.File(pfp),
                            contentDescription = "Student Profile",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.dis_default_pfp)
                        )
                    } ?: run {
                        Image(
                            painter = painterResource(id = R.drawable.dis_default_pfp),
                            contentDescription = "Student Profile",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Edit icon overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp)
                            .size(40.dp)
                            .background(Color(0xFF3FA9F8), CircleShape)
                            .clickable { showEditPfpDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

        // Analytics Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_analytics),
                contentDescription = "Analytics",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Analytics Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AnalyticsCard(
                iconRes = R.drawable.ic_time,
                value = "${uiState.totalPracticeMinutes} mins",
                label = "Total Practice Time",
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                iconRes = R.drawable.ic_sessions,
                value = uiState.sessionsCompleted.toString(),
                label = "Sessions Completed",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Progress Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_progress),
                contentDescription = "Progress",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Progress",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Progress Items
        ProgressItemCard(
            iconRes = R.drawable.ic_apple,
            title = "Vowels",
            status = ProgressStatus.COMPLETED,
            progress = 1f
        )

        Spacer(Modifier.height(12.dp))

        ProgressItemCard(
            iconRes = R.drawable.ic_ball,
            title = "Consonants",
            status = ProgressStatus.IN_PROGRESS,
            progress = 0.6f
        )

        Spacer(Modifier.height(12.dp))

        ProgressItemCard(
            iconRes = R.drawable.ic_flower,
            title = "Stops",
            status = ProgressStatus.NOT_STARTED,
            progress = 0f
        )

        Spacer(Modifier.height(32.dp))

        // Kuu's Tips Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bulb),
                contentDescription = "Tips",
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kuu's Tips",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tip Cards - Always show with default messages if empty
        TipCard(
            title = uiState.firstTipTitle ?: "Keep Up the Great Work!",
            description = uiState.firstTipDescription ?: "You're doing an amazing job! Keep practicing regularly to build strong foundations. Every session brings you closer to mastery.",
            subtitle = uiState.firstTipSubtitle ?: "AI-Generated Suggestion",
            backgroundColor = Color(0xFFEDBB00)
        )
        
        Spacer(Modifier.height(12.dp))

        TipCard(
            title = uiState.secondTipTitle ?: "Practice Makes Progress!",
            description = uiState.secondTipDescription ?: "Remember, learning is a journey! Stay consistent with your practice sessions and celebrate every small achievement along the way.",
            subtitle = uiState.secondTipSubtitle ?: "Based on your recent session",
            backgroundColor = Color(0xFF9067F7)
        )

        Spacer(Modifier.height(100.dp))
        }
    }
    
    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Edit Student Name",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3FA9F8)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the new name for this student:",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        placeholder = {
                            Text(
                                text = "Student Name",
                                color = Color(0xFF999999)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF3FA9F8),
                            unfocusedIndicatorColor = Color(0xFF3FA9F8),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            viewModel.updateStudent(
                                studentId = studentId.toLongOrNull() ?: 0L,
                                fullName = editedName,
                                gradeLevel = uiState.gradeLevel,
                                birthday = uiState.birthday,
                                pfpPath = uiState.pfpPath,
                                onSuccess = {
                                    // Reload student details
                                    viewModel.loadStudentDetails(
                                        studentId = studentId.toLongOrNull() ?: 0L,
                                        classId = classId.toLongOrNull()
                                    )
                                    showEditNameDialog = false
                                },
                                onError = { error ->
                                    // TODO: Show error toast/snackbar
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3FA9F8)
                    ),
                    enabled = editedName.isNotBlank()
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = Color(0xFF3FA9F8))
                }
            }
        )
    }
    
    // Edit Profile Picture Dialog
    if (showEditPfpDialog) {
        AlertDialog(
            onDismissRequest = { showEditPfpDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Edit Profile Picture",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3FA9F8)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose an option:",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(Modifier.height(20.dp))
                    
                    // Upload New Picture Button
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3FA9F8)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Upload New Picture",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Remove Picture Button (only show if there's a custom picture)
                    if (uiState.pfpPath != null) {
                        OutlinedButton(
                            onClick = {
                                // Delete the old image file
                                uiState.pfpPath?.let { path ->
                                    ImageUtil.deleteImage(path)
                                }
                                
                                viewModel.updateStudent(
                                    studentId = studentId.toLongOrNull() ?: 0L,
                                    fullName = uiState.studentName,
                                    gradeLevel = uiState.gradeLevel,
                                    birthday = uiState.birthday,
                                    pfpPath = null,
                                    onSuccess = {
                                        // Reload student details
                                        viewModel.loadStudentDetails(
                                            studentId = studentId.toLongOrNull() ?: 0L,
                                            classId = classId.toLongOrNull()
                                        )
                                        showEditPfpDialog = false
                                    },
                                    onError = { error ->
                                        // TODO: Show error toast/snackbar
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5252))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Remove Picture",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEditPfpDialog = false }) {
                    Text("Cancel", color = Color(0xFF3FA9F8))
                }
            }
        )
    }
}
