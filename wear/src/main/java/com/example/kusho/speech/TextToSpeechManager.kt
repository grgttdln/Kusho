package com.example.kusho.speech

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Manages Text-to-Speech functionality for speaking predicted letters.
 * Configured for a kid-friendly, lively voice.
 */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val DEFAULT_VOLUME = 1.0f // Range: 0.0 to 1.0

        // Kid-friendly voice settings
        private const val KID_FRIENDLY_PITCH = 1.5f    // Higher pitch (1.0 is normal, max ~2.0)
        private const val KID_FRIENDLY_SPEED = 0.9f    // Slightly slower for clarity (1.0 is normal)
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    /** Volume level from 0.0 (silent) to 1.0 (max) */
    var volume: Float = DEFAULT_VOLUME
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
        }

    /** Pitch level - higher values sound more child-like (0.5 to 2.0, default 1.3) */
    var pitch: Float = KID_FRIENDLY_PITCH
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setPitch(field)
        }

    /** Speech rate - controls how fast the voice speaks (0.5 to 2.0, default 0.9) */
    var speechRate: Float = KID_FRIENDLY_SPEED
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(field)
        }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {
                isInitialized = true

                // Apply kid-friendly voice settings
                applyKidFriendlySettings()

                Log.i(TAG, "TextToSpeech initialized successfully with kid-friendly settings")
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
        }
    }

    /**
     * Configure TTS for a kid-friendly, lively voice.
     */
    private fun applyKidFriendlySettings() {
        tts?.apply {
            // Set higher pitch for a more child-like voice
            setPitch(pitch)

            // Set speech rate for clarity
            setSpeechRate(speechRate)

            // Try to find a female voice (often sounds friendlier for kids)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectBestVoiceForKids()
            }
        }
    }

    /**
     * Try to select a voice that's more suitable for children.
     * Prefers female voices as they tend to sound friendlier.
     */
    private fun selectBestVoiceForKids() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        try {
            val voices = tts?.voices ?: return

            // Log available voices for debugging
            voices.forEach { voice ->
                Log.d(TAG, "Available voice: ${voice.name}, locale: ${voice.locale}, quality: ${voice.quality}")
            }

            // Priority order for voice selection:
            // 1. Female US English voice with high quality
            // 2. Any female English voice
            // 3. High quality US English voice
            // 4. Keep default

            val usEnglishVoices = voices.filter {
                it.locale.language == "en" &&
                !it.isNetworkConnectionRequired &&
                it.features?.contains("notInstalled") != true
            }

            // Try to find a female voice (names often contain "female" or specific names)
            val femaleVoice = usEnglishVoices.find { voice ->
                voice.name.lowercase().let { name ->
                    name.contains("female") ||
                    name.contains("samantha") ||
                    name.contains("karen") ||
                    name.contains("moira") ||
                    name.contains("tessa") ||
                    name.contains("fiona") ||
                    name.contains("en-us-x-sfg") || // Google's female voice
                    name.contains("en-us-x-tpf") || // Another female variant
                    name.contains("-f-") // Common female indicator
                }
            }

            // If no female voice found, pick the highest quality English voice
            val selectedVoice = femaleVoice ?: usEnglishVoices.maxByOrNull { it.quality }

            selectedVoice?.let { voice ->
                val result = tts?.setVoice(voice)
                if (result == TextToSpeech.SUCCESS) {
                    Log.i(TAG, "Selected kid-friendly voice: ${voice.name}")
                } else {
                    Log.w(TAG, "Failed to set voice: ${voice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting voice: ${e.message}")
        }
    }

    /**
     * Get list of available voices (for debugging/selection).
     */
    fun getAvailableVoices(): List<Voice>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.voices?.toList()
        } else {
            null
        }
    }

    /**
     * Speak the predicted letter.
     * For single letters, it speaks the letter name (e.g., "A" -> "A").
     * For lowercase letters, it says "lowercase" followed by the letter.
     */
    fun speakLetter(letter: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            return
        }

        if (letter.isBlank() || letter == "?") {
            return
        }

        // Determine what to speak
        val textToSpeak = when {
            letter.length == 1 && letter[0].isLetter() -> {
                if (letter[0].isUpperCase()) {
                    "Big ${letter.uppercase()}"
                } else {
                    "Small ${letter.uppercase()}"
                }
            }
            else -> letter
        }

        Log.d(TAG, "Speaking: $textToSpeak (volume: $volume)")

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "prediction_${System.currentTimeMillis()}")
    }

    /**
     * Speak custom text.
     */
    fun speak(text: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            return
        }

        if (text.isBlank()) return

        Log.d(TAG, "Speaking: $text (volume: $volume)")

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "custom_${System.currentTimeMillis()}")
    }

    /**
     * Stop any ongoing speech.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Release TTS resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d(TAG, "TextToSpeech shutdown")
    }
}
