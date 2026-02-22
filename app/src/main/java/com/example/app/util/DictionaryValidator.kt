package com.example.app.util

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
        Log.d(TAG, "validateWord() called for: '$word'")

        val textServicesManager = applicationContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
                as? TextServicesManager

        if (textServicesManager == null) {
            Log.w(TAG, "TextServicesManager is NULL - no spell checker service on this device")
            return DictionaryResult.Unavailable
        }
        Log.d(TAG, "TextServicesManager obtained successfully")

        // Use withTimeoutOrNull to avoid blocking users.
        // Must run on Main dispatcher because SpellCheckerSession internally
        // creates a Handler which requires a Looper on the current thread.
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    var session: SpellCheckerSession? = null

                    try {
                        session = textServicesManager.newSpellCheckerSession(
                            null,
                            Locale.ENGLISH,
                            object : SpellCheckerSession.SpellCheckerSessionListener {
                                override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                                    // Not used - we use getSentenceSuggestions instead
                                }

                                override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                                    Log.d(TAG, "onGetSentenceSuggestions callback fired for '$word'")
                                    if (!continuation.isActive) {
                                        Log.d(TAG, "Continuation no longer active, ignoring callback")
                                        return
                                    }

                                    try {
                                        if (results == null || results.isEmpty()) {
                                            Log.w(TAG, "Results null or empty → Unavailable")
                                            continuation.resume(DictionaryResult.Unavailable)
                                            session?.close()
                                            return
                                        }

                                        val sentenceSuggestionsInfo = results[0]
                                        Log.d(TAG, "suggestionsCount (sentence level): ${sentenceSuggestionsInfo.suggestionsCount}")
                                        if (sentenceSuggestionsInfo.suggestionsCount == 0) {
                                            Log.w(TAG, "Sentence suggestionsCount == 0 → Unavailable")
                                            continuation.resume(DictionaryResult.Unavailable)
                                            session?.close()
                                            return
                                        }

                                        val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(0)
                                        val attrs = suggestionsInfo.suggestionsAttributes
                                        Log.d(TAG, "suggestionsAttributes: 0x${attrs.toString(16)} (decimal: $attrs)")
                                        Log.d(TAG, "  IN_THE_DICTIONARY flag: ${attrs and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0}")
                                        Log.d(TAG, "  LOOKS_LIKE_TYPO flag: ${attrs and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0}")
                                        Log.d(TAG, "  suggestionsCount (word level): ${suggestionsInfo.suggestionsCount}")

                                        if (suggestionsInfo.suggestionsAttributes and
                                            SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0
                                        ) {
                                            Log.d(TAG, "Word '$word' is IN THE DICTIONARY → Valid")
                                            continuation.resume(DictionaryResult.Valid)
                                            session?.close()
                                            return
                                        }

                                        val allSuggestions = (0 until suggestionsInfo.suggestionsCount)
                                            .mapNotNull { suggestionsInfo.getSuggestionAt(it) }
                                        Log.d(TAG, "Raw suggestions for '$word': $allSuggestions")

                                        val cvcSuggestions = allSuggestions
                                            .filter { WordValidator.isCVCPattern(it) }
                                            .distinct()
                                            .take(MAX_SUGGESTIONS)
                                        Log.d(TAG, "CVC-filtered suggestions: $cvcSuggestions")

                                        continuation.resume(DictionaryResult.Invalid(cvcSuggestions))
                                        session?.close()
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Exception during validation", e)
                                        continuation.resume(DictionaryResult.Unavailable)
                                        session?.close()
                                    }
                                }
                            },
                            true
                        )

                        if (session == null) {
                            Log.w(TAG, "Session creation returned NULL - spell checker not available on this device")
                            continuation.resume(DictionaryResult.Unavailable)
                            return@suspendCancellableCoroutine
                        }
                        Log.d(TAG, "SpellCheckerSession created successfully, sending query for '$word'")

                        continuation.invokeOnCancellation {
                            session?.close()
                        }

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
        }

        // If timeout occurred, return Unavailable
        if (result == null) {
            Log.w(TAG, "Timeout occurred after ${TIMEOUT_MS}ms for word '$word'")
        }
        val finalResult = result ?: DictionaryResult.Unavailable
        Log.d(TAG, "Final result for '$word': $finalResult")
        return finalResult
    }
}
