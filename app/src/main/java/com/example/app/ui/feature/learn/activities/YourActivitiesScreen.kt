package com.example.app.ui.feature.learn.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.SessionManager
import com.example.app.data.entity.Activity
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.DeleteConfirmationDialog
import com.example.app.ui.components.DeleteType
import com.example.app.ui.components.activities.ActivityItemCard

@Composable
fun YourActivitiesScreen(
    onNavigate: (Int) -> Unit,
    onNavigateToSets: (activityId: Long, activityTitle: String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: YourActivitiesViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val userId = remember { sessionManager.getUserId() }
    val uiState by viewModel.uiState.collectAsState()

    // Edit mode state for delete functionality
    var isEditMode by remember { mutableStateOf(false) }
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId > 0) {
            viewModel.loadActivities(userId)
        }
    }

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
        // Adjust for 1-based IDs to ensure ic_activity_1 is used
        // Activity ID 1 → Index 0 → ic_activity_1
        // Activity ID 2 → Index 1 → ic_activity_2, etc.
        val iconIndex = ((activityId - 1) % allIcons.size).toInt()
        return allIcons[iconIndex]
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Back Button, Kusho Logo (centered), and Edit Button
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Button (left)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF3FA9F8)
                    )
                }

                // Kusho Logo (centered)
                Image(
                    painter = painterResource(id = R.drawable.ic_kusho),
                    contentDescription = "Kusho Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .offset(x = 10.dp)
                        .align(Alignment.Center),
                    alignment = Alignment.Center
                )

                // Edit Mode Button (right) - switches between Edit and Delete icons
                IconButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Delete else Icons.Default.Edit,
                        contentDescription = if (isEditMode) "Exit Edit Mode" else "Edit Mode",
                        tint = if (isEditMode) Color(0xFFFF6B6B) else Color(0xFF3FA9F8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Your Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Activity Cards or Loading/Empty State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF3FA9F8)
                    )
                }
            } else if (uiState.activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dis_none),
                            contentDescription = "No activities mascot",
                            modifier = Modifier.size(240.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "No Activities Yet",
                            color = Color(0xFF4A4A4A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tap the button below to create\nyour first activity.",
                            color = Color(0xFF7A7A7A),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(
                        items = uiState.activities,
                        key = { it.id }
                    ) { activity ->
                        // Get persistent icon for this activity based on its ID
                        val activityIcon = getIconForActivity(activity.id)

                        // Check if this activity is selected for deletion
                        val isSelected = activityToDelete?.id == activity.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected && isEditMode) {
                                        Modifier
                                            .border(
                                                width = 3.dp,
                                                color = Color(0xFF3FA9F8),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .background(
                                                color = Color(0x203FA9F8),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            ActivityItemCard(
                                title = activity.title,
                                description = activity.description,
                                iconRes = activityIcon,
                                onClick = {
                                    if (isEditMode) {
                                        activityToDelete = activity
                                        showDeleteDialog = true
                                    } else {
                                        onNavigateToSets(activity.id, activity.title)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Floating Action Button
        Button(
            onClick = { onNavigate(8) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .height(75.dp)
                .width(180.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8)
            ),
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Delete Confirmation Dialog
        DeleteConfirmationDialog(
            isVisible = showDeleteDialog && activityToDelete != null,
            deleteType = DeleteType.ACTIVITY,
            onConfirm = {
                activityToDelete?.let { activity ->
                    viewModel.deleteActivity(activity.id, userId)
                }
                showDeleteDialog = false
                activityToDelete = null
                isEditMode = false
            },
            onDismiss = {
                showDeleteDialog = false
                activityToDelete = null
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun YourActivitiesScreenPreview() {
    YourActivitiesScreen(
        onNavigate = {},
        onBackClick = {}
    )
}
