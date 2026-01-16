package com.example.app.ui.feature.classroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Class
import com.example.app.data.repository.ClassRepository
import com.example.app.data.repository.EnrollmentRepository
import com.example.app.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing a class with student count.
 */
data class ClassWithCount(
    val classEntity: Class,
    val studentCount: Int
)

/**
 * Data class representing a student in the roster.
 */
data class RosterStudent(
    val studentId: Long,
    val fullName: String,
    val pfpPath: String?,
    val enrollmentId: Long
)

/**
 * UI state for ClassScreen.
 */
data class ClassListUiState(
    val classes: List<ClassWithCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI state for ClassDetailsScreen.
 */
data class ClassDetailsUiState(
    val classEntity: Class? = null,
    val students: List<RosterStudent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI state for StudentDetailsScreen.
 */
data class StudentDetailsUiState(
    val studentName: String = "",
    val className: String = "",
    val pfpPath: String? = null,
    val gradeLevel: String = "",
    val birthday: String = "",
    val totalPracticeMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val vowelsProgress: Float = 0.0f,
    val consonantsProgress: Float = 0.0f,
    val stopsProgress: Float = 0.0f,
    val firstTipTitle: String? = null,
    val firstTipDescription: String? = null,
    val firstTipSubtitle: String? = null,
    val secondTipTitle: String? = null,
    val secondTipDescription: String? = null,
    val secondTipSubtitle: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for classroom feature screens.
 * Manages class, student, and enrollment data.
 */
class ClassroomViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val classRepository = ClassRepository(database.classDao())
    private val studentRepository = StudentRepository(database.studentDao())
    private val enrollmentRepository = EnrollmentRepository(database.enrollmentDao(), database.studentDao())
    private val sessionManager = SessionManager.getInstance(application)

    private val _classListUiState = MutableStateFlow(ClassListUiState())
    val classListUiState: StateFlow<ClassListUiState> = _classListUiState.asStateFlow()

    private val _classDetailsUiState = MutableStateFlow(ClassDetailsUiState())
    val classDetailsUiState: StateFlow<ClassDetailsUiState> = _classDetailsUiState.asStateFlow()

    private val _studentDetailsUiState = MutableStateFlow(StudentDetailsUiState())
    val studentDetailsUiState: StateFlow<StudentDetailsUiState> = _studentDetailsUiState.asStateFlow()

    // New: expose all students as a simple StateFlow of RosterStudent (enrollmentId = 0 when not applicable)
    private val _allStudents = MutableStateFlow<List<RosterStudent>>(emptyList())
    val allStudents: StateFlow<List<RosterStudent>> = _allStudents.asStateFlow()

    init {
        loadClasses()
        loadAllStudents()
    }

    /**
     * Load all classes for the current user.
     */
    fun loadClasses() {
        viewModelScope.launch {
            _classListUiState.value = _classListUiState.value.copy(isLoading = true)
            
            sessionManager.currentUser.collect { user ->
                if (user != null) {
                    classRepository.getActiveClassesByUserId(user.id).collect { classes ->
                        // Get student count for each class
                        val classesWithCount = classes.map { classEntity ->
                            val studentCount = enrollmentRepository.getStudentCountByClassId(classEntity.classId)
                            ClassWithCount(classEntity, studentCount)
                        }
                        
                        _classListUiState.value = ClassListUiState(
                            classes = classesWithCount,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Load all students in the database and expose them as RosterStudent.
     */
    private fun loadAllStudents() {
        viewModelScope.launch {
            studentRepository.getAllStudentsFlow().collect { students ->
                val roster = students.map {
                    RosterStudent(
                        studentId = it.studentId,
                        fullName = it.fullName,
                        pfpPath = it.pfpPath,
                        enrollmentId = 0L
                    )
                }
                _allStudents.value = roster
            }
        }
    }

    /**
     * Load details for a specific class.
     *
     * @param classId The class ID to load
     */
    fun loadClassDetails(classId: Long) {
        viewModelScope.launch {
            _classDetailsUiState.value = _classDetailsUiState.value.copy(isLoading = true)
            
            try {
                val classEntity = classRepository.getClassById(classId)
                
                if (classEntity != null) {
                    enrollmentRepository.getStudentsWithEnrollmentByClassId(classId).collect { studentsWithEnrollment ->
                        val rosterStudents = studentsWithEnrollment.map {
                            RosterStudent(
                                studentId = it.student.studentId,
                                fullName = it.student.fullName,
                                pfpPath = it.student.pfpPath,
                                enrollmentId = it.enrollment.enrollmentId
                            )
                        }
                        
                        _classDetailsUiState.value = ClassDetailsUiState(
                            classEntity = classEntity,
                            students = rosterStudents,
                            isLoading = false
                        )
                    }
                } else {
                    _classDetailsUiState.value = ClassDetailsUiState(
                        error = "Class not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _classDetailsUiState.value = ClassDetailsUiState(
                    error = "Failed to load class: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Load details for a specific student in a class (classId optional).
     *
     * @param studentId The student ID
     * @param classId The class ID (nullable). When null, load student-only details without enrollment.
     */
    fun loadStudentDetails(studentId: Long, classId: Long? = null) {
        viewModelScope.launch {
            _studentDetailsUiState.value = _studentDetailsUiState.value.copy(isLoading = true)

            try {
                val student = studentRepository.getStudentById(studentId)

                if (student == null) {
                    _studentDetailsUiState.value = StudentDetailsUiState(
                        error = "Student not found",
                        isLoading = false
                    )
                    return@launch
                }

                // Base state populated from student record
                var uiState = StudentDetailsUiState(
                    studentName = student.fullName,
                    className = "",
                    pfpPath = student.pfpPath,
                    gradeLevel = student.gradeLevel,
                    birthday = student.birthday,
                    isLoading = false
                )

                // If a classId is provided, try to load class and enrollment details
                if (classId != null && classId > 0L) {
                    val classEntity = classRepository.getClassById(classId)
                    val enrollment = enrollmentRepository.getEnrollmentByStudentAndClass(studentId, classId)

                    if (classEntity != null) uiState = uiState.copy(className = classEntity.className)

                    if (enrollment != null) {
                        uiState = uiState.copy(
                            totalPracticeMinutes = enrollment.totalPracticeMinutes,
                            sessionsCompleted = enrollment.sessionsCompleted,
                            vowelsProgress = enrollment.vowelsProgress,
                            consonantsProgress = enrollment.consonantsProgress,
                            stopsProgress = enrollment.stopsProgress,
                            firstTipTitle = enrollment.firstTipTitle,
                            firstTipDescription = enrollment.firstTipDescription,
                            firstTipSubtitle = enrollment.firstTipSubtitle,
                            secondTipTitle = enrollment.secondTipTitle,
                            secondTipDescription = enrollment.secondTipDescription,
                            secondTipSubtitle = enrollment.secondTipSubtitle
                        )
                    }
                }

                _studentDetailsUiState.value = uiState
            } catch (e: Exception) {
                _studentDetailsUiState.value = StudentDetailsUiState(
                    error = "Failed to load student details: ${e.message}",
                    isLoading = false
                )
            }
        }
    }


    /**
     * Create a new class.
     *
     * @param className The class name
     * @param classCode The class code
     * @param bannerPath Optional banner path
     * @param onSuccess Callback on success
     * @param onError Callback on error
     */
    fun createClass(
        className: String,
        classCode: String,
        bannerPath: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            sessionManager.currentUser.value?.let { user ->
                when (val result = classRepository.createClass(user.id, className, classCode, bannerPath)) {
                    is ClassRepository.ClassOperationResult.Success -> onSuccess()
                    is ClassRepository.ClassOperationResult.Error -> onError(result.message)
                }
            } ?: onError("User not logged in")
        }
    }

    /**
     * Update an existing class.
     *
     * @param classId The class ID
     * @param className The new class name
     * @param classCode The new class code
     * @param bannerPath Optional new banner path
     * @param onSuccess Callback on success
     * @param onError Callback on error
     */
    fun updateClass(
        classId: Long,
        className: String,
        classCode: String,
        bannerPath: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = classRepository.updateClass(classId, className, classCode, bannerPath)) {
                is ClassRepository.ClassOperationResult.Success -> onSuccess()
                is ClassRepository.ClassOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Archive a class.
     *
     * @param classId The class ID to archive
     * @param onSuccess Callback on success
     * @param onError Callback on error
     */
    fun archiveClass(
        classId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = classRepository.archiveClass(classId)) {
                is ClassRepository.ClassOperationResult.Success -> onSuccess()
                is ClassRepository.ClassOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Add a student and enroll them in a class.
     *
     * @param fullName Student's full name
     * @param gradeLevel Student's grade level
     * @param birthday Student's birthday
     * @param pfpPath Optional profile picture path
     * @param classId The class ID to enroll in
     * @param onSuccess Callback on success with student ID
     * @param onError Callback on error
     */
    fun addStudentToClass(
        fullName: String,
        gradeLevel: String,
        birthday: String,
        pfpPath: String?,
        classId: Long,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val studentResult = studentRepository.addStudent(fullName, gradeLevel, birthday, pfpPath)) {
                is StudentRepository.StudentOperationResult.Success -> {
                    val studentId = studentResult.studentId
                    when (val enrollResult = enrollmentRepository.enrollStudent(studentId, classId)) {
                        is EnrollmentRepository.EnrollmentOperationResult.Success -> onSuccess(studentId)
                        is EnrollmentRepository.EnrollmentOperationResult.Error -> onError(enrollResult.message)
                    }
                }
                is StudentRepository.StudentOperationResult.Error -> onError(studentResult.message)
            }
        }
    }

    /**
     * Remove a student from a class (unenroll).
     *
     * @param studentId The student ID
     * @param classId The class ID
     * @param onSuccess Callback on success
     * @param onError Callback on error
     */
    fun removeStudentFromClass(
        studentId: Long,
        classId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = enrollmentRepository.unenrollStudent(studentId, classId)) {
                is EnrollmentRepository.EnrollmentOperationResult.Success -> onSuccess()
                is EnrollmentRepository.EnrollmentOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Update student information.
     *
     * @param studentId The student ID
     * @param fullName New full name
     * @param gradeLevel New grade level
     * @param birthday New birthday
     * @param pfpPath Optional new profile picture path
     * @param onSuccess Callback on success
     * @param onError Callback on error
     */
    fun updateStudent(
        studentId: Long,
        fullName: String,
        gradeLevel: String,
        birthday: String,
        pfpPath: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = studentRepository.updateStudent(studentId, fullName, gradeLevel, birthday, pfpPath)) {
                is StudentRepository.StudentOperationResult.Success -> onSuccess()
                is StudentRepository.StudentOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Add a student without enrolling in a class.
     *
     * @param fullName Student's full name
     * @param gradeLevel Student's grade level
     * @param pfpPath Optional profile picture path
     * @param onSuccess Callback on success with student ID
     * @param onError Callback on error
     */
    fun addStudent(
        fullName: String,
        gradeLevel: String,
        pfpPath: String?,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val studentResult = studentRepository.addStudent(fullName, gradeLevel, "", pfpPath)) {
                is StudentRepository.StudentOperationResult.Success -> onSuccess(studentResult.studentId)
                is StudentRepository.StudentOperationResult.Error -> onError(studentResult.message)
            }
        }
    }
}