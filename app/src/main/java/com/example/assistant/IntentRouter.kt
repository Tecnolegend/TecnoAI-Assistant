package com.example.assistant

import com.example.tools.ToolDispatcher
import com.example.tools.ToolResult
import java.util.regex.Pattern

enum class DecisionType {
    LOCAL_ACTION,
    AI_QUESTION,
    WEB_SEARCH,
    SENSITIVE_ACTION,
    CLARIFICATION_REQUIRED,
    UNSUPPORTED
}

data class AssistantDecision(
    val type: DecisionType,
    val toolName: String? = null,
    val parameters: Map<String, Any?> = emptyMap(),
    val directAnswer: String? = null,
    val requiresConfirmation: Boolean = false,
    val queryForAi: String? = null
)

class IntentRouter {

    fun route(userInput: String): AssistantDecision {
        val text = userInput.trim()
        val lower = text.lowercase()

        // Clean wake word if present in text
        val cleaned = lower
            .removePrefix("hey tecno")
            .removePrefix("ok tecno")
            .removePrefix("tecno")
            .removePrefix(",")
            .trim()

        if (cleaned.isEmpty()) {
            return AssistantDecision(
                type = DecisionType.CLARIFICATION_REQUIRED,
                directAnswer = "I'm listening. How can I help you?"
            )
        }

        // 1. Open App Pattern: "open youtube", "launch chrome", "start spotify", etc.
        val openAppRegex = Pattern.compile("^(?:open|launch|start|run|play)\\s+([a-zA-Z0-9\\s\\._-]+)$", Pattern.CASE_INSENSITIVE)
        val openAppMatcher = openAppRegex.matcher(cleaned)
        if (openAppMatcher.find()) {
            val target = openAppMatcher.group(1)?.trim() ?: ""
            // Check if it's settings specifically
            if (target.contains("settings")) {
                val dest = target.replace("settings", "").trim()
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "open_settings",
                    parameters = mapOf("destination" to if (dest.isNotEmpty()) dest else "general")
                )
            }
            if (target.contains(".com") || target.contains(".org") || target.contains(".io") || target.contains(".net") || target.startsWith("http")) {
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "open_url",
                    parameters = mapOf("url" to target)
                )
            }
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "open_app",
                parameters = mapOf("app_name" to target)
            )
        }

        // 2. Open URL Pattern: "go to reddit.com", "open website google.com"
        val openUrlRegex = Pattern.compile("^(?:go to|browse|visit|open website|open link)\\s+(https?://\\S+|[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", Pattern.CASE_INSENSITIVE)
        val urlMatcher = openUrlRegex.matcher(cleaned)
        if (urlMatcher.find()) {
            val url = urlMatcher.group(1) ?: ""
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "open_url",
                parameters = mapOf("url" to url)
            )
        }

        // 3. Settings Patterns: "open wifi settings", "turn on bluetooth", "wifi settings"
        if (cleaned.startsWith("open ") && cleaned.contains("setting")) {
            val dest = cleaned.removePrefix("open").replace("settings", "").replace("setting", "").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "open_settings",
                parameters = mapOf("destination" to if (dest.isNotEmpty()) dest else "general")
            )
        }

        // 4. Timer Pattern: "set a 10 minute timer", "timer 5 minutes", "set timer for 30 seconds"
        val timerRegex = Pattern.compile("(?:set\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(\\d+)\\s*(min|minute|minutes|sec|second|seconds|hour|hours)?(?:\\s+(?:for|labeled|called)\\s+(.+))?", Pattern.CASE_INSENSITIVE)
        val timerMatcher = timerRegex.matcher(cleaned)
        if (timerMatcher.find()) {
            val num = timerMatcher.group(1)?.toIntOrNull() ?: 60
            val unit = timerMatcher.group(2)?.lowercase() ?: "minute"
            val label = timerMatcher.group(3)?.trim() ?: "Timer"
            val durationSeconds = when {
                unit.startsWith("hour") -> num * 3600
                unit.startsWith("sec") -> num
                else -> num * 60 // minutes
            }
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "set_timer",
                parameters = mapOf("duration_seconds" to durationSeconds, "label" to label)
            )
        }

        // 5. Alarm Pattern: "set an alarm for 7:30 am", "set alarm for 8 am", "alarm at 6:00"
        val alarmRegex = Pattern.compile("(?:set\\s+(?:an?\\s+)?)?alarm\\s+(?:for|at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?(?:\\s+(?:for|labeled)\\s+(.+))?", Pattern.CASE_INSENSITIVE)
        val alarmMatcher = alarmRegex.matcher(cleaned)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
            val minutes = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = alarmMatcher.group(3)?.lowercase()
            val label = alarmMatcher.group(4)?.trim() ?: "Alarm"
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "set_alarm",
                parameters = mapOf("hour" to hour, "minutes" to minutes, "label" to label)
            )
        }

        // 6. Flashlight Pattern: "turn on flashlight", "flashlight off", "torch on"
        if (cleaned.contains("flashlight") || cleaned.contains("torch")) {
            val turnOn = !cleaned.contains("off") && !cleaned.contains("disable")
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "toggle_flashlight",
                parameters = mapOf("enabled" to turnOn)
            )
        }

        // 7. Call Contact Pattern: "call mom", "dial 911", "call 1234567890"
        val callRegex = Pattern.compile("^(?:call|dial|phone)\\s+([a-zA-Z0-9\\s+]+)$", Pattern.CASE_INSENSITIVE)
        val callMatcher = callRegex.matcher(cleaned)
        if (callMatcher.find()) {
            val contact = callMatcher.group(1)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.SENSITIVE_ACTION,
                toolName = "call_contact",
                parameters = mapOf("contact_name_or_number" to contact),
                requiresConfirmation = true
            )
        }

        // 8. SMS / Message Pattern: "send message to Alex: I'm coming", "text mom hello"
        val msgRegex = Pattern.compile("^(?:send\\s+(?:a\\s+)?(?:message|text|sms)\\s+to|text|message)\\s+([a-zA-Z0-9\\s+]+?)(?:\\s*[:;]\\s*|\\s+saying\\s+|\\s+that\\s+|\\s+)(.+)$", Pattern.CASE_INSENSITIVE)
        val msgMatcher = msgRegex.matcher(cleaned)
        if (msgMatcher.find()) {
            val contact = msgMatcher.group(1)?.trim() ?: ""
            val body = msgMatcher.group(2)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.SENSITIVE_ACTION,
                toolName = "prepare_message",
                parameters = mapOf("contact_name_or_number" to contact, "message" to body),
                requiresConfirmation = true
            )
        }

        // 9. Media playback pattern: "pause music", "resume music", "next song", "play music"
        if (cleaned == "pause music" || cleaned == "pause" || cleaned == "stop music") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "pause")
            )
        }
        if (cleaned == "play music" || cleaned == "resume music" || cleaned == "play song") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "play")
            )
        }
        if (cleaned == "next song" || cleaned == "skip track" || cleaned == "next track") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "next")
            )
        }

        // 10. Copy clipboard: "copy this text: hello world"
        if (cleaned.startsWith("copy to clipboard") || cleaned.startsWith("copy this:")) {
            val textToCopy = cleaned.substringAfter(":").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "copy_to_clipboard",
                parameters = mapOf("text" to textToCopy)
            )
        }

        // 11. Explicit web search requests: "search the web for...", "google ..."
        if (cleaned.startsWith("search the web for ") || cleaned.startsWith("search for ") || cleaned.startsWith("google ")) {
            val query = cleaned
                .removePrefix("search the web for ")
                .removePrefix("search for ")
                .removePrefix("google ")
                .trim()
            return AssistantDecision(
                type = DecisionType.WEB_SEARCH,
                queryForAi = query,
                toolName = "search_web",
                parameters = mapOf("query" to query)
            )
        }

        // 12. Questions needing real-time info or Gemini AI
        val isCurrentInfo = lower.contains("today") || lower.contains("latest") || lower.contains("news") ||
                lower.contains("current") || lower.contains("weather") || lower.contains("price") || lower.contains("score")

        return if (isCurrentInfo) {
            AssistantDecision(
                type = DecisionType.WEB_SEARCH,
                queryForAi = text
            )
        } else {
            AssistantDecision(
                type = DecisionType.AI_QUESTION,
                queryForAi = text
            )
        }
    }
}
