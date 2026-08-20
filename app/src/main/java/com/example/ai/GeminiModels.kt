package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiToolWrapper>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40,
    val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiToolWrapper(
    val functionDeclarations: List<GeminiFunctionDeclaration>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiParametersSchema? = null
)

@JsonClass(generateAdapter = true)
data class GeminiParametersSchema(
    val type: String = "OBJECT",
    val properties: Map<String, GeminiPropertySchema>,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPropertySchema(
    val type: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: GeminiUsageMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val searchEntryPoint: GeminiSearchEntryPoint? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSearchEntryPoint(
    val renderedContent: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
    val web: GeminiWebChunk? = null
)

@JsonClass(generateAdapter = true)
data class GeminiWebChunk(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)

