package com.example.app.data.repository

import com.example.app.BuildConfig
import com.example.app.data.model.AiGeneratedActivity
import com.example.app.data.model.AiGeneratedSet
import com.example.app.data.model.AiWordConfig
import com.example.app.data.model.Message
import com.example.app.data.model.OpenRouterRequest
import com.example.app.data.model.ResponseFormat
import com.example.app.data.service.GeminiService
import com.example.app.data.model.AiActivityInfo
import com.example.app.data.model.AiGenerationResult
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for generating activities using OpenRouter AI.
 * Handles prompt building, API calls, response parsing, and validation.
 */
class GeminiRepository {

    private val gson = Gson()
    private val apiKey: String = BuildConfig.OPENROUTER_API_KEY

    private val openRouterService: GeminiService by lazy {
        createOpenRouterService()
    }

    private fun createOpenRouterService(): GeminiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiService::class.java)
    }

    /**
     * Regenerate a single set using AI.
     * Uses the same constraints as generateActivity but returns only one set.
     *
     * @param prompt Teacher's natural language description
     * @param availableWords List of words available in the teacher's word bank
     * @param currentSetTitle The title of the set being regenerated (for context)
     * @param currentSetDescription The description of the set being regenerated (for context)
     * @param maxRetries Maximum number of retry attempts on validation failure
     * @return Result containing generated set data or error
     */
    suspend fun regenerateSet(
        prompt: String,
        availableWords: List<com.example.app.data.entity.Word>,
        currentSetTitle: String,
        currentSetDescription: String,
        maxRetries: Int = 2
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        var lastError: String? = null
        
        repeat(maxRetries + 1) { attempt ->
            val result = tryRegenerateSet(prompt, availableWords, currentSetTitle, currentSetDescription)
            
            when (result) {
                is AiGenerationResult.Success -> return@withContext result
                is AiGenerationResult.Error -> {
                    lastError = result.message
                    if (!result.canRetry || attempt >= maxRetries) {
                        return@withContext result
                    }
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                }
            }
        }
        
        AiGenerationResult.Error(lastError ?: "Failed to regenerate set after $maxRetries retries")
    }

    private suspend fun tryRegenerateSet(
        prompt: String,
        availableWords: List<com.example.app.data.entity.Word>,
        currentSetTitle: String,
        currentSetDescription: String
    ): AiGenerationResult {
        return try {
            android.util.Log.d("GeminiRepository", "Regenerating set: $currentSetTitle")
            android.util.Log.d("GeminiRepository", "Available words: ${availableWords.map { it.word }}")

            val request = buildRegenerateRequest(prompt, availableWords, currentSetTitle, currentSetDescription)
            val authHeader = "Bearer $apiKey"

            val response = openRouterService.generateContent(authHeader, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    429 -> "API rate limit exceeded. Please try again later."
                    401 -> "Authentication failed."
                    402 -> "Payment required."
                    403 -> "Permission denied."
                    in 500..599 -> "Server error. Please try again later."
                    else -> "API error (${response.code()}): $errorBody"
                }
                return AiGenerationResult.Error(errorMessage, canRetry = response.code() in 500..599 || response.code() == 429)
            }

            val body = response.body()
                ?: return AiGenerationResult.Error("Empty response from API", canRetry = true)

            if (body.error != null) {
                return AiGenerationResult.Error(
                    message = "OpenRouter error: ${body.error.message}",
                    canRetry = true
                )
            }

            val generatedText = body.choices?.firstOrNull()?.message?.content
                ?: return AiGenerationResult.Error("No content generated", canRetry = true)

            android.util.Log.d("GeminiRepository", "Regenerated set response:\n$generatedText")

            // Parse and validate the response
            return parseAndValidateSetResponse(generatedText, availableWords)

        } catch (e: Exception) {
            android.util.Log.e("GeminiRepository", "Exception in tryRegenerateSet: ${e.message}", e)
            AiGenerationResult.Error("Regeneration failed: ${e.message}", canRetry = true)
        }
    }

    private fun buildRegenerateRequest(
        prompt: String,
        availableWords: List<com.example.app.data.entity.Word>,
        currentSetTitle: String,
        currentSetDescription: String
    ): OpenRouterRequest {
        val wordsWithImages = availableWords.filter { !it.imagePath.isNullOrBlank() }.map { it.word }
        val wordsWithoutImages = availableWords.filter { it.imagePath.isNullOrBlank() }.map { it.word }
        
        val systemInstruction = """
You are an assistant for an educational app called Kusho that helps teachers create handwriting practice activities.

The teacher wants to REGENERATE a single set with a different selection of words.

ORIGINAL SET:
- Title: $currentSetTitle
- Description: $currentSetDescription

AVAILABLE WORDS (MUST ONLY USE THESE EXACT WORDS):
${availableWords.map { it.word }.joinToString(", ")}

WORDS WITH IMAGES (can be used for "name the picture"):
${wordsWithImages.joinToString(", ")}

WORDS WITHOUT IMAGES (CANNOT be used for "name the picture"):
${wordsWithoutImages.joinToString(", ")}

CONFIGURATION TYPES:
- "fill in the blanks": Missing letter index (0-indexed). Example: word "cat" with index 1 = "c_t"
- "name the picture": Show picture, student names the word. CRITICAL: Only use words from the WORDS WITH IMAGES list.
- "write the word": Write word in the air

RULES:
1. ONLY use words from the AVAILABLE WORDS list above
2. Create a DIFFERENT mix of words than the original set
3. Minimum 5 words and Maximum 8 words in this single set
4. Keep the same set title or improve it slightly (max 10 characters)
5. Keep the same description or improve it slightly
6. For "fill in the blanks": selectedLetterIndex must be valid (0 to word.length-1)
7. ONLY "fill in the blanks" uses selectedLetterIndex. For other types, use selectedLetterIndex: 0
8. For "name the picture": ONLY use words that have images

OUTPUT FORMAT - EXTREMELY IMPORTANT:
You MUST respond with ONLY a valid JSON object. No markdown, no code blocks, no explanations.

The JSON must follow this exact structure:
{
  "activity": {
    "title": "Activity Title",
    "description": "Brief description"
  },
  "sets": [
    {
      "title": "Set Title",
      "description": "What this set covers",
      "words": [
        {
          "word": "word_from_bank",
          "configurationType": "fill in the blanks",
          "selectedLetterIndex": 1
        },
        {
          "word": "another_word",
          "configurationType": "name the picture",
          "selectedLetterIndex": 0
        }
      ]
    }
  ]
}

IMPORTANT: Return ONLY ONE set in the sets array (the regenerated set).
        """.trimIndent()

        return OpenRouterRequest(
            model = "nvidia/nemotron-nano-9b-v2:free",
            messages = listOf(
                Message(
                    role = "system",
                    content = systemInstruction
                ),
                Message(
                    role = "user",
                    content = "Teacher's original request: $prompt\n\nPlease regenerate this set with a different selection of words."
                )
            ),
            temperature = 0.8f,  // Slightly higher temperature for variety
            max_tokens = 1024,
            top_p = 0.95f,
            response_format = ResponseFormat(type = "json_object")
        )
    }

    /**
     * Parse and validate a single set response from regeneration.
     */
    private fun parseAndValidateSetResponse(
        jsonText: String,
        availableWords: List<com.example.app.data.entity.Word>
    ): AiGenerationResult {
        // Use the same validation logic as parseAndValidateResponse
        val result = parseAndValidateResponse(jsonText, availableWords)
        
        return when (result) {
            is AiGenerationResult.Success -> {
                // Extract just the first set from the result
                val activity = result.data
                val firstSet = activity.sets.firstOrNull()
                    ?: return AiGenerationResult.Error("No set found in regenerated response", canRetry = true)
                
                AiGenerationResult.Success(
                    AiGeneratedActivity(
                        activity = activity.activity,
                        sets = listOf(firstSet)
                    )
                )
            }
            is AiGenerationResult.Error -> result
        }
    }

    /**
     * Generate an activity with sets using AI.
     * Automatically retries up to 2 times on validation failure.
     *
     * @param prompt Teacher's natural language description
     * @param availableWords List of words available in the teacher's word bank (with image paths for validation)
     * @param maxRetries Maximum number of retry attempts on validation failure
     * @return Result containing generated activity data or error
     */
    suspend fun generateActivity(
        prompt: String,
        availableWords: List<com.example.app.data.entity.Word>,
        maxRetries: Int = 2
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        var lastError: String? = null
        
        repeat(maxRetries + 1) { attempt ->
            val result = tryGenerate(prompt, availableWords)
            
            when (result) {
                is AiGenerationResult.Success -> return@withContext result
                is AiGenerationResult.Error -> {
                    lastError = result.message
                    if (!result.canRetry || attempt >= maxRetries) {
                        return@withContext result
                    }
                    // Wait briefly before retry
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                }
            }
        }
        
        AiGenerationResult.Error(lastError ?: "Failed to generate activity after $maxRetries retries")
    }

    private suspend fun tryGenerate(
        prompt: String,
        availableWords: List<com.example.app.data.entity.Word>
    ): AiGenerationResult {
        return try {
            android.util.Log.d("GeminiRepository", "Building request with prompt: $prompt")
            android.util.Log.d("GeminiRepository", "Available words: ${availableWords.map { it.word }}")

            val request = buildRequest(prompt, availableWords)
            val authHeader = "Bearer $apiKey"

            android.util.Log.d("GeminiRepository", "Making API call to OpenRouter...")
            val response = openRouterService.generateContent(authHeader, request)

            android.util.Log.d("GeminiRepository", "Response code: ${response.code()}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                android.util.Log.e("GeminiRepository", "API Error: ${response.code()} - $errorBody")
                val errorMessage = when (response.code()) {
                    429 -> "API rate limit exceeded. OpenRouter free tier may be temporarily unavailable. Please try again later."
                    401 -> "Authentication failed. Please check your OpenRouter API key."
                    402 -> "Payment required. Please add credits to your OpenRouter account."
                    403 -> "Permission denied. Please check your API key permissions."
                    in 500..599 -> "Server error (${response.code()}). Please try again later."
                    else -> "API error (${response.code()}): $errorBody"
                }
                return AiGenerationResult.Error(
                    errorMessage,
                    canRetry = response.code() in 500..599 || response.code() == 429
                )
            }

            val body = response.body()
            if (body == null) {
                android.util.Log.e("GeminiRepository", "Empty response body")
                return AiGenerationResult.Error("Empty response from API", canRetry = true)
            }

            android.util.Log.d("GeminiRepository", "Response body received")

            if (body.error != null) {
                val errorMessage = body.error.message ?: "Unknown API error"
                android.util.Log.e("GeminiRepository", "OpenRouter error: $errorMessage")
                return AiGenerationResult.Error(
                    message = "OpenRouter error: $errorMessage",
                    canRetry = true
                )
            }

            val generatedText = body.choices?.firstOrNull()?.message?.content
            if (generatedText == null) {
                android.util.Log.e("GeminiRepository", "No content in response choices")
                return AiGenerationResult.Error("No content generated", canRetry = true)
            }

            android.util.Log.d("GeminiRepository", "Generated text length: ${generatedText.length}")
            android.util.Log.d("GeminiRepository", "Full response:\n$generatedText")

            val result = parseAndValidateResponse(generatedText, availableWords)

            when (result) {
                is AiGenerationResult.Success -> {
                    android.util.Log.d("GeminiRepository", "Successfully parsed and validated response")
                }
                is AiGenerationResult.Error -> {
                    android.util.Log.e("GeminiRepository", "Parse/validation error: ${result.message}")
                }
            }

            result

        } catch (e: Exception) {
            android.util.Log.e("GeminiRepository", "Exception in tryGenerate: ${e.message}", e)
            AiGenerationResult.Error("Generation failed: ${e.message}", canRetry = true)
        }
    }

    /**
     * Build the OpenRouter request with system instruction.
     */
    private fun buildRequest(prompt: String, availableWords: List<com.example.app.data.entity.Word>): OpenRouterRequest {
        val systemInstruction = buildSystemInstruction(availableWords)

        return OpenRouterRequest(
            model = "nvidia/nemotron-nano-9b-v2:free",
            messages = listOf(
                Message(
                    role = "system",
                    content = systemInstruction
                ),
                Message(
                    role = "user",
                    content = "Teacher's request: $prompt"
                )
            ),
            temperature = 0.7f,
            max_tokens = 2048,
            top_p = 0.95f,
            response_format = ResponseFormat(type = "json_object")
        )
    }

    private fun buildSystemInstruction(availableWords: List<com.example.app.data.entity.Word>): String {
        val wordsWithImages = availableWords.filter { !it.imagePath.isNullOrBlank() }.map { it.word }
        val wordsWithoutImages = availableWords.filter { it.imagePath.isNullOrBlank() }.map { it.word }
        
        return """
You are an assistant for an educational app called Kusho that helps teachers create handwriting practice activities.

AVAILABLE WORDS (MUST ONLY USE THESE EXACT WORDS):
${availableWords.map { it.word }.joinToString(", ")}

WORDS WITH IMAGES (can be used for "name the picture"):
${wordsWithImages.joinToString(", ")}

WORDS WITHOUT IMAGES (CANNOT be used for "name the picture"):
${wordsWithoutImages.joinToString(", ")}

CONFIGURATION TYPES:
- "fill in the blanks": Missing letter index (0-indexed). Example: word "cat" with index 1 = "c_t"
- "name the picture": Show picture, student names the word. CRITICAL: Only use words from the WORDS WITH IMAGES list above.
- "write the word": Write word in the air (best for 3-5 letter words)

RULES:
1. ONLY use words from the AVAILABLE WORDS list above
2. Minimum 5 words per set and Maximum of 8 words per set
3. Create 1-3 sets total
4. Set titles: max 10 characters
5. Activity title: max 10 characters
6. For "fill in the blanks": selectedLetterIndex must be valid (0 to word.length-1)
7. ONLY "fill in the blanks" uses selectedLetterIndex. For "name the picture" and "write the word", always use selectedLetterIndex: 0
8. For "name the picture": ONLY use words that have images (from WORDS WITH IMAGES list)

OUTPUT FORMAT - EXTREMELY IMPORTANT:
You MUST respond with ONLY a valid JSON object. No markdown formatting, no code blocks (```), no explanations before or after, no extra text. Your entire response must be a single JSON object.

The JSON must follow this exact structure:
{
  "activity": {
    "title": "Short Activity Title",
    "description": "Brief description"
  },
  "sets": [
    {
      "title": "Set 1 Name",
      "description": "What this set covers",
      "words": [
        {
          "word": "word_from_bank",
          "configurationType": "fill in the blanks",
          "selectedLetterIndex": 1
        },
        {
          "word": "another_word",
          "configurationType": "name the picture",
          "selectedLetterIndex": 0
        },
        {
          "word": "third_word",
          "configurationType": "write the word",
          "selectedLetterIndex": 0
        }
      ]
    }
  ]
}

VALIDATION CHECKLIST:
- [ ] All words used are from the available words list
- [ ] selectedLetterIndex is ONLY used for "fill in the blanks" (0 to word.length-1)
- [ ] selectedLetterIndex is 0 for "name the picture" and "write the word"
- [ ] Words for "name the picture" MUST have images available
- [ ] All strings are properly escaped
- [ ] Response starts with { and ends with }
- [ ] No markdown or extra text
        """.trimIndent()
    }

    /**
     * Parse JSON response and validate against constraints.
     */
    private fun parseAndValidateResponse(
        jsonText: String,
        availableWords: List<com.example.app.data.entity.Word>
    ): AiGenerationResult {
        return try {
            android.util.Log.d("GeminiRepository", "Starting JSON extraction from text of length: ${jsonText.length}")

            // Extract JSON from the response - handle various formats
            val cleanJson = extractJson(jsonText)
            if (cleanJson == null) {
                android.util.Log.e("GeminiRepository", "Failed to extract JSON from response")
                android.util.Log.e("GeminiRepository", "Original text:\n$jsonText")
                return AiGenerationResult.Error(
                    "Failed to extract JSON from AI response. The response format was unexpected. Please try again.",
                    canRetry = true
                )
            }

            android.util.Log.d("GeminiRepository", "Extracted JSON (${cleanJson.length} chars):")
            android.util.Log.d("GeminiRepository", cleanJson)

            // Try to parse the JSON
            val response: AiResponse? = try {
                gson.fromJson(cleanJson, AiResponse::class.java)
            } catch (e: Exception) {
                android.util.Log.e("GeminiRepository", "JSON parse error: ${e.message}")
                android.util.Log.e("GeminiRepository", "JSON content that failed to parse:\n$cleanJson")
                null
            }

            if (response == null) {
                android.util.Log.e("GeminiRepository", "Parsed response is null - JSON structure doesn't match expected format")
                return AiGenerationResult.Error(
                    "Failed to parse AI response. The JSON structure doesn't match the expected format. Please try again.",
                    canRetry = true
                )
            }

            android.util.Log.d("GeminiRepository", "JSON parsed successfully. Activity title: ${response.activity.title}")
            android.util.Log.d("GeminiRepository", "Number of sets: ${response.sets.size}")

            // Validate activity info
            if (response.activity.title.isBlank() || response.activity.title.length > 30) {
                android.util.Log.e("GeminiRepository", "Invalid activity title: '${response.activity.title}' (length: ${response.activity.title.length})")
                return AiGenerationResult.Error(
                    "Invalid activity title: must be 1-30 characters (got: ${response.activity.title.length})",
                    canRetry = true
                )
            }

            android.util.Log.d("GeminiRepository", "Activity title validated: ${response.activity.title}")

            // Validate sets
            if (response.sets.isEmpty()) {
                android.util.Log.e("GeminiRepository", "No sets found in response")
                return AiGenerationResult.Error(
                    "At least one set is required",
                    canRetry = true
                )
            }

            android.util.Log.d("GeminiRepository", "Validating ${response.sets.size} sets...")
            
            // Create a map of word strings to Word objects for quick lookup
            val wordMap = availableWords.associateBy { it.word }

            val validatedSets = mutableListOf<AiGeneratedSet>()

            for ((index, set) in response.sets.withIndex()) {
                android.util.Log.d("GeminiRepository", "Validating set ${index + 1}: ${set.title}")

                // Validate set title
                if (set.title.isBlank() || set.title.length > 30) {
                    android.util.Log.e("GeminiRepository", "Invalid set title: '${set.title}' (length: ${set.title.length})")
                    return AiGenerationResult.Error(
                        "Set ${index + 1}: Title must be 1-30 characters (got: ${set.title.length})",
                        canRetry = true
                    )
                }

                // Validate minimum words
                if (set.words.size < 3) {
                    android.util.Log.e("GeminiRepository", "Set ${index + 1} has only ${set.words.size} words (min: 3)")
                    return AiGenerationResult.Error(
                        "Set ${index + 1}: Must have at least 3 words (got: ${set.words.size})",
                        canRetry = true
                    )
                }

                android.util.Log.d("GeminiRepository", "Set ${index + 1} has ${set.words.size} words")

                val validatedWords = mutableListOf<AiWordConfig>()

                for (wordConfig in set.words) {
                    android.util.Log.d("GeminiRepository", "Validating word: ${wordConfig.word} (${wordConfig.configurationType})")

                    // Validate word exists in word bank using wordMap
                    val wordObj = wordMap[wordConfig.word]
                    if (wordObj == null) {
                        android.util.Log.e("GeminiRepository", "Word '${wordConfig.word}' not in available words: ${wordMap.keys}")
                        return AiGenerationResult.Error(
                            "Word '${wordConfig.word}' not found in word bank. Available words: ${wordMap.keys.joinToString(", ")}",
                            canRetry = true
                        )
                    }

                    // Validate configuration type
                    val validTypes = listOf("fill in the blanks", "name the picture", "write the word")
                    var configurationType = wordConfig.configurationType
                    
                    if (!validTypes.contains(configurationType)) {
                        android.util.Log.e("GeminiRepository", "Invalid configuration type: '$configurationType'")
                        return AiGenerationResult.Error(
                            "Invalid configuration type: '$configurationType'. Must be one of: ${validTypes.joinToString(", ")}",
                            canRetry = true
                        )
                    }

                    // Validate that "name the picture" words have images
                    if (configurationType == "name the picture") {
                        if (wordObj.imagePath.isNullOrBlank()) {
                            android.util.Log.w("GeminiRepository", "Word '${wordConfig.word}' assigned to 'name the picture' but has no image. Changing to 'write the word'")
                            // Automatically change to "write the word" instead of failing
                            configurationType = "write the word"
                        }
                    }

                    // Validate letter index for fill in the blanks
                    var selectedLetterIndex = wordConfig.selectedLetterIndex
                    if (configurationType == "fill in the blanks") {
                        if (selectedLetterIndex < 0 || selectedLetterIndex >= wordConfig.word.length) {
                            android.util.Log.e("GeminiRepository", "Invalid letter index $selectedLetterIndex for word '${wordConfig.word}' (length: ${wordConfig.word.length})")
                            return AiGenerationResult.Error(
                                "Invalid letter index ($selectedLetterIndex) for word '${wordConfig.word}'. Must be 0-${wordConfig.word.length - 1}",
                                canRetry = true
                            )
                        }
                    } else {
                        // For non-fill-in-the-blanks, force selectedLetterIndex to 0
                        selectedLetterIndex = 0
                    }

                    validatedWords.add(
                        AiWordConfig(
                            word = wordConfig.word,
                            configurationType = configurationType,
                            selectedLetterIndex = selectedLetterIndex
                        )
                    )
                }

                validatedSets.add(
                    AiGeneratedSet(
                        title = set.title,
                        description = set.description,
                        words = validatedWords
                    )
                )

                android.util.Log.d("GeminiRepository", "Set ${index + 1} validated successfully with ${validatedWords.size} words")
            }

            android.util.Log.d("GeminiRepository", "All validation passed! Activity: '${response.activity.title}' with ${validatedSets.size} sets")

            AiGenerationResult.Success(
                AiGeneratedActivity(
                    activity = AiActivityInfo(
                        title = response.activity.title,
                        description = response.activity.description
                    ),
                    sets = validatedSets
                )
            )

        } catch (e: JsonParseException) {
            AiGenerationResult.Error("Invalid JSON format: ${e.message}", canRetry = true)
        } catch (e: Exception) {
            AiGenerationResult.Error("Validation error: ${e.message}", canRetry = true)
        }
    }

    /**
     * Internal data class matching the expected AI response structure.
     */
    private data class AiResponse(
        val activity: ActivityInfo,
        val sets: List<SetInfo>
    )

    private data class ActivityInfo(
        val title: String,
        val description: String
    )

    private data class SetInfo(
        val title: String,
        val description: String,
        val words: List<WordInfo>
    )

    private data class WordInfo(
        val word: String,
        val configurationType: String,
        val selectedLetterIndex: Int
    )

    companion object {
        private const val BASE_URL = "https://openrouter.ai/"

        /**
         * Extract valid JSON from text that might contain markdown, extra text, etc.
         * Uses brace counting to find the outermost JSON object.
         */
        private fun extractJson(text: String): String? {
            android.util.Log.d("GeminiRepository", "Raw response length: ${text.length}")
            android.util.Log.d("GeminiRepository", "Raw response preview: ${text.take(500)}")

            // First, try to remove markdown code blocks
            var cleanedText = text
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()

            android.util.Log.d("GeminiRepository", "After markdown removal: ${cleanedText.take(500)}")

            // Find the outermost JSON object using brace counting
            var startIndex = -1
            var braceCount = 0
            var inString = false
            var escapeNext = false

            for (i in cleanedText.indices) {
                val char = cleanedText[i]

                if (escapeNext) {
                    escapeNext = false
                    continue
                }

                if (char == '\\') {
                    escapeNext = true
                    continue
                }

                if (char == '"' && !escapeNext) {
                    inString = !inString
                    continue
                }

                if (!inString) {
                    when (char) {
                        '{' -> {
                            if (braceCount == 0) {
                                startIndex = i
                            }
                            braceCount++
                        }
                        '}' -> {
                            braceCount--
                            if (braceCount == 0 && startIndex != -1) {
                                // Found complete JSON object
                                val json = cleanedText.substring(startIndex, i + 1)
                                android.util.Log.d("GeminiRepository", "Extracted JSON: ${json.take(200)}...")
                                return json
                            }
                        }
                    }
                }
            }

            // If brace counting failed, try simple approach as fallback
            if (startIndex == -1) {
                startIndex = cleanedText.indexOf('{')
                val endIndex = cleanedText.lastIndexOf('}')
                if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                    val json = cleanedText.substring(startIndex, endIndex + 1)
                    android.util.Log.d("GeminiRepository", "Fallback JSON extraction: ${json.take(200)}...")
                    return json
                }
            }

            android.util.Log.e("GeminiRepository", "Failed to extract JSON from response")
            return null
        }
    }
}
