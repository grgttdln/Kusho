package com.example.app.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Deepgram Text-to-Speech API integration
 * Uses Aura voices for natural-sounding speech
 *
 * Current voice: aura-2-asteria-en (clear, natural female)
 *
 * Available Aura voices:
 * - aura-2-asteria-en (clear, natural female) ← Currently used
 * - aura-athena-en (warm, natural female)
 * - aura-asteria-en (clear female)
 * - aura-luna-en (soft female)
 * - aura-stella-en (bright female)
 * - aura-orion-en (professional male)
 * - aura-helios-en (friendly male)
 *
 * Playback speed: 0.85x (15% slower) - optimized for children
 * Adjustable via setPlaybackSpeed(speed: Float)
 *
 * Note: "helena" is not a valid Deepgram voice name.
 * Use one of the voices above instead.
 */
class DeepgramTTSManager(private val context: Context) {

    companion object {
        private const val TAG = "DeepgramTTSManager"
        private const val BASE_URL = "https://api.deepgram.com/"
        private const val CACHE_DIR = "deepgram_audio"

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

    private val apiKey: String = BuildConfig.DEEPGRAM_API_KEY

    init {
        Log.d(TAG, "DeepgramTTSManager initialized. API Key configured: ${isConfigured()}")
        Log.d(TAG, "API Key length: ${apiKey.length}")
    }

    private val deepgramApi: DeepgramApi by lazy {
        createDeepgramApi()
    }

    private var currentMediaPlayer: MediaPlayer? = null
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    // Playback speed (1.0 = normal, 0.8 = 20% slower)
    private var playbackSpeed: Float = 0.9f

    interface DeepgramApi {
        @POST("v1/speak?model=aura-2-asteria-en")
        suspend fun synthesizeSpeech(
            @Header("Authorization") authorization: String,
            @Body request: SpeechRequest
        ): Response<ResponseBody>
    }

    data class SpeechRequest(
        val text: String
    )

    private fun createDeepgramApi(): DeepgramApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepgramApi::class.java)
    }

    /**
     * Synthesize and play text using Deepgram TTS
     */
    suspend fun speak(text: String, onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "speak() called with text: $text")

        if (!isConfigured()) {
            Log.e(TAG, "Deepgram API key not configured!")
            onComplete?.invoke()
            return
        }

        try {
            // Check cache first
            val cacheKey = text.hashCode().toString()
            val cachedFile = File(cacheDir, "$cacheKey.mp3")

            val audioFile: File = if (cachedFile.exists()) {
                Log.d(TAG, "Using cached audio")
                cachedFile
            } else {
                Log.d(TAG, "Fetching from Deepgram API...")

                // Make API call on IO thread
                val response = withContext(Dispatchers.IO) {
                    deepgramApi.synthesizeSpeech(
                        authorization = "Token $apiKey",
                        request = SpeechRequest(text)
                    )
                }

                if (response.isSuccessful) {
                    val audioData = response.body()?.bytes()
                    if (audioData != null && audioData.isNotEmpty()) {
                        Log.d(TAG, "Received audio data: ${audioData.size} bytes")
                        withContext(Dispatchers.IO) {
                            FileOutputStream(cachedFile).use { output ->
                                output.write(audioData)
                            }
                        }
                        cachedFile
                    } else {
                        Log.e(TAG, "Empty audio data received")
                        onComplete?.invoke()
                        return
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Deepgram API error: ${response.code()} - $errorBody")
                    onComplete?.invoke()
                    return
                }
            }

            // Play audio on main thread
            Log.d(TAG, "Playing audio...")
            playAudio(audioFile, onComplete)

        } catch (e: Exception) {
            Log.e(TAG, "Error in speak()", e)
            e.printStackTrace()
            onComplete?.invoke()
        }
    }

    private suspend fun playAudio(audioFile: File, onComplete: (() -> Unit)?) {
        withContext(Dispatchers.Main) {
            try {
                // Stop any currently playing audio
                currentMediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }

                currentMediaPlayer = MediaPlayer().apply {
                    setDataSource(audioFile.absolutePath)
                    prepare()
                    
                    // Set slower playback speed for children (API 23+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val params = playbackParams
                        params.speed = playbackSpeed
                        playbackParams = params
                        Log.d(TAG, "Playback speed set to: $playbackSpeed")
                    }
                    
                    setOnCompletionListener {
                        Log.d(TAG, "Audio playback completed")
                        onComplete?.invoke()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        onComplete?.invoke()
                        true
                    }
                    start()
                    Log.d(TAG, "Audio playback started")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio", e)
                e.printStackTrace()
                onComplete?.invoke()
            }
        }
    }

    /**
     * Get a random phrase based on question type
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
     * Speak a random phrase based on question type using Deepgram TTS
     */
    suspend fun speakRandomPhrase(
        questionType: String,
        word: String? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val phrase = getRandomPhrase(questionType, word)
        Log.d(TAG, "Speaking random phrase: $phrase")
        speak(phrase, onComplete)
    }

    /**
     * Stop any ongoing speech
     */
    fun stop() {
        currentMediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        currentMediaPlayer = null
    }

    /**
     * Clear the audio cache to free up storage
     */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "Audio cache cleared")
    }

    /**
     * Check if API key is configured
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "YOUR_DEEPGRAM_API_KEY"
    }

    /**
     * Set playback speed (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     * Recommended for children: 0.8 - 0.9
     */
    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        Log.d(TAG, "Playback speed set to: $playbackSpeed")
    }

    /**
     * Get current playback speed
     */
    fun getPlaybackSpeed(): Float = playbackSpeed
}
