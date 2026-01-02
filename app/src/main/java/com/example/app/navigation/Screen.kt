package com.example.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object PostSignUpOnboarding : Screen("post_signup_onboarding")
    object WatchPairing : Screen("watch_pairing")
    object Home : Screen("home")
    object CreateClass : Screen("create_class")
    object ClassCreatedSuccess : Screen("class_created_success/{className}") {
        fun createRoute(className: String) = "class_created_success/$className"
    }
}

