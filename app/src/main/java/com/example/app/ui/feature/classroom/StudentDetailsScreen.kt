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
import com.example.app.ui.components.classroom.LearnAnnotationCard
import com.example.app.ui.components.classroom.ProgressItemCard
import com.example.app.ui.components.classroom.ProgressStatus
import com.example.app.ui.components.classroom.TipCard
import com.example.app.ui.components.classroom.TutorialAnnotationCard

@Composable
fun StudentDetailsScreen(
    studentId: String,
    studentName: String,
    className: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    classId: String = "",
    onEditStudent: () -> Unit = {},
    onNavigateToTutorialAnnotation: (String) -> Unit = {},
    onNavigateToLearnAnnotation: (String) -> Unit = {},
    viewModel: ClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.studentDetailsUiState.collectAsState()

    // Activity icons (same as LearnModeActivitySelectionScreen)
    val allIcons = remember {
        listOf(
            R.drawable.ic_activity_1, R.drawable.ic_activity_2, R.drawable.ic_activity_3, R.drawable.ic_activity_4,
            R.drawable.ic_activity_5, R.drawable.ic_activity_6, R.drawable.ic_activity_7, R.drawable.ic_activity_8,
            R.drawable.ic_activity_9, R.drawable.ic_activity_10, R.drawable.ic_activity_11, R.drawable.ic_activity_12,
            R.drawable.ic_activity_13, R.drawable.ic_activity_14, R.drawable.ic_activity_15, R.drawable.ic_activity_16,
            R.drawable.ic_activity_17, R.drawable.ic_activity_18, R.drawable.ic_activity_19, R.drawable.ic_activity_20,
            R.drawable.ic_activity_21, R.drawable.ic_activity_22
        )
    }
    fun getIconForActivity(activityId: Long): Int {
        val iconIndex = ((activityId - 1) % allIcons.size).toInt()
        return allIcons[iconIndex]
    }
    
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

        // Header with back button
        Box(
            modifier = Modifier.fillMaxWidth()
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

            Spacer(Modifier.height(24.dp))

            // Dominant Hand Section
            Text(
                text = "Dominant Hand",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Hand Button
                OutlinedButton(
                    onClick = {
                        viewModel.updateStudentDominantHand(
                            studentId = studentId.toLongOrNull() ?: 0L,
                            dominantHand = "LEFT",
                            onSuccess = {
                                viewModel.loadStudentDetails(
                                    studentId = studentId.toLongOrNull() ?: 0L,
                                    classId = classId.toLongOrNull()
                                )
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.dominantHand == "LEFT") Color(0xFF3FA9F8) else Color.White,
                        contentColor = if (uiState.dominantHand == "LEFT") Color.White else Color(0xFF3FA9F8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (uiState.dominantHand == "LEFT") Color(0xFF3FA9F8) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "Left",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Right Hand Button
                OutlinedButton(
                    onClick = {
                        viewModel.updateStudentDominantHand(
                            studentId = studentId.toLongOrNull() ?: 0L,
                            dominantHand = "RIGHT",
                            onSuccess = {
                                viewModel.loadStudentDetails(
                                    studentId = studentId.toLongOrNull() ?: 0L,
                                    classId = classId.toLongOrNull()
                                )
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.dominantHand == "RIGHT") Color(0xFF3FA9F8) else Color.White,
                        contentColor = if (uiState.dominantHand == "RIGHT") Color.White else Color(0xFF3FA9F8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (uiState.dominantHand == "RIGHT") Color(0xFF3FA9F8) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "Right",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(42.dp))

            // Kuu Card with Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dis_kuu_card),
                    contentDescription = "Kuu Card",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )

                // Overlay Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Main Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 80.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = "Try Consonants Tutorial!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "You're amazing at vowel tracing! Let's try consonants next.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }

                    // Start Tutorial Button
                    Button(
                        onClick = { /* TODO: Navigate to tutorial */ },
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Start Tutorial",
                            color = Color(0xFF3FA9F8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Annotations Section
            Text(
                text = "Annotations",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3FA9F8),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Tutorial Annotation Cards - Show completed tutorial sessions
            uiState.completedTutorialSessions.forEach { session ->
                val formattedDate = session.completedAt?.let { timestamp ->
                    val date = java.util.Date(timestamp)
                    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    formatter.format(date)
                } ?: "Jan 01, 2026"
                
                // Extract tags from strengths and challenges, removing duplicates
                val tags = session.annotation?.let { annotation ->
                    val strengthsList = annotation.getStrengthsList()
                    val challengesList = annotation.getChallengesList()
                    (strengthsList + challengesList).distinct().take(2)
                } ?: listOf("Fluency", "Recognition")
                
                val annotationText = session.annotation?.let { annotation ->
                    buildString {
                        if (annotation.strengthsNote.isNotBlank()) {
                            append(annotation.strengthsNote)
                        }
                        if (annotation.challengesNote.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(annotation.challengesNote)
                        }
                    }.takeIf { it.isNotBlank() }
                } ?: "${uiState.studentName} has completed the ${session.tutorialType} ${session.letterType.lowercase()} letters tutorial."
                
                TutorialAnnotationCard(
                    tags = tags,
                    annotation = annotationText,
                    tutorialName = "${session.tutorialType} | ${session.letterType}",
                    date = formattedDate,
                    iconRes = if (session.tutorialType == "Consonants") R.drawable.ic_ball else R.drawable.ic_apple,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { onNavigateToTutorialAnnotation("${session.tutorialType}|${session.letterType}|${session.setId}") }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Learn Annotation Cards - Show completed learn sets
            uiState.completedLearnSets.forEach { completedSet ->
                val formattedDate = completedSet.completedAt?.let { timestamp ->
                    val date = java.util.Date(timestamp)
                    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    formatter.format(date)
                } ?: "Jan 01, 2026"
                
                // Extract tags from strengths and challenges, removing duplicates
                // Only show tags if there are actual notes/annotations, not just for completion
                val tags = completedSet.annotation?.let { annotation ->
                    val hasNotes = annotation.strengthsNote.isNotBlank() || annotation.challengesNote.isNotBlank()
                    val hasTags = annotation.getStrengthsList().isNotEmpty() || annotation.getChallengesList().isNotEmpty()
                    
                    if (hasNotes || hasTags) {
                        val strengthsList = annotation.getStrengthsList()
                        val challengesList = annotation.getChallengesList()
                        (strengthsList + challengesList).distinct().take(2)
                    } else {
                        emptyList()
                    }
                } ?: emptyList()
                
                val annotationText = completedSet.annotation?.let { annotation ->
                    buildString {
                        if (annotation.strengthsNote.isNotBlank()) {
                            append(annotation.strengthsNote)
                        }
                        if (annotation.challengesNote.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(annotation.challengesNote)
                        }
                    }.takeIf { it.isNotBlank() }
                } ?: "${uiState.studentName} has completed this activity set."

                LearnAnnotationCard(
                    tags = tags,
                    annotation = annotationText,
                    lessonName = "${completedSet.activityName} | ${completedSet.setName}",
                    date = formattedDate,
                    iconRes = getIconForActivity(completedSet.activityId),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { onNavigateToLearnAnnotation("${completedSet.activityName}|${completedSet.setId}|${completedSet.activityId}") }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Empty state when no annotations
            if (uiState.completedTutorialSessions.isEmpty() && uiState.completedLearnSets.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Annotations Yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your notes and highlights will appear here.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF9CA3AF),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
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
