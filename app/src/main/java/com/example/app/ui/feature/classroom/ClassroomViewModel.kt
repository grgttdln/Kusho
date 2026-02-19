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
import com.example.app.data.repository.StudentTeacherRepository
import com.example.app.data.entity.AnnotationSummary
import com.example.app.data.entity.LearnerProfileAnnotation
import com.example.app.data.repository.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val enrollmentId: Long,
    val dominantHand: String = "RIGHT"
)

/**
 * Data class representing a completed activity/set for a student.
 */
data class CompletedActivitySet(
    val activityId: Long,
    val activityName: String,
    val setId: Long,
    val setName: String,
    val completedAt: Long?,
    val isTutorial: Boolean = false,
    val annotation: com.example.app.data.entity.LearnerProfileAnnotation? = null
)

/**
 * Data class representing a completed tutorial session for a student.
 */
data class CompletedTutorialSession(
    val tutorialType: String, // "Vowels", "Consonants", "Stops"
    val letterType: String,   // "Capital", "Small"
    val completedAt: Long?,
    val setId: Long = generateTutorialSetId(tutorialType, letterType),
    val annotation: com.example.app.data.entity.LearnerProfileAnnotation? = null
)

/**
 * Generate a unique setId based on tutorial session (title + letterType)
 * Using negative IDs to avoid collision with actual database setIds
 */
