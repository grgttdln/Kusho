package com.example.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.app.data.SessionManager
import com.example.app.ui.feature.home.MainNavigationContainer
import com.example.app.ui.feature.auth.login.LoginScreen
import com.example.app.ui.feature.auth.signup.SignUpScreen
import com.example.app.ui.feature.onboarding.OnboardingScreen
import com.example.app.ui.feature.onboarding.PostSignUpOnboardingScreen
import com.example.app.ui.feature.watch.WatchPairingScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determine start destination based on user session (only calculated once at launch)
    val startDestination = remember {
        val sessionManager = SessionManager.getInstance(context)
        if (sessionManager.isLoggedIn()) {
            Screen.Home.route  // User is logged in, go directly to home
        } else {
            Screen.Onboarding.route  // No session, start with onboarding
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
            MainNavigationContainer(
                onLogout = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
