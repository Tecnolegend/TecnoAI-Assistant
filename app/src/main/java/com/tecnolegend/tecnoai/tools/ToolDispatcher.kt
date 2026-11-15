package com.tecnolegend.tecnoai.tools

import android.content.Context

object ToolRegistry {
    val allTools: List<DeviceTool> = listOf(
        OpenAppTool(),
        YouTubeControlTool(),
        SpotifyControlTool(),
        WhatsAppSendTool(),
        NavigationMapsTool(),
        GlobalGestureTool(),
        CameraModeTool(),
        OpenUrlTool(),
        SearchWebTool(),
        VolumeControlTool(),
        WifiControlTool(),
        LaunchAppActivityTool(),
        BatteryStatusTool(),
        DeviceSpecsTool(),
        CalculateMathTool(),
        OpenSettingsTool(),
        SetTimerTool(),
        SetAlarmTool(),
        CreateCalendarReminderTool(),
        CallContactTool(),
        PrepareMessageTool(),
        MediaControlTool(),
        CopyToClipboardTool(),
        ToggleFlashlightTool(),
        ScreenControlTool(),
        ScreenVisionTool(),
        OptimizeAiTool(),
        WhatsAppSpecializedTool(),
        DiscordControlTool(),
        MessengerControlTool(),
        TikTokControlTool(),
        GoogleEcosystemTool()
    )

    private val toolMap = allTools.associateBy { it.definition.name }

    fun getTool(name: String): DeviceTool? = toolMap[name]
}

class ToolDispatcher(private val context: Context) {
    suspend fun executeTool(
        name: String,
        params: Map<String, Any?>,
        userConfirmed: Boolean = false,
        confirmSensitive: Boolean = true
    ): ToolResult {
        val tool = ToolRegistry.getTool(name) ?: return ToolResult(
            success = false,
            message = "Tool '$name' not recognized.",
            errorCode = "TOOL_NOT_FOUND"
        )

        // Check if sensitive action requires confirmation
        if (tool.definition.requiresConfirmation && confirmSensitive && !userConfirmed) {
            val description = when (name) {
                "call_contact" -> "Call ${params["contact_name_or_number"]}?"
                "prepare_message" -> "Send message to ${params["contact_name_or_number"]}: \"${params["message"]}\"?"
                "whatsapp_send" -> "Send WhatsApp message to ${params["phone_or_contact"]}: \"${params["message"]}\"?"
                "whatsapp_control" -> "Send WhatsApp message to ${params["target_user"]}: \"${params["message"]}\"?"
                "messenger_control" -> "Send Messenger message to ${params["target_user"]}: \"${params["message"]}\"?"
                else -> "Execute ${tool.definition.name}?"
            }
            return ToolResult(
                success = false,
                message = "Confirmation required for sensitive action.",
                isConfirmationPending = true,
                pendingActionDescription = description,
                data = params.mapValues { it.value?.toString() ?: "" }
            )
        }

        return try {
            tool.execute(context, params)
        } catch (e: Exception) {
            ToolResult(
                success = false,
                message = "Execution failed: ${e.localizedMessage}",
                errorCode = "EXECUTION_ERROR"
            )
        }
    }
}
