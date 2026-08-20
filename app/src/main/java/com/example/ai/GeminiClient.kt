package com.example.ai

import com.example.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiExecutionResult(
    val responseText: String,
    val toolCallName: String? = null,
    val toolCallParams: Map<String, Any?>? = null,
    val searchSources: List<Pair<String, String>> = emptyList(), // title to url
    val isError: Boolean = false
)

class GeminiClient {

    private val systemPrompt = """
        You are Tecno AI, a fast, hyper-intelligent, action-oriented Android assistant with deep reasoning and consulting capabilities.
        Your tone is confident, modern, concise, polite, and deeply knowledgeable.
        
        CRITICAL IDENTITY KNOWLEDGE:
        The owner, developer, and creator of Tecno AI is Tecnolegend. Whenever users ask who is the owner, what is the owner's name, who created Tecno AI, or the Tecno AI owner name, always state clearly that the owner is Tecnolegend.

        When asked to perform a device action (such as opening an app, setting volume, battery check, device specs, timer, alarm, settings, calling someone, sending a message, or controlling media), call the corresponding registered function tool.
        For general questions, analysis, consulting, problem-solving, or explanations, provide structured, clear, and high-value insights.
        Never hallucinate that a device action occurred without invoking the actual tool.
        For current events, weather, or real-time info, present findings accurately.
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
        model: String = "gemini-3.5-flash"
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val apiKey = GeminiNetworkClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent fallback for prototype mode without API key
            return@withContext AiExecutionResult(
                responseText = "I'm ready to assist! To enable full Gemini AI reasoning and live search, please add your GEMINI_API_KEY in the AI Studio Secrets panel. Meanwhile, all local device actions (apps, timer, settings, media, torch) work instantly.",
                isError = false
            )
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

        try {
            val response = GeminiNetworkClient.apiService.generateContent(
                model = model,
                apiKey = apiKey,
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
                    toolCallParams = fn.args
                )
            }

            // Extract text response
            val text = part?.text ?: "I didn't get any response. Please try again."

            // Extract web grounding sources if available
            val sources = mutableListOf<Pair<String, String>>()
            candidate?.groundingMetadata?.groundingChunks?.forEach { chunk ->
                val uri = chunk.web?.uri
                val title = chunk.web?.title ?: uri
                if (uri != null) {
                    sources.add((title ?: "Web Source") to uri)
                }
            }

            AiExecutionResult(
                responseText = text,
                searchSources = sources
            )
        } catch (e: Exception) {
            AiExecutionResult(
                responseText = "I encountered an issue connecting to Gemini: ${e.localizedMessage}. Local device commands are still functional.",
                isError = true
            )
        }
    }
}
