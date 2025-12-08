package com.example.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.app.ui.screens.LoginScreen
import com.example.app.ui.screens.OnboardingScreen
import com.example.app.ui.screens.SignUpScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onStartLearning = {
                    navController.navigate(Screen.Login.route) {
                        // Pop onboarding from backstack so back button doesn't return to it
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // TODO: Navigate to home screen after successful login
                    // navController.navigate(Screen.Home.route)
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    // TODO: Navigate to home screen after successful sign up
                    // navController.navigate(Screen.Home.route)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Add more screens here as needed
        // composable(Screen.Home.route) { HomeScreen() }
    }
}

