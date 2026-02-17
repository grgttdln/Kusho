package com.example.kusho.presentation.tutorial

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kusho.ml.AirWritingClassifier
import com.example.kusho.ml.ClassifierLoadResult
import com.example.kusho.sensors.MotionSensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for Tutorial Mode gesture recognition.
 * Handles: countdown -> gesture recording -> classification -> send result to phone
 */
class TutorialModeViewModel(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult,
    private val targetLetter: String,
    private val letterCase: String,
    private val onGestureResult: (isCorrect: Boolean, predictedLetter: String) -> Unit
) : ViewModel() {

    companion object {
        private const val TAG = "TutorialModeVM"
        private const val COUNTDOWN_SECONDS = 3
        private const val RECORDING_SECONDS = 3
        private const val PROGRESS_UPDATE_INTERVAL_MS = 50L
        
        // Letters with similar shapes in uppercase and lowercase
        private val SIMILAR_SHAPE_LETTERS = setOf(
            'C', 'K', 'O', 'P', 'S', 'V', 'W', 'X', 'Z'
        )
    }

    enum class State {
        COUNTDOWN,
        RECORDING,
        PROCESSING,
        SHOWING_PREDICTION,
        RESULT,
        COMPLETE
    }

    data class UiState(
        val state: State = State.COUNTDOWN,
        val countdownSeconds: Int = COUNTDOWN_SECONDS,
        val recordingProgress: Float = 0f,
        val prediction: String? = null,
        val isCorrect: Boolean = false,
        val statusMessage: String = "Get ready...",
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var recordingJob: Job? = null
    private val classifier: AirWritingClassifier?

    init {
        // Process classifier load result
        classifier = when (classifierResult) {
            is ClassifierLoadResult.Success -> {
                Log.i(TAG, "✓ Model loaded for Tutorial Mode")
                classifierResult.classifier
            }
            is ClassifierLoadResult.Error -> {
                Log.e(TAG, "✗ Model failed: ${classifierResult.message}")
                null
            }
        }
        
        // Auto-start the recognition flow
        startRecognition()
    }

    /**
     * Reset the ViewModel state and restart recognition.
     * Call this when user wants to retry after incorrect/error.
     */
    fun reset() {
        Log.d(TAG, "Resetting ViewModel for retry")
        recordingJob?.cancel()
        _uiState.value = UiState()  // Reset to initial state
        startRecognition()
    }

    /**
     * Start the recognition flow: countdown -> record -> classify -> send result
     */
    private fun startRecognition() {
        if (classifier == null) {
            Log.e(TAG, "Cannot start: classifier is null")
            _uiState.update {
                it.copy(
                    state = State.COMPLETE,
                    errorMessage = "Model not loaded",
                    statusMessage = "Error"
                )
            }
            onGestureResult(false, "")
            return
        }

        recordingJob?.cancel()
        // Reset state to initial before starting
        _uiState.value = UiState()
        
        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                // === Phase 1: Countdown ===
                Log.d(TAG, "Starting countdown for letter: $targetLetter")
                for (i in COUNTDOWN_SECONDS downTo 1) {
                    if (!isActive) return@launch
                    _uiState.update {
                        it.copy(
                            state = State.COUNTDOWN,
                            countdownSeconds = i,
                            statusMessage = "Get ready...",
                            errorMessage = null
                        )
                    }
                    delay(1000)
                }

                if (!isActive) return@launch

                // === Phase 2: Recording ===
                Log.d(TAG, "Starting recording...")
                _uiState.update {
                    it.copy(
                        state = State.RECORDING,
                        countdownSeconds = 0,
                        statusMessage = "Write now!",
                        recordingProgress = 0f
                    )
                }

                // Start sensor recording
                sensorManager.startRecording()

                // Wait for recording duration with progress updates
                val totalMs = RECORDING_SECONDS * 1000L
                val totalUpdates = totalMs / PROGRESS_UPDATE_INTERVAL_MS
                var updateCount = 0

                while (isActive && updateCount < totalUpdates) {
                    delay(PROGRESS_UPDATE_INTERVAL_MS)
                    updateCount++
                    _uiState.update {
                        it.copy(recordingProgress = updateCount.toFloat() / totalUpdates)
                    }
                }

                if (!isActive) return@launch

                // Stop sensor recording
                sensorManager.stopRecording()
                Log.d(TAG, "Recording stopped")

                // === Phase 3: Processing ===
                _uiState.update {
                    it.copy(
                        state = State.PROCESSING,
                        statusMessage = "Processing...",
                        recordingProgress = 1f
                    )
                }

                // Get collected samples
                val samples = sensorManager.getCollectedSamples()
                Log.d(TAG, "Collected ${samples.size} samples")

                // Check if we have enough data
                val minSamples = classifier.windowSize / 2
                if (samples.size < minSamples) {
                    Log.w(TAG, "Not enough samples: ${samples.size} < $minSamples")
                    _uiState.update {
                        it.copy(
                            state = State.COMPLETE,
                            errorMessage = "Not enough motion",
                            statusMessage = "Try again",
                            recordingProgress = 0f
                        )
                    }
                    onGestureResult(false, "")
                    return@launch
                }

                // Run classification
                val result = classifier.classify(samples)
                Log.d(TAG, "Classification result: $result")

                if (!result.success) {
                    _uiState.update {
                        it.copy(
                            state = State.COMPLETE,
                            errorMessage = result.errorMessage,
                            statusMessage = "Try again",
                            recordingProgress = 0f
                        )
                    }
                    onGestureResult(false, "")
                    return@launch
                }

                // === Phase 4: Show prediction ===
                val predictedLetter = result.label  // Model always outputs uppercase
                
                // Show the predicted letter first
                _uiState.update {
                    it.copy(
                        state = State.SHOWING_PREDICTION,
                        prediction = predictedLetter,
                        statusMessage = "$predictedLetter",
                        errorMessage = null,
                        recordingProgress = 0f
                    )
                }
                
                // Brief delay to show the prediction
                delay(1000)
                
                // === Phase 5: Check if correct ===
                // Determine the expected letter case based on letterCase parameter
                val expectedLetter = when (letterCase.lowercase()) {
                    "small", "lowercase" -> targetLetter.lowercase()
                    else -> targetLetter.uppercase()
                }
                
                // Check if this letter has similar shapes in both cases
                val targetUppercase = targetLetter.uppercase().firstOrNull()
                val isSimilarShape = targetUppercase != null && targetUppercase in SIMILAR_SHAPE_LETTERS
                
                // For similar shape letters, use case-insensitive comparison
                // For others (like A/a), enforce case-sensitive
                val isCorrect = if (isSimilarShape) {
                    predictedLetter.equals(expectedLetter, ignoreCase = true)
                } else {
                    predictedLetter.equals(expectedLetter, ignoreCase = false)
                }
                
                Log.d(TAG, "Predicted: $predictedLetter, Expected: $expectedLetter (case: $letterCase), Similar: $isSimilarShape, Correct: $isCorrect")
                
                // Show the result state
                _uiState.update {
                    it.copy(
                        state = State.RESULT,
                        isCorrect = isCorrect
                    )
                }
                
                // Wait for TTS to complete (2 seconds)
                delay(2000)
                
                if (!isActive) return@launch
                
                // Then transition to complete state
                _uiState.update {
                    it.copy(
                        state = State.COMPLETE,
                        statusMessage = if (isCorrect) "Correct!" else "Try again"
                    )
                }

                // Send result to phone
                onGestureResult(isCorrect, predictedLetter)

            } catch (e: Exception) {
                Log.e(TAG, "Recognition error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        state = State.COMPLETE,
                        errorMessage = "Error: ${e.message}",
                        statusMessage = "Try again",
                        recordingProgress = 0f
                    )
                }
                onGestureResult(false, "")
            }
        }
    }

    /**
     * Cancel current recording
     */
    fun cancelRecognition() {
        Log.d(TAG, "Cancelling recognition")
        recordingJob?.cancel()
        recordingJob = null
        sensorManager.stopRecording()
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        sensorManager.stopRecording()
    }
}

/**
 * Factory for TutorialModeViewModel
 */
class TutorialModeViewModelFactory(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult,
    private val targetLetter: String,
    private val letterCase: String,
    private val onGestureResult: (Boolean, String) -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TutorialModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TutorialModeViewModel(sensorManager, classifierResult, targetLetter, letterCase, onGestureResult) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
