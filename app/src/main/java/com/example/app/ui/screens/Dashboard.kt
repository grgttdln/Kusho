package com.example.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.components.BottomNavBar

@Composable
fun DashboardScreen(
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dashboard",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D2D)
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = 0,
            onTabSelected = { onNavigate(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun MainNavigationContainer(
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(0) }

    when (currentScreen) {
        0 -> DashboardScreen(
            onNavigate = { currentScreen = it },
            modifier = modifier
        )
        1 -> LearnScreen(
            onNavigate = { currentScreen = it },
            modifier = modifier
        )
        2 -> ClassScreen(
            onNavigate = { currentScreen = it },
            modifier = modifier
        )
        3 -> LessonScreen(
            onNavigate = { currentScreen = it },
            modifier = modifier
        )
        4 -> TutorialModeScreen(
            onBack = { currentScreen = 1 },
            modifier = modifier
        )
        5 -> LearnModeScreen(
            onBack = { currentScreen = 1 },
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MainNavigationContainer()
}

