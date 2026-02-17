package com.example.app.data.service

import com.example.app.data.model.OpenRouterRequest
import com.example.app.data.model.OpenRouterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service interface for OpenRouter API.
 */
interface GeminiService {

    /**
     * Generate content using OpenRouter API with nvidia/nemotron-nano-9b-v2:free model.
     *
     * @param authHeader Authorization header with API key (Bearer token)
     * @param request The generation request with messages and configuration
     * @return Response containing generated content or error
     */
    @POST("api/v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authHeader: String,
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>
}
