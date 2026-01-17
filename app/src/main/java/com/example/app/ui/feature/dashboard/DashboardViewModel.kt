package com.example.app.ui.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Class
import com.example.app.data.repository.ClassRepository
import com.example.app.data.repository.ActivityRepository
import com.example.app.data.repository.EnrollmentRepository
import com.example.app.data.repository.StudentRepository
import com.example.app.data.repository.StudentTeacherRepository
import com.example.app.service.WatchConnectionManager
import com.example.app.service.WatchDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "Guest",
    val watchDevice: WatchDeviceInfo = WatchDeviceInfo(),
    val isLoading: Boolean = false,
    val totalStudents: Int = 0,
    val totalActivities: Int = 0,
    val recentClass: Class? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val watchConnectionManager = WatchConnectionManager.getInstance(application)
    private val sessionManager = SessionManager.getInstance(application)
    
    private val database = AppDatabase.getInstance(application)
    private val classRepository = ClassRepository(database.classDao())
    private val enrollmentRepository = EnrollmentRepository(database.enrollmentDao(), database.studentDao())
    private val studentRepository = StudentRepository(database.studentDao())
    private val studentTeacherRepository = StudentTeacherRepository(database.studentTeacherDao())
    private val activityRepository = ActivityRepository(database.activityDao())

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
                // Get all active (non-archived) classes for the user
                classRepository.getActiveClassesByUserId(userId).collect { activeClasses ->
                    // Count active classrooms (kept for possible future use)
                    val classroomCount = activeClasses.size
                    
                    // Count students assigned to this teacher via the student_teachers join table
                    val teacherStudentIds = studentTeacherRepository.getStudentIdsForTeacher(userId)
                    val totalStudentsCount = teacherStudentIds.distinct().size

                    // Get total activities for the user
                    val activityCountResult = activityRepository.getActivityCount(userId)
                    val totalActivitiesCount = activityCountResult.getOrNull() ?: 0
                     
                     // Get most recent class (highest classId = most recently created)
                     val recentClass = activeClasses.maxByOrNull { it.classId }
                     
                     _uiState.value = _uiState.value.copy(
                        totalStudents = totalStudentsCount,
                        totalActivities = totalActivitiesCount,
                         recentClass = recentClass
                     )
                 }
            } catch (e: Exception) {
                // Handle error silently or log it
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