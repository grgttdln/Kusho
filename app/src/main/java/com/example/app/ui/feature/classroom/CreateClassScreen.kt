package com.example.app.ui.feature.classroom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
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
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.PrimaryButton

@Composable
fun CreateClassScreen(
    onNavigateBack: () -> Unit,
    onClassCreated: (className: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var className by remember { mutableStateOf("") }
    var classCode by remember { mutableStateOf("") }
    var selectedBannerRes by remember { mutableStateOf<Int?>(null) }
    var customBannerUri by remember { mutableStateOf<Uri?>(null) }
    var showBannerPicker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customBannerUri = it
            selectedBannerRes = null // Clear preset selection
        }
    }

    val isFormValid = className.isNotBlank() && classCode.isNotBlank()

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
                Spacer(Modifier.height(45.dp))

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

            // Title - Centered like "Your Classes"
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create a Class",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0B0B0B)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Banner/Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(169.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedBannerRes != null) Color.Transparent
                        else Color(0xFFE3F2FD)
                    )
                    .clickable { showBannerPicker = true },
                contentAlignment = Alignment.Center
            ) {
                when {
                    customBannerUri != null -> {
                        AsyncImage(
                            model = customBannerUri,
                            contentDescription = "Custom Class Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    selectedBannerRes != null -> {
                        Image(
                            painter = painterResource(id = selectedBannerRes!!),
                            contentDescription = "Class Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_banner),
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Add a Class Banner",
                                fontSize = 16.sp,
                                color = Color(0xFF64B5F6),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Plus button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3FA9F8))
                        .clickable { showBannerPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Banner",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Class Name TextField
            TextField(
                value = className,
                onValueChange = { className = it },
                placeholder = {
                    Text(
                        text = "Add Class Name",
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

            // Class Code TextField
            TextField(
                value = classCode,
                onValueChange = { classCode = it },
                placeholder = {
                    Text(
                        text = "Add Class Code",
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

            // Create New Class Button - At Bottom
            PrimaryButton(
                text = "Create New Class",
                onClick = {
                    if (isFormValid) {
                        onClassCreated(className)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
        }
    }

    // Banner Picker Modal
    if (showBannerPicker) {
        BannerPickerModal(
            onDismiss = { showBannerPicker = false },
            onBannerSelected = { bannerRes ->
                selectedBannerRes = bannerRes
                customBannerUri = null // Clear custom image
                showBannerPicker = false
            },
            onUploadCustom = {
                showBannerPicker = false
                imagePickerLauncher.launch("image/*")
            }
        )
    }
}

@Composable
fun BannerPickerModal(
    onDismiss: () -> Unit,
    onBannerSelected: (Int) -> Unit,
    onUploadCustom: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "Choose Class Banner",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FA9F8)
            )
        },
        text = {
            Column {
                Text(
                    text = "Select a default banner:",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(Modifier.height(16.dp))

                // Banner options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ABC Banner
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onBannerSelected(R.drawable.ic_class_abc) }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_class_abc),
                            contentDescription = "ABC Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Stars Banner
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onBannerSelected(R.drawable.ic_class_stars) }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_class_stars),
                            contentDescription = "Stars Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Upload Custom Banner Button
                Button(
                    onClick = onUploadCustom,
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
                        text = "Upload Custom Banner",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF3FA9F8))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CreateClassScreenPreview() {
    CreateClassScreen(
        onNavigateBack = {},
        onClassCreated = {}
    )
}
