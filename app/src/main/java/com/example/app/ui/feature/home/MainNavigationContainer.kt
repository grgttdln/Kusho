package com.example.app.ui.feature.home

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.app.ui.feature.dashboard.DashboardScreen
import com.example.app.ui.feature.learn.LearnScreen
import com.example.app.ui.feature.classroom.ClassScreen
import com.example.app.ui.feature.classroom.CreateClassScreen
import com.example.app.ui.feature.classroom.EditClassScreen
import com.example.app.ui.feature.classroom.ClassDetailsScreen
import com.example.app.ui.feature.classroom.AddStudentScreen
import com.example.app.ui.feature.classroom.ClassCreatedSuccessScreen
import com.example.app.ui.feature.classroom.StudentAddedSuccessScreen
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
    var createdClassName by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var selectedClassName by remember { mutableStateOf("") }
    var selectedClassCode by remember { mutableStateOf("") }
    var addedStudentName by remember { mutableStateOf("") }

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
            onNavigateToCreateClass = { currentScreen = 8 },
            onNavigateToClassDetails = { classId ->
                selectedClassId = classId
                currentScreen = 10
            },
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
        8 -> CreateClassScreen(
            onNavigateBack = { currentScreen = 2 },
            onClassCreated = { className ->
                createdClassName = className
                currentScreen = 9
            },
            modifier = modifier
        )
        9 -> ClassCreatedSuccessScreen(
            className = createdClassName,
            onContinue = { currentScreen = 2 },
            modifier = modifier
        )
        10 -> ClassDetailsScreen(
            classId = selectedClassId,
            onNavigateBack = { currentScreen = 2 },
            onNavigateToAddStudent = { className ->
                selectedClassName = className
                currentScreen = 11
            },
            onNavigateToEditClass = { classId, className, classCode ->
                selectedClassId = classId
                selectedClassName = className
                selectedClassCode = classCode
                currentScreen = 13
            },
            modifier = modifier
        )
        11 -> AddStudentScreen(
            className = selectedClassName,
            onNavigateBack = { currentScreen = 10 },
            onStudentAdded = { studentName ->
                addedStudentName = studentName
                currentScreen = 12
            },
            modifier = modifier
        )
        12 -> StudentAddedSuccessScreen(
            studentName = addedStudentName,
            onContinue = { currentScreen = 10 },
            modifier = modifier
        )
        13 -> EditClassScreen(
            classId = selectedClassId,
            initialClassName = selectedClassName,
            initialClassCode = selectedClassCode,
            initialBannerRes = com.example.app.R.drawable.ic_class_abc,
            onNavigateBack = { currentScreen = 10 },
            onSaveChanges = { newClassName, newClassCode ->
                // TODO: Save to database
                selectedClassName = newClassName
                selectedClassCode = newClassCode
            },
            onArchiveClass = {
                // TODO: Archive class in database
                currentScreen = 2
            },
            modifier = modifier
        )
    }
}

