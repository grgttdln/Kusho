package com.example.app.ui.feature.learn.learnmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.learnmode.LearnModeActivityCard
import com.example.app.data.SessionManager

@Composable
fun LearnModeActivitySelectionScreen(
    studentId: Long,
    classId: Long,
    onBack: () -> Unit,
    onSelectActivity: (activityId: Long, activityTitle: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnModeActivitySelectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val userId = remember { sessionManager.getUserId() }
    val uiState by viewModel.uiState.collectAsState()

    // Default icons for activities (persistent assignment based on activity ID)
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

    // Function to get persistent icon for an activity based on its ID
    fun getIconForActivity(activityId: Long): Int {
        val iconIndex = ((activityId - 1) % allIcons.size).toInt()
        return allIcons[iconIndex]
    }

    LaunchedEffect(userId) {
        if (userId > 0) {
            viewModel.loadActivities(userId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 25.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2196F3)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_kusho),
                contentDescription = "Kusho Logo",
                modifier = Modifier
                    .height(54.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .offset(x = 10.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )


            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3FA9F8))
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "An error occurred",
                            color = Color(0xFF49A9FF),
                            fontSize = 16.sp
                        )
                    }
                }
                uiState.activities.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.offset(y = (-70).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dis_none),
                                contentDescription = "No activities mascot",
                                modifier = Modifier.size(240.dp),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "No Activities Yet",
                                color = Color(0xFF4A4A4A),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Create some activities first\nto get started!",
                                color = Color(0xFF7A7A7A),
                                fontSize = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(
                            items = uiState.activities,
                            key = { it.id }
                        ) { activity ->
                            LearnModeActivityCard(
                                title = activity.title,
                                iconRes = getIconForActivity(activity.id),
                                isSelected = uiState.selectedActivity?.id == activity.id,
                                onClick = { viewModel.toggleActivitySelection(activity) }
                            )
                        }
                    }
                }
            }
        }

        // Select Activity Button at the bottom
        Button(
            onClick = {
                uiState.selectedActivity?.let { activity ->
                    onSelectActivity(activity.id, activity.title)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = uiState.selectedActivity != null
        ) {
            Text(
                text = "Select Activity",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LearnModeActivitySelectionScreenPreview() {
    LearnModeActivitySelectionScreen(
        studentId = 1L,
        classId = 1L,
        onBack = {},
        onSelectActivity = { _, _ -> }
    )
}