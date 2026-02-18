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

    // Activities flow exposed to the UI
    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()
    
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
        val firstName = _uiState.value.userName.split(" ").firstOrNull() ?: "there"
        
        return when (hour) {
            in 0..11 -> "Good Morning, $firstName!"
            in 12..17 -> "Good Afternoon, $firstName!"
            else -> "Good Evening, $firstName!"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't cleanup singleton - it's shared across screens
        watchConnectionManager.stopMonitoring()
    }
}