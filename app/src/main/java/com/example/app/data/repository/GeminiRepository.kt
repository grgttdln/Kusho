package com.example.app.data.repository

import android.util.Log
import com.example.app.BuildConfig
import com.example.app.data.entity.Word
import com.example.app.data.model.AiActivityInfo
import com.example.app.data.model.AiGeneratedActivity
import com.example.app.data.model.AiGeneratedSet
import com.example.app.data.model.AiGenerationResult
import com.example.app.data.model.AiWordConfig
import com.example.app.data.model.FilteredWordsResponse
import com.example.app.data.model.GroupedSetsResponse
import com.example.app.data.model.SetGroupingResponse
import com.example.app.data.model.TitleSimilarity
import com.example.app.data.model.TitleSimilarityResponse
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Generation phase for UI progress tracking.
 */
sealed class GenerationPhase {
    object Idle : GenerationPhase()
    object Filtering : GenerationPhase()
    object Grouping : GenerationPhase()
    object Configuring : GenerationPhase()
    object Complete : GenerationPhase()
}

/**
 * Represents a structural pattern detected in a teacher's prompt.
 * Used to bypass AI filtering and apply deterministic code-based filtering.
 */
sealed class DetectedPattern {
    abstract fun matches(onset: Char, vowel: Char, coda: Char, rime: String): Boolean
    abstract val displayName: String

    data class Rime(val rime: String) : DetectedPattern() {
        override fun matches(onset: Char, vowel: Char, coda: Char, rime: String) = rime == this.rime
        override val displayName = "-$rime"
    }
    data class Onset(val onset: Char) : DetectedPattern() {
        override fun matches(onset: Char, vowel: Char, coda: Char, rime: String) = onset == this.onset
        override val displayName = "starting with '$onset'"
    }
    data class Vowel(val vowel: Char) : DetectedPattern() {
        override fun matches(onset: Char, vowel: Char, coda: Char, rime: String) = vowel == this.vowel
        override val displayName = "with vowel '$vowel'"
    }
    data class Coda(val coda: Char) : DetectedPattern() {
        override fun matches(onset: Char, vowel: Char, coda: Char, rime: String) = coda == this.coda
        override val displayName = "ending in '$coda'"
    }
}

/**
 * Repository for generating activities using Gemini 2.5 Flash Lite.
 * Uses a 3-step chained approach:
 *   Step 1: Filter words matching teacher's request
 *   Step 2: Group filtered words into coherent sets
 *   Step 3: Assign configuration types to each word
 */
class GeminiRepository {

