package com.example.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.app.ui.screens.MainNavigationContainer
import com.example.app.ui.screens.LoginScreen
import com.example.app.ui.screens.OnboardingScreen
import com.example.app.ui.screens.PostSignUpOnboardingScreen
import com.example.app.ui.screens.SignUpScreen
import com.example.app.ui.screens.WatchPairingScreen

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
                    navController.navigate(Screen.WatchPairing.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.PostSignUpOnboarding.route) {
                        popUpTo(Screen.SignUp.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PostSignUpOnboarding.route) {
            PostSignUpOnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.WatchPairing.route) {
                        popUpTo(Screen.PostSignUpOnboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.WatchPairing.route) {
            WatchPairingScreen(
                onRefresh = {
                    // TODO: Implement device refresh logic
                },
                onProceedToDashboard = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.WatchPairing.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            MainNavigationContainer()
        }
    }
}

