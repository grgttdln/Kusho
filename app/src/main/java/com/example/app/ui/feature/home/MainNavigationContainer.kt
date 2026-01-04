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
import com.example.app.ui.feature.learn.activities.ActivitySetsScreen
import com.example.app.ui.feature.learn.activities.LinkSetsToActivityScreen
import com.example.app.ui.feature.learn.set.SelectSetsScreen
import com.example.app.ui.feature.learn.set.YourSetsScreen
import com.example.app.ui.feature.learn.set.AddSetScreen
import com.example.app.ui.feature.learn.set.EditSetScreen
import com.example.app.ui.feature.learn.set.SelectWordsScreen
import com.example.app.ui.feature.learn.ConfirmationScreen

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
    var selectedSetId by remember { mutableStateOf(0L) }

    // Counter to force YourSetsScreen refresh when returning from edit
    var yourSetsScreenKey by remember { mutableStateOf(0) }

    // State for Add Set Screen - selected words persist across navigation
    var wordsForCreation by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }
    var selectedWordsWithConfigs by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }
    
    // State for Edit Set Screen - words being added to an existing set
    var wordsForEdit by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }

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
                currentScreen = 16  // Navigate to ActivitySetsScreen
            },
            onBackClick = { currentScreen = 3 },
            modifier = modifier
        )
        7 -> {
            // YourSetsScreen - for viewing ALL user sets (from "Your Sets" menu)
            key(yourSetsScreenKey) {
                YourSetsScreen(
                    userId = userId,
                    onNavigate = { currentScreen = it },
                    onBackClick = { currentScreen = 3 },
                    onAddSetClick = {
                        // Reset words and navigate to AddSetScreen (without activity link)
                        selectedActivityId = 0L
                        selectedActivityTitle = ""
                        wordsForCreation = emptyList()
                        selectedWordsWithConfigs = emptyList()
                        currentScreen = 11
                    },
                    onEditSetClick = { setId ->
                        // Navigate to EditSetScreen with the selected set ID
                        selectedSetId = setId
                        wordsForEdit = emptyList()
                        currentScreen = 14
                    },
                    modifier = modifier
                )
            }
        }
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
            title = "Activity Created!",
            onContinueClick = { currentScreen = 6 },
            modifier = modifier
        )
        11 -> {
            AddSetScreen(
                userId = userId,
                activityId = if (selectedActivityId > 0L) selectedActivityId else null,
                onBackClick = {
                    wordsForCreation = emptyList()
                    selectedWordsWithConfigs = emptyList()
                    // Go back to ActivitySetsScreen if from activity, otherwise YourSetsScreen
                    currentScreen = if (selectedActivityId > 0L) 16 else 7
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
        13 -> ConfirmationScreen(
            title = "Set Created!",
            subtitle = createdSetTitle,
            onContinueClick = {
                createdSetTitle = ""
                yourSetsScreenKey++ // Force screens to refresh
                // Go back to ActivitySetsScreen if from activity, otherwise YourSetsScreen
                currentScreen = if (selectedActivityId > 0L) 16 else 7
            },
            modifier = modifier
        )
        14 -> {
            EditSetScreen(
                setId = selectedSetId,
                userId = userId,
                onBackClick = {
                    wordsForEdit = emptyList()
                    yourSetsScreenKey++ // Force screens to refresh
                    // Go back to ActivitySetsScreen if from activity, otherwise YourSetsScreen
                    currentScreen = if (selectedActivityId > 0L) 16 else 7
                },
                onAddWordsClick = { currentScreen = 15 },
                onUpdateSuccess = {
                    wordsForEdit = emptyList()
                    yourSetsScreenKey++ // Force screens to refresh
                    // Go back to ActivitySetsScreen if from activity, otherwise YourSetsScreen
                    currentScreen = if (selectedActivityId > 0L) 16 else 7
                },
                onDeleteSuccess = {
                    wordsForEdit = emptyList()
                    yourSetsScreenKey++ // Force screens to refresh
                    // Go back to ActivitySetsScreen if from activity, otherwise YourSetsScreen
                    currentScreen = if (selectedActivityId > 0L) 16 else 7
                },
                selectedWords = wordsForEdit,
                modifier = modifier
            )
        }
        15 -> {
            LaunchedEffect(Unit) {
                availableWords = wordRepository.getWordsForUserOnce(userId).map { it.word }
            }
            SelectWordsScreen(
                availableWords = availableWords,
                onBackClick = { currentScreen = 14 },
                onWordsSelected = { selectedWords ->
                    wordsForEdit = selectedWords
                    currentScreen = 14
                },
                modifier = modifier
            )
        }
        // ActivitySetsScreen - for viewing sets within a specific activity
        16 -> {
            key(yourSetsScreenKey) {
                ActivitySetsScreen(
                    activityId = selectedActivityId,
                    activityTitle = selectedActivityTitle,
                    onNavigate = { currentScreen = it },
                    onBackClick = {
                        selectedActivityId = 0L
                        selectedActivityTitle = ""
                        currentScreen = 6  // Back to YourActivitiesScreen
                    },
                    onAddSetClick = {
                        // Navigate to screen to link existing sets to this activity
                        currentScreen = 17
                    },
                    onViewSetClick = { setId ->
                        // Navigate to EditSetScreen to view/edit the set
                        selectedSetId = setId
                        wordsForEdit = emptyList()
                        currentScreen = 14
                    },
                    modifier = modifier
                )
            }
        }
        // LinkSetsToActivityScreen - for adding existing sets to an activity
        17 -> {
            LinkSetsToActivityScreen(
                activityId = selectedActivityId,
                userId = userId,
                onBackClick = { currentScreen = 16 },
                onSetsLinked = {
                    yourSetsScreenKey++ // Force refresh
                    currentScreen = 16
                },
                modifier = modifier
            )
        }
    }
}
