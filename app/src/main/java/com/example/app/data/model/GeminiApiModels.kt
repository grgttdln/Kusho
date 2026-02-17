package com.example.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request/Response data classes for OpenRouter API calls.
 */

/**
 * Main request body for OpenRouter API (OpenAI-compatible format).
 */
data class OpenRouterRequest(
    val model: String = "nvidia/nemotron-nano-9b-v2:free",
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2048,
    val top_p: Float = 0.95f,
    val response_format: ResponseFormat? = null
)

data class Message(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

/**
 * Response from OpenRouter API (OpenAI-compatible format).
 */
data class OpenRouterResponse(
    val id: String? = null,
    val object_type: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: OpenRouterError? = null
)

data class Choice(
    val index: Int? = null,
    val message: Message? = null,
    val finish_reason: String? = null
)

data class Usage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

data class OpenRouterError(
    val message: String? = null,
    val type: String? = null,
    val param: String? = null,
    val code: String? = null
)

/**
 * Legacy Gemini models (kept for compatibility, not used with OpenRouter).
 */
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val role: String = "user",
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 2048,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null
)

data class SafetyRating(
    val category: String,
    val probability: String
)

data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

data class ResponseSchema(
    val type: String = "object",
    val properties: Map<String, SchemaProperty>,
    val required: List<String>
)

data class SchemaProperty(
    val type: String? = null,
    val description: String? = null,
    val properties: Map<String, SchemaProperty>? = null,
    val items: SchemaProperty? = null,
    val required: List<String>? = null,
    val enum: List<String>? = null
)
