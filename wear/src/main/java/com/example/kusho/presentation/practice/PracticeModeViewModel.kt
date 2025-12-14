package com.example.kusho.presentation.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kusho.ml.Preprocessor
import com.example.kusho.ml.TFLiteAirWritingClassifier
import com.example.kusho.ml.WindowConfig
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
 * ViewModel that manages Practice Mode: sensor collection, inference, and UI state.
 */
class PracticeModeViewModel(
    private val motionSensorManager: MotionSensorManager,
    private val preprocessor: Preprocessor,
    private val classifier: TFLiteAirWritingClassifier?
) : ViewModel() {

    data class PracticeModeUiState(
        val isCollecting: Boolean = false,
        val currentPrediction: String? = null,
        val confidence: Float? = null,
        val statusMessage: String = "Tap Start, write a vowel, then wait for the prediction",
        val errorMessage: String? = null,
        // True during the 2-second hold after a prediction so the UI can react if needed
        val isHoldingPrediction: Boolean = false
    )

    private val _uiState = MutableStateFlow(PracticeModeUiState())
    val uiState: StateFlow<PracticeModeUiState> = _uiState

    private var practiceJob: Job? = null

    /**
     * Start continuous practice: repeatedly capture strokes and run inference
     * until [stopPractice] is called. [onPrediction] is invoked after each
     * successful prediction so the UI can, for example, play a sound.
     */
    fun startPractice(onPrediction: (() -> Unit)? = null) {
        if (_uiState.value.isCollecting) return

        if (classifier == null) {
            _uiState.value = _uiState.value.copy(
                isCollecting = false,
                errorMessage = "Model failed to load",
                statusMessage = "Restart the app or reinstall.",
                isHoldingPrediction = false
            )
            return
        }

        val windowSize = classifier.modelWindowSize
        val channels = classifier.modelChannels

        try {
            motionSensorManager.start()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isCollecting = false,
                errorMessage = "Sensors unavailable",
                statusMessage = "Motion sensors not available on this watch.",
                isHoldingPrediction = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isCollecting = true,
            errorMessage = null,
            statusMessage = "Write a vowel in the air now",
            isHoldingPrediction = false
        )

        practiceJob?.cancel()
        practiceJob = viewModelScope.launch(Dispatchers.Default) {
            val minSamples = maxOf(WindowConfig.MIN_SAMPLES, windowSize / 2)

            while (isActive && _uiState.value.isCollecting) {
                var strokeDone = false
                var idleCount = 0
                var hadPredictionForStroke = false

                // Capture one stroke
                while (isActive && _uiState.value.isCollecting && !strokeDone) {
                    val samples = motionSensorManager.getRecentSamples(windowSize)

                    if (!preprocessor.hasEnoughData(samples, minSamples)) {
                        _uiState.update { it.copy(statusMessage = "Keep writing…") }
                        delay(100)
                        continue
                    }

                    if (samples.size >= windowSize) {
                        val window = preprocessor.prepareInput(samples, windowSize, channels)
                        val result = try {
                            classifier.classify(window)
                        } catch (e: Exception) {
                            val msg = e.message ?: "Unknown inference error"
                            _uiState.update {
                                it.copy(
                                    errorMessage = "Inference error: $msg",
                                    statusMessage = "Inference error, try again",
                                    isHoldingPrediction = false
                                )
                            }
                            stopPracticeInternal()
                            return@launch
                        }

                        _uiState.update {
                            it.copy(
                                currentPrediction = result.label,
                                confidence = result.confidence,
                                statusMessage = if (result.label != null)
                                    "Predicted: ${result.label}. Hold still…"
                                else
                                    "Uncertain prediction, try again",
                                isHoldingPrediction = result.label != null
                            )
                        }

                        if (result.label != null) {
                            hadPredictionForStroke = true
                        }

                        strokeDone = true
                    } else {
                        idleCount++
                        if (idleCount > 20) {
                            _uiState.update {
                                it.copy(
                                    statusMessage = "Not enough motion, try again",
                                    errorMessage = "Too little data for a prediction",
                                    isHoldingPrediction = false
                                )
                            }
                            strokeDone = true
                        }
                    }

                    delay(100)
                }

                // If we got a valid prediction this stroke and we're still collecting,
                // hold it on-screen for 2 seconds, then play the sound, then prompt for next.
                if (hadPredictionForStroke && isActive && _uiState.value.isCollecting) {
                    // 2-second hold where prediction and status do not change
                    val holdStartPrediction = _uiState.value.currentPrediction
                    val holdStartConfidence = _uiState.value.confidence

                    val holdDurationMillis = 2000L
                    val stepMillis = 100L
                    var elapsed = 0L
                    while (isActive && _uiState.value.isCollecting && elapsed < holdDurationMillis) {
                        // Ensure we keep the same prediction and confidence during the hold
                        _uiState.update {
                            it.copy(
                                currentPrediction = holdStartPrediction,
                                confidence = holdStartConfidence,
                                isHoldingPrediction = true
                            )
                        }
                        delay(stepMillis)
                        elapsed += stepMillis
                    }

                    if (!isActive || !_uiState.value.isCollecting) {
                        // Session stopped during hold
                        break
                    }

                    // Play the sound AFTER the hold if still collecting
                    onPrediction?.invoke()

                    // Now ready for the next letter
                    _uiState.update {
                        it.copy(
                            statusMessage = "Write the next vowel in the air",
                            isHoldingPrediction = false
                        )
                    }
                } else if (isActive && _uiState.value.isCollecting) {
                    // No valid prediction, just prompt again
                    _uiState.update {
                        it.copy(
                            statusMessage = "Write a vowel in the air now",
                            isHoldingPrediction = false
                        )
                    }
                }
            }

            stopPracticeInternal()
        }
    }

    fun stopPractice() {
        _uiState.update {
            it.copy(
                isCollecting = false,
                statusMessage = "Tap Start and write the next vowel",
                isHoldingPrediction = false
            )
        }
        stopPracticeInternal()
    }

    private fun stopPracticeInternal() {
        practiceJob?.cancel()
        practiceJob = null
        motionSensorManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopPracticeInternal()
        classifier?.close()
    }
}

class PracticeModeViewModelFactory(
    private val motionSensorManager: MotionSensorManager,
    private val preprocessor: Preprocessor,
    private val classifier: TFLiteAirWritingClassifier?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PracticeModeViewModel(
                motionSensorManager = motionSensorManager,
                preprocessor = preprocessor,
                classifier = classifier
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}