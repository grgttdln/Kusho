package com.example.kusho.presentation.learn

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state holder for Learn Mode data.
 * This allows the WearMessageListenerService to update state that the ViewModel can observe.
 * All updates are posted to the main thread to ensure thread safety.
 */
object LearnModeStateHolder {

    private const val TAG = "LearnModeStateHolder"

    private val mainHandler = Handler(Looper.getMainLooper())

    data class WordData(
        val word: String = "",
        val maskedIndex: Int = -1,
        val configurationType: String = "",
        val timestamp: Long = 0L // Used to detect new data
    )

    data class SessionData(
        val setTitle: String = "",
        val totalWords: Int = 0,
        val isActive: Boolean = false,
        val timestamp: Long = 0L
    )

    private val _wordData = MutableStateFlow(WordData())
    val wordData: StateFlow<WordData> = _wordData.asStateFlow()

    private val _sessionData = MutableStateFlow(SessionData())
    val sessionData: StateFlow<SessionData> = _sessionData.asStateFlow()

    /**
     * Called by WearMessageListenerService when word data is received
     */
    fun updateWordData(word: String, maskedIndex: Int, configurationType: String) {
        runOnMainThread {
            try {
                Log.d(TAG, "📚 Updating word data: $word, masked: $maskedIndex, type: $configurationType")
                _wordData.value = WordData(
                    word = word,
                    maskedIndex = maskedIndex,
                    configurationType = configurationType,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating word data", e)
            }
        }
    }

    /**
     * Called by WearMessageListenerService when session starts
     */
    fun startSession(setTitle: String, totalWords: Int) {
        runOnMainThread {
            try {
                Log.d(TAG, "🎯 Starting session: $setTitle ($totalWords words)")
                _sessionData.value = SessionData(
                    setTitle = setTitle,
                    totalWords = totalWords,
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
                _wordData.value = WordData()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error ending session", e)
            }
        }
    }

    /**
     * Reset all state
     */
    fun reset() {
        runOnMainThread {
            _wordData.value = WordData()
            _sessionData.value = SessionData()
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

