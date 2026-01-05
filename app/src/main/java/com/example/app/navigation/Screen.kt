package com.example.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object PostSignUpOnboarding : Screen("post_signup_onboarding")
    object WatchPairing : Screen("watch_pairing")
    object Home : Screen("home")
    object CreateClass : Screen("create_class")
    object ClassDetails : Screen("class_details/{classId}") {
        fun createRoute(classId: String) = "class_details/$classId"
    }
    object AddStudent : Screen("add_student/{className}/{classId}") {
        fun createRoute(className: String, classId: String) = "add_student/$className/$classId"
    }
    object ClassCreatedSuccess : Screen("class_created_success/{className}") {
        fun createRoute(className: String) = "class_created_success/$className"
    }
    object StudentAddedSuccess : Screen("student_added_success/{studentName}") {
        fun createRoute(studentName: String) = "student_added_success/$studentName"
    }
    object EditClass : Screen("edit_class/{classId}/{className}/{classCode}") {
        fun createRoute(classId: String, className: String, classCode: String) = "edit_class/$classId/$className/$classCode"
    }
    object StudentDetails : Screen("student_details/{studentId}/{studentName}/{className}") {
        fun createRoute(studentId: String, studentName: String, className: String) = "student_details/$studentId/$studentName/$className"
    }
}

