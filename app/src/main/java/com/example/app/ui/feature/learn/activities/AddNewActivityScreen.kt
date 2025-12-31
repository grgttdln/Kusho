package com.example.app.ui.feature.learn.activities

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.activities.AddedChapterPill
import com.example.app.ui.components.activities.ChapterCard

@Composable
fun AddNewActivityScreen(
    userId: Long,
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onActivityCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState(initial = AddActivityUiState())
    
    // Use local references to prevent unnecessary recompositions
    val activityTitle = uiState.activityTitle
    val addedChapters = uiState.selectedChapters

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // Handle selected image if needed for cover
        // For now, we'll just acknowledge the selection
    }

    // Memoize colors to avoid reallocation on every recomposition
    val primaryColor = remember { Color(0xFF3FA9F8) }
    val lightBg = remember { Color(0xFFF0F8FE) }
    val darkText = remember { Color(0xFF0B0B0B) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Back Button and Kusho Logo - Same Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF3FA9F8)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_kusho),
                    contentDescription = "Kusho Logo",
                    modifier = Modifier
                        .height(54.dp)
                        .weight(1f)
                        .padding(horizontal = 30.dp)
                        .offset(x = 10.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title "Add a New Activity"
            Text(
                text = "Add a New Activity",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF000000),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Upload Cover Area (Optional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(horizontal = 15.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF3FA9F8),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF0F8FE))
                    .clickable {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Add Icon Circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload",
                            tint = Color(0xFF3FA9F8),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Upload Cover",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF3FA9F8),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "(Optional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF3FA9F8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Add a Lesson Title
            Text(
                text = "Add a Lesson Title",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lesson Title Input Field
            OutlinedTextField(
                value = activityTitle,
                onValueChange = { viewModel.setActivityTitle(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 15.dp),
                placeholder = {
                    Text(
                        text = "E.g, Tail letters",
                        fontSize = 16.sp,
                        color = Color(0xFFC5E5FD),
                        fontWeight = FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color(0x803FA9F8),
                    focusedIndicatorColor = Color(0xFF3FA9F8),
                    focusedTextColor = Color(0xFF000000),
                    unfocusedTextColor = Color(0xFF000000),
                    cursorColor = Color(0xFF3FA9F8)
                ),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = Color(0xFF000000)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Added Chapters or Add Chapters section
            if (addedChapters.isNotEmpty()) {
                // Show Added Chapters
                Text(
                    text = "Added Chapters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    addedChapters.forEachIndexed { index, chapter ->
                        AddedChapterPill(
                            number = index + 1,
                            title = chapter.title,
                            itemCount = chapter.itemCount,
                            onRemove = {
                                // Remove from ViewModel
                                viewModel.removeChapter(index)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add More Sets Button
                Button(
                    onClick = { onNavigate(9) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = Color(0xFF3FA9F8)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add More",
                        tint = Color(0xFF3FA9F8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add More Sets",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF3FA9F8)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                // Show Add Chapters
                Text(
                    text = "Add Chapters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Chapter Cards Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chapter 1
                    ChapterCard(
                        modifier = Modifier.size(82.dp),
                        onClick = { onNavigate(9) }
                    )

                    // Chapter 2
                    ChapterCard(
                        modifier = Modifier.size(82.dp),
                        onClick = { onNavigate(9) }
                    )

                    // Chapter 3
                    ChapterCard(
                        modifier = Modifier.size(82.dp),
                        onClick = { onNavigate(9) }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Create Activity Button
        Button(
            onClick = { 
                if (viewModel.createActivity(userId)) {
                    onNavigate(10) // Navigate to confirmation screen
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .fillMaxWidth(0.86f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8),
                disabledContainerColor = Color(0xFFB3E5FC),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp),
            enabled = activityTitle.isNotBlank() && addedChapters.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                Text(
                    text = "Creating...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Create Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Display error message if any
        if (!uiState.errorMessage.isNullOrEmpty()) {
            LaunchedEffect(uiState.errorMessage) {
                // Error is shown - you can add a snackbar or toast here
            }
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddNewActivityScreenPreview() {
    AddNewActivityScreen(
        userId = 1L,
        onNavigate = {},
        onBackClick = {},
        onActivityCreated = {}
    )
}
