package com.example.tools

import android.content.Context
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ToolParameter(
    val name: String,
    val type: String, // "STRING", "INTEGER", "BOOLEAN"
    val description: String,
    val required: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val requiresConfirmation: Boolean = false,
    val requiredPermission: String? = null
)

@JsonClass(generateAdapter = true)
data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, String>? = null,
    val errorCode: String? = null,
    val isConfirmationPending: Boolean = false,
    val pendingActionDescription: String? = null
)

interface DeviceTool {
    val definition: ToolDefinition
    suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult
}
