package com.example.app.ui.feature.learn.set

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.entity.Set
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.DeleteConfirmationDialog
import com.example.app.ui.components.DeleteType
import com.example.app.ui.components.SetItemCard

@Composable
fun YourSetsScreen(
    modifier: Modifier = Modifier,
    userId: Long = 0L,
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onAddSetClick: () -> Unit = {},
    onEditSetClick: (Long) -> Unit = {},
    viewModel: YourSetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Edit mode state for delete functionality
    var isEditMode by remember { mutableStateOf(false) }
    var setToDelete by remember { mutableStateOf<Set?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Create stable callback references using rememberUpdatedState
    // This ensures the lambda always has the latest callback reference
    val currentOnEditSetClick by rememberUpdatedState(onEditSetClick)
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    val currentOnAddSetClick by rememberUpdatedState(onAddSetClick)

    // Load sets when screen is displayed
    LaunchedEffect(userId) {
        if (userId > 0L) {
            viewModel.loadSets(userId)
        }
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
            Spacer(Modifier.height(24.dp))

            // Back Button, Kusho Logo (centered), and Edit/Delete Button
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Button (left)
                IconButton(
                    onClick = { currentOnBackClick() },
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

                // Edit/Delete Mode Button (right)
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

            // Title
            Text(
                text = "My Activities",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0B0B)
            )

            Spacer(Modifier.height(32.dp))

            // Sets or Loading/Empty State
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
            } else if (uiState.sets.isEmpty()) {
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
                            contentDescription = "No sets mascot",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.sets,
                        key = { it.id }
                    ) { set ->
                        // Check if this set is selected for deletion
                        val isSelected = setToDelete?.id == set.id

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
                            SetItemCard(
                                title = set.title,
                                iconRes = R.drawable.ic_pencil,
                                itemCount = set.itemCount,
                                onClick = {
                                    if (isEditMode) {
                                        setToDelete = set
                                        showDeleteDialog = true
                                    } else {
                                        currentOnEditSetClick(set.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Action Buttons: "Add Activity" and Magic Wand
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { currentOnAddSetClick() },
                modifier = Modifier
                    .width(217.dp)
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
                    text = "Add Activity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Magic Wand Button
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .size(75.dp)
                    .background(Color(0xFF3FA9F8), RoundedCornerShape(37.5.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_wand),
                    contentDescription = "Magic Wand",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { currentOnNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Delete Confirmation Dialog
        DeleteConfirmationDialog(
            isVisible = showDeleteDialog && setToDelete != null,
            deleteType = DeleteType.SET,
            onConfirm = {
                setToDelete?.let { set ->
                    viewModel.deleteSet(set.id)
                }
                showDeleteDialog = false
                setToDelete = null
                isEditMode = false
            },
            onDismiss = {
                showDeleteDialog = false
                setToDelete = null
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun YourSetsScreenPreview() {
    YourSetsScreen(
        userId = 1L,
        onNavigate = {},
        onBackClick = {}
    )
}
