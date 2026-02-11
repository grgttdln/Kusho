package com.example.app.ui.feature.home

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.app.data.SessionManager
import com.example.app.data.repository.WordRepository
import com.example.app.data.repository.SetRepository
import com.example.app.data.AppDatabase
import com.example.app.ui.feature.dashboard.DashboardScreen
import com.example.app.ui.feature.learn.LearnScreen
import com.example.app.ui.feature.classroom.*
import com.example.app.ui.feature.learn.LessonScreen
import com.example.app.ui.feature.learn.tutorialmode.TutorialModeScreen
import com.example.app.ui.feature.learn.tutorialmode.TutorialModeStudentScreen
import com.example.app.ui.feature.learn.tutorialmode.TutorialSessionScreen
import com.example.app.ui.feature.learn.tutorialmode.SessionAnalyticsScreen
import com.example.app.ui.feature.learn.tutorialmode.TutorialFinishedScreen
import com.example.app.ui.feature.learn.tutorialmode.TutorialStudentSelectionScreen
import com.example.app.ui.feature.learn.learnmode.LearnModeScreen
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
import com.example.app.ui.feature.learn.learnmode.LearnModeActivitySelectionScreen

@Composable
fun MainNavigationContainer(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onNavigateToWatchPairing: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(0) }

    // --- CLASS SECTION STATE ---
    var createdClassName by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var selectedClassName by remember { mutableStateOf("") }
    var selectedClassCode by remember { mutableStateOf("") }
    var selectedClassBannerPath by remember { mutableStateOf<String?>(null) }
    var addedStudentName by remember { mutableStateOf("") }
    var addedStudentCount by remember { mutableIntStateOf(0) }
    var selectedStudentId by remember { mutableStateOf("") }
    var selectedStudentName by remember { mutableStateOf("") }

    // --- TUTORIAL MODE STATE ---
    var tutorialModeStudentId by remember { mutableStateOf(0L) }
    var tutorialModeClassId by remember { mutableStateOf(0L) }
    var tutorialModeStudentName by remember { mutableStateOf("") }
    var tutorialSessionTitle by remember { mutableStateOf("") }
    var tutorialLetterType by remember { mutableStateOf("capital") }
    var tutorialSessionStudentId by remember { mutableStateOf(0L) }
    
    // --- DASHBOARD TUTORIAL FLOW STATE ---
    var dashboardTutorialSection by remember { mutableStateOf("") } // "Vowels" or "Consonants"
    
    // --- DASHBOARD LEARN FLOW STATE ---
    var dashboardLearnActivityId by remember { mutableStateOf(0L) }
    var dashboardLearnActivityTitle by remember { mutableStateOf("") }

    // --- ACTIVITIES & SETS STATE ---
    var selectedActivityId by remember { mutableStateOf(0L) }
    var selectedActivityTitle by remember { mutableStateOf("") }
    var selectedActivityIconRes by remember { mutableStateOf(com.example.app.R.drawable.ic_apple) }
    var availableWords by remember { mutableStateOf(listOf<com.example.app.data.entity.Word>()) }
    var createdSetTitle by remember { mutableStateOf("") }
    var selectedSetId by remember { mutableStateOf(0L) }
    var learnModeSessionKey by remember { mutableStateOf(0) } // Key to force fresh ViewModel
    var yourSetsScreenKey by remember { mutableStateOf(0) }
    var wordsForCreation by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }
    var wordsForEdit by remember { mutableStateOf(listOf<SetRepository.SelectedWordConfig>()) }
    var wordsToExclude by remember { mutableStateOf(listOf<String>()) }

    // --- REPOSITORY & CONTEXT HELPERS ---
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val userId = remember { sessionManager.getUserId() }
    val wordRepository = remember { WordRepository(AppDatabase.getInstance(context).wordDao()) }
    val database = remember { AppDatabase.getInstance(context) }
    val setRepository = remember { SetRepository(database) }

    when (currentScreen) {
        0 -> DashboardScreen(
            modifier = modifier,
            onNavigate = { currentScreen = it },
            onLogout = onLogout,
            onNavigateToWatchPairing = onNavigateToWatchPairing,
            onNavigateToClassDetails = { classId, className, bannerPath ->
                selectedClassId = classId
                selectedClassName = className
                selectedClassBannerPath = bannerPath
                currentScreen = 22
            },
            onNavigateToTutorialStudentSelection = { section ->
                dashboardTutorialSection = section
                currentScreen = 36 // Navigate to TutorialStudentSelectionScreen from Dashboard
            },
            onNavigateToLearnStudentSelection = { activityId, activityTitle ->
                dashboardLearnActivityId = activityId
                dashboardLearnActivityTitle = activityTitle
                currentScreen = 41 // Navigate to student selection for Learn mode
            }
        )
        1 -> LearnScreen(onNavigate = { currentScreen = it }, modifier = modifier)
        2 -> ClassScreen(
            onNavigate = { currentScreen = it },
            onNavigateToCreateClass = { currentScreen = 20 },
            onNavigateToClassDetails = { classId ->
                selectedClassId = classId
                currentScreen = 22
            },
            onNavigateToStudentDetails = { sId, sName, cName ->
                selectedStudentId = sId
                selectedStudentName = sName
                selectedClassName = cName
                currentScreen = 26
            },
            onNavigateToAddStudent = {
                // Open Add Student flow from the top-level Class screen
                currentScreen = 23
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
            onStudentSelected = { studentId, classId, studentName ->
                tutorialModeStudentId = studentId
                tutorialModeClassId = classId
                tutorialModeStudentName = studentName
                currentScreen = 27
            },
            modifier = modifier
        )
        5 -> LearnModeScreen(
            onBack = { currentScreen = 1 },
            onStudentSelected = { studentId, classId, studentName ->
                selectedStudentId = studentId.toString()
                selectedStudentName = studentName
                selectedClassId = classId.toString()
                currentScreen = 31 // Navigate to activity selection screen
            },
            modifier = modifier
        )
        6 -> YourActivitiesScreen(
            onNavigate = { currentScreen = it },
            onNavigateToSets = { activityId, activityTitle ->
                selectedActivityId = activityId
                selectedActivityTitle = activityTitle
                currentScreen = 16 
            },
            onBackClick = { currentScreen = 3 },
            modifier = modifier
        )
        7 -> {
            key(yourSetsScreenKey) {
                YourSetsScreen(
                    userId = userId,
                    onNavigate = { currentScreen = it },
                    onBackClick = { currentScreen = 3 },
                    onAddSetClick = {
                        selectedActivityId = 0L
                        wordsForCreation = emptyList()
                        currentScreen = 11
                    },
                    onEditSetClick = { setId ->
                        selectedSetId = setId
                        selectedActivityId = 0L // Reset to ensure back navigation goes to YourSetsScreen
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
        11 -> AddSetScreen(
            userId = userId,
            activityId = if (selectedActivityId > 0L) selectedActivityId else null,
            onBackClick = { currentScreen = if (selectedActivityId > 0L) 16 else 7 },
            onAddWordsClick = { existingWords ->
                wordsToExclude = existingWords
                currentScreen = 12
            },
            selectedWords = wordsForCreation,
            onCreateSet = { title, _, _ ->
                createdSetTitle = title
                currentScreen = 13
            },
            modifier = modifier
        )
        12 -> {
            LaunchedEffect(Unit) {
                availableWords = wordRepository.getWordsForUserOnce(userId)
            }
            SelectWordsScreen(
                availableWords = availableWords,
                excludeWords = wordsToExclude,
                onBackClick = { currentScreen = 11 },
                onWordsSelected = { words ->
                    wordsForCreation = words
                    currentScreen = 11
                },
                modifier = modifier
            )
        }
        13 -> ConfirmationScreen(
            title = "Set Created!",
            subtitle = createdSetTitle,
            onContinueClick = {
                yourSetsScreenKey++
                currentScreen = if (selectedActivityId > 0L) 16 else 7
            },
            modifier = modifier
        )
        14 -> EditSetScreen(
            setId = selectedSetId,
            userId = userId,
            onBackClick = { currentScreen = if (selectedActivityId > 0L) 16 else 7 },
            onAddWordsClick = { existingWords ->
                wordsToExclude = existingWords
                currentScreen = 15
            },
            onUpdateSuccess = {
                yourSetsScreenKey++
                currentScreen = if (selectedActivityId > 0L) 16 else 7
            },
            onDeleteSuccess = {
                yourSetsScreenKey++
                currentScreen = if (selectedActivityId > 0L) 16 else 7
            },
            selectedWords = wordsForEdit,
            modifier = modifier
        )
        15 -> {
            LaunchedEffect(Unit) {
                availableWords = wordRepository.getWordsForUserOnce(userId)
            }
            SelectWordsScreen(
                availableWords = availableWords,
                excludeWords = wordsToExclude,
                isAddingToExistingSet = true,
                onBackClick = { currentScreen = 14 },
                onWordsSelected = { words ->
                    wordsForEdit = words
                    currentScreen = 14
                },
                modifier = modifier
            )
        }
        16 -> {
            key(yourSetsScreenKey) {
                ActivitySetsScreen(
                    activityId = selectedActivityId,
                    activityTitle = selectedActivityTitle,
                    onNavigate = { currentScreen = it },
                    onBackClick = { currentScreen = 6 },
                    onAddSetClick = { currentScreen = 17 },
                    onViewSetClick = { setId ->
                        selectedSetId = setId
                        currentScreen = 14
                    },
                    modifier = modifier
                )
            }
        }
        17 -> LinkSetsToActivityScreen(
            activityId = selectedActivityId,
            userId = userId,
            onBackClick = { currentScreen = 16 },
            onSetsLinked = {
                yourSetsScreenKey++
                currentScreen = 16
            },
            modifier = modifier
        )

        // --- Classroom Flow (20-26) ---
        20 -> CreateClassScreen(
            onNavigateBack = { currentScreen = 2 },
            onClassCreated = { className ->
                createdClassName = className
                currentScreen = 21
            },
            modifier = modifier
        )
        21 -> ClassCreatedSuccessScreen(
            className = createdClassName,
            onContinue = { currentScreen = 2 },
            modifier = modifier
        )
        22 -> ClassDetailsScreen(
            classId = selectedClassId,
            onNavigateBack = { currentScreen = 2 },
            onNavigateToAddStudent = { className ->
                selectedClassName = className
                currentScreen = 23
            },
            onNavigateToEditClass = { id, name, code, banner ->
                selectedClassId = id
                selectedClassName = name
                selectedClassCode = code
                selectedClassBannerPath = banner
                currentScreen = 25
            },
            onNavigateToStudentDetails = { sId, sName, cName ->
                selectedStudentId = sId
                selectedStudentName = sName
                selectedClassName = cName
                currentScreen = 26
            },
            modifier = modifier
        )
        23 -> AddStudentScreen(
            onNavigateBack = { currentScreen = 2 },
            onStudentAdded = { studentName, studentCount ->
                addedStudentName = studentName
                addedStudentCount = studentCount
                currentScreen = 24
            },
            modifier = modifier
        )
        24 -> StudentAddedSuccessScreen(
            studentName = addedStudentName,
            studentCount = addedStudentCount,
            onContinue = { currentScreen = 2 },
            modifier = modifier
        )
        25 -> EditClassScreen(
            classId = selectedClassId,
            initialClassName = selectedClassName,
            initialClassCode = selectedClassCode,
            initialBannerPath = selectedClassBannerPath,
            onNavigateBack = { currentScreen = 22 },
            onSaveChanges = { newName, newCode ->
                selectedClassName = newName
                selectedClassCode = newCode
            },
            onArchiveClass = { currentScreen = 2 },
            modifier = modifier
        )
        26 -> StudentDetailsScreen(
            studentId = selectedStudentId,
            studentName = selectedStudentName,
            className = selectedClassName,
            classId = selectedClassId,
            onNavigateBack = { currentScreen = 2 },
            modifier = modifier
        )
        27 -> TutorialModeStudentScreen(
            studentId = tutorialModeStudentId,
            classId = tutorialModeClassId,
            studentName = tutorialModeStudentName,
            onBack = { currentScreen = 4 },
            onStartSession = { title, letterType, studentName ->
                tutorialSessionTitle = title
                tutorialLetterType = letterType
                tutorialModeStudentName = studentName
                tutorialSessionStudentId = tutorialModeStudentId
                currentScreen = 28
            },
            modifier = modifier
        )
        28 -> TutorialSessionScreen(
            title = tutorialSessionTitle,
            letterType = tutorialLetterType,
            studentName = tutorialModeStudentName,
            studentId = tutorialSessionStudentId,
            onEndSession = { currentScreen = 29 },
            modifier = modifier
        )
        29 -> SessionAnalyticsScreen(
            onPracticeAgain = { currentScreen = 27 }, // Go back to vowels/consonants selection
            onContinue = { currentScreen = 30 }, // Go to TutorialFinishedScreen
            modifier = modifier
        )
        30 -> TutorialFinishedScreen(
            onEndSession = { currentScreen = 1 }, // Go back to LearnScreen
            modifier = modifier
        )
        31 -> LearnModeActivitySelectionScreen(
            studentId = selectedStudentId.toLongOrNull() ?: 0L,
            classId = selectedClassId.toLongOrNull() ?: 0L,
            onBack = { currentScreen = 5 },
            onSelectActivity = { activityId: Long, activityTitle: String ->
                selectedActivityId = activityId
                selectedActivityTitle = activityTitle
                // Optionally set a default icon or handle icon selection elsewhere
                currentScreen = 32 // Navigate to LearnModeSetStatusScreen
            },
            modifier = modifier
        )
        32 -> {
            val context = LocalContext.current
            val database = remember { AppDatabase.getInstance(context) }
            val studentSetProgressDao = remember { database.studentSetProgressDao() }
            val setsFlow = remember(selectedActivityId) { setRepository.getSetsForActivity(selectedActivityId) }
            val sets by setsFlow.collectAsState(initial = emptyList())
            val studentIdLong = selectedStudentId.toLongOrNull() ?: 0L
            val progressFlow = remember(selectedActivityId, studentIdLong) {
                if (studentIdLong > 0) {
                    studentSetProgressDao.getProgressForStudentAndActivity(studentIdLong, selectedActivityId)
                } else {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
            val progressList by progressFlow.collectAsState(initial = emptyList())
            val completedSetIds = remember(progressList) {
                progressList.filter { it.isCompleted }.map { it.setId }.toSet()
            }
            val activitySetStatuses = sets.map { set ->
                val isCompleted = completedSetIds.contains(set.id)
                com.example.app.ui.feature.learn.learnmode.ActivitySetStatus(
                    setId = set.id,
                    title = set.title,
                    status = if (isCompleted) "Completed" else "Not Started"
                )
            }
            com.example.app.ui.feature.learn.learnmode.LearnModeSetStatusScreen(
                activityIconRes = selectedActivityIconRes,
                activityTitle = selectedActivityTitle,
                sets = activitySetStatuses,
                onBack = { currentScreen = 31 },
                onStartSet = { set ->
                    // Navigate to LearnModeSessionScreen and pass the set id and title
                    selectedSetId = set.setId
                    tutorialSessionTitle = set.title
                    learnModeSessionKey++ // Increment to force fresh ViewModel
                    currentScreen = 33
                },
                modifier = modifier
            )
        }
        33 -> {
            com.example.app.ui.feature.learn.learnmode.LearnModeSessionScreen(
                setId = selectedSetId,
                activityId = selectedActivityId,
                activityTitle = tutorialSessionTitle,
                sessionKey = learnModeSessionKey,
                studentId = selectedStudentId,
                studentName = selectedStudentName,
                modifier = modifier,
                onSessionComplete = { currentScreen = 34 }
            )
        }
        34 -> com.example.app.ui.feature.learn.learnmode.LearnModeSessionAnalyticsScreen(
            onPracticeAgain = { currentScreen = 31 },
            onContinue = { currentScreen = 35 },
            modifier = modifier
        )
        35 -> com.example.app.ui.feature.learn.learnmode.LearnModeFinishedScreen(
            onEndSession = { currentScreen = 1 },
            modifier = modifier
        )
        // --- DASHBOARD TUTORIAL FLOW ---
        36 -> TutorialStudentSelectionScreen(
            onBack = { currentScreen = 0 },
            onSelectStudent = { studentId, studentName, classId, letterType ->
                tutorialModeStudentId = studentId
                tutorialModeStudentName = studentName
                tutorialModeClassId = classId
                tutorialLetterType = letterType
                tutorialSessionStudentId = studentId
                currentScreen = 38 // Go directly to TutorialSessionScreen (skip vowels/consonants selection)
            },
            showLetterTypeDialog = true,
            modifier = modifier
        )
        37 -> TutorialModeStudentScreen(
            studentId = tutorialModeStudentId,
            classId = tutorialModeClassId,
            studentName = tutorialModeStudentName,
            preselectedSection = dashboardTutorialSection,
            onBack = { currentScreen = 36 },
            onStartSession = { title, letterType, studentName ->
                tutorialSessionTitle = title
                tutorialLetterType = letterType
                tutorialModeStudentName = studentName
                tutorialSessionStudentId = tutorialModeStudentId
                currentScreen = 38
            },
            modifier = modifier
        )
        38 -> TutorialSessionScreen(
            title = dashboardTutorialSection,
            letterType = tutorialLetterType,
            studentName = tutorialModeStudentName,
            studentId = tutorialSessionStudentId,
            onEndSession = { currentScreen = 39 },
            modifier = modifier
        )
        39 -> SessionAnalyticsScreen(
            onPracticeAgain = { currentScreen = 37 },
            onContinue = { currentScreen = 40 },
            modifier = modifier
        )
        40 -> TutorialFinishedScreen(
            onEndSession = { currentScreen = 0 },
            modifier = modifier
        )
        // --- DASHBOARD LEARN FLOW ---
        41 -> TutorialStudentSelectionScreen(
            onBack = { currentScreen = 0 },
            onSelectStudent = { studentId, studentName, classId, _ ->
                selectedStudentId = studentId.toString()
                selectedStudentName = studentName
                selectedClassId = classId.toString()
                selectedActivityId = dashboardLearnActivityId
                selectedActivityTitle = dashboardLearnActivityTitle
                currentScreen = 42 // Go to LearnModeSetStatusScreen
            },
            showLetterTypeDialog = false,
            modifier = modifier
        )
        42 -> {
            val context = LocalContext.current
            val database = remember { AppDatabase.getInstance(context) }
            val studentSetProgressDao = remember { database.studentSetProgressDao() }
            val setsFlow = remember(selectedActivityId) { setRepository.getSetsForActivity(selectedActivityId) }
            val sets by setsFlow.collectAsState(initial = emptyList())
            val studentIdLong = selectedStudentId.toLongOrNull() ?: 0L
            val progressFlow = remember(selectedActivityId, studentIdLong) {
                if (studentIdLong > 0) {
                    studentSetProgressDao.getProgressForStudentAndActivity(studentIdLong, selectedActivityId)
                } else {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
            val progressList by progressFlow.collectAsState(initial = emptyList())
            val completedSetIds = remember(progressList) {
                progressList.filter { it.isCompleted }.map { it.setId }.toSet()
            }
            val activitySetStatuses = sets.map { set ->
                val isCompleted = completedSetIds.contains(set.id)
                com.example.app.ui.feature.learn.learnmode.ActivitySetStatus(
                    setId = set.id,
                    title = set.title,
                    status = if (isCompleted) "Completed" else "Not Started"
                )
            }
            com.example.app.ui.feature.learn.learnmode.LearnModeSetStatusScreen(
                activityIconRes = selectedActivityIconRes,
                activityTitle = selectedActivityTitle,
                sets = activitySetStatuses,
                onBack = { currentScreen = 41 },
                onStartSet = { set ->
                    selectedSetId = set.setId
                    tutorialSessionTitle = set.title
                    learnModeSessionKey++
                    currentScreen = 43
                },
                modifier = modifier
            )
        }
        43 -> {
            com.example.app.ui.feature.learn.learnmode.LearnModeSessionScreen(
                setId = selectedSetId,
                activityId = selectedActivityId,
                activityTitle = tutorialSessionTitle,
                sessionKey = learnModeSessionKey,
                studentId = selectedStudentId,
                studentName = selectedStudentName,
                modifier = modifier,
                onSessionComplete = { currentScreen = 44 }
            )
        }
        44 -> com.example.app.ui.feature.learn.learnmode.LearnModeSessionAnalyticsScreen(
            onPracticeAgain = { currentScreen = 42 },
            onContinue = { currentScreen = 45 },
            modifier = modifier
        )
        45 -> com.example.app.ui.feature.learn.learnmode.LearnModeFinishedScreen(
            onEndSession = { currentScreen = 0 },
            modifier = modifier
        )
    }
}