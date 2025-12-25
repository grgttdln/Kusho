package com.example.kusho.presentation.practice

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
 * ViewModel for Practice Mode.
 * Handles: countdown -> gesture recording -> classification -> result display
 */
class PracticeModeViewModel(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult
) : ViewModel() {

    // Recording configuration
    companion object {
        private const val TAG = "PracticeModeVM"
        private const val COUNTDOWN_SECONDS = 3
        private const val RECORDING_SECONDS = 3
        private const val RESULT_DISPLAY_SECONDS = 3
        private const val PROGRESS_UPDATE_INTERVAL_MS = 50L
    }

    enum class State {
        IDLE,
        COUNTDOWN,
        RECORDING,
        PROCESSING,
        RESULT
    }

    data class UiState(
        val state: State = State.IDLE,
        val countdownSeconds: Int = 0,
        val recordingProgress: Float = 0f,
        val prediction: String? = null,
        val confidence: Float? = null,
        val statusMessage: String = "Tap Start",
        val errorMessage: String? = null,
        val modelInfo: String? = null,
        val isModelLoaded: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var recordingJob: Job? = null
    private val classifier: AirWritingClassifier?

    init {
        // Process classifier load result
        when (classifierResult) {
            is ClassifierLoadResult.Success -> {
                classifier = classifierResult.classifier
                val info = "${classifier.modelName}: ${classifier.windowSize}×${classifier.channels} → ${classifier.numClasses} classes"
                Log.i(TAG, "✓ Model loaded: $info")
                _uiState.update {
                    it.copy(
                        modelInfo = info,
                        isModelLoaded = true,
                        statusMessage = "Tap Start"
                    )
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
    }

    /**
     * Start the recording flow: countdown -> record -> classify -> show result
     */
    fun startRecording() {
        if (_uiState.value.state != State.IDLE) {
            Log.d(TAG, "Not in IDLE state, ignoring start")
            return
        }

        if (classifier == null) {
            Log.e(TAG, "Cannot start: classifier is null")
            _uiState.update {
                it.copy(errorMessage = "Model not loaded")
            }
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
                            confidence = null
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

                // Start sensor recording (same as data collection app)
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
                            state = State.IDLE,
                            errorMessage = "Not enough motion (${samples.size} samples)",
                            statusMessage = "Move more during recording",
                            recordingProgress = 0f
                        )
                    }
                    return@launch
                }

                // Run classification
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

                // === Phase 4: Show Result ===
                _uiState.update {
                    it.copy(
                        state = State.RESULT,
                        prediction = result.label,
                        confidence = result.confidence,
                        statusMessage = "${result.label} (${(result.confidence * 100).toInt()}%)",
                        errorMessage = null,
                        recordingProgress = 0f
                    )
                }

                // Auto-reset after showing result
                delay(RESULT_DISPLAY_SECONDS * 1000L)
                if (isActive && _uiState.value.state == State.RESULT) {
                    _uiState.update {
                        it.copy(
                            state = State.IDLE,
                            statusMessage = "Tap Start"
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
                statusMessage = "Tap Start",
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
                statusMessage = "Tap Start",
                errorMessage = null,
                recordingProgress = 0f,
                countdownSeconds = 0
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        sensorManager.stopRecording()
        classifier?.close()
    }
}

/**
 * Factory for PracticeModeViewModel
 */
class PracticeModeViewModelFactory(
    private val sensorManager: MotionSensorManager,
    private val classifierResult: ClassifierLoadResult
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PracticeModeViewModel(sensorManager, classifierResult) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
