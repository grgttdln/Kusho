package com.example.app.ui.feature.learn.set

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.BottomNavBar
import com.example.app.ui.components.SetItemCard
import com.example.app.ui.feature.learn.activities.AddActivityViewModel

@Composable
fun SelectSetsScreen(
    onNavigate: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddActivityViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSets by remember { mutableStateOf(setOf<Long>()) }
    
    val uiState by viewModel.uiState.collectAsState()

    // Load all available sets from the database
    LaunchedEffect(Unit) {
        viewModel.loadAllSets()
    }

    // Filter sets based on search query - memoized to prevent excessive filtering
    val filteredSets = remember(searchQuery, uiState.availableSets) {
        uiState.availableSets.filter { set ->
            set.title.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                text = "Select Set/s",
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
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = Color(0xFF000000)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Set Cards List or Loading State
            if (uiState.availableSets.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sets available.\nCreate a set to get started!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF808080),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (uiState.isLoading) {
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredSets.forEach { set ->
                        SetItemCard(
                            title = set.title,
                            iconRes = R.drawable.ic_pencil,
                            isSelected = selectedSets.contains(set.id),
                            onClick = {
                                selectedSets = if (selectedSets.contains(set.id)) {
                                    selectedSets - set.id
                                } else {
                                    selectedSets + set.id
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Add Sets Button
        Button(
            onClick = {
                // Add selected sets to ViewModel with their item counts
                filteredSets
                    .filter { selectedSets.contains(it.id) }
                    .forEach { set ->
                        viewModel.addChapter(set.title, set.itemCount)
                    }
                onNavigate(8)
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
            enabled = selectedSets.isNotEmpty()
        ) {
            Text(
                text = "Add ${selectedSets.size} Sets",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
fun SelectSetsScreenPreview() {
    SelectSetsScreen(
        onNavigate = {},
        onBackClick = {}
    )
}