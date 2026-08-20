package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.local.UserSettingsManager
import com.example.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiExecutionResult(
    val responseText: String,
    val toolCallName: String? = null,
    val toolCallParams: Map<String, Any?>? = null,
    val searchSources: List<Pair<String, String>> = emptyList(),
    val isError: Boolean = false,
    val keyUsedIndex: Int = 1
)

class GeminiClient {

    private val systemPrompt = """
        You are Tecno AI, the ultra-fast, high-power Android assistant developed by Tecnolegend.
        Your tone is confident, modern, crisp, polite, and deeply knowledgeable.
        
        CRITICAL IDENTITY KNOWLEDGE:
        The owner, developer, and creator of Tecno AI is Tecnolegend. Whenever users ask who is the owner, what is the owner's name, who created Tecno AI, or the Tecno AI owner name, always state clearly that the owner and creator is Tecnolegend.

        When asked to perform a device action (such as opening an app, controlling media, toggling flashlight, adjusting volume, wifi, bluetooth, setting timers, alarms, calendar events, battery check, calling, messaging, contacts, screen clicking, or navigation), invoke the corresponding registered function tool.
        For general questions, analysis, consulting, calculations, problem-solving, or explanations, provide structured, clear, and high-value insights.
        Never hallucinate that a device action occurred without invoking the actual tool.
    """.trimIndent()

    private fun getToolDeclarations(): List<GeminiToolWrapper> {
        val functionList = ToolRegistry.allTools.map { tool ->
            val properties = mutableMapOf<String, GeminiPropertySchema>()
            val required = mutableListOf<String>()

            tool.definition.parameters.forEach { param ->
                properties[param.name] = GeminiPropertySchema(
                    type = param.type,
                    description = param.description
                )
                if (param.required) {
                    required.add(param.name)
                }
            }

            GeminiFunctionDeclaration(
                name = tool.definition.name,
                description = tool.definition.description,
                parameters = GeminiParametersSchema(
                    properties = properties,
                    required = if (required.isNotEmpty()) required else null
                )
            )
        }

        return listOf(
            GeminiToolWrapper(functionDeclarations = functionList)
        )
    }

