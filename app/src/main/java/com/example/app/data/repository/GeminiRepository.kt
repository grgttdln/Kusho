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
        existingActivityTitles: List<String> = emptyList(),
        existingSetTitles: List<String> = emptyList(),
        onPhaseChange: suspend (GenerationPhase) -> Unit = {}
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        try {
            val analyses = analyzeCVCWords(availableWords)
            val patterns = detectCVCPatterns(analyses)
            val analysisTable = buildWordAnalysisTable(analyses)
            val patternsSummary = buildPatternsSummary(patterns)

            // === Step 1: Filter Words ===
            onPhaseChange(GenerationPhase.Filtering)
            val filteredWords = retryStep(MAX_RETRIES, "Step 1: Filter") {
                stepFilterWords(prompt, analysisTable, patternsSummary)
            }

            val wordBank = availableWords.map { it.word }.toSet()
            if (filteredWords == null || filteredWords.words.isNullOrEmpty()) {
                Log.w(TAG, "Step 1 failed or returned empty, using all words as fallback")
                cachedFilteredWords = availableWords.map { it.word }
            } else {
                // Strip hallucinated words not in word bank
                cachedFilteredWords = filteredWords.words.filter { it in wordBank }
                if (cachedFilteredWords!!.size < 3) {
                    Log.w(TAG, "Too few valid words after filtering (${cachedFilteredWords!!.size}), using all words")
                    cachedFilteredWords = availableWords.map { it.word }
                }
            }

            // Rebuild analysis for filtered words only
            val filteredWordObjs = availableWords.filter { it.word in cachedFilteredWords!! }
            val filteredAnalyses = analyzeCVCWords(filteredWordObjs)
            val filteredPatterns = detectCVCPatterns(filteredAnalyses)
            val filteredAnalysisTable = buildWordAnalysisTable(filteredAnalyses)
            val filteredPatternsSummary = buildPatternsSummary(filteredPatterns)

            // === Step 2: Group into Sets ===
            onPhaseChange(GenerationPhase.Grouping)
            val groupedSets = retryStep(MAX_RETRIES, "Step 2: Group") {
                stepGroupIntoSets(prompt, cachedFilteredWords!!, filteredAnalysisTable, filteredPatternsSummary, existingSetTitles)
            }

            val setsToUse: GroupedSetsResponse
            if (groupedSets == null || groupedSets.sets.isNullOrEmpty()) {
                Log.w(TAG, "Step 2 failed, using algorithmic fallback")
                setsToUse = algorithmicGrouping(filteredAnalyses, filteredPatterns)
            } else {
                setsToUse = validateGroupedSets(groupedSets, wordBank)
            }

            // === Step 3: Assign Configurations ===
            onPhaseChange(GenerationPhase.Configuring)
            val result = retryStep(MAX_RETRIES, "Step 3: Configure") {
                stepAssignConfigurations(setsToUse, filteredWordObjs, filteredAnalyses, existingActivityTitles, existingSetTitles)
            }

            if (result == null) {
                Log.w(TAG, "Step 3 failed, using default configurations")
                return@withContext defaultConfigurations(setsToUse, filteredWordObjs)
            }

            // Final validation
            validateFinalResult(result, availableWords)

        } catch (e: Exception) {
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
        try {
            // Use cached words from Step 1, or fall back to all words
            val wordsToUse = cachedFilteredWords ?: availableWords.map { it.word }
            val wordObjs = availableWords.filter { it.word in wordsToUse }
            val analyses = analyzeCVCWords(wordObjs)
            val patterns = detectCVCPatterns(analyses)
            val analysisTable = buildWordAnalysisTable(analyses)
            val patternsSummary = buildPatternsSummary(patterns)

            // Step 2: Regroup (skip Step 1)
            onPhaseChange(GenerationPhase.Grouping)
            val groupedSets = retryStep(maxRetries, "Regen Step 2") {
                stepRegroupSet(
                    prompt, wordsToUse, analysisTable, patternsSummary,
                    currentSetTitle, currentSetDescription, existingSetTitles
                )
            }

            if (groupedSets == null || groupedSets.sets.isNullOrEmpty()) {
                return@withContext AiGenerationResult.Error(
                    "Failed to regenerate set. Please try again.",
                    canRetry = true
                )
            }

            val wordBank = availableWords.map { it.word }.toSet()
            val validatedSets = validateGroupedSets(groupedSets, wordBank)

            // Step 3: Configure
            onPhaseChange(GenerationPhase.Configuring)
            val result = retryStep(maxRetries, "Regen Step 3") {
                stepAssignConfigurations(validatedSets, wordObjs, analyses, emptyList(), existingSetTitles)
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
                    if (annotation.strengthsNote.isNotBlank()) append(" Strengths note: ${annotation.strengthsNote}.")
                    val challenges = annotation.getChallengesList()
                    if (challenges.isNotEmpty()) append(" Challenges: ${challenges.joinToString(", ")}.")
                    if (annotation.challengesNote.isNotBlank()) append(" Challenges note: ${annotation.challengesNote}.")
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

Suggest exactly 2 concise, helpful prompts that the teacher could use to generate their first batch of CVC words. Each prompt should be a natural sentence describing what kind of CVC words to create.

Good examples: "Common short vowel 'a' words like cat, bat, hat", "Animal-themed CVC words for beginners"
Bad examples: "Generate words" (too vague), "Create a comprehensive phonics curriculum" (too broad)

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

EXISTING WORDS: $wordList

DETECTED PATTERNS IN EXISTING WORDS:
$patternsSummary

RULES:
1. Suggest prompts that fill GAPS in the existing collection (missing vowel sounds, missing word families, missing themes)
2. Each prompt should be a natural sentence (10-20 words) describing what CVC words to create
3. Do NOT suggest words that overlap with existing patterns — focus on what's MISSING
4. Keep suggestions practical for ages 4-8

Respond with a JSON object: {"prompts": ["prompt 1", "prompt 2"]}
                """.trimIndent()

                userPrompt = "Based on the existing word bank, suggest 2 prompts for generating complementary CVC words."
            }

            val model = createModel(systemInstruction)
            val response = model.generateContent(userPrompt)
            val json = response.text ?: throw Exception("Empty response from suggested prompts")

            Log.d(TAG, "Suggested prompts response: $json")
            val parsed = gson.fromJson(json, SuggestedPromptsResponse::class.java)

            val prompts = parsed.prompts.filter { it.isNotBlank() }.take(2)
            if (prompts.size < 2) {
                Log.w(TAG, "Fewer than 2 prompts returned: ${prompts.size}")
                return@withContext emptyList()
            }

            prompts
        } catch (e: Exception) {
            Log.e(TAG, "Suggested prompts generation failed: ${e.message}", e)
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

        val userPrompt = "Teacher's request: $prompt\n\nSelect the words from the word analysis that best match this request."

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 1")

        Log.d(TAG, "Step 1 response: $json")
        return gson.fromJson(json, FilteredWordsResponse::class.java)
    }

    /**
     * Step 2: Group filtered words into coherent educational sets.
     */
    private suspend fun stepGroupIntoSets(
        prompt: String,
        filteredWords: List<String>,
        analysisTable: String,
        patternsSummary: String,
        existingSetTitles: List<String> = emptyList()
    ): GroupedSetsResponse {
        val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
            """

EXISTING SET TITLES:
${existingSetTitles.joinToString("\n") { "- $it" }}

CRITICAL: Generated set titles MUST NOT be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.

If a generated set title is semantically similar to an existing one (even if not identical), include a "titleSimilarity" field in that set's JSON object with "similarTo" (the existing title it resembles), "reason" (one-sentence explanation), and "alternateTitle" (a different 15-char-max title that avoids the similarity).
"""
        } else ""

        val systemInstruction = """
You are a reading teacher's assistant. Group these CVC words into coherent educational sets.

CRITICAL CONSTRAINT — EVERY set MUST contain AT LEAST 3 words. Sets with fewer than 3 words are INVALID and will be rejected. If a grouping pattern only matches 1-2 words, either merge those words into another set or find a broader pattern that includes at least 3 words.

GROUPING STRATEGIES (use in priority order):
1. Word Family Sets (highest priority): Group by same rime (cat/bat/hat = "-at" family)
2. Vowel Sound Sets: Group by same middle vowel (cat/bag/hat = short 'a')
3. Onset Consonant Sets: Group by same starting letter (cat/cup/can = 'c')
4. Theme-Based Sets: Group by meaning if the teacher suggests a theme

RULES:
1. EVERY set MUST have at least 3 words — this is mandatory and non-negotiable
2. ONLY use words from the provided list - do not invent words
3. Every word in a set MUST belong to the set's theme - no filler words
4. Maximum 10 words per set
5. Create 1-5 sets to coherently group the words
6. Set titles: max 15 characters. Be creative — vary your naming style, don't always use the same pattern. Examples: "-at Rhyme Time", "Cat Bat Hat!", "A Sound Fun", "Vowel 'e' Mix", "B Words Go!"
7. A smaller, coherent set is always better than a larger, mixed one — but never fewer than 3 words
$existingSetTitlesSection
Respond with a JSON object containing:
- "sets": array of objects, each with "title" (string), "description" (string), "words" (array of strings, minimum 3), and optionally "titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings)
        """.trimIndent()

        val userPrompt = """
Teacher's request: $prompt

Available words: ${filteredWords.joinToString(", ")}

WORD ANALYSIS:
$analysisTable

DETECTED PATTERNS:
$patternsSummary

Group these words into coherent educational sets.
        """.trimIndent()

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 2")

        Log.d(TAG, "Step 2 response: $json")
        return gson.fromJson(json, GroupedSetsResponse::class.java)
    }

    /**
     * Step 2 variant: Regroup words for set regeneration (avoids old set's theme).
     */
    private suspend fun stepRegroupSet(
        prompt: String,
        filteredWords: List<String>,
        analysisTable: String,
        patternsSummary: String,
        oldSetTitle: String,
        oldSetDescription: String,
        existingSetTitles: List<String> = emptyList()
    ): GroupedSetsResponse {
        val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
            """

EXISTING SET TITLES:
${existingSetTitles.joinToString("\n") { "- $it" }}

CRITICAL: The generated set title MUST NOT be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.

If the generated set title is semantically similar to an existing one (even if not identical), include a "titleSimilarity" field with "similarTo" (the existing title it resembles), "reason" (one-sentence explanation), and "alternateTitle" (a different 15-char-max title that avoids the similarity).
"""
        } else ""

        val systemInstruction = """
You are a reading teacher's assistant. Create a SINGLE new educational set from these CVC words.

CRITICAL CONSTRAINT — The set MUST contain AT LEAST 3 words. Sets with fewer than 3 words are INVALID and will be rejected.

YOUR PRIMARY GOAL: The teacher originally asked for "$prompt". The new set MUST be tailored to this request. Re-read the teacher's instruction carefully and create a set that directly addresses what the teacher is looking for — the words you choose and how you group them should serve the teacher's intent.

SECONDARY CONSTRAINT: Avoid duplicating the previous set:
- Previous set title: "$oldSetTitle"
- Previous set description: "$oldSetDescription"
Choose a different subset of words or a different angle on the teacher's request, but ALWAYS stay aligned with the original instruction.

GROUPING STRATEGIES (pick the one that best matches the teacher's request):
1. Word Family Sets: Group by same rime (cat/bat/hat = "-at" family)
2. Vowel Sound Sets: Group by same middle vowel
3. Onset Consonant Sets: Group by same starting letter
4. Theme-Based Sets: Group by meaning if the teacher suggests a theme

RULES:
1. The set MUST have at least 3 words — this is mandatory and non-negotiable
2. ONLY use words from the provided list
3. Every word must belong to the set's theme
4. Maximum 10 words
5. Set title: max 15 characters. Be creative and unique — don't reuse common patterns like "Short X Words" or "X Family". Examples: "-at Rhyme Time", "Cat Bat Hat!", "Vowel 'e' Mix", "B Words Go!". Do NOT include the teacher's full request in the title.
6. Return ONLY ONE set
$existingSetTitlesSection
Respond with a JSON object containing:
- "sets": array with exactly ONE object having "title", "description", "words" (minimum 3), and optionally "titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings) fields
        """.trimIndent()

        val userPrompt = """
Teacher's original request: $prompt

I want a new set that is tailored to this request. Pick words and a grouping strategy that best serve what the teacher is asking for.

Available words: ${filteredWords.joinToString(", ")}

WORD ANALYSIS:
$analysisTable

DETECTED PATTERNS:
$patternsSummary

Create one new set aligned with the teacher's request (but different from "$oldSetTitle").
        """.trimIndent()

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 2 (regen)")

        Log.d(TAG, "Step 2 (regen) response: $json")
        return gson.fromJson(json, GroupedSetsResponse::class.java)
    }

    /**
     * Step 3: Assign configuration types and letter indices to each word.
     */
    private suspend fun stepAssignConfigurations(
        groupedSets: GroupedSetsResponse,
        availableWords: List<Word>,
        analyses: List<CVCAnalysis>,
        existingActivityTitles: List<String> = emptyList(),
        existingSetTitles: List<String> = emptyList()
    ): AiGenerationResult {
        val wordsWithImages = availableWords.filter { !it.imagePath.isNullOrBlank() }.map { it.word }
        val wordsWithoutImages = availableWords.filter { it.imagePath.isNullOrBlank() }.map { it.word }
        val setsDescription = groupedSets.sets.joinToString("\n") { set ->
            "Set \"${set.title}\" (${set.description}): ${set.words.joinToString(", ")}"
        }

        val existingTitlesSection = buildString {
            if (existingActivityTitles.isNotEmpty()) {
                appendLine()
                appendLine("EXISTING ACTIVITY TITLES (avoid generating an activity title semantically similar to these):")
                existingActivityTitles.forEach { appendLine("- $it") }
            }
            if (existingSetTitles.isNotEmpty()) {
                appendLine()
                appendLine("EXISTING SET TITLES (avoid generating set titles semantically similar to these):")
                existingSetTitles.forEach { appendLine("- $it") }
            }
            if (existingActivityTitles.isNotEmpty() || existingSetTitles.isNotEmpty()) {
                appendLine()
                appendLine("CRITICAL: Neither the activity title nor any set title may be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.")
                appendLine()
                appendLine("If a generated title (activity or set) is semantically similar to an existing one (even if not identical), include a \"titleSimilarity\" field in that object with \"similarTo\" (the existing title), \"reason\" (one-sentence explanation), and \"alternateTitle\" (a different 15-char-max title that avoids the similarity).")
            }
        }

        val systemInstruction = """
You are a reading teacher's assistant. Assign configuration types and letter indices to each word in each set.

CRITICAL CONSTRAINT — EVERY set MUST keep AT LEAST 3 words. Do NOT remove words from sets. Include ALL words from each set in your output with their configurations.

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
1. EVERY set MUST contain at least 3 words — include ALL words from the input sets
2. For "fill in the blanks": selectedLetterIndex must be 0, 1, or 2
3. For "name the picture" and "write the word": always use selectedLetterIndex: 0
4. NEVER use "name the picture" for words without images
5. Activity title: STRICTLY max 15 characters (count carefully!). Be creative — derive the title from the phonics pattern but vary your naming style. Examples: "-at Blast!", "A Sound Fun", "B Word Buzz", "Rhyme & Write". Do NOT use generic titles like "CVC Practice" or "Word Practice".
6. Set titles: max 15 characters. Keep the set titles from the input unless they exceed the limit or are too generic.
$existingTitlesSection
Respond with a JSON object containing:
- "activity": object with "title" (string, STRICTLY max 15 chars), and optionally "titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings)
- "sets": array of objects, each with:
  - "title" (string)
  - "description" (string)
  - "titleSimilarity" (optional object with "similarTo", "reason", and "alternateTitle" strings)
  - "words": array of objects with "word" (string), "configurationType" (string), "selectedLetterIndex" (integer) — minimum 3 words per set
        """.trimIndent()

        val wordAnalysis = analyses.joinToString("\n") { a ->
            "- ${a.word}: onset='${a.onset}', vowel='${a.vowel}', coda='${a.coda}', rime='-${a.rime}', has image: ${if (a.hasImage) "yes" else "no"}"
        }

        val userPrompt = """
Here are the grouped sets to configure:
$setsDescription

WORD ANALYSIS:
$wordAnalysis

Assign appropriate configuration types and letter indices to each word based on its set's theme and the word's properties.
        """.trimIndent()

        val model = createModel(systemInstruction)
        val response = model.generateContent(userPrompt)
        val json = response.text ?: throw Exception("Empty response from Step 3")

        Log.d(TAG, "Step 3 response: $json")
        val aiResponse = gson.fromJson(json, AiResponse::class.java)
        return parseStep3Response(aiResponse, availableWords, existingActivityTitles, existingSetTitles)
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
     * Tries alternateTitle first, then appends suffixes. Respects 15-character max limit.
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
        if (alternateTitle.isNotBlank() && alternateTitle.length <= 15 &&
            alternateTitle.lowercase() !in allTaken
        ) {
            usedTitles.add(alternateTitle)
            return alternateTitle
        }

        // Suffix fallback — truncate at word boundary to avoid broken words
        val suffixes = listOf(" II", " III", " IV", " V")
        for (suffix in suffixes) {
            val base = if (title.length + suffix.length <= 15) {
                title
            } else {
                val maxBase = 15 - suffix.length
                val truncated = title.take(maxBase)
                val lastSpace = truncated.lastIndexOf(' ')
                if (lastSpace > 3) truncated.substring(0, lastSpace).trimEnd() else truncated.trimEnd()
            }
            val candidate = base + suffix
            if (candidate.length <= 15 && candidate.lowercase() !in allTaken) {
                usedTitles.add(candidate)
                return candidate
            }
        }

        // Last resort: truncate at word boundary and add number
        val num = existingTitles.size + usedTitles.size + 1
        val numSuffix = " $num"
        val maxBase = 15 - numSuffix.length
        val truncated = title.take(maxBase)
        val lastSpace = truncated.lastIndexOf(' ')
        val base = if (lastSpace > 3) truncated.substring(0, lastSpace).trimEnd() else truncated.trimEnd()
        val candidate = (base + numSuffix).take(15)
        usedTitles.add(candidate)
        return candidate
    }

    /**
     * Parse and validate Step 3 response into AiGenerationResult.
     */
    private fun parseStep3Response(
        response: AiResponse,
        availableWords: List<Word>,
        existingActivityTitles: List<String> = emptyList(),
        existingSetTitles: List<String> = emptyList()
    ): AiGenerationResult {
        val wordMap = availableWords.associateBy { it.word }

        // Dedup tracking sets
        val usedActivityTitles = mutableSetOf<String>()
        val usedSetTitles = mutableSetOf<String>()

        // Auto-correct activity title (enforce 15-char limit)
        val activityTitle = when {
            response.activity.title.isBlank() -> "CVC Practice"
            response.activity.title.length > 15 -> {
                val truncated = response.activity.title.take(15)
                val lastSpace = truncated.lastIndexOf(' ')
                if (lastSpace > 5) truncated.substring(0, lastSpace).trimEnd() else truncated.trimEnd()
            }
            else -> response.activity.title
        }

        // Dedup activity title against existing activity titles
        val dedupedActivityTitle = ensureUniqueTitle(
            activityTitle,
            response.activity.titleSimilarity?.alternateTitle ?: "",
            existingActivityTitles.toSet(),
            usedActivityTitles
        )

        // Extract activity-level title similarity
        val activityTitleSimilarity = response.activity.titleSimilarity?.let { sim ->
            if (sim.similarTo.isNotBlank()) {
                TitleSimilarity(
                    generatedTitle = dedupedActivityTitle,
                    similarToExisting = sim.similarTo,
                    reason = sim.reason,
                    alternateTitle = sim.alternateTitle
                )
            } else null
        }

        if (response.sets.isEmpty()) {
            return AiGenerationResult.Error("No sets in response", canRetry = true)
        }

        val validatedSets = mutableListOf<AiGeneratedSet>()
        val orphanWords = mutableListOf<AiWordConfig>()

        for ((index, set) in response.sets.withIndex()) {
            // Auto-correct set title (truncate at word boundary)
            val setTitle = when {
                set.title.isBlank() -> "Set ${index + 1}"
                set.title.length > 15 -> {
                    val truncated = set.title.take(15)
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

            // If set has < 3 valid words, collect them as orphans to merge later
            if (validatedWords.size < 3) {
                Log.w(TAG, "Set '${dedupedSetTitle}' has only ${validatedWords.size} valid words, merging into another set")
                orphanWords.addAll(validatedWords)
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
                    words = validatedWords,
                    titleSimilarity = setTitleSimilarity
                )
            )
        }

        // Merge orphan words into the last valid set
        if (orphanWords.isNotEmpty() && validatedSets.isNotEmpty()) {
            val lastSet = validatedSets.last()
            val existingWordNames = lastSet.words.map { it.word }.toSet()
            val uniqueOrphans = orphanWords.filter { it.word !in existingWordNames }
            if (uniqueOrphans.isNotEmpty()) {
                validatedSets[validatedSets.lastIndex] = lastSet.copy(
                    words = lastSet.words + uniqueOrphans
                )
            }
        } else if (validatedSets.isEmpty() && orphanWords.size >= 3) {
            // All sets were undersized but we have enough orphan words for one set
            validatedSets.add(
                AiGeneratedSet(
                    title = "CVC Practice",
                    description = "CVC word practice",
                    words = orphanWords
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
                    title = dedupedActivityTitle,
                    description = "",
                    titleSimilarity = activityTitleSimilarity
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
            set.copy(words = validWords)
        }

        // Separate into valid sets (>= 3 words) and undersized sets
        val validSets = cleanedSets.filter { it.words.size >= 3 }.toMutableList()
        val orphanWords = cleanedSets.filter { it.words.size < 3 }.flatMap { it.words }

        if (orphanWords.isNotEmpty() && validSets.isNotEmpty()) {
            // Merge orphan words into the last valid set
            val lastSet = validSets.last()
            validSets[validSets.lastIndex] = lastSet.copy(words = lastSet.words + orphanWords)
        } else if (validSets.isEmpty()) {
            // All sets were undersized — combine all words into a single set
            val allWords = cleanedSets.flatMap { it.words }
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
     * Algorithmic grouping using CVC patterns when Step 2 fails.
     */
    private fun algorithmicGrouping(
        analyses: List<CVCAnalysis>,
        patterns: CVCPatterns
    ): GroupedSetsResponse {
        val sets = mutableListOf<SetGroupingResponse>()
        val used = mutableSetOf<String>()

        // Priority 1: Word families
        for ((rime, words) in patterns.wordFamilies) {
            val groupWords = words.filter { it.word !in used }.take(10)
            if (groupWords.size >= 3) {
                sets.add(
                    SetGroupingResponse(
                        title = "-$rime Family",
                        description = "Words ending in -$rime",
                        words = groupWords.map { it.word }
                    )
                )
                used.addAll(groupWords.map { it.word })
            }
        }

        // Priority 2: Same vowel groups (only if no word family sets found)
        if (sets.isEmpty()) {
            for ((vowel, words) in patterns.sameVowel) {
                val groupWords = words.filter { it.word !in used }.take(10)
                if (groupWords.size >= 3) {
                    sets.add(
                        SetGroupingResponse(
                            title = "Short '$vowel' Words",
                            description = "Words with the '$vowel' vowel sound",
                            words = groupWords.map { it.word }
                        )
                    )
                    used.addAll(groupWords.map { it.word })
                }
            }
        }

        // Remaining words in a mixed set
        val remaining = analyses.filter { it.word !in used }
        if (remaining.size >= 3) {
            sets.add(
                SetGroupingResponse(
                    title = "CVC Practice",
                    description = "Mixed CVC word practice",
                    words = remaining.map { it.word }
                )
            )
        }

        // Ultimate fallback: everything in one set
        if (sets.isEmpty()) {
            sets.add(
                SetGroupingResponse(
                    title = "CVC Practice",
                    description = "CVC word practice",
                    words = analyses.map { it.word }
                )
            )
        }

        return GroupedSetsResponse(sets = sets)
    }

    /**
     * Default configuration fallback when Step 3 fails.
     * Assigns "write the word" to all words.
     */
    private fun defaultConfigurations(
        groupedSets: GroupedSetsResponse,
        availableWords: List<Word>
    ): AiGenerationResult {
        val sets = groupedSets.sets.map { set ->
            AiGeneratedSet(
                title = set.title,
                description = set.description,
                words = set.words.map { word ->
                    AiWordConfig(
                        word = word,
                        configurationType = "write the word",
                        selectedLetterIndex = 0
                    )
                }
            )
        }

        val activityTitle = if (sets.size == 1) sets[0].title.take(15) else "CVC Practice"

        return AiGenerationResult.Success(
            AiGeneratedActivity(
                activity = AiActivityInfo(
                    title = activityTitle,
                    description = ""
                ),
                sets = sets
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
        val vowels = setOf('a', 'e', 'i', 'o', 'u')
        return words.mapNotNull { w ->
            val lower = w.word.lowercase()
            if (lower.length == 3 && !vowels.contains(lower[0]) && vowels.contains(lower[1]) && !vowels.contains(lower[2])) {
                CVCAnalysis(
                    word = w.word,
                    onset = lower[0],
                    vowel = lower[1],
                    coda = lower[2],
                    rime = "${lower[1]}${lower[2]}",
                    hasImage = !w.imagePath.isNullOrBlank()
                )
            } else {
                // Non-CVC word - still include with basic decomposition
                CVCAnalysis(
                    word = w.word,
                    onset = lower.getOrElse(0) { ' ' },
                    vowel = lower.getOrElse(1) { ' ' },
                    coda = lower.getOrElse(2) { ' ' },
                    rime = lower.drop(1),
                    hasImage = !w.imagePath.isNullOrBlank()
                )
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
