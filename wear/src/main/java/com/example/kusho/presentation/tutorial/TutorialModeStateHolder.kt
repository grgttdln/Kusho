package com.example.kusho.presentation.tutorial

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state holder for Tutorial Mode data.
 * This allows the WearMessageListenerService to update state that the ViewModel can observe.
 * All updates are posted to the main thread to ensure thread safety.
 */
object TutorialModeStateHolder {

    private const val TAG = "TutorialModeStateHolder"

    private val mainHandler = Handler(Looper.getMainLooper())

    data class LetterData(
        val letter: String = "",
        val letterCase: String = "", // "uppercase" or "lowercase"
        val currentIndex: Int = 0,
        val totalLetters: Int = 0,
        val dominantHand: String = "RIGHT", // "LEFT" or "RIGHT"
        val timestamp: Long = 0L // Used to detect new data
    )

    data class SessionData(
        val studentName: String = "",
        val lessonTitle: String = "",
        val isActive: Boolean = false,
        val timestamp: Long = 0L
    )

    data class FeedbackData(
        val isCorrect: Boolean = false,
        val timestamp: Long = 0L,
        val shouldShow: Boolean = false
    )

    private val _letterData = MutableStateFlow(LetterData())
    val letterData: StateFlow<LetterData> = _letterData.asStateFlow()

    private val _sessionData = MutableStateFlow(SessionData())
    val sessionData: StateFlow<SessionData> = _sessionData.asStateFlow()

    private val _feedbackData = MutableStateFlow(FeedbackData())
    val feedbackData: StateFlow<FeedbackData> = _feedbackData.asStateFlow()

    private val _isSessionComplete = MutableStateFlow(false)
    val isSessionComplete: StateFlow<Boolean> = _isSessionComplete.asStateFlow()
    
    // Retry trigger - timestamp when retry was requested from mobile
    private val _retryTrigger = MutableStateFlow(0L)
    val retryTrigger: StateFlow<Long> = _retryTrigger.asStateFlow()

    // Tracks whether the watch user is currently on the TutorialModeScreen
    // Used to gate handshake replies so we don't reply when on other screens
    private val _isWatchOnTutorialScreen = MutableStateFlow(false)
    val isWatchOnTutorialScreen: StateFlow<Boolean> = _isWatchOnTutorialScreen.asStateFlow()

    fun setWatchOnTutorialScreen(onScreen: Boolean) {
        _isWatchOnTutorialScreen.value = onScreen
    }

    /**
     * Called by WearMessageListenerService when letter data is received
     */
    fun updateLetterData(letter: String, letterCase: String, currentIndex: Int, totalLetters: Int, dominantHand: String = "RIGHT") {
        runOnMainThread {
            try {
                Log.d(TAG, "📝 Updating letter data: $letter ($letterCase), index: $currentIndex/$totalLetters, hand: $dominantHand")
                // Clear feedback atomically when new letter arrives (teacher tapped Continue)
                _feedbackData.value = FeedbackData(shouldShow = false)
                _letterData.value = LetterData(
                    letter = letter,
                    letterCase = letterCase,
                    currentIndex = currentIndex,
                    totalLetters = totalLetters,
                    dominantHand = dominantHand,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating letter data", e)
            }
        }
    }

    /**
     * Called by WearMessageListenerService when session starts
     */
    fun startSession(studentName: String, lessonTitle: String) {
        runOnMainThread {
            try {
                Log.d(TAG, "🎯 Starting session: $lessonTitle for $studentName")
                // Clear all previous state first
                _letterData.value = LetterData()
                _feedbackData.value = FeedbackData()
                _isSessionComplete.value = false
                
                // Then set new session data
                _sessionData.value = SessionData(
                    studentName = studentName,
                    lessonTitle = lessonTitle,
                    isActive = true,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting session", e)
            }
        }
    }

    /**
     * Called by WearMessageListenerService when session ends
     */
    fun endSession() {
        runOnMainThread {
            try {
                Log.d(TAG, "🏁 Ending session")
                _sessionData.value = SessionData(
                    isActive = false,
                    timestamp = System.currentTimeMillis()
                )
                _letterData.value = LetterData()
                _feedbackData.value = FeedbackData()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error ending session", e)
            }
        }
    }

    /**
     * Called when mobile app signals that session is complete
     */
    fun markSessionComplete() {
        runOnMainThread {
            try {
                Log.d(TAG, "🎊 Session marked as complete")
                _isSessionComplete.value = true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error marking session complete", e)
            }
        }
    }

    /**
     * Show feedback on watch (correct/incorrect)
     */
    fun showFeedback(isCorrect: Boolean) {
        runOnMainThread {
            try {
                Log.d(TAG, if (isCorrect) "✅ Showing correct feedback" else "❌ Showing incorrect feedback")
                _feedbackData.value = FeedbackData(
                    isCorrect = isCorrect,
                    timestamp = System.currentTimeMillis(),
                    shouldShow = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error showing feedback", e)
            }
        }
    }

    /**
     * Clear feedback display
     */
    fun clearFeedback() {
        runOnMainThread {
            _feedbackData.value = FeedbackData(shouldShow = false)
        }
    }
    
    /**
     * Trigger retry from mobile app
     */
    fun triggerRetry() {
        runOnMainThread {
            Log.d(TAG, "🔄 Retry triggered from mobile")
            // Clear feedback atomically when retry arrives (teacher tapped Continue on incorrect)
            _feedbackData.value = FeedbackData(shouldShow = false)
            _retryTrigger.value = System.currentTimeMillis()
        }
    }
    
    /**
     * Reset entire session state (called when session ends or user navigates away)
     */
    fun resetSession() {
        runOnMainThread {
            Log.d(TAG, "♻️ Resetting entire session state")
            _letterData.value = LetterData()
            _sessionData.value = SessionData()
            _feedbackData.value = FeedbackData()
            _isSessionComplete.value = false
            _retryTrigger.value = 0L
        }
    }

    /**
     * Reset all state
     */
    fun reset() {
        runOnMainThread {
            _letterData.value = LetterData()
            _sessionData.value = SessionData()
            _feedbackData.value = FeedbackData()
            _isSessionComplete.value = false
        }
    }

    /**
     * Run a block on the main thread safely
     */
    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
