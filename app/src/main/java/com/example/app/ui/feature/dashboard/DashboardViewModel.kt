package com.example.app.ui.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Class
import com.example.app.data.entity.Activity
import com.example.app.data.repository.ClassRepository
import com.example.app.data.repository.ActivityRepository
import com.example.app.data.repository.StudentTeacherRepository
import com.example.app.data.repository.WordRepository
import com.example.app.data.dao.VideoTutorialDao
import com.example.app.data.entity.VideoTutorial
import com.example.app.service.WatchConnectionManager
import com.example.app.service.WatchDeviceInfo
import com.example.app.service.PairingRequestEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "Guest",
    val watchDevice: WatchDeviceInfo = WatchDeviceInfo(),
    val isLoading: Boolean = false,
    val totalStudents: Int = 0,
    val totalActivities: Int = 0,
    val totalWords: Int = 0,
    val recentClass: Class? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val watchConnectionManager = WatchConnectionManager.getInstance(application)
    private val sessionManager = SessionManager.getInstance(application)
    
    private val database = AppDatabase.getInstance(application)
    private val classRepository = ClassRepository(database.classDao())
    private val activityRepository = ActivityRepository(database.activityDao())
    private val wordRepository = WordRepository(database.wordDao())
    private val studentTeacherRepository = StudentTeacherRepository(database.studentTeacherDao())
    private val videoTutorialDao: VideoTutorialDao = database.videoTutorialDao()

    // Activities flow exposed to the UI
    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    // Video tutorials flow exposed to the UI
    private val _videoTutorials = MutableStateFlow<List<VideoTutorial>>(emptyList())
    val videoTutorials: StateFlow<List<VideoTutorial>> = _videoTutorials.asStateFlow()
    private var tutorialsSeeded = false
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        // Collect watch device info updates
        viewModelScope.launch {
            watchConnectionManager.deviceInfo.collect { deviceInfo ->
                _uiState.value = _uiState.value.copy(watchDevice = deviceInfo)
            }
        }
        
        // Collect current user updates and load analytics
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    userName = user?.name ?: "Guest"
                )
                // Load analytics when user is available
                user?.let { loadAnalytics(it.id) }
            }
        }

        // Start monitoring watch connection
        watchConnectionManager.startMonitoring()
        
        // Initial connection check
        checkWatchConnection()
        
        // IMPORTANT: Force battery request when Dashboard loads
        requestBatteryUpdate()
    }
    
    /**
     * Refresh analytics data - call this when returning to dashboard
     */
    fun refreshAnalytics() {
        viewModelScope.launch {
            sessionManager.currentUser.value?.let { user ->
                loadAnalytics(user.id)
            }
        }
    }
    
    /**
     * Load analytics data from database
     */
    private fun loadAnalytics(userId: Long) {
        viewModelScope.launch {
            try {
                // Collect active classes (updates recentClass and totalActivities)
                launch {
                    classRepository.getActiveClassesByUserId(userId).collect { activeClasses ->
                        val classroomCount = activeClasses.size
                        val activityCountResult = activityRepository.getActivityCount(userId)
                        val totalActivitiesCount = activityCountResult.getOrNull() ?: 0
                        val recentClass = activeClasses.maxByOrNull { it.classId }

                        _uiState.value = _uiState.value.copy(
                            totalActivities = totalActivitiesCount,
                            recentClass = recentClass
                        )
                    }
                }

                // Collect the number of distinct students assigned to this teacher reactively
                launch {
                    studentTeacherRepository.getStudentCountForTeacherFlow(userId).collect { count ->
                        _uiState.value = _uiState.value.copy(totalStudents = count)
                    }
                }

                // Load total words for this user (one-shot)
                launch {
                    try {
                        val wordCount = wordRepository.getWordCount(userId)
                        _uiState.value = _uiState.value.copy(totalWords = wordCount)
                    } catch (e: Exception) {
                        // fallback to 0 on error
                        _uiState.value = _uiState.value.copy(totalWords = 0)
                    }
                }

                // Collect activities for this user reactively and expose to UI
                launch {
                    activityRepository.getActivitiesForUser(userId).collect { activities ->
                        _activities.value = activities
                    }
                }

                // Load video tutorials, seeding if empty or outdated
                launch {
                    if (!tutorialsSeeded) {
                        tutorialsSeeded = true
                        val expected = getPlaceholderTutorials(userId)
                        val count = videoTutorialDao.getCount(userId)
                        if (count != expected.size) {
                            videoTutorialDao.deleteAllForUser(userId)
                            videoTutorialDao.insertAll(expected)
                        }
                    }
                    videoTutorialDao.getByUserId(userId).collect { tutorials ->
                        _videoTutorials.value = tutorials
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Check watch connection manually
     */
    fun checkWatchConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            watchConnectionManager.checkConnection()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Force battery status request from connected watch
     * Call this when Dashboard is shown/resumed
     */
    fun requestBatteryUpdate() {
        watchConnectionManager.requestBatteryFromConnectedWatch()
    }
    
    /**
     * Request device info from watch
     */
    fun requestDeviceInfo() {
        watchConnectionManager.requestDeviceInfo()
    }
    
    // Expose pairing request from watch for UI to observe
    val pairingRequest: StateFlow<PairingRequestEvent?> = watchConnectionManager.pairingRequest
    
    /**
     * Accept a pairing request from watch
     */
    fun acceptPairingRequest(nodeId: String) {
        watchConnectionManager.acceptPairing(nodeId)
    }
    
    /**
     * Decline a pairing request from watch
     */
    fun declinePairingRequest(nodeId: String) {
        watchConnectionManager.declinePairing(nodeId)
    }
    
    /**
     * Get the greeting based on time of day
     */
    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val fullName = _uiState.value.userName.ifBlank { "there" }
        
        return when (hour) {
            in 0..11 -> "Good Morning, $fullName!"
            in 12..17 -> "Good Afternoon, $fullName!"
            else -> "Good Evening, $fullName!"
        }
    }
    
    private fun getPlaceholderTutorials(userId: Long): List<VideoTutorial> = listOf(
        VideoTutorial(
            title = "1. Welcome to Kusho",
            description = "Get a quick overview of Kusho and how it supports air writing and early literacy development.",
            category = "GUIDE",
            durationMinutes = 2,
            videoResName = "tutorial_welcome",
            sortOrder = 1,
            userId = userId
        ),
        VideoTutorial(
            title = "2. Setting Up Your Class",
            description = "Learn how to add students and prepare your classroom for guided air writing sessions.",
            category = "GUIDE",
            durationMinutes = 2,
            videoResName = "tutorial_class_setup",
            sortOrder = 2,
            userId = userId
        ),
        VideoTutorial(
            title = "3. Building Your Word Bank",
            description = "Add and organize CVC words to create a strong foundation for Learn Mode and activities.",
            category = "GUIDE",
            durationMinutes = 3,
            videoResName = "tutorial_word_bank",
            sortOrder = 3,
            userId = userId
        ),
        VideoTutorial(
            title = "4. Creating Activities & Sets",
            description = "Design structured learning tasks using custom activities and organized activity sets.",
            category = "GUIDE",
            durationMinutes = 3,
            videoResName = "tutorial_activities",
            sortOrder = 4,
            userId = userId
        ),
        VideoTutorial(
            title = "5. Using Learn Mode",
            description = "Guide students through structured word formation practice using your Word Bank.",
            category = "GUIDE",
            durationMinutes = 3,
            videoResName = "tutorial_learn_mode",
            sortOrder = 5,
            userId = userId
        ),
        VideoTutorial(
            title = "6. Using Practice Mode",
            description = "Allow students to independently practice air writing with guided feedback.",
            category = "GUIDE",
            durationMinutes = 2,
            videoResName = "tutorial_practice_mode",
            sortOrder = 6,
            userId = userId
        ),
        VideoTutorial(
            title = "7. Using Tutorial Mode",
            description = "Introduce new words step-by-step with tracing guidance and assisted writing support.",
            category = "GUIDE",
            durationMinutes = 3,
            videoResName = "tutorial_tutorial_mode",
            sortOrder = 7,
            userId = userId
        ),
        VideoTutorial(
            title = "8. Adding Annotations & Feedback",
            description = "Review student outputs and provide meaningful annotations to support improvement.",
            category = "GUIDE",
            durationMinutes = 2,
            videoResName = "tutorial_annotations",
            sortOrder = 8,
            userId = userId
        )
    )

    override fun onCleared() {
        super.onCleared()
        // Don't cleanup singleton - it's shared across screens
        watchConnectionManager.stopMonitoring()
    }
}