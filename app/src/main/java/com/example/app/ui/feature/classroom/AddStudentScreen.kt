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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
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
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.util.ImageUtil

@Composable
fun AddStudentScreen(
    classId: String,
    className: String = "Grade 1 Bright Sparks",
    onNavigateBack: () -> Unit,
    onStudentAdded: (studentName: String) -> Unit,
    viewModel: ClassroomViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var studentName by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
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

    val isFormValid = studentName.isNotBlank() && gradeLevel.isNotBlank() && birthday.isNotBlank()

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
                    imageVector = Icons.Default.KeyboardArrowLeft,
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

                // Class Name TextField (Read-only)
                TextField(
                    value = className,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        disabledIndicatorColor = Color(0xFF3FA9F8),
                        disabledContainerColor = Color.Transparent,
                        disabledTextColor = Color(0xFF666666)
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))

                // Student Name TextField
                TextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    placeholder = {
                        Text(
                            text = "Enter Student's Name",
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

                Spacer(Modifier.height(20.dp))

                // Birthday TextField
                TextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    placeholder = {
                        Text(
                            text = "Enter Student's Birthday",
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

            // Add A New Student Button - At Bottom
            PrimaryButton(
                text = "Add A New Student",
                onClick = {
                    if (isFormValid) {
                        viewModel.addStudentToClass(
                            fullName = studentName,
                            gradeLevel = gradeLevel,
                            birthday = birthday,
                            pfpPath = profileImagePath,
                            classId = classId.toLongOrNull() ?: 0L,
                            onSuccess = { studentId ->
                                onStudentAdded(studentName)
                            },
                            onError = { error ->
                                // TODO: Show error toast/snackbar
                            }
                        )
                    }
                },
                enabled = isFormValid,
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
        classId = "1",
        className = "Preview Class",
        onNavigateBack = {},
        onStudentAdded = {}
    )
}
