package com.example.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object WatchPairing : Screen("watch_pairing")
    object Home : Screen("home")
}

