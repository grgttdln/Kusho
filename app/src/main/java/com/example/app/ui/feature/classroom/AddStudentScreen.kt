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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.util.ImageUtil
import com.example.app.data.SessionManager

@Composable
fun AddStudentScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onStudentAdded: (studentName: String) -> Unit,
    viewModel: ClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImagePath by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            // Save image to internal storage
            profileImagePath = ImageUtil.saveImageToInternalStorage(context, it, "profile")
        }
    }

    val isFormValid = firstName.isNotBlank() && lastName.isNotBlank() && gradeLevel.isNotBlank()
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

                // Back Button - positioned like Kusho logo
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
                        text = "Add a New Student",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B0B0B)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Profile Picture Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(169.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD))
                            .clickable { imagePickerLauncher.launch("image/*") },
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.dis_pfp),
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Add Profile Picture",
                                    fontSize = 16.sp,
                                    color = Color(0xFF64B5F6),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // Plus/Close button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3FA9F8))
                                .clickable {
                                    if (profileImageUri != null) {
                                        profileImageUri = null
                                    } else {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (profileImageUri != null) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (profileImageUri != null) "Remove Picture" else "Add Picture",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Student First Name TextField
                TextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = {
                        Text(
                            text = "Enter Student's First Name",
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

                Spacer(Modifier.height(12.dp))

                // Student Last Name TextField
                TextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = {
                        Text(
                            text = "Enter Student's Last Name",
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

                Spacer(Modifier.height(20.dp))

                // Grade Level TextField
                TextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    placeholder = {
                        Text(
                            text = "Enter Student's Grade Level",
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

                Spacer(Modifier.height(24.dp))
            }

            // Snackbar host for errors
            Box(modifier = Modifier.fillMaxWidth()) {
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
            }

            PrimaryButton(
                text = if (isAdding) "Adding..." else "Add A New Student",
                onClick = {
                    if (!isFormValid || isAdding) return@PrimaryButton
                    isAdding = true
                    val fullName = "${firstName.trim()} ${lastName.trim()}"
                    // Auto-assign current logged-in user as a teacher for this student
                    val sessionManager = SessionManager.getInstance(context)
                    val currentUserId = sessionManager.currentUser.value?.id ?: sessionManager.getUserId()
                    val teacherIds = if (currentUserId > 0L) listOf(currentUserId) else emptyList()

                    viewModel.addStudentWithTeachers(
                        fullName = fullName,
                        gradeLevel = gradeLevel,
                        pfpPath = profileImagePath,
                        teacherIds = teacherIds,
                        onSuccess = { studentId: Long ->
                            isAdding = false
                            onStudentAdded(fullName)
                        },
                        onError = { error: String ->
                            isAdding = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    )
                },
                enabled = isFormValid && !isAdding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddStudentScreenPreview() {
    AddStudentScreen(
        onNavigateBack = {},
        onStudentAdded = {}
    )
}