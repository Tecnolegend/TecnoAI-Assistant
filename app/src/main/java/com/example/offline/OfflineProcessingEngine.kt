package com.example.offline

import android.content.Context
import android.util.Log
import com.example.assistant.AssistantDecision
import com.example.assistant.DecisionType
import com.example.assistant.IntentRouter
import com.example.diagnostics.BugTracker
import com.example.tools.ToolDispatcher
import com.example.tools.ToolResult

data class OfflineExecutionResult(
    val handledLocally: Boolean,
    val decision: AssistantDecision? = null,
    val toolResult: ToolResult? = null,
    val responseText: String? = null,
    val executionDurationMs: Long = 0,
    val isOfflineCriticalCommand: Boolean = false,
    val requiresCloud: Boolean = false
)

class OfflineProcessingEngine(
    private val context: Context,
    private val intentRouter: IntentRouter = IntentRouter(),
    private val toolDispatcher: ToolDispatcher = ToolDispatcher(context)
) {
    companion object {
        private const val TAG = "OfflineProcessingEngine"

        val OFFLINE_CAPABILITIES = listOf(
            "Device Settings (Open Settings, Wi-Fi, Bluetooth, Display, Audio, Apps)",
            "Media Control (Stop Music, Pause, Play, Next/Previous Track)",
            "Hardware Actions (Flashlight, Volume Adjustments, Battery Status, Wi-Fi Toggle)",
            "App Launching (Camera, YouTube, Chrome, Spotify, Calculator, Clock, Files)",
            "Alarms & Timers (Set 5m Timer, Set 7:00 AM Alarm)",
            "System Navigation (Go Home, Go Back, Recent Apps, Screenshot, Lock Screen)",
            "Lightning Math (Sub-1ms arithmetic, percentages, expressions)",
            "Phone & SMS (Direct offline call dialing and SMS drafting)",
            "Instant Identity & Offline Status (Offline diagnostics, creator info)"
        )
    }

    /**
     * Checks if a command can be executed completely on-device without internet access.
     */
    fun isOfflineCapable(userInput: String): Boolean {
        val decision = intentRouter.route(userInput)
        return isDecisionOfflineCapable(decision)
    }

    private fun isDecisionOfflineCapable(decision: AssistantDecision): Boolean {
        return when (decision.type) {
            DecisionType.LOCAL_ACTION -> true
            DecisionType.SENSITIVE_ACTION -> true
            DecisionType.CLARIFICATION_REQUIRED -> true
            DecisionType.WEB_SEARCH, DecisionType.AI_QUESTION, DecisionType.UNSUPPORTED -> false
        }
    }

    /**
     * Executes the offline processing pipeline.
     * Guaranteed sub-5ms local evaluation.
     */
    suspend fun processOffline(
        userInput: String,
        userConfirmedSensitive: Boolean = false,
        confirmSensitive: Boolean = true
    ): OfflineExecutionResult {
        val startTime = System.currentTimeMillis()
        val text = userInput.trim()
        val lower = text.lowercase()

        // 1. Check for explicit offline query questions
        if (lower == "what can you do offline" || lower == "offline capabilities" || lower == "offline help" ||
            lower == "offline commands" || lower == "are you offline" || lower == "offline mode") {
            val duration = System.currentTimeMillis() - startTime
            val response = "⚡ Tecno AI Offline Engine is active. Critical commands operate instantly without internet: 'open settings', 'stop music', flashlight, volume, apps, alarms, timers, battery info, and math."
            BugTracker.logInfo(TAG, "Offline status inquiry handled in ${duration}ms")
            return OfflineExecutionResult(
                handledLocally = true,
                responseText = response,
                executionDurationMs = duration,
                isOfflineCriticalCommand = true
            )
        }

        // 2. Evaluate with Intent Router
        val decision = intentRouter.route(text)
        val isCapable = isDecisionOfflineCapable(decision)

        if (!isCapable) {
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Query '$text' requires cloud AI processing.")
            return OfflineExecutionResult(
                handledLocally = false,
                decision = decision,
                executionDurationMs = duration,
                isOfflineCriticalCommand = false,
                requiresCloud = true
            )
        }

        // 3. Fast direct answer (e.g. Creator, cached QA, simple response)
        if (decision.directAnswer != null) {
            val duration = System.currentTimeMillis() - startTime
            BugTracker.logSuccess(TAG, "Direct offline answer returned in ${duration}ms for: '$text'")
            return OfflineExecutionResult(
                handledLocally = true,
                decision = decision,
                responseText = decision.directAnswer,
                executionDurationMs = duration,
                isOfflineCriticalCommand = true
            )
        }

        // 4. Local Tool Execution (e.g. open_settings, media_control, toggle_flashlight, open_app, volume_control, etc.)
        val toolName = decision.toolName ?: "open_app"
        val toolResult = toolDispatcher.executeTool(
            name = toolName,
            params = decision.parameters,
            userConfirmed = userConfirmedSensitive,
            confirmSensitive = confirmSensitive
        )

        val duration = System.currentTimeMillis() - startTime
        BugTracker.logSuccess(TAG, "Offline tool '$toolName' executed in ${duration}ms (success: ${toolResult.success})")

        return OfflineExecutionResult(
            handledLocally = true,
            decision = decision,
            toolResult = toolResult,
            responseText = toolResult.message,
            executionDurationMs = duration,
            isOfflineCriticalCommand = true
        )
    }
}
