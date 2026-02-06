package com.example.app.ui.feature.learn.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.entity.Set
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.DeleteConfirmationDialog
import com.example.app.ui.components.DeleteType

/**
 * Screen to display and manage sets within a specific activity.
 * This is separate from YourSetsScreen - changes here only affect the activity's set links,
 * not the original sets themselves.
 */
@Composable
fun ActivitySetsScreen(
    activityId: Long,
    activityTitle: String,
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onAddSetClick: () -> Unit = {},
    onViewSetClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ActivitySetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load sets for this activity when screen is displayed
    LaunchedEffect(activityId) {
        viewModel.loadSetsForActivity(activityId)
    }

    // Edit mode state for delete functionality
    var isEditMode by remember { mutableStateOf(false) }
    var setToUnlink by remember { mutableStateOf<Set?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            Spacer(Modifier.height(24.dp))

            // Back Button, Kusho Logo, and Edit Button
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

                // Edit/Delete mode toggle button (right)
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

            Spacer(Modifier.height(32.dp))

            // Title - Activity name + "Sets"
            Text(
                text = "$activityTitle Sets",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(Modifier.height(32.dp))

            // Sets or Loading/Empty State
            when {
                uiState.isLoading -> {
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
                }
                uiState.sets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sets in this activity.\nAdd sets to get started!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF808080),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(
                            items = uiState.sets,
                            key = { set: Set -> set.id }
                        ) { set: Set ->
                            // Check if this set is selected for deletion
                            val isSelected = setToUnlink?.id == set.id

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
                                ActivitySetCard(
                                    title = set.title,
                                    itemCount = set.itemCount,
                                    onClick = {
                                        if (isEditMode) {
                                            setToUnlink = set
                                            showDeleteDialog = true
                                        } else {
                                            onViewSetClick(set.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Floating "Add Sets" Button
        Button(
            onClick = onAddSetClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .width(207.dp)
                .height(75.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add Sets",
                fontSize = 20.sp,
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
            isVisible = showDeleteDialog && setToUnlink != null,
            deleteType = DeleteType.SET_FROM_ACTIVITY,
            onConfirm = {
                setToUnlink?.let { set ->
                    viewModel.unlinkSetFromActivity(set.id, activityId)
                }
                showDeleteDialog = false
                setToUnlink = null
                isEditMode = false
            },
            onDismiss = {
                showDeleteDialog = false
                setToUnlink = null
            }
        )
    }
}

/**
 * Beautiful card component for displaying a set within an activity.
 * Matches the SetItemCard design with title on the left, large pencil icon on the right.
 */
@Composable
private fun ActivitySetCard(
    title: String,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FBFF))
            .border(
                width = 1.5.dp,
                color = Color(0xFFB8DDF8),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title and word count on the left
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0B0B),
                    lineHeight = 28.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$itemCount words",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF808080)
                )
            }

            // Pencil icon on the right
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_pencil),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(400.dp)
                        .offset(x = 60.dp, y = (-80).dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}

