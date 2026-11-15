package com.tecnolegend.tecnoai.assistant

import com.tecnolegend.tecnoai.tools.ToolDispatcher
import com.tecnolegend.tecnoai.tools.ToolResult
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

    fun parseFastIntent(userInput: String): AssistantDecision? {
        val decision = route(userInput)
        return if (decision.type == DecisionType.LOCAL_ACTION || decision.type == DecisionType.SENSITIVE_ACTION || decision.directAnswer != null) {
            decision
        } else {
            null
        }
    }

    fun route(userInput: String): AssistantDecision {
        val text = userInput.trim()
        val lower = text.lowercase()

        // Clean wake words if present in text
        val cleaned = lower
            .removePrefix("hey tecno")
            .removePrefix("hi tecno")
            .removePrefix("hello tecno")
            .removePrefix("ok tecno")
            .removePrefix("okay tecno")
            .removePrefix("tecno")
            .removePrefix("hey techno")
            .removePrefix("hi techno")
            .removePrefix("techno")
            .removePrefix(",")
            .trim()

        if (cleaned.isEmpty()) {
            return AssistantDecision(
                type = DecisionType.CLARIFICATION_REQUIRED,
                directAnswer = "I'm listening. How can I help you?"
            )
        }

        // 0. OWNER & CREATOR IDENTIFICATION (Tecnolegend)
        if (cleaned.contains("owner") || cleaned.contains("creator") || cleaned.contains("who made you") ||
            cleaned.contains("who built you") || cleaned.contains("who owns") || cleaned.contains("who is your developer") ||
            cleaned.contains("who created tecno") || cleaned.contains("who made tecno")) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                directAnswer = "The owner, developer, and creator of Tecno AI is Tecnolegend."
            )
        }

        // 0.1 FAST PRE-BUILT WORDS & CONVERSATIONAL CACHE (Sub-5ms Ultra Low Latency Responses)
        val instantAnswer = matchPrebuiltConversationalPhrase(cleaned)
        if (instantAnswer != null) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                directAnswer = instantAnswer
            )
        }

        // 0.2 OPTIMIZE AI & SYSTEM ENGINE
        if (cleaned == "optimize" || cleaned == "optimize ai" || cleaned == "optimize the ai" ||
            cleaned.contains("optimize phone") || cleaned.contains("optimize device") ||
            cleaned.contains("boost ai") || cleaned.contains("speed up") || cleaned.contains("clean memory")) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "optimize_ai",
                parameters = emptyMap()
            )
        }

        // 0.3 SCREEN SEEING & VISION: "read screen", "what's on my screen", "see screen", "summarize screen"
        if (cleaned == "read screen" || cleaned == "see screen" || cleaned == "what is on my screen" ||
            cleaned == "what's on my screen" || cleaned == "scan screen" || cleaned == "summarize screen" ||
            cleaned == "read current screen") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "screen_vision",
                parameters = emptyMap()
            )
        }

        // 0.4 YOUTUBE INTERACTIVE ACTIONS (Like video, Subscribe, Skip Ad)
        if (cleaned == "like" || cleaned == "like video" || cleaned == "like this video" ||
            cleaned == "hit like" || cleaned == "thumbs up" || cleaned == "like this") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "screen_control",
                parameters = mapOf("action" to "like_video")
            )
        }
        if (cleaned == "subscribe" || cleaned == "subscribe to channel" || cleaned == "hit subscribe" ||
            cleaned == "subscribe channel") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "screen_control",
                parameters = mapOf("action" to "subscribe")
            )
        }
        if (cleaned == "skip ad" || cleaned == "skip this ad" || cleaned == "skip ads" || cleaned == "skip") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "screen_control",
                parameters = mapOf("action" to "skip_ad")
            )
        }

        // 0.5 SCREEN & GAME CONTROLS: "click [button]", "tap [button]", "tap center", "attack", "jump", "spam tap"
        val clickRegex = Pattern.compile("^(?:click|tap|press|hit)\\s+(?:the\\s+)?([a-zA-Z0-9\\s_-]+)(?:\\s+button)?$", Pattern.CASE_INSENSITIVE)
        val clickMatcher = clickRegex.matcher(cleaned)
        if (clickMatcher.find()) {
            val target = clickMatcher.group(1)?.trim() ?: ""
            if (target.isNotEmpty() && target != "flashlight" && target != "settings" && target != "wifi" && target != "camera") {
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "screen_control",
                    parameters = mapOf("action" to "click_button", "target_text" to target)
                )
            }
        }
        if (cleaned == "tap center" || cleaned == "attack" || cleaned == "jump" || cleaned == "spam tap" || cleaned == "game tap") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "screen_control",
                parameters = mapOf("action" to cleaned)
            )
        }

        // 1. YouTube Deep Actions: "search youtube for ...", "play ... on youtube", "open youtube"
        val ytRegex = Pattern.compile("^(?:search\\s+youtube\\s+(?:for\\s+)?|play\\s+(.+)\\s+on\\s+youtube|open\\s+youtube\\s+(?:and\\s+search\\s+for\\s+|and\\s+play\\s+)?)(.+)$", Pattern.CASE_INSENSITIVE)
        val ytMatcher = ytRegex.matcher(cleaned)
        if (ytMatcher.find()) {
            val query = (ytMatcher.group(1) ?: ytMatcher.group(2))?.trim() ?: ""
            if (query.isNotEmpty() && query != "youtube") {
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "youtube_control",
                    parameters = mapOf("query" to query)
                )
            }
        }
        if (cleaned.startsWith("youtube ") || cleaned.contains("on youtube")) {
            val q = cleaned.removePrefix("youtube").replace("on youtube", "").replace("search", "").replace("play", "").trim()
            if (q.isNotEmpty()) {
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "youtube_control",
                    parameters = mapOf("query" to q)
                )
            }
        }

        // 2. Spotify Deep Actions: "play ... on spotify", "spotify search ..."
        val spotifyRegex = Pattern.compile("^(?:play\\s+(.+)\\s+on\\s+spotify|search\\s+spotify\\s+(?:for\\s+)?(.+)|spotify\\s+(.+))$", Pattern.CASE_INSENSITIVE)
        val spMatcher = spotifyRegex.matcher(cleaned)
        if (spMatcher.find()) {
            val q = (spMatcher.group(1) ?: spMatcher.group(2) ?: spMatcher.group(3))?.trim() ?: ""
            if (q.isNotEmpty() && q != "spotify") {
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "spotify_control",
                    parameters = mapOf("query" to q)
                )
            }
        }

        // 3. WhatsApp Messaging & Controls: "whatsapp mom hello", "send whatsapp to ...", "whatsapp call dad", "open whatsapp chat with ..."
        val waCallRegex = Pattern.compile("^(?:whatsapp\\s+call|call\\s+on\\s+whatsapp|video\\s+call\\s+on\\s+whatsapp)\\s+(?:to\\s+)?(.+)$", Pattern.CASE_INSENSITIVE)
        val waCallMatcher = waCallRegex.matcher(cleaned)
        if (waCallMatcher.find()) {
            val target = waCallMatcher.group(1)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "whatsapp_control",
                parameters = mapOf("action" to "call", "target_user" to target)
            )
        }

        val waRegex = Pattern.compile("^(?:send\\s+(?:a\\s+)?whatsapp\\s+(?:message\\s+)?to\\s+|whatsapp\\s+)([a-zA-Z0-9\\s+]+?)(?:\\s*[:;]\\s*|\\s+saying\\s+|\\s+that\\s+|\\s+)(.+)$", Pattern.CASE_INSENSITIVE)
        val waMatcher = waRegex.matcher(cleaned)
        if (waMatcher.find()) {
            val target = waMatcher.group(1)?.trim() ?: ""
            val msg = waMatcher.group(2)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.SENSITIVE_ACTION,
                toolName = "whatsapp_control",
                parameters = mapOf("action" to "send_message", "target_user" to target, "message" to msg),
                requiresConfirmation = true
            )
        }

        // 3.1 Discord Specialized Controls: "send discord message to ...", "discord chat ...", "open discord server"
        val discordMsgRegex = Pattern.compile("^(?:send\\s+(?:a\\s+)?discord\\s+(?:message\\s+)?to\\s+|discord\\s+(?:message|chat)\\s+to\\s+|discord\\s+)([a-zA-Z0-9\\s_-]+?)(?:\\s*[:;]\\s*|\\s+saying\\s+|\\s+that\\s+|\\s+)(.+)$", Pattern.CASE_INSENSITIVE)
        val discordMsgMatcher = discordMsgRegex.matcher(cleaned)
        if (discordMsgMatcher.find()) {
            val target = discordMsgMatcher.group(1)?.trim() ?: ""
            val msg = discordMsgMatcher.group(2)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "discord_control",
                parameters = mapOf("action" to "send_message", "target_user_or_channel" to target, "message" to msg)
            )
        }
        if (cleaned.startsWith("discord ") || cleaned == "open discord" || cleaned.contains("on discord")) {
            val query = cleaned.removePrefix("open discord").removePrefix("discord").replace("on discord", "").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "discord_control",
                parameters = mapOf("action" to if (query.isNotBlank()) "open_channel" else "open", "target_user_or_channel" to query)
            )
        }

        // 3.2 Facebook Messenger Controls: "send messenger message to ...", "message ... on messenger"
        val messengerRegex = Pattern.compile("^(?:send\\s+(?:a\\s+)?messenger\\s+(?:message\\s+)?to\\s+|message\\s+(.+?)\\s+on\\s+messenger(?:\\s*[:;]\\s*|\\s+saying\\s+|\\s+that\\s+|\\s+)|messenger\\s+([a-zA-Z0-9\\s_-]+?)(?:\\s*[:;]\\s*|\\s+saying\\s+|\\s+that\\s+|\\s+))(.+)$", Pattern.CASE_INSENSITIVE)
        val messengerMatcher = messengerRegex.matcher(cleaned)
        if (messengerMatcher.find()) {
            val target = (messengerMatcher.group(1) ?: messengerMatcher.group(2))?.trim() ?: ""
            val msg = messengerMatcher.group(3)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.SENSITIVE_ACTION,
                toolName = "messenger_control",
                parameters = mapOf("action" to "send_message", "target_user" to target, "message" to msg),
                requiresConfirmation = true
            )
        }

        // 3.3 TikTok Specialized Controls: "search tiktok for ...", "next tiktok", "like tiktok", "tiktok scroll down"
        if (cleaned.contains("tiktok") || cleaned.contains("tik tok")) {
            val isNext = cleaned.contains("next") || cleaned.contains("scroll down") || cleaned.contains("swipe up") || cleaned.contains("swipe down")
            val isPrev = cleaned.contains("previous") || cleaned.contains("scroll up") || cleaned.contains("prev")
            val isLike = cleaned.contains("like") || cleaned.contains("heart")
            val isTrends = cleaned.contains("trend") || cleaned.contains("explore") || cleaned.contains("popular")
            val isSearch = cleaned.startsWith("search tiktok") || cleaned.contains("on tiktok") || cleaned.contains("search for")

            when {
                isNext -> return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "tiktok_control", parameters = mapOf("action" to "next_video"))
                isPrev -> return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "tiktok_control", parameters = mapOf("action" to "prev_video"))
                isLike -> return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "tiktok_control", parameters = mapOf("action" to "like"))
                isTrends -> return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "tiktok_control", parameters = mapOf("action" to "open_trends"))
                isSearch -> {
                    val q = cleaned.replace("search tiktok for", "").replace("search tiktok", "").replace("on tiktok", "").replace("tiktok", "").replace("search", "").trim()
                    return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "tiktok_control", parameters = mapOf("action" to "search", "query" to q))
                }
                else -> return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "tiktok_control", parameters = mapOf("action" to "open"))
            }
        }

        // 3.4 Google Ecosystem (Google Search, Maps, Gmail Compose, Photos)
        val emailRegex = Pattern.compile("^(?:send\\s+(?:an?\\s+)?email\\s+to|compose\\s+email\\s+to|gmail\\s+to)\\s+([a-zA-Z0-9@\\.\\s_-]+?)(?:\\s+about\\s+|\\s+saying\\s+|\\s+with\\s+subject\\s+|\\s+)(.+)$", Pattern.CASE_INSENSITIVE)
        val emailMatcher = emailRegex.matcher(cleaned)
        if (emailMatcher.find()) {
            val recipient = emailMatcher.group(1)?.trim() ?: ""
            val subject = emailMatcher.group(2)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "google_ecosystem",
                parameters = mapOf("service" to "gmail_compose", "query_or_recipient" to recipient, "subject_or_message" to subject)
            )
        }
        if (cleaned.startsWith("photos of ") || cleaned.startsWith("google photos ") || cleaned.startsWith("search photos for ")) {
            val query = cleaned.removePrefix("photos of ").removePrefix("google photos ").removePrefix("search photos for ").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "google_ecosystem",
                parameters = mapOf("service" to "photos_search", "query_or_recipient" to query)
            )
        }
        if (cleaned.startsWith("google ") && !cleaned.contains("maps") && !cleaned.contains("photos") && !cleaned.contains("drive")) {
            val q = cleaned.removePrefix("google ").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "google_ecosystem",
                parameters = mapOf("service" to "search", "query_or_recipient" to q)
            )
        }

        // 4. Navigation & Maps: "navigate to ...", "directions to ...", "take me to ...", "find ... near me"
        val navRegex = Pattern.compile("^(?:navigate\\s+to|directions\\s+to|take\\s+me\\s+to|drive\\s+to|route\\s+to|find\\s+places?\\s+for)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val navMatcher = navRegex.matcher(cleaned)
        if (navMatcher.find()) {
            val dest = navMatcher.group(1)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "navigation_maps",
                parameters = mapOf("destination" to dest, "mode" to "navigate")
            )
        }
        if (cleaned.contains("near me") || (cleaned.startsWith("find ") && (cleaned.contains("gas") || cleaned.contains("coffee") || cleaned.contains("restaurant") || cleaned.contains("hotel") || cleaned.contains("hospital")))) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "navigation_maps",
                parameters = mapOf("destination" to cleaned.removePrefix("find").trim(), "mode" to "search")
            )
        }

        // 5. Global Gestures & Accessibility Navigation
        if (cleaned == "go back" || cleaned == "back" || cleaned == "go back a screen") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "back"))
        }
        if (cleaned == "go home" || cleaned == "home screen" || cleaned == "take me home") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "home"))
        }
        if (cleaned == "recent apps" || cleaned == "show recents" || cleaned == "app switcher" || cleaned == "open recents") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "recents"))
        }
        if (cleaned == "open notifications" || cleaned == "show notifications" || cleaned == "notification shade") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "notifications"))
        }
        if (cleaned == "open quick settings" || cleaned == "quick settings") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "quick_settings"))
        }
        if (cleaned == "take screenshot" || cleaned == "capture screen" || cleaned == "screenshot") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "screenshot"))
        }
        if (cleaned == "lock screen" || cleaned == "lock phone" || cleaned == "turn off screen") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "lock_screen"))
        }
        if (cleaned == "scroll down" || cleaned == "scroll page down") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "scroll_down"))
        }
        if (cleaned == "scroll up" || cleaned == "scroll page up") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "global_gesture", parameters = mapOf("action" to "scroll_up"))
        }

        // 6. Camera Modes: "open video camera", "take selfie", "record video"
        if (cleaned.contains("video camera") || cleaned == "record video" || cleaned == "take video" || cleaned == "start recording") {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "camera_mode", parameters = mapOf("mode" to "video"))
        }
        if (cleaned.contains("selfie") || cleaned.contains("front camera")) {
            return AssistantDecision(type = DecisionType.LOCAL_ACTION, toolName = "camera_mode", parameters = mapOf("mode" to "selfie"))
        }

        // 7. Battery Status: "battery", "battery percentage", "check battery", "how much battery"
        if (cleaned.contains("battery") || cleaned == "power level" || cleaned.contains("battery percent")) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "battery_status"
            )
        }

        // 8. Device Specs: "device specs", "phone specs", "system info", "what phone is this"
        if (cleaned.contains("device specs") || cleaned.contains("phone specs") || cleaned.contains("system info") || cleaned == "specs") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "device_specs"
            )
        }

        // 8.1 WI-FI DEEP CONTROL: "turn on wifi", "disable wifi", "toggle wifi", "wifi status", "wifi settings"
        if (cleaned.contains("wifi") || cleaned.contains("wi-fi")) {
            val isStatus = cleaned.contains("status") || cleaned.contains("check") || cleaned.startsWith("is wifi") || cleaned.contains("info")
            val isToggle = cleaned.contains("toggle") || cleaned.contains("switch")
            val isOn = cleaned.contains("turn on") || cleaned.contains("enable") || cleaned.contains("start") || cleaned.endsWith(" on") || cleaned == "wifi on"
            val isOff = cleaned.contains("turn off") || cleaned.contains("disable") || cleaned.contains("stop") || cleaned.endsWith(" off") || cleaned == "wifi off"
            val isSettings = cleaned.contains("setting") || cleaned.contains("panel") || cleaned.contains("network") || cleaned.startsWith("open")

            val wifiAction = when {
                isStatus -> "status"
                isToggle -> "toggle"
                isOn -> "on"
                isOff -> "off"
                isSettings -> "settings"
                else -> "toggle"
            }

            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "wifi_control",
                parameters = mapOf("action" to wifiAction)
            )
        }

        // 9. DEEP VOLUME & AUDIO STREAM CONTROL: "volume up", "volume down", "mute", "unmute", "max volume", "set volume to 75%", "vibrate", "silent mode", "alarm volume to 80%"
        if (cleaned.contains("volume") || cleaned == "mute" || cleaned == "unmute" || cleaned == "silence" ||
            cleaned.contains("vibrate") || cleaned.contains("vibration") || cleaned.contains("silent mode") ||
            cleaned.contains("ringer") || cleaned.contains("sound mode")) {

            val stream = when {
                cleaned.contains("ring") || cleaned.contains("ringer") || cleaned.contains("call ring") -> "ring"
                cleaned.contains("alarm") -> "alarm"
                cleaned.contains("notification") || cleaned.contains("alert") -> "notification"
                cleaned.contains("call") || cleaned.contains("voice") -> "voice_call"
                cleaned.contains("system") -> "system"
                else -> "media"
            }

            val isStatus = cleaned.contains("status") || cleaned.contains("check") || cleaned.contains("levels") || cleaned.contains("what is the volume")
            val isVibrate = cleaned.contains("vibrate") || cleaned.contains("vibration")
            val isSilent = cleaned.contains("silent mode") || (cleaned.contains("silent") && !cleaned.contains("media"))
            val isNormal = cleaned.contains("normal mode") || cleaned.contains("normal sound") || cleaned.contains("unmute phone")

            val numRegex = Pattern.compile("(\\d{1,3})\\s*%?")
            val numMatcher = numRegex.matcher(cleaned)
            val level = if (numMatcher.find()) numMatcher.group(1)?.toIntOrNull() else null

            val deltaRegex = Pattern.compile("(?:by|up by|down by)\\s*(\\d{1,3})\\s*%?")
            val deltaMatcher = deltaRegex.matcher(cleaned)
            val delta = if (deltaMatcher.find()) deltaMatcher.group(1)?.toIntOrNull() else null

            val action = when {
                isStatus -> "status"
                isVibrate -> "vibrate"
                isSilent -> "silent_mode"
                isNormal -> "normal_mode"
                level != null -> "set"
                cleaned.contains("up") || cleaned.contains("raise") || cleaned.contains("increase") -> "up"
                cleaned.contains("down") || cleaned.contains("lower") || cleaned.contains("decrease") -> "down"
                cleaned.contains("mute") || cleaned.contains("silence") -> "mute"
                cleaned.contains("unmute") -> "unmute"
                cleaned.contains("max") || cleaned.contains("full") -> "max"
                else -> "up"
            }

            val params = mutableMapOf<String, Any?>("action" to action, "stream_type" to stream)
            if (level != null) params["level_percent"] = level
            if (delta != null) params["delta_percent"] = delta

            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "volume_control",
                parameters = params
            )
        }

        // 9.1 SPECIFIC INSTALLED APP ACTIVITIES & DEEP SECTIONS
        // "open subscriptions in youtube", "open incognito in chrome", "open updates in play store", "app info for spotify"
        val deepActivityRegex = Pattern.compile("^(?:open|launch|show|start)\\s+(.+?)\\s+(?:in|on|for|of)\\s+([a-zA-Z0-9\\s\\._-]+)$", Pattern.CASE_INSENSITIVE)
        val deepActivityMatcher = deepActivityRegex.matcher(cleaned)
        if (deepActivityMatcher.find()) {
            val section = deepActivityMatcher.group(1)?.trim() ?: ""
            val app = deepActivityMatcher.group(2)?.trim() ?: ""
            if (section.isNotEmpty() && app.isNotEmpty()) {
                return AssistantDecision(
                    type = DecisionType.LOCAL_ACTION,
                    toolName = "launch_app_activity",
                    parameters = mapOf("app_name" to app, "section_name" to section)
                )
            }
        }

        // Shortcut patterns: "youtube subscriptions", "chrome incognito", "play store updates", "chrome downloads"
        if (cleaned.startsWith("youtube ") && (cleaned.contains("subscription") || cleaned.contains("history") || cleaned.contains("shorts") || cleaned.contains("library"))) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "launch_app_activity",
                parameters = mapOf("app_name" to "youtube", "section_name" to cleaned.removePrefix("youtube").trim())
            )
        }
        if (cleaned.startsWith("chrome ") && (cleaned.contains("incognito") || cleaned.contains("download") || cleaned.contains("bookmark") || cleaned.contains("history"))) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "launch_app_activity",
                parameters = mapOf("app_name" to "chrome", "section_name" to cleaned.removePrefix("chrome").trim())
            )
        }
        if ((cleaned.contains("play store") || cleaned.contains("google play")) && (cleaned.contains("update") || cleaned.contains("my apps") || cleaned.contains("subscription"))) {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "launch_app_activity",
                parameters = mapOf("app_name" to "play store", "section_name" to "updates")
            )
        }
        if (cleaned.startsWith("app info ") || cleaned.startsWith("app details ") || cleaned.startsWith("app permissions ")) {
            val targetApp = cleaned.removePrefix("app info ").removePrefix("app details ").removePrefix("app permissions ").replace("for", "").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "launch_app_activity",
                parameters = mapOf("app_name" to targetApp, "section_name" to "info")
            )
        }

        // 10. Quick Math Calculator: "what is 25 * 4", "calculate 500 + 20", "15% of 800"
        val mathRegex = Pattern.compile("^(?:calculate|what is|compute)?\\s*(\\d+(?:\\.\\d+)?\\s*(?:\\+|-|\\*|x|/|times|plus|minus|divided by|% of|percent of)\\s*\\d+(?:\\.\\d+)?)\\s*\\??$", Pattern.CASE_INSENSITIVE)
        val mathMatcher = mathRegex.matcher(cleaned)
        if (mathMatcher.find()) {
            val expr = mathMatcher.group(1)?.trim() ?: ""
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "calculate_math",
                parameters = mapOf("expression" to expr)
            )
        }

        // 11. Flashlight Pattern: "turn on flashlight", "flashlight off", "torch on"
        if (cleaned.contains("flashlight") || cleaned.contains("torch")) {
            val turnOn = !cleaned.contains("off") && !cleaned.contains("disable") && !cleaned.contains("stop")
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "toggle_flashlight",
                parameters = mapOf("enabled" to turnOn)
            )
        }

        // 12. Open App Pattern: "open youtube", "launch chrome", "start spotify", etc.
        val openAppRegex = Pattern.compile("^(?:open|launch|start|run)\\s+([a-zA-Z0-9\\s\\._-]+)$", Pattern.CASE_INSENSITIVE)
        val openAppMatcher = openAppRegex.matcher(cleaned)
        if (openAppMatcher.find()) {
            val target = openAppMatcher.group(1)?.trim() ?: ""
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

        // 13. Open URL Pattern: "go to reddit.com", "open website google.com"
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

        // 14. Settings Patterns: "open settings", "settings", "open wifi settings", "bluetooth settings", "wifi settings"
        if (cleaned == "open settings" || cleaned == "settings" || cleaned == "phone settings" ||
            cleaned == "system settings" || cleaned == "device settings" || cleaned == "go to settings" ||
            cleaned == "launch settings" || cleaned == "android settings") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "open_settings",
                parameters = mapOf("destination" to "general")
            )
        }

        if ((cleaned.startsWith("open ") || cleaned.startsWith("turn on ") || cleaned.startsWith("show ") || cleaned.startsWith("go to ")) &&
            (cleaned.contains("setting") || cleaned.contains("wifi") || cleaned.contains("bluetooth") || cleaned.contains("display") || cleaned.contains("overlay") || cleaned.contains("sound") || cleaned.contains("battery"))) {
            val dest = cleaned.removePrefix("open").removePrefix("turn on").removePrefix("show").removePrefix("go to").replace("settings", "").replace("setting", "").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "open_settings",
                parameters = mapOf("destination" to if (dest.isNotEmpty()) dest else "general")
            )
        }

        // 15. Timer Pattern: "set a 10 minute timer", "timer 5 minutes", "set timer for 30 seconds"
        val timerRegex = Pattern.compile("(?:set\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(\\d+)\\s*(min|minute|minutes|sec|second|seconds|hour|hours)?(?:\\s+(?:for|labeled|called)\\s+(.+))?", Pattern.CASE_INSENSITIVE)
        val timerMatcher = timerRegex.matcher(cleaned)
        if (timerMatcher.find()) {
            val num = timerMatcher.group(1)?.toIntOrNull() ?: 60
            val unit = timerMatcher.group(2)?.lowercase() ?: "minute"
            val label = timerMatcher.group(3)?.trim() ?: "Timer"
            val durationSeconds = when {
                unit.startsWith("hour") -> num * 3600
                unit.startsWith("sec") -> num
                else -> num * 60
            }
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "set_timer",
                parameters = mapOf("duration_seconds" to durationSeconds, "label" to label)
            )
        }

        // 16. Alarm Pattern: "set an alarm for 7:30 am", "set alarm for 8 am", "alarm at 6:00"
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

        // 17. Call Contact Pattern: "call mom", "dial 911", "call 1234567890"
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

        // 18. SMS / Message Pattern: "send message to Alex: I'm coming", "text mom hello"
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

        // 19. Media playback: "stop music", "pause music", "resume music", "next song", "play music"
        if (cleaned == "stop music" || cleaned == "stop audio" || cleaned == "stop song" ||
            cleaned == "stop playing" || cleaned == "stop playback" || cleaned == "halt music") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "stop")
            )
        }
        if (cleaned == "pause music" || cleaned == "pause" || cleaned == "pause song" ||
            cleaned == "pause playback" || cleaned == "pause audio") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "pause")
            )
        }
        if (cleaned == "play music" || cleaned == "resume music" || cleaned == "play song" ||
            cleaned == "resume song" || cleaned == "unpause" || cleaned == "resume playback") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "play")
            )
        }
        if (cleaned == "next song" || cleaned == "skip track" || cleaned == "next track" ||
            cleaned == "skip song" || cleaned == "forward track") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "next")
            )
        }
        if (cleaned == "previous song" || cleaned == "prev track" || cleaned == "previous track" ||
            cleaned == "restart song" || cleaned == "prev song") {
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "media_control",
                parameters = mapOf("action" to "previous")
            )
        }

        // 20. Copy clipboard: "copy this text: hello world"
        if (cleaned.startsWith("copy to clipboard") || cleaned.startsWith("copy this:")) {
            val textToCopy = cleaned.substringAfter(":").trim()
            return AssistantDecision(
                type = DecisionType.LOCAL_ACTION,
                toolName = "copy_to_clipboard",
                parameters = mapOf("text" to textToCopy)
            )
        }

        // 21. Explicit web search requests: "search the web for...", "google ..."
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

        // 22. AI Questions and Deep Reasoning
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

    private fun matchPrebuiltConversationalPhrase(cleaned: String): String? {
        val str = cleaned.replace("?", "").replace("!", "").replace(".", "").trim()

        // 1. Basic Greetings & Introductions
        if (str in listOf("hi", "hello", "hey", "hey tecno", "hi tecno", "hello tecno", "yo", "sup", "what's up", "whats up", "greetings")) {
            return "Hello! I am Tecno AI, your high-performance Android assistant created by Tecnolegend. What can I do for you?"
        }

        // 2. How are you & status
        if (str in listOf("how are you", "how are you doing", "hows it going", "how's it going", "how are things", "you good", "how do you do")) {
            return "I'm running at peak performance! Fully charged, connected, and ready to assist you. How are you doing today?"
        }

        // 3. Identity & What are you
        if (str in listOf("who are you", "what is your name", "what are you", "introduce yourself", "tell me about yourself", "your name")) {
            return "I am Tecno AI 2.0, an advanced mobile voice assistant engineered by Tecnolegend to execute fast device actions, in-app automation, and intelligent reasoning."
        }

        // 4. Capabilities & Features
        if (str in listOf("what can you do", "help", "features", "commands", "what are your capabilities", "show me what you can do")) {
            return "I can control device hardware (Wi-Fi, volume, torch), open deep app sections, like YouTube videos, click in-game buttons, read your screen, make calls, and solve any complex question!"
        }

        // 5. Time Greetings
        if (str in listOf("good morning", "morning")) {
            return "Good morning! Wishing you a productive and wonderful day ahead. How can I help get your day started?"
        }
        if (str in listOf("good afternoon", "afternoon")) {
            return "Good afternoon! Hope your day is going great. What can I assist you with right now?"
        }
        if (str in listOf("good evening", "evening")) {
            return "Good evening! I'm here to assist you with anything you need tonight."
        }
        if (str in listOf("good night", "night", "sweet dreams")) {
            return "Good night! Rest well and have peaceful dreams. I'll be right here whenever you need me."
        }

        // 6. Gratitude & Appreciation
        if (str in listOf("thank you", "thanks", "thank you so much", "thanks a lot", "appreciate it", "good job", "nice work", "awesome")) {
            return "You're very welcome! I'm always happy to help you."
        }

        // 7. Farewell
        if (str in listOf("bye", "goodbye", "see you", "see ya", "later", "catch you later", "exit")) {
            return "Goodbye! Have a great time. Just say 'Hey Tecno' whenever you need me."
        }

        // 8. Jokes & Humor
        if (str in listOf("tell me a joke", "joke", "make me laugh", "say something funny")) {
            val jokes = listOf(
                "Why don't programmers like nature? It has too many bugs!",
                "Why did the smartphone wear glasses? Because it lost its contacts!",
                "There are 10 types of people in the world: those who understand binary, and those who don't!",
                "Why was the computer cold? It left its Windows open!"
            )
            return jokes.random()
        }

        // 9. Time & Date
        if (str in listOf("what time is it", "current time", "what is the time", "tell me the time", "time now")) {
            val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            return "The current time is $time."
        }
        if (str in listOf("what is today's date", "what date is it", "today's date", "what is the date", "todays date")) {
            val date = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            return "Today is $date."
        }

        // 10. AI Reality & Human check
        if (str in listOf("are you real", "are you ai", "are you human", "are you a robot")) {
            return "I am Tecno AI, a real artificial intelligence assistant created by Tecnolegend to help you get things done faster on your phone."
        }

        return null
    }
}
