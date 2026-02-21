package com.example.app.util

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Result of dictionary validation for a word.
 */
sealed class DictionaryResult {
    /**
     * Word is found in the dictionary.
     */
    data object Valid : DictionaryResult()

    /**
     * Word is not found in the dictionary, with CVC-filtered suggestions.
     *
     * @param suggestions List of suggested words that follow CVC pattern (max 5)
     */
    data class Invalid(val suggestions: List<String>) : DictionaryResult()

    /**
     * Spell checker service is not present or timed out.
     */
    data object Unavailable : DictionaryResult()
}

/**
 * Validates words against Android's spell checker dictionary.
 *
 * Uses [TextServicesManager] and [SpellCheckerSession] to check if words exist
 * in the system dictionary. Filters suggestions through [WordValidator.isCVCPattern]
 * to provide pedagogically appropriate alternatives.
 *
 * @param context Application context for accessing system services
 */
class DictionaryValidator(context: Context) {
    private val applicationContext = context.applicationContext

    companion object {
        private const val TAG = "DictionaryValidator"
        private const val TIMEOUT_MS = 3000L
        private const val MAX_SUGGESTIONS = 5
    }

    /**
     * Validates a word against the system dictionary.
     *
     * This method:
     * - Creates a spell checker session for English locale
     * - Checks if the word is in the dictionary
     * - If not found, provides up to 5 CVC-pattern suggestions
     * - Times out after 3 seconds to avoid blocking users
     * - Returns [DictionaryResult.Unavailable] if service is unavailable or times out
     *
     * @param word The word to validate
     * @return [DictionaryResult] indicating validation result
     */
    suspend fun validateWord(word: String): DictionaryResult {
        val textServicesManager = applicationContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
                as? TextServicesManager

        if (textServicesManager == null) {
            Log.w(TAG, "TextServicesManager unavailable")
            return DictionaryResult.Unavailable
        }

        // Use withTimeoutOrNull to avoid blocking users
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                var session: SpellCheckerSession? = null

                try {
                    // Create session with callback listener
                    session = textServicesManager.newSpellCheckerSession(
                        null, // use default spell checker
                        Locale.ENGLISH,
                        object : SpellCheckerSession.SpellCheckerSessionListener {
                            override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                                // Not used - we use getSentenceSuggestions instead
                            }

                            override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                                // Check if continuation is still active before resuming
                                if (!continuation.isActive) return

                                try {
                                    if (results == null || results.isEmpty()) {
                                        continuation.resume(DictionaryResult.Unavailable)
                                        session?.close()
                                        return
                                    }

                                    val sentenceSuggestionsInfo = results[0]
                                    if (sentenceSuggestionsInfo.suggestionsCount == 0) {
                                        continuation.resume(DictionaryResult.Unavailable)
                                        session?.close()
                                        return
                                    }

                                    val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(0)

                                    // Check if word is in dictionary
                                    if (suggestionsInfo.suggestionsAttributes and
                                        SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0) {
                                        continuation.resume(DictionaryResult.Valid)
                                        session?.close()
                                        return
                                    }

                                    // Word not in dictionary, filter suggestions through CVC pattern
                                    val cvcSuggestions = (0 until suggestionsInfo.suggestionsCount)
                                        .mapNotNull { suggestionsInfo.getSuggestionAt(it) }
                                        .filter { WordValidator.isCVCPattern(it) }
                                        .distinct()
                                        .take(MAX_SUGGESTIONS)

                                    continuation.resume(DictionaryResult.Invalid(cvcSuggestions))
                                    session?.close()
                                } catch (e: Exception) {
                                    Log.w(TAG, "Exception during validation", e)
                                    continuation.resume(DictionaryResult.Unavailable)
                                    session?.close()
                                }
                            }
                        },
                        true // means "return suggestions even if word is correct"
                    )

                    if (session == null) {
                        Log.w(TAG, "Session creation failed")
                        continuation.resume(DictionaryResult.Unavailable)
                        return@suspendCancellableCoroutine
                    }

                    // Set up cancellation handler to close session
                    continuation.invokeOnCancellation {
                        session?.close()
                    }

                    // Request suggestions for the word
                    session.getSentenceSuggestions(
                        arrayOf(TextInfo(word.lowercase())),
                        MAX_SUGGESTIONS
                    )

                } catch (e: Exception) {
                    Log.w(TAG, "Exception during validation setup", e)
                    session?.close()
                    continuation.resume(DictionaryResult.Unavailable)
                }
            }
        }

        // If timeout occurred, return Unavailable
        if (result == null) {
            Log.w(TAG, "Timeout occurred")
        }
        return result ?: DictionaryResult.Unavailable
    }
}
