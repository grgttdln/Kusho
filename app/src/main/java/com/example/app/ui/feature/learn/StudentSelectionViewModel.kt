package com.example.app.ui.feature.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.Class
import com.example.app.data.repository.ClassRepository
import com.example.app.data.repository.EnrollmentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Data class representing a student for selection.
 */
data class SelectableStudent(
    val studentId: Long,
    val fullName: String,
    val pfpPath: String?
)

/**
 * Data class representing a class with its students.
 */
data class ClassWithStudents(
    val classEntity: Class,
    val students: List<SelectableStudent>
)

/**
 * UI state for student selection screens.
 */
data class StudentSelectionUiState(
    val classesWithStudents: List<ClassWithStudents> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStudent: SelectableStudent? = null,
    val selectedClass: Class? = null
)

/**
 * Shared ViewModel for student selection screens (TutorialMode and LearnMode).
 * Manages loading classes and their enrolled students for selection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val classRepository = ClassRepository(database.classDao())
    private val enrollmentRepository = EnrollmentRepository(database.enrollmentDao(), database.studentDao())
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(StudentSelectionUiState())
    val uiState: StateFlow<StudentSelectionUiState> = _uiState.asStateFlow()

    init {
        loadClassesWithStudents()
    }

    /**
     * Load all active classes with their enrolled students for the current user.
     * Uses flatMapLatest and combine to properly react to database changes.
     */
    fun loadClassesWithStudents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            sessionManager.currentUser
                .flatMapLatest { user ->
                    if (user != null) {
                        classRepository.getActiveClassesByUserId(user.id)
                            .flatMapLatest { classes ->
                                if (classes.isEmpty()) {
                                    flowOf(emptyList())
                                } else {
                                    // Combine all student flows for each class
                                    val studentFlows = classes.map { classEntity ->
                                        enrollmentRepository.getStudentsWithEnrollmentByClassId(classEntity.classId)
                                            .map { studentsWithEnrollment ->
                                                ClassWithStudents(
                                                    classEntity = classEntity,
                                                    students = studentsWithEnrollment.map {
                                                        SelectableStudent(
                                                            studentId = it.student.studentId,
                                                            fullName = it.student.fullName,
                                                            pfpPath = it.student.pfpPath
                                                        )
                                                    }
                                                )
                                            }
                                    }
                                    // Combine all class flows into a single list
                                    combine(studentFlows) { it.toList() }
                                }
                            }
                    } else {
                        flowOf(null) // Emit null to indicate no user
                    }
                }
                .collect { result ->
                    when {
                        result == null -> {
                            _uiState.value = StudentSelectionUiState(
                                isLoading = false,
                                error = "Please log in to view students"
                            )
                        }
                        else -> {
                            _uiState.value = StudentSelectionUiState(
                                classesWithStudents = result,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    /**
     * Select a student.
     *
     * @param student The student to select
     * @param classEntity The class the student belongs to
     */
    fun selectStudent(student: SelectableStudent, classEntity: Class) {
        _uiState.value = _uiState.value.copy(
            selectedStudent = student,
            selectedClass = classEntity
        )
    }

    /**
     * Clear the current selection.
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedStudent = null,
            selectedClass = null
        )
    }
}