    private val gson = Gson()
    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    // Cached Step 1 result for reuse during set regeneration
    private var cachedFilteredWords: List<String>? = null
    private var cacheTimestamp: Long = 0L
    private val CACHE_TTL_MS = 5L * 60 * 1000 // 5 minutes

    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
    )

    companion object {
        private const val TAG = "GeminiRepository"
        private const val MODEL_NAME = "gemini-2.5-flash-lite"
        private const val MAX_RETRIES = 2
    }

    fun invalidateWordCache() {
        cachedFilteredWords = null
        cacheTimestamp = 0L
    }

    private fun isCacheValid(): Boolean {
        return cachedFilteredWords != null &&
               (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS
    }

    private val lastCallTimestamps = mutableMapOf<String, Long>()
    private val DEBOUNCE_MS = 3000L

    private fun shouldThrottle(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastCallTimestamps[key] ?: 0L
        if (now - last < DEBOUNCE_MS) {
            Log.w(TAG, "Throttling $key (${now - last}ms since last call)")
            return true
        }
        lastCallTimestamps[key] = now
        return false
    }

    /**
     * Classify a teacher's prompt to detect structural patterns.
     * Returns null for semantic/thematic prompts that need AI filtering.
     */
    fun classifyPrompt(prompt: String): DetectedPattern? {
        val lower = prompt.lowercase().trim()

        // Rime patterns: "-un", "_un", "ending in -un", "words ending in un", "-un words"
        val rimeRegex = Regex("""(?:^|(?<=\s))[-_]([aeiou][bcdfghjklmnprstvwxyz])\b""")
        val rimeMatch = rimeRegex.find(lower)
        if (rimeMatch != null) {
            return DetectedPattern.Rime(rimeMatch.groupValues[1])
        }

        // "ending in -un" / "end in un" / "that end with un"
        val endingInRimeRegex = Regex("""end(?:ing)?\s+(?:in|with)\s+[-_]?([aeiou][bcdfghjklmnprstvwxyz])\b""")
        val endingInRimeMatch = endingInRimeRegex.find(lower)
        if (endingInRimeMatch != null) {
            return DetectedPattern.Rime(endingInRimeMatch.groupValues[1])
        }

        // Onset patterns: "starting with b", "words that start with c", "b__ words",
        // "letters with g", "words with g", "letter g", "g words"
        val onsetRegex = Regex("""start(?:ing)?\s+with\s+(?:the\s+)?(?:letter\s+)?['"]?([bcdfghjklmnprstvwxyz])['"]?""")
        val onsetMatch = onsetRegex.find(lower)
        if (onsetMatch != null) {
            return DetectedPattern.Onset(onsetMatch.groupValues[1][0])
        }
        val onsetBlankRegex = Regex("""\b([bcdfghjklmnprstvwxyz])_+\b""")
        val onsetBlankMatch = onsetBlankRegex.find(lower)
        if (onsetBlankMatch != null) {
            return DetectedPattern.Onset(onsetBlankMatch.groupValues[1][0])
        }
        // Vowel patterns — checked BEFORE onset "letters/words with" to prevent
        // "words with the short 'a'" from matching 's' in "short" as an onset
        val vowelRegex = Regex("""(?:short|long|vowel)\s+['"]?([aeiou])['"]?""")
        val vowelMatch = vowelRegex.find(lower)
        if (vowelMatch != null) {
            return DetectedPattern.Vowel(vowelMatch.groupValues[1][0])
        }
        val wordsWithVowelRegex = Regex("""words?\s+with\s+['"]([aeiou])['"]""")
        val wordsWithVowelMatch = wordsWithVowelRegex.find(lower)
        if (wordsWithVowelMatch != null) {
            return DetectedPattern.Vowel(wordsWithVowelMatch.groupValues[1][0])
        }

        // "letters with g", "words with g", "letter g words"
        val lettersWithRegex = Regex("""(?:letters?|words?)\s+with\s+(?:the\s+)?(?:letter\s+)?['"]?([bcdfghjklmnprstvwxyz])['"]?""")
        val lettersWithMatch = lettersWithRegex.find(lower)
        if (lettersWithMatch != null) {
            return DetectedPattern.Onset(lettersWithMatch.groupValues[1][0])
        }
        // "letter g", "the letter g"
        val letterXRegex = Regex("""letter\s+['"]?([bcdfghjklmnprstvwxyz])['"]?""")
        val letterXMatch = letterXRegex.find(lower)
        if (letterXMatch != null) {
            return DetectedPattern.Onset(letterXMatch.groupValues[1][0])
        }

        // Coda patterns: "ending in t" (single consonant, not a rime)
        val codaRegex = Regex("""end(?:ing)?\s+(?:in|with)\s+['"]?([bcdfghjklmnprstvwxyz])['"]?(?:\s|$)""")
        val codaMatch = codaRegex.find(lower)
        if (codaMatch != null) {
            return DetectedPattern.Coda(codaMatch.groupValues[1][0])
        }

        // No structural pattern detected — needs AI
        return null
    }

    /**
     * Filter words using a detected structural pattern (no AI needed).
     */
    fun filterWordsByPattern(
        pattern: DetectedPattern,
        availableWords: List<Word>
    ): List<String> {
        return availableWords.mapNotNull { w ->
            val lower = w.word.lowercase()
            if (!isValidCVC(lower)) return@mapNotNull null
            val onset = lower[0]
            val vowel = lower[1]
            val coda = lower[2]
            val rime = "${lower[1]}${lower[2]}"
            if (pattern.matches(onset, vowel, coda, rime)) w.word else null
        }
    }

    /**
     * Create a GenerativeModel with the given system instruction.
     */
    private fun createModel(systemInstruction: String): GenerativeModel {
        return GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f
                topP = 0.95f
                maxOutputTokens = 2048
                responseMimeType = "application/json"
            },
            safetySettings = safetySettings,
            systemInstruction = content { text(systemInstruction) }
        )
    }

    // ========== Public API ==========

    /**
     * Generate a complete activity with sets using a 3-step chained approach.
     *
     * @param prompt Teacher's natural language description
     * @param availableWords List of words from the teacher's word bank
     * @param onPhaseChange Callback for UI progress updates
     * @return Result containing generated activity data or error
     */
    suspend fun generateActivity(
        prompt: String,
        availableWords: List<Word>,
        existingSetTitles: List<String> = emptyList(),
        onPhaseChange: suspend (GenerationPhase) -> Unit = {}
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        if (shouldThrottle("generateActivity")) {
            return@withContext AiGenerationResult.Error(
                "Please wait before generating another activity.",
                canRetry = true
            )
        }
        if (!isCacheValid()) {
            invalidateWordCache()
        }
        try {
            val analyses = analyzeCVCWords(availableWords)
            val patterns = detectCVCPatterns(analyses)
            val analysisTable = buildWordAnalysisTable(analyses)
            val patternsSummary = buildPatternsSummary(patterns)

            // === Step 1: Filter Words ===
            onPhaseChange(GenerationPhase.Filtering)

            val wordBank = availableWords.map { it.word }.toSet()
            val detectedPattern = classifyPrompt(prompt)

            if (detectedPattern != null) {
                // Structural pattern: use deterministic code-based filtering
                Log.d(TAG, "Structural pattern detected: ${detectedPattern.displayName}")
                val codeFiltered = filterWordsByPattern(detectedPattern, availableWords)

                if (codeFiltered.size < 3) {
                    Log.w(TAG, "Insufficient words for pattern ${detectedPattern.displayName}: ${codeFiltered.size} found")
                    return@withContext AiGenerationResult.InsufficientWords(
                        pattern = detectedPattern.displayName,
                        matchingWords = codeFiltered,
                        needed = 3,
                        originalPrompt = prompt
                    )
                }

                cachedFilteredWords = codeFiltered
            } else {
                // Semantic/thematic prompt: use AI Step 1
                val filteredWords = retryStep(MAX_RETRIES, "Step 1: Filter") {
                    stepFilterWords(prompt, analysisTable, patternsSummary)
                }

                if (filteredWords == null || filteredWords.words.isNullOrEmpty()) {
                    Log.w(TAG, "Step 1 failed or returned empty, using all words as fallback")
                    cachedFilteredWords = availableWords.map { it.word }
                } else {
                    val filtered = filteredWords.words.filter { it in wordBank }
                    if (filtered.size < 3) {
                        Log.w(TAG, "Too few valid words after filtering (${filtered.size}), using all words")
                        cachedFilteredWords = availableWords.map { it.word }
                    } else {
                        cachedFilteredWords = filtered
                    }
                }
            }
            cacheTimestamp = System.currentTimeMillis()
            val cached = cachedFilteredWords ?: availableWords.map { it.word }

            // Rebuild analysis for filtered words only
            val filteredWordObjs = availableWords.filter { it.word in cached }
            val filteredAnalyses = analyzeCVCWords(filteredWordObjs)
            val filteredPatterns = detectCVCPatterns(filteredAnalyses)
            val filteredAnalysisTable = buildWordAnalysisTable(filteredAnalyses)
            val filteredPatternsSummary = buildPatternsSummary(filteredPatterns)

            // === Step 2: Select coherent subset for ONE set ===
            onPhaseChange(GenerationPhase.Grouping)
            val selectedSet = retryStep(MAX_RETRIES, "Step 2: Select") {
                stepSelectSubset(prompt, cached, filteredAnalysisTable, filteredPatternsSummary, existingSetTitles)
            }

            val setsToUse: GroupedSetsResponse
            if (selectedSet == null || selectedSet.sets.isNullOrEmpty() || selectedSet.sets[0].words.size < 3) {
                Log.w(TAG, "Step 2 failed, using algorithmic fallback")
                setsToUse = algorithmicSingleSetSelection(filteredAnalyses, filteredPatterns)
            } else {
                setsToUse = validateGroupedSets(selectedSet, wordBank)
            }

            // === Step 3: Assign Configurations for ONE set ===
            onPhaseChange(GenerationPhase.Configuring)
            val result = retryStep(MAX_RETRIES, "Step 3: Configure") {
                stepConfigureSingleSet(setsToUse.sets[0], filteredWordObjs, filteredAnalyses, existingSetTitles)
            }

            if (result == null) {
                Log.w(TAG, "Step 3 failed, using default configurations")
                return@withContext defaultConfigurations(setsToUse, filteredWordObjs)
            }

            // Final validation
            validateFinalResult(result, availableWords)

        } catch (e: Exception) {
            invalidateWordCache()
            handleException(e, "Generation")
        }
    }

    /**
     * Regenerate a single set using Steps 2->3 (skips Step 1, reuses cached filtered words).
     *
     * @param prompt Teacher's original prompt
     * @param availableWords Words from the teacher's word bank
     * @param currentSetTitle Title of the set being regenerated
     * @param currentSetDescription Description of the set being regenerated
     * @param onPhaseChange Callback for UI progress updates
     * @param maxRetries Maximum retry attempts per step
     * @return Result containing regenerated set data or error
     */
    suspend fun regenerateSet(
        prompt: String,
        availableWords: List<Word>,
        currentSetTitle: String,
        currentSetDescription: String,
        existingSetTitles: List<String> = emptyList(),
        onPhaseChange: suspend (GenerationPhase) -> Unit = {},
        maxRetries: Int = MAX_RETRIES
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        if (shouldThrottle("regenerateSet")) {
            return@withContext AiGenerationResult.Error(
                "Please wait before generating another activity.",
                canRetry = true
            )
        }
        try {
            // Use cached words from Step 1, validate against current word bank
            val wordBank = availableWords.map { it.word }.toSet()
            val wordsToUse = cachedFilteredWords
                ?.filter { it in wordBank }
                ?.takeIf { it.size >= 3 }
                ?: availableWords.map { it.word }
            val wordObjs = availableWords.filter { it.word in wordsToUse }
            val analyses = analyzeCVCWords(wordObjs)
            val patterns = detectCVCPatterns(analyses)
            val analysisTable = buildWordAnalysisTable(analyses)
            val patternsSummary = buildPatternsSummary(patterns)

            // Step 2: Select new subset (skip Step 1)
            onPhaseChange(GenerationPhase.Grouping)
            val selectedSet = retryStep(maxRetries, "Regen Step 2") {
                stepSelectSubset(
                    prompt, wordsToUse, analysisTable, patternsSummary,
                    existingSetTitles, avoidTitle = currentSetTitle, avoidDescription = currentSetDescription
                )
            }

            if (selectedSet == null || selectedSet.sets.isNullOrEmpty()) {
                return@withContext AiGenerationResult.Error(
                    "Failed to regenerate set. Please try again.",
                    canRetry = true
                )
            }

            val validatedSets = validateGroupedSets(selectedSet, wordBank)

            // Step 3: Configure single set
            onPhaseChange(GenerationPhase.Configuring)
            val result = retryStep(maxRetries, "Regen Step 3") {
                stepConfigureSingleSet(validatedSets.sets[0], wordObjs, analyses, existingSetTitles)
            }

            if (result == null) {
                return@withContext defaultConfigurations(validatedSets, wordObjs)
            }

            validateFinalResult(result, availableWords)

        } catch (e: Exception) {
            handleException(e, "Regeneration")
        }
    }

    // ========== Annotation Summary ==========

    /**
     * Generate an AI summary from a list of teacher annotations for a set/category.
     *
     * @param annotations All annotations for a given (studentId, setId, sessionMode)
     * @param sessionMode "LEARN" or "TUTORIAL"
     * @return Generated summary text, or null if generation fails
     */
    suspend fun generateAnnotationSummary(
        annotations: List<com.example.app.data.entity.LearnerProfileAnnotation>,
        sessionMode: String,
        itemNames: Map<Int, String> = emptyMap()
    ): String? = withContext(Dispatchers.IO) {
        if (annotations.isEmpty()) return@withContext null
        if (shouldThrottle("generateAnnotationSummary")) {
            return@withContext null
        }

        try {
            val isTutorial = sessionMode == com.example.app.data.entity.LearnerProfileAnnotation.MODE_TUTORIAL
            val itemLabel = if (isTutorial) "Letter" else "Word"

            val formattedAnnotations = annotations.joinToString("\n") { annotation ->
                buildString {
                    val name = itemNames[annotation.itemId]
                    if (name != null) {
                        append("- $itemLabel $name:")
                    } else {
                        append("- $itemLabel ${annotation.itemId + 1}:")
                    }
                    annotation.levelOfProgress?.let { append(" Level: $it.") }
                    val strengths = annotation.getStrengthsList()
                    if (strengths.isNotEmpty()) append(" Strengths: ${strengths.joinToString(", ")}.")
                    if (annotation.strengthsNote.isNotBlank()) append(" Strengths note: ${sanitizeInput(annotation.strengthsNote)}.")
                    val challenges = annotation.getChallengesList()
                    if (challenges.isNotEmpty()) append(" Challenges: ${challenges.joinToString(", ")}.")
                    if (annotation.challengesNote.isNotBlank()) append(" Challenges note: ${sanitizeInput(annotation.challengesNote)}.")
                }
            }

            val modeLabel = if (isTutorial) "tutorial" else "learn"

            val systemInstruction = """
You are an educational assessment assistant for young learners (ages 4-8) learning to read and write letters.
Based on the teacher's annotations, generate a concise 2-4 sentence summary.

The summary should:
- Reference specific ${itemLabel.lowercase()}s by name when discussing strengths or challenges
- Highlight overall strengths and areas for improvement
- Note specific patterns across items
- Provide one actionable recommendation for the teacher

Respond with ONLY the summary text, no JSON, no markdown formatting.
            """.trimIndent()

            val userPrompt = """
Here are the teacher's annotations for a student's $modeLabel session:

$formattedAnnotations

Generate a concise 2-4 sentence summary. Reference specific ${itemLabel.lowercase()}s by name.
            """.trimIndent()

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.3f
                    topP = 0.95f
                    maxOutputTokens = 256
                },
                safetySettings = safetySettings,
                systemInstruction = content { text(systemInstruction) }
            )

            val response = model.generateContent(userPrompt)
            val summary = response.text?.trim()

            if (summary.isNullOrBlank()) {
                Log.w(TAG, "Annotation summary generation returned empty")
                return@withContext null
            }

            Log.d(TAG, "Annotation summary generated: $summary")
            summary
        } catch (e: Exception) {
            Log.e(TAG, "Annotation summary generation failed: ${e.message}", e)
            null
        }
    }

    // ========== Activity Description ==========

    /**
     * Build a phonics context string from a list of word names and their selected letter indices.
     * Used to enrich activity description generation with CVC pattern info.
     */
    fun buildPhonicsContext(
        wordNames: List<String>,
        selectedLetterIndices: List<Int>,
        configurations: List<String>
    ): String {
        data class SimpleAnalysis(
            val word: String,
            val onset: Char,
            val vowel: Char,
            val coda: Char,
            val rime: String
        )

        val analyses = wordNames.map { w ->
            val lower = w.lowercase()
            SimpleAnalysis(
                word = w,
                onset = lower.getOrElse(0) { ' ' },
                vowel = lower.getOrElse(1) { ' ' },
                coda = lower.getOrElse(2) { ' ' },
                rime = if (lower.length >= 2) lower.drop(1) else ""
            )
        }

        val sb = StringBuilder()

        // Detect word families (same rime, 2+ words)
        val wordFamilies = analyses.groupBy { it.rime }.filter { it.value.size >= 2 }
        if (wordFamilies.isNotEmpty()) {
            wordFamilies.forEach { (rime, words) ->
                sb.appendLine("Word family: -$rime (${words.joinToString(", ") { it.word }})")
            }
        }

        // Detect same vowel groups (only if no word family covers all words)
        val familyWords = wordFamilies.values.flatten().map { it.word }.toSet()
        val nonFamilyWords = analyses.filter { it.word !in familyWords }
        if (nonFamilyWords.isNotEmpty()) {
            val sameVowel = analyses.groupBy { it.vowel }.filter { it.value.size >= 2 }
            sameVowel.forEach { (vowel, words) ->
                sb.appendLine("Shared vowel sound: short '$vowel' (${words.joinToString(", ") { it.word }})")
            }
        }

        // Detect same onset consonant groups
        val sameOnset = analyses.groupBy { it.onset }.filter { it.value.size >= 2 }
        if (sameOnset.isNotEmpty() && wordFamilies.isEmpty()) {
            sameOnset.forEach { (onset, words) ->
                sb.appendLine("Same starting letter: '$onset' (${words.joinToString(", ") { it.word }})")
            }
        }

        // Describe what letter positions are being practiced in fill-in-the-blanks
        val fillIndices = wordNames.zip(configurations).zip(selectedLetterIndices)
            .filter { (pair, _) -> pair.second.lowercase() in listOf("fill in the blanks", "fill in the blank") }
            .map { (pair, idx) -> Triple(pair.first, pair.second, idx) }

        if (fillIndices.isNotEmpty()) {
            val positionGroups = fillIndices.groupBy { it.third }
            val positionDescriptions = positionGroups.map { (idx, words) ->
                val posName = when (idx) {
                    0 -> "onset (first letter)"
                    1 -> "medial vowel (middle letter)"
                    2 -> "coda (last letter)"
                    else -> "letter index $idx"
                }
                "$posName: ${words.joinToString(", ") { it.first }}"
            }
            sb.appendLine("Fill-in-the-blanks targets: ${positionDescriptions.joinToString("; ")}")
        }

        return sb.toString().trimEnd()
    }

    /**
     * Generate a short description of an activity or tutorial lesson.
     *
     * @param activityTitle The title of the activity/lesson
     * @param sessionMode "LEARN" or "TUTORIAL"
     * @param items List of item names (letters for tutorials, words for learn mode)
     * @param configurations Optional list of configuration types for learn mode
     * @param phonicsContext Optional phonics analysis context for learn mode
     * @return Generated description text, or null if generation fails
     */
    suspend fun generateActivityDescription(
        activityTitle: String,
        sessionMode: String,
        items: List<String>,
        configurations: List<String>? = null,
        phonicsContext: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val isTutorial = sessionMode == "TUTORIAL"
            val itemType = if (isTutorial) "letters" else "words"

            val configInfo = if (!isTutorial && !configurations.isNullOrEmpty()) {
                val uniqueConfigs = configurations.distinct()
                "\nActivity types used: ${uniqueConfigs.joinToString(", ")}."
            } else ""

            val phonicsInfo = if (!isTutorial && !phonicsContext.isNullOrBlank()) {
                "\nPhonics analysis:\n$phonicsContext"
            } else ""

            val systemInstruction = """
You are an educational content assistant. Generate a single factual sentence describing what students learn in this lesson and which specific items it covers.

The description should:
- State the learning objective (what skill students practice)
- Name the specific letters or words covered
- Mention the activity type if provided (air writing, fill-in-the-blanks, etc.)
- If phonics analysis is provided and shows a clear pattern (word family, shared vowel, same onset letter), mention the pattern naturally in the description
- If no clear phonics pattern exists, just describe the activity and words simply
- Be objective and informative, written for teachers
- Maximum 25 words

Examples:

Input: Tutorial, Uppercase Vowels, letters A E I O U
Output: Students practice forming and recognizing uppercase vowels A, E, I, O, U through guided writing.

Input: Tutorial, Lowercase Consonants, letters b c d f g h j k l m n p q r s t v w x y z
Output: Covers stroke-by-stroke practice of all 21 lowercase consonant letters.

Input: Learn, "-at Family", words cat bat hat, configurations: fill in the blanks, phonics: Word family: -at (cat, bat, hat); Fill-in-the-blanks targets: onset (first letter)
Output: Students identify missing onset consonants in -at family words cat, bat, and hat through fill-in-the-blanks.

Input: Learn, "Short O Words", words dog bob mob, configurations: fill in the blanks, phonics: Shared vowel sound: short 'o' (dog, bob, mob); Fill-in-the-blanks targets: medial vowel (middle letter)
Output: Students practice short-o vowel recognition in CVC words dog, bob, and mob using fill-in-the-blanks.

Input: Learn, "Mixed CVC", words cat dog pen, configurations: write the word, fill in the blanks
Output: Students practice reading and writing CVC words cat, dog, and pen using air writing and fill-in-the-blanks.

Respond with ONLY the description text, no JSON, no markdown, no quotes.
            """.trimIndent()

            val userPrompt = """
Lesson: $activityTitle
Mode: ${if (isTutorial) "Tutorial (letter practice)" else "Learn (word practice)"}
${itemType.replaceFirstChar { it.uppercase() }} covered: ${items.joinToString(", ")}$configInfo$phonicsInfo

Generate one factual sentence describing this lesson.
            """.trimIndent()

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.3f
                    topP = 0.95f
                    maxOutputTokens = 64
                },
                safetySettings = safetySettings,
                systemInstruction = content { text(systemInstruction) }
            )

            val response = model.generateContent(userPrompt)
            val description = response.text?.trim()

            if (description.isNullOrBlank()) {
                Log.w(TAG, "Activity description generation returned empty")
                return@withContext null
            }

            Log.d(TAG, "Activity description generated: $description")
            description
        } catch (e: Exception) {
            Log.e(TAG, "Activity description generation failed: ${e.message}", e)
            null
        }
    }

    // ========== Kuu Recommendation ==========

    /**
     * Context for generating a student recommendation.
     */
    data class RecommendationContext(
        val studentName: String,
        val completedTutorials: List<String>,
        val completedLearnSets: List<String>,
        val annotationSummary: String,
        val availableTutorials: List<AvailableTutorial>,
        val availableLearnActivities: List<AvailableLearnActivity>
    )

    data class AvailableTutorial(
        val type: String,
        val letterType: String,
        val setId: Long
    )

    data class AvailableLearnActivity(
        val activityId: Long,
        val activityName: String,
        val sets: List<String>
    )

    data class RecommendationResponse(
        val title: String = "",
        val description: String = "",
        val activityType: String = "",
        val targetActivityId: Long = 0,
        val targetSetId: Long? = null
    )

    /**
     * Generate a personalized activity recommendation for a student using Gemini AI.
     *
     * @param context The student's progress context
     * @return A recommendation response, or null if generation fails
     */
    suspend fun generateRecommendation(
        context: RecommendationContext
    ): RecommendationResponse? = withContext(Dispatchers.IO) {
        if (shouldThrottle("generateRecommendation")) {
            return@withContext null
        }
        try {
            val completedTutorialsText = if (context.completedTutorials.isEmpty()) {
                "None"
            } else {
                context.completedTutorials.joinToString(", ")
            }

            val completedLearnText = if (context.completedLearnSets.isEmpty()) {
                "None"
            } else {
                context.completedLearnSets.joinToString(", ")
            }

            val availableTutorialsText = context.availableTutorials.joinToString("\n") { t ->
                "- ${t.type} ${t.letterType} (setId: ${t.setId})"
            }

            val availableLearnText = if (context.availableLearnActivities.isEmpty()) {
                "None available"
            } else {
                context.availableLearnActivities.joinToString("\n") { a ->
                    "- Activity \"${a.activityName}\" (activityId: ${a.activityId}), Sets: ${a.sets.joinToString(", ")}"
                }
            }

            val annotationsText = context.annotationSummary.ifBlank { "No annotations yet." }

            val systemInstruction = """
You are Kuu, a friendly learning assistant for young learners (ages 4-8) learning to read and write letters. Your job is to recommend the SINGLE best next activity for a student based on their progress.

STUDENT: ${context.studentName}

COMPLETED TUTORIALS: $completedTutorialsText
COMPLETED LEARN SETS: $completedLearnText

TEACHER ANNOTATIONS SUMMARY:
$annotationsText

AVAILABLE TUTORIALS:
$availableTutorialsText

AVAILABLE LEARN ACTIVITIES:
$availableLearnText

RECOMMENDATION RULES:
1. If the student has NO completions at all, recommend "Vowels Capital" tutorial (setId: -1).
2. Follow natural tutorial progression: Vowels Capital -> Vowels Small -> Consonants Capital -> Consonants Small.
3. If the student has completed some tutorials, recommend the next uncompleted tutorial in the progression.
4. If ALL tutorials are completed and learn activities are available, recommend a learn activity that addresses the student's challenges based on annotations.
5. If all tutorials are completed but no learn activities exist, recommend repeating a tutorial where the student had challenges.
6. ONLY recommend activities from the "available" lists above. Use the exact setId/activityId values provided.
7. For tutorials, use activityType "TUTORIAL" and set targetActivityId to the tutorial setId (negative number).
8. For learn activities, use activityType "LEARN" and set targetActivityId to the activityId.

RESPONSE FORMAT:
Return a JSON object with:
- "title": A short, encouraging card title (max 35 characters). Examples: "Try Vowels Tutorial!", "Practice Consonants!"
- "description": A brief 1 sentence description (max 80 characters) explaining why this is recommended. Be encouraging and specific.
- "activityType": Either "TUTORIAL" or "LEARN"
- "targetActivityId": The tutorial setId (for TUTORIAL) or activityId (for LEARN)
- "targetSetId": null for tutorials, or null for learn activities (teacher picks the set)
            """.trimIndent()

            val userPrompt = "Based on ${context.studentName}'s progress, recommend the best next activity."

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.3f
                    topP = 0.95f
                    maxOutputTokens = 256
                    responseMimeType = "application/json"
                },
                safetySettings = safetySettings,
                systemInstruction = content { text(systemInstruction) }
            )

            val response = model.generateContent(userPrompt)
            val json = response.text?.trim()

            if (json.isNullOrBlank()) {
                Log.w(TAG, "Recommendation generation returned empty")
                return@withContext null
            }

            Log.d(TAG, "Recommendation response: $json")
            val parsed = gson.fromJson(json, RecommendationResponse::class.java)

            // Validate the response
            if (parsed.title.isBlank() || parsed.activityType.isBlank()) {
                Log.w(TAG, "Invalid recommendation response: missing title or activityType")
                return@withContext null
            }

            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Recommendation generation failed: ${e.message}", e)
            null
        }
    }

    // ========== Suggested Prompts ==========

    /**
     * Generate 2 contextual prompt suggestions for the CVC word generation modal.
     *
     * @param words Current word bank contents (may be empty)
     * @return List of exactly 2 prompt suggestion strings, or empty list on failure
     */
    suspend fun generateSuggestedPrompts(words: List<Word>): List<String> = withContext(Dispatchers.IO) {
        try {
            val validWords = words.filter { it.word.isNotBlank() }
            val systemInstruction: String
            val userPrompt: String

            if (validWords.isEmpty()) {
                systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). The teacher has an empty word bank and wants to generate CVC (Consonant-Vowel-Consonant) words using AI.

CVC words are EXACTLY 3 letters: one single consonant, one vowel, one single consonant. Examples: cat, dog, run, pen, sit, hop, bug, net.

Suggest exactly 2 concise, helpful prompts that the teacher could use to generate their first batch of CVC words. Each prompt should be a natural sentence describing what kind of CVC words to create.

CRITICAL: Every prompt you suggest MUST only describe words that fit the 3-letter CVC pattern.
- NEVER suggest digraphs (sh, ch, th, wh, ph) or consonant blends (bl, cr, st, tr, fl, etc.) — these produce 4+ letter words, NOT CVC words
- NEVER use example words longer than 3 letters (e.g., ship, chat, stop, crab are NOT CVC)
- Only reference single consonants and short vowel sounds

Good examples: "Short vowel 'a' words like cat, bat, hat", "Animal-themed CVC words like dog, pig, hen"
Bad examples: "Words with 'sh' like ship, shop" (digraph, 4 letters), "Blend words like stop, crab" (blends, 4 letters), "Generate words" (too vague)

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
                """.trimIndent()

                userPrompt = "The teacher's word bank is empty. Suggest 2 helpful starter prompts for generating CVC words."
            } else {
                val analyses = analyzeCVCWords(validWords)
                val patterns = detectCVCPatterns(analyses)
                val wordList = if (validWords.size > 30) {
                    "${validWords.take(30).joinToString(", ") { it.word }} (and ${validWords.size - 30} more)"
                } else {
                    validWords.joinToString(", ") { it.word }
                }
                val patternsSummary = buildPatternsSummary(patterns)

                systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). The teacher already has CVC words in their word bank. Suggest exactly 2 concise prompts for generating NEW CVC words that would complement their existing collection.

CVC words are EXACTLY 3 letters: one single consonant, one vowel, one single consonant. Examples: cat, dog, run, pen, sit, hop, bug, net.

EXISTING WORDS: $wordList

DETECTED PATTERNS IN EXISTING WORDS:
$patternsSummary

RULES:
1. Suggest prompts that fill GAPS in the existing collection (missing vowel sounds, missing word families, missing themes)
2. Each prompt should be a natural sentence (10-20 words) describing what CVC words to create
3. Do NOT suggest words that overlap with existing patterns — focus on what's MISSING
4. Keep suggestions practical for ages 4-8
5. NEVER suggest digraphs (sh, ch, th, wh, ph) or consonant blends (bl, cr, st, tr, fl, etc.) — these produce 4+ letter words, NOT CVC words
6. NEVER use example words longer than 3 letters (e.g., ship, chat, stop, crab are NOT CVC)
7. Only reference single consonants and short vowel sounds

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
                """.trimIndent()

                userPrompt = "Based on the existing word bank, suggest 2 prompts for generating complementary CVC words."
            }

            val model = createModel(systemInstruction)
            val response = model.generateContent(userPrompt)
            val json = response.text ?: throw Exception("Empty response from suggested prompts")

            Log.d(TAG, "Suggested prompts response: $json")
            val parsed = gson.fromJson(json, SuggestedPromptsResponse::class.java)

            val prompts = parsed.prompts
                .filter { it.isNotBlank() }
                .filter { isValidPromptSuggestion(it) }
                .take(2)
            if (prompts.size < 2) {
                Log.w(TAG, "Fewer than 2 valid prompts returned: ${prompts.size}")
                return@withContext emptyList()
            }

            prompts
        } catch (e: Exception) {
            Log.e(TAG, "Suggested prompts generation failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Generate 2 contextual prompt suggestions for the activity creation modal.
     * Only called when the word bank is non-empty.
     *
     * @param words Current word bank contents (must be non-empty)
     * @return List of exactly 2 activity prompt suggestion strings, or empty list on failure
     */
    suspend fun generateActivitySuggestedPrompts(
        words: List<Word>,
        existingSets: Map<String, List<String>> = emptyMap()
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val validWords = words.filter { it.word.isNotBlank() }
            if (validWords.isEmpty()) return@withContext emptyList()

            val analyses = analyzeCVCWords(validWords)
            val patterns = detectCVCPatterns(analyses)
            val wordList = if (validWords.size > 30) {
                "${validWords.take(30).joinToString(", ") { it.word }} (and ${validWords.size - 30} more)"
            } else {
                validWords.joinToString(", ") { it.word }
            }
            val patternsSummary = buildPatternsSummary(patterns)

            val existingSetsSummary = if (existingSets.isEmpty()) {
                "None yet"
            } else {
                existingSets.entries.joinToString("\n") { (title, words) ->
                    "- \"$title\": ${words.joinToString(", ")}"
                }
            }

            val systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). The teacher wants to create a single air-writing activity using words from their word bank.

An activity is a focused word grouping for air-writing practice. It contains words that share a single learning objective (e.g., same word family, same vowel sound, same beginning consonant, or same theme). The teacher will later organize multiple activities into an activity set themselves.

WORD BANK: $wordList

DETECTED PATTERNS:
$patternsSummary

EXISTING ACTIVITIES:
$existingSetsSummary

Suggest exactly 2 concise prompts the teacher could use to describe what kind of activity to create. Each prompt should be a natural sentence (10-20 words) describing a single learning objective or word grouping.

Good examples: "Practice the -at word family with rhyming words", "Group words by their short 'o' vowel sound"
Bad examples: "Create an activity set with multiple groups" (too broad — suggest a single focus), "Generate new words" (wrong purpose — words already exist)

RULES:
1. Each prompt must describe ONE focused grouping of EXISTING words, not multiple groupings
2. Suggest strategies that leverage the detected patterns (word families, vowel sounds, themes)
3. AVOID suggesting activities that overlap with existing ones — check the existing activities list and suggest DIFFERENT patterns or angles
4. Keep suggestions practical and age-appropriate (ages 4-8)
5. Each prompt should suggest a different grouping strategy

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
            """.trimIndent()

            val userPrompt = "Based on the word bank and existing activities, suggest 2 prompts for creating a new air-writing activity."

            val model = createModel(systemInstruction)
            val response = model.generateContent(userPrompt)
            val json = response.text ?: throw Exception("Empty response from activity suggested prompts")

            Log.d(TAG, "Activity suggested prompts response: $json")
            val parsed = gson.fromJson(json, SuggestedPromptsResponse::class.java)

            val prompts = parsed.prompts
                .filter { it.isNotBlank() }
                .filter { isValidPromptSuggestion(it) }
                .take(2)
            if (prompts.size < 2) {
                Log.w(TAG, "Fewer than 2 valid activity prompts returned: ${prompts.size}")
                return@withContext emptyList()
            }

            prompts
        } catch (e: Exception) {
            Log.e(TAG, "Activity suggested prompts generation failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Validate that a suggested prompt is coherent and not repetitive garbage.
     * Rejects prompts where any 3+ character substring repeats excessively.
     */
    private fun isValidPromptSuggestion(prompt: String): Boolean {
        if (prompt.length > 200) {
            Log.w(TAG, "Prompt too long (${prompt.length} chars), rejecting")
            return false
        }

        // Check for repeated words: if any word appears more than 3 times, it's garbage
        val words = prompt.lowercase().split(Regex("[\\s,.'\"]+")).filter { it.length >= 2 }
        val wordCounts = words.groupingBy { it }.eachCount()
        val maxRepeats = wordCounts.values.maxOrNull() ?: 0
        if (maxRepeats > 3) {
            Log.w(TAG, "Prompt has excessive word repetition ($maxRepeats repeats), rejecting: ${prompt.take(80)}")
            return false
        }

        return true
    }

    // ========== CVC Word Generation ==========

    /**
     * Generate CVC words for the word bank based on teacher's prompt.
     *
     * @param prompt Teacher's description of what kind of CVC words to generate
     * @param count Desired number of words (will over-generate by ~50% to account for filtering)
     * @param existingWords Current word bank contents (for dedup context in prompt)
     * @return List of candidate CVC word strings (lowercase, trimmed)
     */
    suspend fun generateCVCWords(
        prompt: String,
        count: Int,
        existingWords: List<Word>
    ): List<String> = withContext(Dispatchers.IO) {
        if (shouldThrottle("generateCVCWords")) {
            return@withContext emptyList()
        }
        val requestCount = (count * 1.5).toInt().coerceAtLeast(count + 2)

        val existingWordList = existingWords
            .map { it.word.lowercase() }
            .distinct()
            .joinToString(", ")

        val existingSection = if (existingWords.isNotEmpty()) {
            "\n- Do NOT include any of these existing words: $existingWordList"
        } else ""

        val systemInstruction = """
You generate CVC (Consonant-Vowel-Consonant) words for early readers (ages 4-8).

RULES — follow these strictly:
- Each word must be EXACTLY 3 letters long
- Letter 1 must be a consonant (b, c, d, f, g, h, j, k, l, m, n, p, r, s, t, v, w, x, y, z)
- Letter 2 must be a vowel (a, e, i, o, u)
- Letter 3 must be a consonant (b, c, d, f, g, h, j, k, l, m, n, p, r, s, t, v, w, x, y, z)
- Every word must be a real, common English word that a child aged 4-8 would recognize and understand
- ONLY use words from a kindergarten/first-grade vocabulary level
- Do NOT use obscure, technical, or adult-vocabulary words like: vat, lat, fen, ken, wen, cob, cot, cud, dab, din, don, dun, fad, gab, gig, hag, hem, hob, hod, jab, jig, jot, jut, keg, kin, lax, lob, nab, nib, nod, pal, peg, rig, rut, sag, sod, tab, tat, tux, vex, vim, wad, wig, wit, yam, zap
- Prefer words that can be illustrated with a simple picture (cat, dog, sun, cup, bed, etc.)
- No proper nouns, slang, or obscure words
- No duplicate words in the list$existingSection

PATTERN ENFORCEMENT — THIS IS THE MOST IMPORTANT RULE:
- If the teacher's request specifies a word pattern or structure (e.g., "_en", "-at", "words ending in -un", "short 'a' words"), then EVERY SINGLE word you generate MUST follow that exact pattern. No exceptions.
- For example, if the request is "_en words", ALL words must end in "en" (e.g., hen, pen, ten, den, men, ben). Do NOT include words that do not match the pattern.
- If the request is "-at words", ALL words must end in "at" (e.g., cat, bat, hat, mat, rat, sat, fat, pat). Do NOT include words with different endings.
- The pattern takes absolute priority. Never substitute a non-matching word just to fill the count.

Return a JSON object: {"words": ["word1", "word2", ...]}
Return exactly $requestCount lowercase words. No extra text.
        """.trimIndent()

        val userPrompt = "Generate $requestCount CVC words based on this request: ${sanitizeInput(prompt)}"

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from CVC word generation")

        Log.d(TAG, "CVC word generation response: $json")
        val detectedPattern = classifyPrompt(prompt)
        try {
            val parsed = gson.fromJson(json, GeneratedWordsResponse::class.java)
            parsed.words
                .map { it.lowercase().trim() }
                .filter { it.isNotBlank() }
                .filter { isValidCVC(it) }
                .filter { it !in BLOCKED_WORDS }
                .filter { word ->
                    // If a structural pattern was detected, enforce it programmatically
                    if (detectedPattern != null) {
                        val onset = word[0]
                        val vowel = word[1]
                        val coda = word[2]
                        val rime = "${word[1]}${word[2]}"
                        detectedPattern.matches(onset, vowel, coda, rime)
                    } else true
                }
                .distinct()
        } catch (e: Exception) {
            Log.e(TAG, "CVC word generation JSON parse failed: ${e.message}", e)
            emptyList()
        }
    }

    // ========== Step Functions ==========

    /**
     * Step 1: Filter words matching teacher's request.
     */
    private suspend fun stepFilterWords(
        prompt: String,
        analysisTable: String,
        patternsSummary: String
    ): FilteredWordsResponse {
        val systemInstruction = """
You are a reading teacher's assistant for young learners (ages 4-8). You ONLY help with phonics, CVC words, and early reading activities. If the teacher's instruction is unrelated to reading/phonics education, or contains inappropriate content, return an empty word list with reasoning explaining why you cannot fulfill the request.

Your task: Given a word list and a teacher's instruction, select ONLY the words that are relevant to the instruction. Consider phonics patterns, word families, and themes when filtering.

PATTERN ENFORCEMENT — CRITICAL:
- If the teacher specifies a word pattern or structure (e.g., "_en", "-at", "words ending in -un"), then EVERY word you select MUST match that exact pattern. Do NOT include any word that does not match.
- For example: if the instruction is "-at words", only select words whose rime is "at" (cat, bat, hat, etc.). Exclude all others.
- If no words in the list match the pattern, return an empty word list with reasoning explaining that no matching words were found.

CVC CONTEXT:
- All words are 3-letter CVC (Consonant-Vowel-Consonant) words
- Students air-write ONE LETTER AT A TIME on a smartwatch

WORD ANALYSIS:
$analysisTable

DETECTED PATTERNS:
$patternsSummary

Respond with a JSON object containing:
- "words": array of selected word strings (ONLY from the word analysis list above)
- "reasoning": brief explanation of why these words were selected
        """.trimIndent()

        val userPrompt = "Teacher's request: ${sanitizeInput(prompt)}\n\nSelect the words from the word analysis that best match this request."

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 1")

        Log.d(TAG, "Step 1 response: $json")
        try {
            return gson.fromJson(json, FilteredWordsResponse::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to parse Step 1 response: ${e.message}", e)
        }
    }

    /**
     * Step 2: Select a coherent subset of words for ONE focused set.
     * Used for both initial generation and regeneration (with avoidance context).
     */
    private suspend fun stepSelectSubset(
        prompt: String,
        filteredWords: List<String>,
        analysisTable: String,
        patternsSummary: String,
        existingSetTitles: List<String> = emptyList(),
        avoidTitle: String? = null,
        avoidDescription: String? = null
    ): GroupedSetsResponse {
        val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
            """

EXISTING SET TITLES:
${existingSetTitles.joinToString("\n") { "- $it" }}

CRITICAL: The generated set title MUST NOT be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.

If the generated set title is semantically similar to an existing one (even if not identical), include a "titleSimilarity" field with "similarTo" (the existing title it resembles), "reason" (one-sentence explanation), and "alternateTitle" (a different 30-char-max title that avoids the similarity).
"""
        } else ""

        val avoidanceSection = if (avoidTitle != null) {
            """

AVOIDANCE CONSTRAINT: Do NOT duplicate the previous set:
- Previous set title: "$avoidTitle"
- Previous set description: "${avoidDescription ?: ""}"
Choose a different subset of words or a different angle on the teacher's request, but ALWAYS stay aligned with the original instruction.
"""
        } else ""

        val systemInstruction = """
You are a reading teacher's assistant. From the available words, select a coherent subset for ONE focused air-writing activity.

CRITICAL WORD COUNT CONSTRAINT — STRICTLY ENFORCED:
- The set MUST contain AT LEAST 3 words and ABSOLUTELY NO MORE THAN 10 words.
- If you have more than 10 matching words, pick the BEST 10. NEVER return 11 or more words.
- Count your words before responding. If the count exceeds 10, remove the least fitting words until you have exactly 10.

PATTERN ENFORCEMENT — CRITICAL:
- If the teacher specifies a word pattern or structure (e.g., "_en", "-at", "words ending in -un"), then EVERY word you select MUST match that exact pattern. Do NOT include any word that does not match the requested pattern.
- For example: if the instruction is "-at words", only select words whose rime is "at" (cat, bat, hat, etc.). Exclude all others regardless of how many words remain.

SELECTION STRATEGIES (use in priority order):
1. Word Family (highest priority): Same rime (cat/bat/hat = "-at" family)
2. Vowel Sound: Same middle vowel (cat/bag/hat = short 'a')
3. Onset Consonant: Same starting letter (cat/cup/can = 'c')
4. Theme-Based: Group by meaning if the teacher suggests a theme

RULES:
1. Select 3 to 10 words (NEVER more than 10) that form ONE coherent grouping matching the teacher's request
2. ONLY use words from the provided list
3. Set title: max 30 characters. Be creative — vary your naming style. Examples: "-at Rhyme Time", "Cat Bat Hat!", "A Sound Fun", "Vowel 'e' Mix", "B Words Go!"
4. A smaller, coherent set is better than a larger, mixed one
$existingSetTitlesSection$avoidanceSection
Respond with a JSON object containing:
- "sets": array with exactly ONE object having "title" (string), "description" (string), "words" (array of strings, minimum 3, MAXIMUM 10), and optionally "titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings)
        """.trimIndent()

        val userPrompt = """
Teacher's request: ${sanitizeInput(prompt)}

Available words: ${filteredWords.joinToString(", ")}

WORD ANALYSIS:
$analysisTable

DETECTED PATTERNS:
$patternsSummary

Select a coherent subset of words for one focused activity.
        """.trimIndent()

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 2")

        Log.d(TAG, "Step 2 response: $json")
        try {
            return gson.fromJson(json, GroupedSetsResponse::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to parse Step 2 response: ${e.message}", e)
        }
    }

    /**
     * Step 3: Assign configuration types and letter indices to words in a single set.
     */
    private suspend fun stepConfigureSingleSet(
        setGrouping: SetGroupingResponse,
        availableWords: List<Word>,
        analyses: List<CVCAnalysis>,
        existingSetTitles: List<String> = emptyList()
    ): AiGenerationResult {
        val wordsWithImages = availableWords.filter { !it.imagePath.isNullOrBlank() }.map { it.word }
        val wordsWithoutImages = availableWords.filter { it.imagePath.isNullOrBlank() }.map { it.word }

        val existingTitlesSection = if (existingSetTitles.isNotEmpty()) {
            buildString {
                appendLine()
                appendLine("EXISTING SET TITLES (avoid generating set titles semantically similar to these):")
                existingSetTitles.forEach { appendLine("- $it") }
                appendLine()
                appendLine("CRITICAL: The set title may not be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.")
                appendLine()
                appendLine("If the generated set title is semantically similar to an existing one (even if not identical), include a \"titleSimilarity\" field with \"similarTo\" (the existing title), \"reason\" (one-sentence explanation), and \"alternateTitle\" (a different 30-char-max title that avoids the similarity).")
            }
        } else ""

        val systemInstruction = """
You are a reading teacher's assistant. Assign configuration types and letter indices to each word in this set.

CRITICAL CONSTRAINT — The set MUST keep AT LEAST 3 words. Do NOT remove words. Include ALL words in your output with their configurations.

CONFIGURATION TYPES:
1. "write the word": Air-write all 3 letters. Good for full word spelling practice.
2. "fill in the blanks": Air-write ONE missing letter. Choose based on learning goal:
   - Blank ONSET (selectedLetterIndex: 0) for word family sets -> "_at" teaches beginning consonants
   - Blank VOWEL (selectedLetterIndex: 1) for same-onset sets -> "c_t" teaches vowel recognition
   - Blank CODA (selectedLetterIndex: 2) -> "ca_" teaches ending consonants
3. "name the picture": See picture, air-write all 3 letters. ONLY for words WITH images.

WORDS WITH IMAGES (can use "name the picture"):
${wordsWithImages.joinToString(", ").ifEmpty { "(none)" }}

WORDS WITHOUT IMAGES (CANNOT use "name the picture"):
${wordsWithoutImages.joinToString(", ").ifEmpty { "(none)" }}

RULES:
1. The set MUST contain at least 3 words — include ALL words from the input
2. For "fill in the blanks": selectedLetterIndex must be 0, 1, or 2
3. For "name the picture" and "write the word": always use selectedLetterIndex: 0
4. NEVER use "name the picture" for words without images
5. Set title: max 30 characters. Keep the title from the input unless it exceeds the limit or is too generic.
$existingTitlesSection
Respond with a JSON object containing:
- "sets": array with exactly ONE object having:
  - "title" (string)
  - "description" (string)
  - "titleSimilarity" (optional object with "similarTo", "reason", and "alternateTitle" strings)
  - "words": array of objects with "word" (string), "configurationType" (string), "selectedLetterIndex" (integer) — minimum 3 words
        """.trimIndent()

        val wordAnalysis = analyses.joinToString("\n") { a ->
            "- ${a.word}: onset='${a.onset}', vowel='${a.vowel}', coda='${a.coda}', rime='-${a.rime}', has image: ${if (a.hasImage) "yes" else "no"}"
        }

        val userPrompt = """
Here is the set to configure:
Set "${setGrouping.title}" (${setGrouping.description}): ${setGrouping.words.joinToString(", ")}

WORD ANALYSIS:
$wordAnalysis

Assign appropriate configuration types and letter indices to each word based on the set's theme and the word's properties.
        """.trimIndent()

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 3")

        Log.d(TAG, "Step 3 response: $json")
        val aiResponse = try {
            gson.fromJson(json, AiResponse::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to parse Step 3 response: ${e.message}", e)
        }
        return parseStep3Response(aiResponse, availableWords, existingSetTitles)
    }

    // ========== Retry Logic ==========

    private suspend fun <T> retryStep(
        maxRetries: Int,
        stepName: String,
        block: suspend () -> T
    ): T? {
        var lastException: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                // Don't retry safety-related blocks
                val msg = e.message ?: ""
                if (msg.contains("blocked", ignoreCase = true) ||
                    msg.contains("safety", ignoreCase = true) ||
                    msg.contains("SAFETY", ignoreCase = true)
                ) {
                    throw e
                }
                lastException = e
                Log.w(TAG, "$stepName attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < maxRetries) {
                    delay(1000L * (attempt + 1))
                }
            }
        }
        Log.e(TAG, "$stepName failed after ${maxRetries + 1} attempts", lastException)
        return null
    }

    // ========== Validation ==========

    /**
     * Ensures a title is unique against existing titles and already-used titles (case-insensitive).
     * Tries alternateTitle first, then appends suffixes. Respects 30-character max limit.
     */
    private fun ensureUniqueTitle(
        title: String,
        alternateTitle: String,
        existingTitles: Set<String>,
        usedTitles: MutableSet<String>
    ): String {
        val allTaken = (existingTitles + usedTitles).map { it.lowercase() }.toSet()

        // If title is already unique, use it
        if (title.lowercase() !in allTaken) {
            usedTitles.add(title)
            return title
        }

        // Try AI-provided alternate title
        if (alternateTitle.isNotBlank() && alternateTitle.length <= 30 &&
            alternateTitle.lowercase() !in allTaken
        ) {
            usedTitles.add(alternateTitle)
            return alternateTitle
        }

        // Suffix fallback — truncate at word boundary to avoid broken words
        val suffixes = listOf(" II", " III", " IV", " V")
        for (suffix in suffixes) {
            val base = if (title.length + suffix.length <= 30) {
                title
            } else {
                val maxBase = 30 - suffix.length
                val truncated = title.take(maxBase)
                val lastSpace = truncated.lastIndexOf(' ')
                if (lastSpace > 3) truncated.substring(0, lastSpace).trimEnd() else truncated.trimEnd()
            }
            val candidate = base + suffix
            if (candidate.length <= 30 && candidate.lowercase() !in allTaken) {
                usedTitles.add(candidate)
                return candidate
            }
        }

        // Last resort: truncate at word boundary and add number
        val num = existingTitles.size + usedTitles.size + 1
        val numSuffix = " $num"
        val maxBase = 30 - numSuffix.length
        val truncated = title.take(maxBase)
        val lastSpace = truncated.lastIndexOf(' ')
        val base = if (lastSpace > 3) truncated.substring(0, lastSpace).trimEnd() else truncated.trimEnd()
        val candidate = (base + numSuffix).take(30)
        usedTitles.add(candidate)
        return candidate
    }

    /**
     * Parse and validate Step 3 response into AiGenerationResult.
     */
    private fun parseStep3Response(
        response: AiResponse,
        availableWords: List<Word>,
        existingSetTitles: List<String> = emptyList()
    ): AiGenerationResult {
        val wordMap = availableWords.associateBy { it.word }

        // Dedup tracking sets
        val usedSetTitles = mutableSetOf<String>()

        if (response.sets.isEmpty()) {
            return AiGenerationResult.Error("No sets in response", canRetry = true)
        }

        val validatedSets = mutableListOf<AiGeneratedSet>()
        val orphanWords = mutableListOf<AiWordConfig>()

        for ((index, set) in response.sets.withIndex()) {
            // Auto-correct set title (truncate at word boundary)
            val setTitle = when {
                set.title.isBlank() -> "Set ${index + 1}"
                set.title.length > 30 -> {
                    val truncated = set.title.take(30)
                    val lastSpace = truncated.lastIndexOf(' ')
                    if (lastSpace > 5) truncated.substring(0, lastSpace).trimEnd() else truncated.trimEnd()
                }
                else -> set.title
            }

            // Dedup set title against existing set titles and other generated sets
            val dedupedSetTitle = ensureUniqueTitle(
                setTitle,
                set.titleSimilarity?.alternateTitle ?: "",
                existingSetTitles.toSet(),
                usedSetTitles
            )

            val validatedWords = mutableListOf<AiWordConfig>()

            for (wordConfig in set.words) {
                val wordObj = wordMap[wordConfig.word]
                if (wordObj == null) {
                    // Skip unknown words instead of failing hard
                    Log.w(TAG, "Word '${wordConfig.word}' not in word bank, skipping")
                    continue
                }

                // Normalize config type
                val validTypes = mapOf(
                    "fill in the blanks" to "fill in the blanks",
                    "fill in the blank" to "fill in the blanks",
                    "name the picture" to "name the picture",
                    "write the word" to "write the word"
                )
                var configType = validTypes[wordConfig.configurationType.lowercase().trim()]
                    ?: "write the word"

                // Can't use "name the picture" without image
                if (configType == "name the picture" && wordObj.imagePath.isNullOrBlank()) {
                    Log.w(TAG, "Word '${wordConfig.word}' has no image, downgrading from 'name the picture' to 'fill in the blanks'")
                    configType = "fill in the blanks"
                }

                // Clamp letter index
                var letterIndex = wordConfig.selectedLetterIndex
                if (configType == "fill in the blanks") {
                    letterIndex = letterIndex.coerceIn(0, wordConfig.word.length - 1)
                } else {
                    letterIndex = 0
                }

                validatedWords.add(
                    AiWordConfig(
                        word = wordConfig.word,
                        configurationType = configType,
                        selectedLetterIndex = letterIndex
                    )
                )
            }

            // Hard cap at 10 words per set
            val cappedWords = if (validatedWords.size > 10) {
                Log.w(TAG, "Set '${dedupedSetTitle}' has ${validatedWords.size} words, capping to 10")
                validatedWords.take(10).toMutableList()
            } else {
                validatedWords
            }

            // If set has < 3 valid words, collect them as orphans to merge later
            if (cappedWords.size < 3) {
                Log.w(TAG, "Set '${dedupedSetTitle}' has only ${cappedWords.size} valid words, merging into another set")
                orphanWords.addAll(cappedWords)
                continue
            }

            // Extract set-level title similarity
            val setTitleSimilarity = set.titleSimilarity?.let { sim ->
                if (sim.similarTo.isNotBlank()) {
                    TitleSimilarity(
                        generatedTitle = dedupedSetTitle,
                        similarToExisting = sim.similarTo,
                        reason = sim.reason,
                        alternateTitle = sim.alternateTitle
                    )
                } else null
            }

            validatedSets.add(
                AiGeneratedSet(
                    title = dedupedSetTitle,
                    description = set.description,
                    words = cappedWords,
                    titleSimilarity = setTitleSimilarity
                )
            )
        }

        // Merge orphan words into the last valid set, capped at 10
        if (orphanWords.isNotEmpty() && validatedSets.isNotEmpty()) {
            val lastSet = validatedSets.last()
            val existingWordNames = lastSet.words.map { it.word }.toSet()
            val uniqueOrphans = orphanWords.filter { it.word !in existingWordNames }
            val spotsAvailable = 10 - lastSet.words.size
            val toMerge = uniqueOrphans.take(spotsAvailable.coerceAtLeast(0))
            if (toMerge.isNotEmpty()) {
                validatedSets[validatedSets.lastIndex] = lastSet.copy(
                    words = lastSet.words + toMerge
                )
            }
        } else if (validatedSets.isEmpty() && orphanWords.size >= 3) {
            // All sets were undersized but we have enough orphan words for one set (cap at 10)
            validatedSets.add(
                AiGeneratedSet(
                    title = "CVC Practice",
                    description = "CVC word practice",
                    words = orphanWords.take(10)
                )
            )
        }

        if (validatedSets.isEmpty()) {
            return AiGenerationResult.Error(
                "Not enough valid words to create a set (minimum 3 required)",
                canRetry = true
            )
        }

        return AiGenerationResult.Success(
            AiGeneratedActivity(
                activity = AiActivityInfo(
                    title = "",
                    description = ""
                ),
                sets = validatedSets
            )
        )
    }

    /**
     * Validate grouped sets: remove words not in word bank, deduplicate across sets,
     * and merge undersized sets into others to enforce the 3-word minimum.
     */
    private fun validateGroupedSets(
        groupedSets: GroupedSetsResponse,
        wordBank: Set<String>
    ): GroupedSetsResponse {
        val seen = mutableSetOf<String>()
        val cleanedSets = groupedSets.sets.map { set ->
            val validWords = set.words
                .filter { it in wordBank }  // Only keep words in word bank
                .filter { seen.add(it) }    // Remove duplicates across sets
                .take(10)                   // Hard cap at 10 words per set
            set.copy(words = validWords)
        }

        // Separate into valid sets (>= 3 words) and undersized sets
        val validSets = cleanedSets.filter { it.words.size >= 3 }.toMutableList()
        val orphanWords = cleanedSets.filter { it.words.size < 3 }.flatMap { it.words }

        if (orphanWords.isNotEmpty() && validSets.isNotEmpty()) {
            // Merge orphan words into the last valid set, but cap at 10
            val lastSet = validSets.last()
            val spotsAvailable = 10 - lastSet.words.size
            val toMerge = orphanWords.take(spotsAvailable.coerceAtLeast(0))
            if (toMerge.isNotEmpty()) {
                validSets[validSets.lastIndex] = lastSet.copy(words = lastSet.words + toMerge)
            }
        } else if (validSets.isEmpty()) {
            // All sets were undersized — combine all words into a single set (cap at 10)
            val allWords = cleanedSets.flatMap { it.words }.take(10)
            if (allWords.size >= 3) {
                return GroupedSetsResponse(
                    sets = listOf(
                        SetGroupingResponse(
                            title = "CVC Practice",
                            description = "CVC word practice",
                            words = allWords
                        )
                    )
                )
            }
            // Not enough valid words at all — fall back to original (will error downstream)
            return groupedSets
        }

        return GroupedSetsResponse(sets = validSets)
    }

    /**
     * Pass-through validation for final result (main validation is in parseStep3Response).
     */
    private fun validateFinalResult(
        result: AiGenerationResult,
        availableWords: List<Word>
    ): AiGenerationResult {
        return result
    }

    // ========== Fallbacks ==========

    /**
     * Algorithmic single-set selection using CVC patterns when Step 2 fails.
     * Returns the FIRST valid grouping (>= 3 words) as a single set.
     */
    private fun algorithmicSingleSetSelection(
        analyses: List<CVCAnalysis>,
        patterns: CVCPatterns
    ): GroupedSetsResponse {
        // Priority 1: First word family with >= 3 words
        for ((rime, words) in patterns.wordFamilies) {
            val groupWords = words.take(10)
            if (groupWords.size >= 3) {
                return GroupedSetsResponse(
                    sets = listOf(
                        SetGroupingResponse(
                            title = "-$rime Family",
                            description = "Words ending in -$rime",
                            words = groupWords.map { it.word }
                        )
                    )
                )
            }
        }

        // Priority 2: First vowel group with >= 3 words
        for ((vowel, words) in patterns.sameVowel) {
            val groupWords = words.take(10)
            if (groupWords.size >= 3) {
                return GroupedSetsResponse(
                    sets = listOf(
                        SetGroupingResponse(
                            title = "Short '$vowel' Words",
                            description = "Words with the '$vowel' vowel sound",
                            words = groupWords.map { it.word }
                        )
                    )
                )
            }
        }

        // Fallback: first 3-10 filtered words as "CVC Practice"
        val fallbackWords = analyses.take(10).map { it.word }
        return GroupedSetsResponse(
            sets = listOf(
                SetGroupingResponse(
                    title = "CVC Practice",
                    description = "CVC word practice",
                    words = fallbackWords
                )
            )
        )
    }

    /**
     * Default configuration fallback when Step 3 fails.
     * Assigns "write the word" to all words in the first set.
     */
    private fun defaultConfigurations(
        groupedSets: GroupedSetsResponse,
        availableWords: List<Word>
    ): AiGenerationResult {
        val firstSet = groupedSets.sets.first()
        val singleSet = AiGeneratedSet(
            title = firstSet.title,
            description = firstSet.description,
            words = firstSet.words.map { word ->
                AiWordConfig(
                    word = word,
                    configurationType = "write the word",
                    selectedLetterIndex = 0
                )
            }
        )

        return AiGenerationResult.Success(
            AiGeneratedActivity(
                activity = AiActivityInfo(
                    title = "",
                    description = ""
                ),
                sets = listOf(singleSet)
            )
        )
    }

    // ========== Error Handling ==========

    private fun handleException(e: Exception, context: String): AiGenerationResult {
        val msg = e.message ?: ""
        return when {
            msg.contains("blocked", ignoreCase = true) ||
            msg.contains("safety", ignoreCase = true) ->
                AiGenerationResult.Error(
                    "The request couldn't be processed. Please rephrase your instruction.",
                    canRetry = false
                )
            else -> {
                Log.e(TAG, "$context failed: ${e.message}", e)
                AiGenerationResult.Error("$context failed: ${e.message}", canRetry = true)
            }
        }
    }

    // ========== Internal Response Parsing Classes ==========

    private data class AiResponse(
        val activity: ActivityInfo = ActivityInfo(),
        val sets: List<SetInfo> = emptyList()
    )

    private data class ActivityInfo(
        val title: String = "",
        val description: String = "",
        val titleSimilarity: TitleSimilarityResponse? = null
    )

    private data class SetInfo(
        val title: String = "",
        val description: String = "",
        val words: List<WordInfo> = emptyList(),
        val titleSimilarity: TitleSimilarityResponse? = null
    )

    private data class WordInfo(
        val word: String = "",
        val configurationType: String = "",
        val selectedLetterIndex: Int = 0
    )

    // ========== Suggested Prompts ==========

    private data class SuggestedPromptsResponse(
        val prompts: List<String> = emptyList()
    )

    // ========== CVC Word Generation ==========

    private data class GeneratedWordsResponse(
        val words: List<String> = emptyList()
    )

    // ========== Input Sanitization ==========

    private fun sanitizeInput(input: String, maxLength: Int = 500): String {
        return input
            .take(maxLength)
            .replace(Regex("[\\r\\n\\t]"), " ")
            .replace(Regex("[{}``]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ========== CVC Validation ==========

    private fun isValidCVC(word: String): Boolean {
        if (word.length != 3) return false
        val vowels = setOf('a', 'e', 'i', 'o', 'u')
        return word[0] !in vowels && word[1] in vowels && word[2] !in vowels
    }

    private val BLOCKED_WORDS = setOf(
        "ass", "cum", "fag", "fuc", "fuk", "gay", "god",
        "hoe", "nig", "pis", "poo", "sex", "tit"
    )

    // ========== CVC Word Analysis ==========

    private data class CVCAnalysis(
        val word: String,
        val onset: Char,
        val vowel: Char,
        val coda: Char,
        val rime: String,
        val hasImage: Boolean
    )

    private fun analyzeCVCWords(words: List<Word>): List<CVCAnalysis> {
        return words.mapNotNull { w ->
            val lower = w.word.lowercase()
            if (isValidCVC(lower)) {
                CVCAnalysis(
                    word = w.word,
                    onset = lower[0],
                    vowel = lower[1],
                    coda = lower[2],
                    rime = "${lower[1]}${lower[2]}",
                    hasImage = !w.imagePath.isNullOrBlank()
                )
            } else {
                Log.w(TAG, "Word '${w.word}' is not a valid CVC word, skipping analysis")
                null
            }
        }
    }

    private data class CVCPatterns(
        val wordFamilies: Map<String, List<CVCAnalysis>>,
        val sameVowel: Map<Char, List<CVCAnalysis>>,
        val sameOnset: Map<Char, List<CVCAnalysis>>,
        val sameCoda: Map<Char, List<CVCAnalysis>>
    )

    private fun detectCVCPatterns(analyses: List<CVCAnalysis>): CVCPatterns {
        return CVCPatterns(
            wordFamilies = analyses.groupBy { it.rime }.filter { it.value.size >= 2 },
            sameVowel = analyses.groupBy { it.vowel }.filter { it.value.size >= 2 },
            sameOnset = analyses.groupBy { it.onset }.filter { it.value.size >= 2 },
            sameCoda = analyses.groupBy { it.coda }.filter { it.value.size >= 2 }
        )
    }

    private fun buildWordAnalysisTable(analyses: List<CVCAnalysis>): String {
        return analyses.joinToString("\n") { a ->
            "- ${a.word}: onset='${a.onset}', vowel='${a.vowel}', coda='${a.coda}', rime='-${a.rime}', has image: ${if (a.hasImage) "yes" else "no"}"
        }
    }

    private fun buildPatternsSummary(patterns: CVCPatterns): String {
        val sb = StringBuilder()

        if (patterns.wordFamilies.isNotEmpty()) {
            sb.appendLine("Word Families (same rime):")
            patterns.wordFamilies.forEach { (rime, words) ->
                sb.appendLine("- \"-$rime\" family: ${words.joinToString(", ") { it.word }}")
            }
            sb.appendLine()
        }

        if (patterns.sameVowel.isNotEmpty()) {
            sb.appendLine("Same Vowel:")
            patterns.sameVowel.forEach { (vowel, words) ->
                sb.appendLine("- Vowel '$vowel': ${words.joinToString(", ") { it.word }}")
            }
            sb.appendLine()
        }

        if (patterns.sameOnset.isNotEmpty()) {
            sb.appendLine("Same Onset:")
            patterns.sameOnset.forEach { (onset, words) ->
                sb.appendLine("- Starting with '$onset': ${words.joinToString(", ") { it.word }}")
            }
            sb.appendLine()
        }

        if (patterns.sameCoda.isNotEmpty()) {
            sb.appendLine("Same Coda:")
            patterns.sameCoda.forEach { (coda, words) ->
                sb.appendLine("- Ending with '$coda': ${words.joinToString(", ") { it.word }}")
            }
        }

        return sb.toString().trimEnd()
    }
}
