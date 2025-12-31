package com.example.app.ui.feature.home

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.app.data.SessionManager
import com.example.app.data.repository.WordRepository
import com.example.app.data.repository.SetRepository
import com.example.app.data.AppDatabase
import com.example.app.ui.feature.dashboard.DashboardScreen
import com.example.app.ui.feature.learn.LearnScreen
import com.example.app.ui.feature.classroom.ClassScreen
import com.example.app.ui.feature.learn.LessonScreen
import com.example.app.ui.feature.learn.TutorialModeScreen
import com.example.app.ui.feature.learn.LearnModeScreen
import com.example.app.ui.feature.learn.activities.YourActivitiesScreen
import com.example.app.ui.feature.learn.activities.AddNewActivityScreen
import com.example.app.ui.feature.learn.activities.ConfirmationScreen
import com.example.app.ui.feature.learn.set.SelectSetsScreen
import com.example.app.ui.feature.learn.set.YourSetsScreen
import com.example.app.ui.feature.learn.set.AddSetScreen
import com.example.app.ui.feature.learn.set.SelectWordsScreen
import com.example.app.ui.feature.learn.ConfirmationScreen as LearnConfirmationScreen

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
    var selectedActivityId by remember { mutableStateOf(0L) }
    var selectedActivityTitle by remember { mutableStateOf("") }
    var availableWords by remember { mutableStateOf(listOf<String>()) }
    var createdSetTitle by remember { mutableStateOf("") }
    
    // State for Add Set Screen - selected words persist across navigation
    var wordsForCreation by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }
    var selectedWordsWithConfigs by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }
    
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val userId = remember { sessionManager.getUserId() }
    val wordRepository = remember { WordRepository(AppDatabase.getInstance(context).wordDao()) }

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
            onNavigateToSets = { activityId, activityTitle ->
                selectedActivityId = activityId
                selectedActivityTitle = activityTitle
                currentScreen = 7
            },
            onBackClick = { currentScreen = 3 },
            modifier = modifier
        )
        7 -> YourSetsScreen(
            userId = userId,
            onNavigate = { currentScreen = it },
            onBackClick = { currentScreen = 3 },
            onAddSetClick = { 
                // Reset words and navigate to AddSetScreen
                wordsForCreation = emptyList()
                selectedWordsWithConfigs = emptyList()
                currentScreen = 11 
            },
            modifier = modifier
        )
        8 -> AddNewActivityScreen(
            userId = userId,
            onNavigate = { currentScreen = it },
            onBackClick = { currentScreen = 6 },
            onActivityCreated = { currentScreen = 10 },
            modifier = modifier
        )
        9 -> SelectSetsScreen(
            onNavigate = { currentScreen = it },
            onBackClick = { currentScreen = 8 },
            modifier = modifier
        )
        10 -> ConfirmationScreen(
            onNavigate = { currentScreen = it },
            modifier = modifier
        )
        11 -> {
            AddSetScreen(
                userId = userId,
                onBackClick = { 
                    wordsForCreation = emptyList()
                    selectedWordsWithConfigs = emptyList()
                    currentScreen = 7 
                },
                onAddWordsClick = { currentScreen = 12 },
                selectedWords = wordsForCreation,
                onCreateSet = { title, description, words ->
                    createdSetTitle = title
                    currentScreen = 13
                },
                modifier = modifier
            )
        }
        12 -> {
            LaunchedEffect(Unit) {
                availableWords = wordRepository.getWordsForUserOnce(userId).map { it.word }
            }
            SelectWordsScreen(
                availableWords = availableWords,
                onBackClick = { currentScreen = 11 },
                onWordsSelected = { selectedWords ->
                    wordsForCreation = selectedWords
                    currentScreen = 11
                },
                modifier = modifier
            )
        }
        13 -> LearnConfirmationScreen(
            title = "Set Created!",
            subtitle = createdSetTitle,
            onNavigate = { 
                createdSetTitle = ""
                // YourSetsScreen will automatically refresh via Flow when returning
                currentScreen = 7 
            },
            modifier = modifier
        )
    }
}
