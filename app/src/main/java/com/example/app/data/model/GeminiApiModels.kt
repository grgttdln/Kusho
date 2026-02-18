package com.example.app.data.model

/**
 * Intermediate response models for chained Gemini API calls.
 * Used between the 3 generation steps: Filter -> Group -> Configure.
 */

/**
 * Step 1 output: Filtered word list with reasoning.
 */
data class FilteredWordsResponse(
    val words: List<String> = emptyList(),
    val reasoning: String = ""
)

/**
 * Step 2 output: Grouped sets without configuration types.
 */
data class GroupedSetsResponse(
    val sets: List<SetGroupingResponse> = emptyList()
)

data class SetGroupingResponse(
    val title: String = "",
    val description: String = "",
    val words: List<String> = emptyList(),
    val titleSimilarity: TitleSimilarityResponse? = null
)

/**
 * AI-reported title similarity for Gson parsing.
 */
data class TitleSimilarityResponse(
    val similarTo: String = "",
    val reason: String = "",
    val alternateTitle: String = ""
)