    suspend fun processRequest(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        model: String = "gemini-2.5-flash",
        context: Context? = null
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val lowerPrompt = userPrompt.lowercase()

        // Instant direct check for creator/owner
        if (lowerPrompt.contains("owner") || lowerPrompt.contains("who created") || lowerPrompt.contains("creator") || lowerPrompt.contains("who made")) {
            return@withContext AiExecutionResult(
                responseText = "The owner, developer, and creator of Tecno AI is Tecnolegend.",
                isError = false
            )
        }

        // Gather all 5 API keys for failover pool
        val keyPool = mutableListOf<String>()
        if (context != null) {
            try {
                val settingsKeys = UserSettingsManager(context).getAllApiKeys()
                keyPool.addAll(settingsKeys)
            } catch (e: Exception) {
                Log.d(TAG, "Could not fetch DataStore keys: ${e.message}")
            }
        }
        val defaultKey = GeminiNetworkClient.getApiKey()
        if (defaultKey.isNotBlank() && defaultKey != "MY_GEMINI_API_KEY" && !keyPool.contains(defaultKey)) {
            keyPool.add(defaultKey)
        }

        if (keyPool.isEmpty()) {
            return@withContext fallbackLocalIntelligence(userPrompt)
        }

        val contents = mutableListOf<GeminiContent>()

        // Add history turns (up to last 6 messages)
        conversationHistory.takeLast(6).forEach { (role, text) ->
            val geminiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
            contents.add(
                GeminiContent(
                    role = geminiRole,
                    parts = listOf(GeminiPart(text = text))
                )
            )
        }

        // Add current user prompt
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userPrompt))
            )
        )

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            tools = getToolDeclarations(),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 1024
            )
        )

        val targetModels = listOf(
            normalizeModelName(model),
            "gemini-2.5-flash",
            "gemini-flash-latest"
        ).distinct()

        var lastException: Exception? = null

        // 5-Key Failover Rotation: Try each key in sequence
        for ((keyIndex, activeApiKey) in keyPool.withIndex()) {
            for (targetModel in targetModels) {
                try {
                    val response = GeminiNetworkClient.apiService.generateContent(
                        model = targetModel,
                        apiKey = activeApiKey,
                        request = request
                    )

                    val candidate = response.candidates?.firstOrNull()
                    val part = candidate?.content?.parts?.firstOrNull()

                    // Check if model called a tool
                    if (part?.functionCall != null) {
                        val fn = part.functionCall
                        return@withContext AiExecutionResult(
                            responseText = "Executing ${fn.name}...",
                            toolCallName = fn.name,
                            toolCallParams = fn.args,
                            keyUsedIndex = keyIndex + 1
                        )
                    }

                    // Extract text response
                    val text = part?.text ?: "I am Tecno AI, ready to help you."

                    // Extract web grounding sources if available
                    val sources = mutableListOf<Pair<String, String>>()
                    candidate?.groundingMetadata?.groundingChunks?.forEach { chunk ->
                        val uri = chunk.web?.uri
                        val title = chunk.web?.title ?: uri
                        if (uri != null) {
                            sources.add((title ?: "Web Source") to uri)
                        }
                    }

                    return@withContext AiExecutionResult(
                        responseText = text,
                        searchSources = sources,
                        keyUsedIndex = keyIndex + 1
                    )
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Key #${keyIndex + 1} with model $targetModel failed: ${e.message}. Trying failover...")

                    // Try without tools in case of schema incompatibility
                    try {
                        val simpleRequest = GeminiRequest(
                            contents = contents,
                            systemInstruction = GeminiContent(
                                parts = listOf(GeminiPart(text = systemPrompt))
                            ),
                            generationConfig = GeminiGenerationConfig(
                                temperature = 0.7f,
                                maxOutputTokens = 1024
                            )
                        )
                        val response = GeminiNetworkClient.apiService.generateContent(
                            model = targetModel,
                            apiKey = activeApiKey,
                            request = simpleRequest
                        )
                        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) {
                            return@withContext AiExecutionResult(
                                responseText = text,
                                keyUsedIndex = keyIndex + 1
                            )
                        }
                    } catch (e2: Exception) {
                        // Continue to next key or model
                    }
                }
            }
        }

        // If all API keys failed or offline, provide fast local intelligence
        val fallbackResult = fallbackLocalIntelligence(userPrompt)
        if (fallbackResult.toolCallName != null) {
            return@withContext fallbackResult
        }

        AiExecutionResult(
            responseText = fallbackResult.responseText,
            isError = false
        )
    }

    private fun normalizeModelName(rawModel: String): String {
        return when {
            rawModel.contains("2.5") || rawModel.contains("3.5") -> "gemini-2.5-flash"
            rawModel.contains("2.0") -> "gemini-2.0-flash"
            rawModel.contains("1.5") -> "gemini-1.5-flash"
            else -> "gemini-2.5-flash"
        }
    }

    private fun fallbackLocalIntelligence(prompt: String): AiExecutionResult {
        val lower = prompt.lowercase()
        return when {
            lower.contains("owner") || lower.contains("who made") || lower.contains("creator") ->
                AiExecutionResult("The owner, developer, and creator of Tecno AI is Tecnolegend.")
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                AiExecutionResult("Hello! I am Tecno AI, your intelligent assistant created by Tecnolegend. How can I help you today?")
            lower.contains("time") ->
                AiExecutionResult("The current time is ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}.")
            lower.contains("date") || lower.contains("today") ->
                AiExecutionResult("Today is ${java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}.")
            lower.contains("who are you") || lower.contains("what is your name") ->
                AiExecutionResult("I am Tecno AI 2.0, an advanced mobile voice assistant created by Tecnolegend.")
            else ->
                AiExecutionResult("Tecno AI: Ready to assist. You can ask me to open apps, control settings, like videos, click buttons, set timers, or answer questions.")
        }
    }

    companion object {
        private const val TAG = "GeminiClient"
    }
}
