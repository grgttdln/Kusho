package com.example.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale

/**
 * Manages Text-to-Speech functionality for the mobile app.
 * Provides TTS with different voice types and callback support.
 */
class TextToSpeechManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TextToSpeechManager"
        
        // Question type phrases for Learn Mode
        val FILL_IN_BLANK_PHRASES = listOf(
            "Fill in the missing letter to make the word %s!",
            "What letter is missing in the word %s?",
            "Can you complete the word %s?",
            "Trace the right letter to finish the word %s."
        )
        
        val WRITE_WORD_PHRASES = listOf(
            "Can you write the word %s?",
            "Let's write the word %s!",
            "Try writing the word %s.",
            "Can you spell the word %s?",
            "Trace and write the word %s."
        )
        
        val NAME_PICTURE_PHRASES = listOf(
            "What is this picture? Trace and write its name!",
            "Look at the picture. What is it? Trace the word!",
            "What do you see? Trace and write the name!",
            "Can you name this picture? Let's trace it!",
            "What is shown in the picture? Trace its name!",
            "Name the picture and trace the word.",
            "Do you know what this is? Trace and write it!"
        )
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()
    
    init {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Language not supported")
                    } else {
                        tts.setSpeechRate(0.9f) // Slightly slower for children
                        tts.setPitch(1.1f) // Slightly higher pitch for friendlier tone
                        isInitialized = true
                        Log.d(TAG, "TTS initialized successfully")
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
            initDeferred.complete(isInitialized)
        }
    }
    
    /**
     * Speak the given text.
     * @param text The text to speak
     * @param utteranceId Optional ID for tracking completion
     * @param onComplete Optional callback when speech completes
     */
    fun speak(
        text: String,
        utteranceId: String = "utterance_${System.currentTimeMillis()}",
        onComplete: (() -> Unit)? = null
    ) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet, queuing speech")
            // Try to speak after initialization
            initDeferred.invokeOnCompletion {
                if (isInitialized) {
                    doSpeak(text, utteranceId, onComplete)
                } else {
                    Log.e(TAG, "TTS failed to initialize, cannot speak")
                }
            }
            return
        }
        
        doSpeak(text, utteranceId, onComplete)
    }
    
    private fun doSpeak(
        text: String,
        utteranceId: String,
        onComplete: (() -> Unit)?
    ) {
        textToSpeech?.let { tts ->
            if (onComplete != null) {
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    
                    override fun onDone(utteranceId: String?) {
                        onComplete()
                    }
                    
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS error for utterance: $utteranceId")
                        onComplete()
                    }
                })
            }
            
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            @Suppress("DEPRECATION")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
    }
    
    /**
     * Get a random phrase based on question type.
     * @param questionType The type of question ("Fill in the Blank", "Write the Word", "Name the Picture")
     * @param word The word to insert into the phrase (if applicable)
     * @return The formatted phrase ready for TTS
     */
    fun getRandomPhrase(questionType: String, word: String? = null): String {
        return when (questionType) {
            "Fill in the Blank" -> {
                val phrase = FILL_IN_BLANK_PHRASES.random()
                word?.let { phrase.format(it.uppercase()) } ?: phrase
            }
            "Write the Word" -> {
                val phrase = WRITE_WORD_PHRASES.random()
                word?.let { phrase.format(it.uppercase()) } ?: phrase
            }
            "Name the Picture" -> {
                NAME_PICTURE_PHRASES.random()
            }
            else -> {
                word?.let { "Can you write the word $it?" } ?: "What should you write?"
            }
        }
    }
    
    /**
     * Speak a random phrase based on question type.
     * @param questionType The type of question
     * @param word The word to insert (if applicable)
     * @param onComplete Optional callback when speech completes
     */
    fun speakRandomPhrase(
        questionType: String,
        word: String? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val phrase = getRandomPhrase(questionType, word)
        speak(phrase, onComplete = onComplete)
    }
    
    /**
     * Stop any ongoing speech.
     */
    fun stop() {
        textToSpeech?.stop()
    }
    
    /**
     * Shutdown the TTS engine. Call this when done.
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
    
    /**
     * Check if TTS is initialized and ready.
     */
    fun isReady(): Boolean = isInitialized
}