fun generateTutorialSetId(title: String, letterType: String): Long {
    return when {
        title.equals("Vowels", ignoreCase = true) && letterType.equals("capital", ignoreCase = true) -> -1L
        title.equals("Vowels", ignoreCase = true) && letterType.equals("small", ignoreCase = true) -> -2L
        title.equals("Consonants", ignoreCase = true) && letterType.equals("capital", ignoreCase = true) -> -3L
        title.equals("Consonants", ignoreCase = true) && letterType.equals("small", ignoreCase = true) -> -4L
        else -> -(kotlin.math.abs("$title-$letterType".hashCode().toLong()) % 1000 + 5)
    }
}

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
    val dominantHand: String = "RIGHT",
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
    val completedLearnSets: List<CompletedActivitySet> = emptyList(),
    val completedTutorialSessions: List<CompletedTutorialSession> = emptyList(),
    val annotationSummaries: Map<String, String> = emptyMap(),
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
    private val enrollmentRepository =
        EnrollmentRepository(database.enrollmentDao(), database.studentDao())
    private val studentTeacherRepository = StudentTeacherRepository(database.studentTeacherDao())
    private val sessionManager = SessionManager.getInstance(application)
    private val annotationSummaryDao = database.annotationSummaryDao()
    private val geminiRepository = GeminiRepository()

    private val _classListUiState = MutableStateFlow(ClassListUiState())
    val classListUiState: StateFlow<ClassListUiState> = _classListUiState.asStateFlow()

    private val _classDetailsUiState = MutableStateFlow(ClassDetailsUiState())
    val classDetailsUiState: StateFlow<ClassDetailsUiState> = _classDetailsUiState.asStateFlow()

    private val _studentDetailsUiState = MutableStateFlow(StudentDetailsUiState())
    val studentDetailsUiState: StateFlow<StudentDetailsUiState> =
        _studentDetailsUiState.asStateFlow()

    // New: expose all students as a simple StateFlow of RosterStudent (enrollmentId = 0 when not applicable)
    private val _allStudents = MutableStateFlow<List<RosterStudent>>(emptyList())
    val allStudents: StateFlow<List<RosterStudent>> = _allStudents.asStateFlow()

    // Global student directory (all students in DB)
    private val _studentDirectory = MutableStateFlow<List<RosterStudent>>(emptyList())
    val studentDirectory: StateFlow<List<RosterStudent>> = _studentDirectory.asStateFlow()

    init {
        loadClasses()
        loadAllStudents()
        loadStudentDirectory()
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
                            val studentCount =
                                enrollmentRepository.getStudentCountByClassId(classEntity.classId)
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
            sessionManager.currentUser.collect { user ->
                if (user != null) {
                    database.studentDao().getStudentsForTeacherFlow(user.id).collect { studentsForTeacher ->
                        val roster = studentsForTeacher.map {
                            RosterStudent(studentId = it.studentId, fullName = it.fullName, pfpPath = it.pfpPath, enrollmentId = 0L, dominantHand = it.dominantHand)
                        }
                        _allStudents.value = roster
                    }
                } else {
                    // No user logged in — expose all students
                    studentRepository.getAllStudentsFlow().collect { students ->
                        val roster = students.map {
                            RosterStudent(
                                studentId = it.studentId,
                                fullName = it.fullName,
                                pfpPath = it.pfpPath,
                                enrollmentId = 0L,
                                dominantHand = it.dominantHand
                            )
                        }
                        _allStudents.value = roster
                    }
                }
            }
        }
    }

    /**
     * Load the global student directory (all students in DB). This is used by AddExistingStudentsScreen.
     */
    fun loadStudentDirectory() {
        viewModelScope.launch {
            database.studentDao().getAllStudentsFlow().collect { students ->
                val list = students.map { RosterStudent(studentId = it.studentId, fullName = it.fullName, pfpPath = it.pfpPath, enrollmentId = 0L, dominantHand = it.dominantHand) }
                _studentDirectory.value = list
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
                    enrollmentRepository.getStudentsWithEnrollmentByClassId(classId)
                        .collect { studentsWithEnrollment ->
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
                    dominantHand = student.dominantHand,
                    isLoading = false
                )

                // If a classId is provided, try to load class and enrollment details
                if (classId != null && classId > 0L) {
                    val classEntity = classRepository.getClassById(classId)
                    val enrollment =
                        enrollmentRepository.getEnrollmentByStudentAndClass(studentId, classId)

                    if (classEntity != null) uiState =
                        uiState.copy(className = classEntity.className)

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

                // Load completed sets for the student
                val studentSetProgressDao = database.studentSetProgressDao()
                val activityDao = database.activityDao()
                val setDao = database.setDao()
                
                val completedProgress = studentSetProgressDao.getProgressForStudent(studentId)
                    .first()
                    .filter { it.isCompleted }
                
                // Load annotations for learn mode
                val annotationDao = database.learnerProfileAnnotationDao()
                
                val completedSets = completedProgress.mapNotNull { progress ->
                    val activity = activityDao.getActivityById(progress.activityId)
                    val set = setDao.getSetById(progress.setId)
                    
                    if (activity != null && set != null) {
                        // Load annotation for this completed set (scoped to activity)
                        val annotation = annotationDao.getAnnotationsForStudentInSet(
                            studentId = studentId.toString(),
                            setId = set.id,
                            sessionMode = com.example.app.data.entity.LearnerProfileAnnotation.MODE_LEARN,
                            activityId = activity.id
                        ).firstOrNull()
                        
                        CompletedActivitySet(
                            activityId = activity.id,
                            activityName = activity.title,
                            setId = set.id,
                            setName = set.title,
                            completedAt = progress.completedAt,
                            isTutorial = false, // Learn mode sets
                            annotation = annotation
                        )
                    } else null
                }
                
                // Load tutorial annotations from learner_profile_annotations table
                val tutorialSessions = mutableListOf<CompletedTutorialSession>()
                
                // Define all possible tutorial combinations
                val tutorialCombinations = listOf(
                    Triple("Vowels", "Capital", -1L),
                    Triple("Vowels", "Small", -2L),
                    Triple("Consonants", "Capital", -3L),
                    Triple("Consonants", "Small", -4L)
                )
                
                // Load tutorial completions
                val tutorialCompletionDao = database.tutorialCompletionDao()
                val tutorialCompletions = tutorialCompletionDao.getCompletionsForStudent(studentId)
                val completedTutorialSetIds = tutorialCompletions.associate { it.tutorialSetId to it.completedAt }

                // Fetch annotations for each tutorial type — one card per category
                tutorialCombinations.forEach { (tutorialType, letterType, setId) ->
                    val annotations = annotationDao.getAnnotationsForStudentInSet(
                        studentId = studentId.toString(),
                        setId = setId,
                        sessionMode = com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL
                    )

                    if (annotations.isNotEmpty()) {
                        // Has annotations — show card with most recent annotation
                        val mostRecent = annotations.maxByOrNull { it.updatedAt }
                        tutorialSessions.add(
                            CompletedTutorialSession(
                                tutorialType = tutorialType,
                                letterType = letterType,
                                completedAt = mostRecent?.createdAt,
                                setId = setId,
                                annotation = mostRecent
                            )
                        )
                    } else if (completedTutorialSetIds.containsKey(setId)) {
                        // No annotations but tutorial was completed — show placeholder card
                        tutorialSessions.add(
                            CompletedTutorialSession(
                                tutorialType = tutorialType,
                                letterType = letterType,
                                completedAt = completedTutorialSetIds[setId],
                                setId = setId,
                                annotation = null
                            )
                        )
                    }
                }
                
                // Load AI annotation summaries
                val summaries = annotationSummaryDao.getSummariesForStudent(studentId.toString())
                val summaryMap = summaries.associate { "${it.setId}|${it.sessionMode}|${it.activityId}" to it.summaryText }

                uiState = uiState.copy(
                    completedLearnSets = completedSets,
                    completedTutorialSessions = tutorialSessions,
                    annotationSummaries = summaryMap
                )

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
     * Generate an AI summary for a set/category's annotations and store it.
     * Called after annotation save in session screens. Runs silently — failures are logged but not surfaced.
     */
    fun generateAnnotationSummary(studentId: String, setId: Long, sessionMode: String, activityId: Long = 0L) {
        viewModelScope.launch {
            try {
                val annotationDao = database.learnerProfileAnnotationDao()
                val annotations = annotationDao.getAnnotationsForStudentInSet(
                    studentId = studentId,
                    setId = setId,
                    sessionMode = sessionMode,
                    activityId = activityId
                )

                if (annotations.isEmpty()) return@launch

                val summaryText = geminiRepository.generateAnnotationSummary(annotations, sessionMode)
                    ?: return@launch

                val summary = AnnotationSummary(
                    studentId = studentId,
                    setId = setId,
                    sessionMode = sessionMode,
                    activityId = activityId,
                    summaryText = summaryText
                )
                annotationSummaryDao.insertOrUpdate(summary)
            } catch (e: Exception) {
                android.util.Log.e("ClassroomViewModel", "Summary generation failed: ${e.message}", e)
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
                when (val result =
                    classRepository.createClass(user.id, className, classCode, bannerPath)) {
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
            when (val result =
                classRepository.updateClass(classId, className, classCode, bannerPath)) {
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
            when (val studentResult =
                studentRepository.addStudent(fullName, gradeLevel, birthday, pfpPath)) {
                is StudentRepository.StudentOperationResult.Success -> {
                    val studentId = studentResult.studentId
                    when (val enrollResult =
                        enrollmentRepository.enrollStudent(studentId, classId)) {
                        is EnrollmentRepository.EnrollmentOperationResult.Success -> onSuccess(
                            studentId
                        )

                        is EnrollmentRepository.EnrollmentOperationResult.Error -> onError(
                            enrollResult.message
                        )
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
            when (val result = studentRepository.updateStudent(
                studentId,
                fullName,
                gradeLevel,
                birthday,
                pfpPath
            )) {
                is StudentRepository.StudentOperationResult.Success -> onSuccess()
                is StudentRepository.StudentOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Update a student's dominant hand.
     *
     * @param studentId The student ID
     * @param dominantHand "LEFT" or "RIGHT"
     * @param onSuccess Callback on success
     * @param onError Callback on error
     */
    fun updateStudentDominantHand(
        studentId: Long,
        dominantHand: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                database.studentDao().updateStudentDominantHand(studentId, dominantHand)
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to update dominant hand: ${e.message}")
            }
        }
    }

    /**
     * Add a student without enrolling in a class.
     * Kept for screens that create students independently.
     */
    fun addStudent(
        fullName: String,
        gradeLevel: String,
        pfpPath: String?,
        dominantHand: String = "RIGHT",
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val studentResult =
                studentRepository.addStudent(fullName, gradeLevel, "", pfpPath, dominantHand)) {
                is StudentRepository.StudentOperationResult.Success -> onSuccess(studentResult.studentId)
                is StudentRepository.StudentOperationResult.Error -> onError(studentResult.message)
            }
        }
    }

    /**
     * Add a student and link to multiple teacher users.
     */
    fun addStudentWithTeachers(
        fullName: String,
        gradeLevel: String,
        pfpPath: String?,
        teacherIds: List<Long>,
        dominantHand: String = "RIGHT",
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val studentResult = studentRepository.addStudent(fullName, gradeLevel, "", pfpPath, dominantHand)) {
                is StudentRepository.StudentOperationResult.Success -> {
                    val studentId = studentResult.studentId
                    val linkResult = studentTeacherRepository.linkStudentToTeachers(studentId, teacherIds)
                    if (linkResult.isSuccess) onSuccess(studentId) else onError(linkResult.exceptionOrNull()?.message ?: "Failed to link teachers")
                }
                is StudentRepository.StudentOperationResult.Error -> onError(studentResult.message)
            }
        }
    }

    /**
     * Helper to get current user ID: prefers in-memory session currentUser, falls back to persisted prefs.
     */
    fun getCurrentUserId(): Long {
        return sessionManager.currentUser.value?.id ?: sessionManager.getUserId()
    }

    /**
     * Delete a student completely from the database (also cascades enrollments and teacher mappings).
     */
    fun deleteStudent(
        studentId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // First, delete the student-teacher mappings
            studentTeacherRepository.deleteMappingsForStudent(studentId)

            // Then delete the student (which also cascades enrollments)
            when (val result = studentRepository.deleteStudent(studentId)) {
                is StudentRepository.StudentOperationResult.Success -> onSuccess()
                is StudentRepository.StudentOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Add an existing student to a class (create class enrollment only).
     */
    fun addExistingStudentToClass(studentId: Long, classId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = enrollmentRepository.enrollStudent(studentId, classId)) {
                is com.example.app.data.repository.EnrollmentRepository.EnrollmentOperationResult.Success -> {
                    // refresh class details and roster
                    loadClassDetails(classId)
                    onSuccess()
                }
                is com.example.app.data.repository.EnrollmentRepository.EnrollmentOperationResult.Error -> onError(result.message)
            }
        }
    }

    /**
     * Create a new student and add them to a class.
     */
    fun createStudentAndAddToClass(fullName: String, gradeLevel: String, birthday: String, pfpPath: String?, classId: Long, onSuccess: (Long) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val studentResult = studentRepository.addStudent(fullName, gradeLevel, birthday, pfpPath)) {
                is StudentRepository.StudentOperationResult.Success -> {
                    val studentId = studentResult.studentId
                    when (val enrollResult = enrollmentRepository.enrollStudent(studentId, classId)) {
                        is com.example.app.data.repository.EnrollmentRepository.EnrollmentOperationResult.Success -> {
                            loadClassDetails(classId)
                            onSuccess(studentId)
                        }
                        is com.example.app.data.repository.EnrollmentRepository.EnrollmentOperationResult.Error -> onError(enrollResult.message)
                    }
                }
                is StudentRepository.StudentOperationResult.Error -> onError(studentResult.message)
            }
        }
    }
}
