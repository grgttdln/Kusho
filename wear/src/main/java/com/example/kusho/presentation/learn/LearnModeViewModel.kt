package com.example.kusho.presentation.learn

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
 * ViewModel for Learn Mode.
 * Handles gesture recognition for fill-in-the-blanks mode.
 */
class LearnModeViewModel(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult
) : ViewModel() {

    companion object {
        private const val TAG = "LearnModeVM"
        private const val COUNTDOWN_SECONDS = 3
        private const val RECORDING_SECONDS = 3
        private const val PREDICTION_DISPLAY_MS = 1500L // 1.5 seconds
        private const val RESULT_DISPLAY_SECONDS = 3
        private const val PROGRESS_UPDATE_INTERVAL_MS = 50L
    }

    enum class State {
        IDLE,
        COUNTDOWN,
        RECORDING,
        PROCESSING,
        SHOWING_PREDICTION,
        RESULT
    }

    data class UiState(
        val title: String = "Learn Mode",
        val state: State = State.IDLE,
        val countdownSeconds: Int = 0,
        val recordingProgress: Float = 0f,
        val prediction: String? = null,
        val confidence: Float? = null,
        val isCorrect: Boolean? = null,
        val statusMessage: String = "Tap to guess",
        val errorMessage: String? = null,
        val isModelLoaded: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var recordingJob: Job? = null
    private val classifier: AirWritingClassifier?

    // Current word data from LearnModeStateHolder
    private var currentMaskedLetter: String = ""

    init {
        // Process classifier load result
        when (classifierResult) {
            is ClassifierLoadResult.Success -> {
                classifier = classifierResult.classifier
                Log.i(TAG, "✓ Model loaded for Learn Mode")
                _uiState.update {
                    it.copy(isModelLoaded = true, statusMessage = "Tap to guess")
                }
            }
            is ClassifierLoadResult.Error -> {
                classifier = null
                Log.e(TAG, "✗ Model failed: ${classifierResult.message}")
                _uiState.update {
                    it.copy(
                        errorMessage = classifierResult.message,
                        isModelLoaded = false,
                        statusMessage = "Model not loaded"
                    )
                }
            }
        }

        // Observe word data from LearnModeStateHolder
        viewModelScope.launch {
            LearnModeStateHolder.wordData.collect { wordData ->
                if (wordData.word.isNotEmpty() && wordData.maskedIndex >= 0) {
                    // Preserve the original case of the masked letter
                    currentMaskedLetter = wordData.word.getOrNull(wordData.maskedIndex)?.toString() ?: ""
                    Log.d(TAG, "📚 Masked letter to guess: $currentMaskedLetter")
                    // Reset to idle when new word arrives
                    if (_uiState.value.state == State.RESULT) {
                        _uiState.update {
                            it.copy(
                                state = State.IDLE,
                                statusMessage = "Tap to guess",
                                prediction = null,
                                isCorrect = null
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Start the recording flow for guessing the masked letter
     */
    fun startRecording() {
        if (_uiState.value.state != State.IDLE) {
            Log.d(TAG, "Not in IDLE state, ignoring start")
            return
        }

        if (classifier == null) {
            Log.e(TAG, "Cannot start: classifier is null")
            _uiState.update { it.copy(errorMessage = "Model not loaded") }
            return
        }

        if (currentMaskedLetter.isEmpty()) {
            Log.e(TAG, "Cannot start: no masked letter to guess")
            _uiState.update { it.copy(errorMessage = "No letter to guess") }
            return
        }

        recordingJob?.cancel()
        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                // === Phase 1: Countdown ===
                Log.d(TAG, "Starting countdown...")
                for (i in COUNTDOWN_SECONDS downTo 1) {
                    if (!isActive) return@launch
                    _uiState.update {
                        it.copy(
                            state = State.COUNTDOWN,
                            countdownSeconds = i,
                            statusMessage = "Get ready...",
                            errorMessage = null,
                            prediction = null,
                            isCorrect = null
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
                        statusMessage = "Write now!",
                        recordingProgress = 0f
                    )
                }

                sensorManager.startRecording()

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

                val samples = sensorManager.getCollectedSamples()
                Log.d(TAG, "Collected ${samples.size} samples")

                val minSamples = classifier.windowSize / 2
                if (samples.size < minSamples) {
                    Log.w(TAG, "Not enough samples: ${samples.size} < $minSamples")
                    _uiState.update {
                        it.copy(
                            state = State.IDLE,
                            errorMessage = "Not enough motion",
                            statusMessage = "Try again",
                            recordingProgress = 0f
                        )
                    }
                    return@launch
                }

                val result = classifier.classify(samples)
                Log.d(TAG, "Classification result: $result")

                if (!result.success) {
                    _uiState.update {
                        it.copy(
                            state = State.IDLE,
                            errorMessage = result.errorMessage,
                            statusMessage = "Try again",
                            recordingProgress = 0f
                        )
                    }
                    return@launch
                }

                // === Phase 4: Show the predicted letter first ===
                // Keep the predicted letter as the raw input from the user (no case conversion)
                val predictedLetter = result.label
                // Compare case-sensitively - user must write the correct case
                val isCorrect = predictedLetter == currentMaskedLetter

                Log.d(TAG, "Predicted: $predictedLetter, Expected: $currentMaskedLetter, Correct: $isCorrect")

                // Show the predicted letter for 1.5 seconds
                _uiState.update {
                    it.copy(
                        state = State.SHOWING_PREDICTION,
                        prediction = predictedLetter,
                        confidence = result.confidence,
                        isCorrect = isCorrect,
                        errorMessage = null,
                        recordingProgress = 0f
                    )
                }

                // Wait 1.5 seconds showing the prediction
                delay(PREDICTION_DISPLAY_MS)

                if (!isActive) return@launch

                // === Phase 5: Show the result (correct/wrong) ===
                _uiState.update {
                    it.copy(
                        state = State.RESULT,
                        statusMessage = if (isCorrect) "Correct! ✓" else "Try again"
                    )
                }

                // Auto-reset after showing result
                delay(RESULT_DISPLAY_SECONDS * 1000L)
                if (isActive && _uiState.value.state == State.RESULT) {
                    _uiState.update {
                        it.copy(
                            state = State.IDLE,
                            statusMessage = "Tap to guess"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Recording error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        state = State.IDLE,
                        errorMessage = "Error: ${e.message}",
                        statusMessage = "Try again",
                        recordingProgress = 0f
                    )
                }
            }
        }
    }

    /**
     * Cancel current recording/countdown
     */
    fun cancelRecording() {
        Log.d(TAG, "Cancelling recording")
        recordingJob?.cancel()
        recordingJob = null
        sensorManager.stopRecording()
        _uiState.update {
            it.copy(
                state = State.IDLE,
                statusMessage = "Tap to guess",
                errorMessage = null,
                recordingProgress = 0f,
                countdownSeconds = 0
            )
        }
    }

    /**
     * Reset from result state back to idle
     */
    fun resetToIdle() {
        Log.d(TAG, "Resetting to idle")
        _uiState.update {
            it.copy(
                state = State.IDLE,
                statusMessage = "Tap to guess",
                errorMessage = null,
                prediction = null,
                isCorrect = null,
                recordingProgress = 0f,
                countdownSeconds = 0
            )
        }
    }

    /**
     * Get the current masked letter (for UI display)
     */
    fun getCurrentMaskedLetter(): String = currentMaskedLetter

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        sensorManager.stopRecording()
        classifier?.close()
    }
}

class LearnModeViewModelFactory(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LearnModeViewModel::class.java)) {
            return LearnModeViewModel(sensorManager, classifierResult) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

