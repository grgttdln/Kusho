package com.example.app.ui.feature.classroom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.components.classroom.ArchiveClassDialog
import com.example.app.util.ImageUtil

@Composable
fun EditClassScreen(
    classId: String,
    initialClassName: String,
    initialClassCode: String,
    initialBannerPath: String? = null,
    onNavigateBack: () -> Unit,
    onSaveChanges: (String, String) -> Unit,
    onArchiveClass: () -> Unit,
    viewModel: ClassroomViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var className by remember { mutableStateOf(initialClassName) }
    var classCode by remember { mutableStateOf(initialClassCode) }
    
    // Parse initial banner - check if it's a drawable resource, file path, or null
    val (initBannerRes, initBannerPath) = remember(initialBannerPath) {
        when {
            initialBannerPath == null -> Pair(null, null)
            initialBannerPath.startsWith("drawable://") -> {
                val resName = initialBannerPath.removePrefix("drawable://")
                val resId = when (resName) {
                    "ic_class_abc" -> R.drawable.ic_class_abc
                    "ic_class_stars" -> R.drawable.ic_class_stars
                    else -> null
                }
                Pair(resId, null)
            }
            else -> Pair(null, initialBannerPath) // It's a file path
        }
    }
    
    var selectedBannerRes by remember { mutableStateOf<Int?>(initBannerRes) }
    var customBannerUri by remember { mutableStateOf<Uri?>(null) }
    var customBannerPath by remember { mutableStateOf<String?>(initBannerPath) }
    var showBannerPicker by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customBannerUri = it
            // Save image to internal storage
            customBannerPath = ImageUtil.saveImageToInternalStorage(context, it, "banner")
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
                Spacer(Modifier.height(24.dp))

                // Header with back button
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF3FA9F8)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Title - Centered like "Your Classes"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit a Class",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B0B0B)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Banner Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(169.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedBannerRes != null || customBannerUri != null) Color.Transparent
                            else Color(0xFFE3F2FD)
                        )
                        .clickable { showBannerPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    // Display image
                    when {
                        customBannerUri != null -> {
                            AsyncImage(
                                model = customBannerUri,
                                contentDescription = "Custom Class Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        customBannerPath != null -> {
                            AsyncImage(
                                model = java.io.File(customBannerPath),
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
                            contentDescription = "Change Banner",
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
                            text = "Edit Class Name",
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
                            text = "Edit Class Code",
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

            // Save Changes Button - At Bottom
            PrimaryButton(
                text = "Save Changes",
                onClick = {
                    if (isFormValid) {
                        // Determine banner path: saved file path, drawable resource, or null
                        val bannerPath = when {
                            customBannerPath != null -> customBannerPath
                            selectedBannerRes != null -> {
                                // Save drawable resource ID as special string format
                                when (selectedBannerRes) {
                                    R.drawable.ic_class_abc -> "drawable://ic_class_abc"
                                    R.drawable.ic_class_stars -> "drawable://ic_class_stars"
                                    else -> null
                                }
                            }
                            else -> null
                        }
                        
                        viewModel.updateClass(
                            classId = classId.toLongOrNull() ?: 0L,
                            className = className,
                            classCode = classCode,
                            bannerPath = bannerPath,
                            onSuccess = {
                                showSuccessDialog = true
                            },
                            onError = { error ->
                                // TODO: Show error toast/snackbar
                            }
                        )
                        onSaveChanges(className, classCode)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            )

            // Archive Class Button (Outlined)
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    showArchiveDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF49A9FF))
            ) {
                Text(
                    text = "Archive Class",
                    fontSize = 20.sp,
                    color = Color(0xFF49A9FF)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Banner Picker Modal
    if (showBannerPicker) {
        BannerPickerModal(
            onDismiss = { showBannerPicker = false },
            onBannerSelected = { bannerRes: Int ->
                selectedBannerRes = bannerRes
                customBannerUri = null // Clear custom image URI
                customBannerPath = null // Clear custom image path so default banner displays
                showBannerPicker = false
            },
            onUploadCustom = {
                showBannerPicker = false
                imagePickerLauncher.launch("image/*")
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        EditClassSuccessDialog(
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
            }
        )
    }
    
    // Archive Confirmation Dialog
    if (showArchiveDialog) {
        ArchiveClassDialog(
            className = className,
            onConfirm = {
                viewModel.archiveClass(
                    classId = classId.toLongOrNull() ?: 0L,
                    onSuccess = {
                        showArchiveDialog = false
                        onArchiveClass()
                    },
                    onError = { error ->
                        // TODO: Show error toast/snackbar
                        showArchiveDialog = false
                    }
                )
            },
            onDismiss = {
                showArchiveDialog = false
            }
        )
    }
}

@Composable
fun EditClassSuccessDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Confetti Animation (reuse from ClassCreatedSuccessScreen)
            ConfettiAnimationEdit()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Success Mascot - dis_study.png
                    Image(
                        painter = painterResource(id = R.drawable.dis_study),
                        contentDescription = "Success",
                        modifier = Modifier
                            .size(350.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.height(1.dp))

                    // Success Title
                    Text(
                        text = "Saved Changes",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Success Message
                    Text(
                        text = "Edit Successfully\nChanged",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }

                // Great Button - At Bottom
                PrimaryButton(
                    text = "Great!",
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ConfettiAnimationEdit() {
    val confettiColors = listOf(
        Color(0xFFFFC107), // Yellow
        Color(0xFFE91E63), // Pink
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF5722), // Red-Orange
        Color(0xFF9C27B0)  // Purple
    )

    // Create 20 confetti pieces
    for (i in 0..19) {
        ConfettiPieceEdit(
            color = confettiColors.random(),
            startX = kotlin.random.Random.nextFloat(),
            delay = kotlin.random.Random.nextInt(0, 500)
        )
    }
}

@Composable
fun ConfettiPieceEdit(
    color: Color,
    startX: Float,
    delay: Int
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "confetti")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 3000 + kotlin.random.Random.nextInt(-500, 500),
                easing = androidx.compose.animation.core.LinearEasing,
                delayMillis = delay
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "confettiY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1000 + kotlin.random.Random.nextInt(-200, 200),
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "confettiRotation"
    )

    val offsetX by infiniteTransition.animateFloat(
        initialValue = startX * 400f,
        targetValue = startX * 400f + kotlin.random.Random.nextFloat() * 100f - 50f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 2000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "confettiX"
    )

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(12.dp)
            .graphicsLayer {
                rotationZ = rotation
            }
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawRect(color = color)
        }
    }
}
