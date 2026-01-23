package com.example.kusho.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.kusho.presentation.screens.home.HomeScreen
import com.example.kusho.presentation.practice.PracticeModeScreen
import com.example.kusho.presentation.tutorial.TutorialModeScreen
import com.example.kusho.presentation.learn.LearnModeScreen

/**
 * Navigation graph for the app
 */
@Composable
fun AppNavigation() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = NavigationRoutes.HOME
    ) {
        // Home screen with mode arcs
        composable(NavigationRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        // Practice Mode screen with real-time inference
        composable(NavigationRoutes.PRACTICE_MODE) {
            PracticeModeScreen()
        }

        // Tutorial Mode screen
        composable(NavigationRoutes.TUTORIAL_MODE) {
            TutorialModeScreen()
        }

        // Learn Mode screen
        composable(NavigationRoutes.LEARN_MODE) {
            LearnModeScreen()
        }
    }
}
