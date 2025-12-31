package com.example.app.ui.feature.learn.activities

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

    LaunchedEffect(userId) {
        if (userId > 0) {
            viewModel.loadActivities(userId)
        }
    }

    // Default icons for activities (rotation)
    val defaultIcons = listOf(R.drawable.ic_apple, R.drawable.ic_ball, R.drawable.ic_flower)

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

            // Back Button and Kusho Logo
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
                    Text(
                        text = "No activities yet.\nCreate one to get started!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF808080),
                        textAlign = TextAlign.Center
                    )
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
                        // Get default icon based on position (rotate through apple, ball, flower)
                        val iconIndex = (uiState.activities.indexOf(activity)) % defaultIcons.size
                        val defaultIcon = defaultIcons[iconIndex]

                        ActivityItemCard(
                            title = activity.title,
                            description = activity.description,
                            iconRes = defaultIcon,
                            onClick = { onNavigateToSets(activity.id, activity.title) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Floating Add Activity Button
        Button(
            onClick = { onNavigate(8) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .width(207.dp)
                .height(75.dp),
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
