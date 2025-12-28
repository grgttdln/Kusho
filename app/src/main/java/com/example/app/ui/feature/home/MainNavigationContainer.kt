package com.example.app.ui.feature.home

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.app.ui.feature.dashboard.DashboardScreen
import com.example.app.ui.feature.learn.LearnScreen
import com.example.app.ui.feature.classroom.ClassScreen
import com.example.app.ui.feature.learn.LessonScreen
import com.example.app.ui.feature.learn.TutorialModeScreen
import com.example.app.ui.feature.learn.LearnModeScreen
import com.example.app.ui.feature.learn.activities.YourActivitiesScreen
import com.example.app.ui.feature.learn.set.YourSetsScreen

/**
 * Main navigation container for the home section of the app.
 * Manages navigation between Dashboard, Learn, Class, and Lesson screens.
 */
@Composable
fun MainNavigationContainer(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(0) }

    when (currentScreen) {
        0 -> DashboardScreen(
            onNavigate = { currentScreen = it },
            onLogout = onLogout,
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
            onNavigateToActivities = { currentScreen = 6 },
            onNavigateToSets = { currentScreen = 7 },
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
        6 -> YourActivitiesScreen(
            onNavigate = { currentScreen = it },
            onBackClick = { currentScreen = 3 },
            modifier = modifier
        )
        7 -> YourSetsScreen(
            onNavigate = { currentScreen = it },
            onBackClick = { currentScreen = 3 },
            modifier = modifier
        )
    }
}

