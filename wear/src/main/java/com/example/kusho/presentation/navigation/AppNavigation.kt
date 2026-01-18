package com.example.kusho.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.kusho.presentation.pairing.PairingScreen
import com.example.kusho.presentation.screens.home.HomeScreen
import com.example.kusho.presentation.practice.PracticeModeScreen
import com.example.kusho.presentation.tutorial.TutorialModeScreen
import com.example.kusho.presentation.learn.LearnModeScreen
import com.example.kusho.presentation.service.ConnectionMonitor

/**
 * Navigation graph for the app
 */
@Composable
fun AppNavigation() {
    val navController = rememberSwipeDismissableNavController()
    val context = LocalContext.current
    
    // Get connection monitor instance
    val connectionMonitor = ConnectionMonitor.getInstance(context)
    val isConnected by connectionMonitor.isConnected.collectAsState()
    
    // Check if previously paired
    val prefs = context.getSharedPreferences("kusho_prefs", android.content.Context.MODE_PRIVATE)
    val isPaired = prefs.getBoolean("is_paired", false)
    
    // Start at pairing screen if not paired, otherwise home
    val startDestination = if (isPaired) NavigationRoutes.HOME else NavigationRoutes.PAIRING
    
    // Start monitoring when navigation starts (only if paired)
    LaunchedEffect(isPaired) {
        if (isPaired) {
            connectionMonitor.startMonitoring()
        }
    }
    
    // Handle connection loss - navigate to pairing screen
    LaunchedEffect(isConnected) {
        if (!isConnected && isPaired) {
            // Connection lost, clear pairing and go back to pairing screen
            connectionMonitor.clearPairingStatus()
            connectionMonitor.stopMonitoring()
            
            // Navigate to pairing screen and clear back stack
            navController.navigate(NavigationRoutes.PAIRING) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            connectionMonitor.stopMonitoring()
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Pairing screen
        composable(NavigationRoutes.PAIRING) {
            PairingScreen(
                onPairingComplete = {
                    navController.navigate(NavigationRoutes.HOME) {
                        popUpTo(NavigationRoutes.PAIRING) { inclusive = true }
                    }
                }
            )
        }
        
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
