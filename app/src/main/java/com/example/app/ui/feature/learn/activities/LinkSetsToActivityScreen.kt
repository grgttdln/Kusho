package com.example.app.ui.feature.learn.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.data.entity.Set
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.SetItemCard

/**
 * Screen to link existing sets to an activity.
 * Shows all user's sets and allows selecting which ones to add to the activity.
 */
@Composable
fun LinkSetsToActivityScreen(
    activityId: Long,
    userId: Long,
    onBackClick: () -> Unit,
    onSetsLinked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinkSetsToActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSetIds by remember { mutableStateOf(setOf<Long>()) }

    // Load user's sets and already linked sets
    LaunchedEffect(activityId, userId) {
        viewModel.loadData(userId, activityId)
    }

    // Filter sets based on search query
    val filteredSets = remember(searchQuery, uiState.availableSets) {
        uiState.availableSets.filter { set ->
            set.title.contains(searchQuery, ignoreCase = true)
        }
    }

    // Check if a set is already linked to this activity
    val alreadyLinkedIds = uiState.alreadyLinkedSetIds

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

            // Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
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

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Add Sets to Activity",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF000000),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 15.dp),
                placeholder = {
                    Text(
                        text = "Search for Sets",
                        fontSize = 16.sp,
                        color = Color(0xFF3FA9F8),
                        fontWeight = FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF3FA9F8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color(0x803FA9F8),
                    focusedIndicatorColor = Color(0xFF3FA9F8),
                    focusedTextColor = Color(0xFF000000),
                    unfocusedTextColor = Color(0xFF000000),
                    cursorColor = Color(0xFF3FA9F8)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = Color(0xFF000000)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Set Cards List or Loading State
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
                uiState.availableSets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sets available.\nCreate sets first in 'Your Sets'!",
                            fontSize = 16.sp,
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
                            .padding(horizontal = 15.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = filteredSets,
                            key = { set: Set -> set.id }
                        ) { set: Set ->
                            val isAlreadyLinked = alreadyLinkedIds.contains(set.id)
                            val isSelected = selectedSetIds.contains(set.id)

                            SetItemCard(
                                title = set.title,
                                iconRes = R.drawable.ic_pencil,
                                isSelected = isSelected || isAlreadyLinked,
                                onClick = {
                                    if (!isAlreadyLinked) {
                                        selectedSetIds = if (isSelected) {
                                            selectedSetIds - set.id
                                        } else {
                                            selectedSetIds + set.id
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add Selected Sets Button
        Button(
            onClick = {
                if (selectedSetIds.isNotEmpty()) {
                    viewModel.linkSetsToActivity(selectedSetIds.toList(), activityId)
                    onSetsLinked()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .fillMaxWidth(0.86f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3FA9F8),
                disabledContainerColor = Color(0xFFB3E5FC)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedSetIds.isNotEmpty() && !uiState.isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedSetIds.isEmpty()) "Select Sets" else "Add ${selectedSetIds.size} Set(s)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 3,
            onTabSelected = { },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

