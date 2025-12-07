package com.example.kusho.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.kusho.presentation.screens.home.HomeScreen
import com.example.kusho.presentation.screens.mode.ModeScreen
import com.example.kusho.presentation.theme.AppColors

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

        // Practice Mode screen
        composable(NavigationRoutes.PRACTICE_MODE) {
            ModeScreen(
                modeName = "Practice Mode",
                modeColor = AppColors.PracticeModeColor
            )
        }

        // Tutorial Mode screen
        composable(NavigationRoutes.TUTORIAL_MODE) {
            ModeScreen(
                modeName = "Tutorial Mode",
                modeColor = AppColors.TutorialModeColor
            )
        }

        // Learn Mode screen
        composable(NavigationRoutes.LEARN_MODE) {
            ModeScreen(
                modeName = "Learn Mode",
                modeColor = AppColors.LearnModeColor
            )
        }
    }
}

