package com.example.app.ui.feature.classroom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.components.common.ErrorDialog
import com.example.app.util.ImageUtil
import com.example.app.data.SessionManager

/**
 * Data class to hold student form data
 */
private data class StudentFormData(
    val firstName: String = "",
    val lastName: String = "",
    val profileImageUri: Uri? = null,
    val profileImagePath: String? = null
)

@Composable
fun AddStudentScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onStudentAdded: (studentName: String, studentCount: Int) -> Unit,
    viewModel: ClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    
    // List of students to add
    var students by remember { mutableStateOf(listOf(StudentFormData())) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateStudentName by remember { mutableStateOf("") }
    
    // Track which student card is picking an image
    var currentPickingIndex by remember { mutableIntStateOf(-1) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (currentPickingIndex >= 0 && currentPickingIndex < students.size) {
                val imagePath = ImageUtil.saveImageToInternalStorage(context, it, "profile")
                students = students.toMutableList().apply {
                    this[currentPickingIndex] = this[currentPickingIndex].copy(
                        profileImageUri = it,
                        profileImagePath = imagePath
                    )
                }
            }
        }
        currentPickingIndex = -1
    }

    // Check if at least one student has valid data
    val isFormValid = students.any { it.firstName.isNotBlank() && it.lastName.isNotBlank() }
    var isAdding by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(40.dp))

                // Back Button
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color(0xFF3FA9F8),
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 10.dp)
                        .clickable { onNavigateBack() }
                )

                Spacer(Modifier.height(28.dp))

                // Title - Centered
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add New Students",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B0B0B)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Student Cards
                students.forEachIndexed { index, studentData ->
                    StudentCard(
                        studentNumber = index + 1,
                        firstName = studentData.firstName,
                        lastName = studentData.lastName,
                        profileImageUri = studentData.profileImageUri,
                        onFirstNameChange = { newFirstName ->
                            students = students.toMutableList().apply {
                                this[index] = this[index].copy(firstName = newFirstName)
                            }
                        },
                        onLastNameChange = { newLastName ->
                            students = students.toMutableList().apply {
                                this[index] = this[index].copy(lastName = newLastName)
                            }
                        },
                        onImageClick = {
                            currentPickingIndex = index
                            imagePickerLauncher.launch("image/*")
                        },
                        onRemoveImage = {
                            students = students.toMutableList().apply {
                                this[index] = this[index].copy(
                                    profileImageUri = null,
                                    profileImagePath = null
                                )
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(16.dp))
                }

                // Add More Student Button
                AddMoreStudentButton(
                    studentsToAdd = 1,
                    onClick = {
                        students = students + StudentFormData()
                    }
                )

                Spacer(Modifier.height(24.dp))
            }

            // Snackbar host for errors
            Box(modifier = Modifier.fillMaxWidth()) {
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
            }

            PrimaryButton(
                text = if (isAdding) "Adding..." else "Add Students",
                onClick = {
                    if (!isFormValid || isAdding) return@PrimaryButton
                    
                    // Get valid students only
                    val validStudents = students.filter { 
                        it.firstName.isNotBlank() && it.lastName.isNotBlank() 
                    }
                    
                    // Check for duplicates
                    for (student in validStudents) {
                        val fullName = "${student.firstName.trim()} ${student.lastName.trim()}"
                        val isDuplicate = allStudents.any { existingStudent ->
                            existingStudent.fullName.equals(fullName, ignoreCase = true)
                        }
                        if (isDuplicate) {
                            duplicateStudentName = fullName
                            showDuplicateDialog = true
                            return@PrimaryButton
                        }
                    }
                    
                    isAdding = true
                    val sessionManager = SessionManager.getInstance(context)
                    val currentUserId = sessionManager.getUserId()
                    val teacherIds = if (currentUserId > 0L) listOf(currentUserId) else emptyList()

                    // Add all valid students
                    var addedCount = 0
                    var lastAddedName = ""
                    
                    validStudents.forEach { student ->
                        val fullName = "${student.firstName.trim()} ${student.lastName.trim()}"
                        viewModel.addStudentWithTeachers(
                            fullName = fullName,
                            gradeLevel = "",
                            pfpPath = student.profileImagePath,
                            teacherIds = teacherIds,
                            onSuccess = { _: Long ->
                                addedCount++
                                lastAddedName = fullName
                                            if (addedCount == validStudents.size) {
                                                    isAdding = false
                                                    onStudentAdded(lastAddedName, addedCount)
                                                }
                            },
                            onError = { error: String ->
                                isAdding = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    }
                },
                enabled = isFormValid && !isAdding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
        }
        
        // Duplicate student error dialog
        ErrorDialog(
            isVisible = showDuplicateDialog,
            title = "Student Already Exists!",
            message = "A student with the name \"$duplicateStudentName\" already exists.",
            buttonText = "OK",
            onDismiss = { showDuplicateDialog = false }
        )
    }
}

@Composable
private fun StudentCard(
    studentNumber: Int,
    firstName: String,
    lastName: String,
    profileImageUri: Uri?,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onRemoveImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Student label
            Text(
                text = "STUDENT $studentNumber",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9E9E9E),
                letterSpacing = 1.sp
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture with dashed border
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .dashedBorder(
                            color = Color(0xFF90CAF9),
                            shape = RoundedCornerShape(12.dp),
                            strokeWidth = 2.dp,
                            dashLength = 8.dp,
                            gapLength = 4.dp
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE3F2FD))
                        .clickable { onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Student Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.dis_pfp),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    // Plus/Close button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF90CAF9))
                            .clickable {
                                if (profileImageUri != null) {
                                    onRemoveImage()
                                } else {
                                    onImageClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (profileImageUri != null) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (profileImageUri != null) "Remove Picture" else "Add Picture",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                // Name fields - same height as image, centered vertically
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // First Name TextField
                    TextField(
                        value = firstName,
                        onValueChange = onFirstNameChange,
                        placeholder = {
                            Text(
                                text = "First Name",
                                color = Color(0xFFBDBDBD),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFFE0E0E0),
                            unfocusedIndicatorColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    
                    // Last Name TextField
                    TextField(
                        value = lastName,
                        onValueChange = onLastNameChange,
                        placeholder = {
                            Text(
                                text = "Last Name",
                                color = Color(0xFFBDBDBD),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFFE0E0E0),
                            unfocusedIndicatorColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMoreStudentButton(
    studentsToAdd: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(
                color = Color(0xFF90CAF9),
                shape = RoundedCornerShape(24.dp),
                strokeWidth = 2.dp,
                dashLength = 8.dp,
                gapLength = 4.dp
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF3FA9F8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add $studentsToAdd More Student",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3FA9F8)
            )
        }
    }
}

/**
 * Extension function to create a dashed border
 */
private fun Modifier.dashedBorder(
    color: Color,
    shape: RoundedCornerShape,
    strokeWidth: Dp,
    dashLength: Dp,
    gapLength: Dp
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), gapLength.toPx())
            )
        )
        val cornerRadius = shape.topStart
        drawRoundRect(
            color = color,
            style = stroke,
            cornerRadius = CornerRadius(
                (cornerRadius as androidx.compose.foundation.shape.CornerSize).toPx(size, this)
            )
        )
    }
)

@Preview(showBackground = true)
@Composable
fun AddStudentScreenPreview() {
    AddStudentScreen(
        onNavigateBack = {},
        onStudentAdded = { _, _ -> }
    )
}