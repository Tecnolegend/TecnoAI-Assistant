package com.tecnolegend.tecnoai.tools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tecnolegend.tecnoai.accessibility.TecnoAccessibilityService

/**
 * Specialized WhatsApp Controller:
 * - Send message to specific contact / phone number
 * - Open chat with user
 * - Start WhatsApp voice call or video call
 */
class WhatsAppSpecializedTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "whatsapp_control",
        description = "Advanced WhatsApp control: send message to a specific user/number, open a chat, or start a voice/video call.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'send_message', 'open_chat', 'call', 'video_call', 'status'", required = true),
            ToolParameter("target_user", "STRING", "Contact name or phone number with country code", required = false),
            ToolParameter("message", "STRING", "Text message content to send", required = false)
        ),
        requiresConfirmation = true
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "send_message"
        val target = (params["target_user"] as? String)?.trim() ?: ""
        val message = (params["message"] as? String)?.trim() ?: ""

        val cleanNumber = target.replace("+", "").replace("-", "").replace(" ", "")
        val isDigits = cleanNumber.all { it.isDigit() } && cleanNumber.length >= 7

        return try {
            when (action) {
                "send_message", "message", "text" -> {
                    val uri = if (isDigits) {
                        Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
                    } else {
                        Uri.parse("whatsapp://send?text=${Uri.encode(message)}")
                    }
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    val desc = if (target.isNotBlank()) "Sending WhatsApp to $target: '$message'" else "Sending WhatsApp message: '$message'"
                    ToolResult(true, desc, mapOf("recipient" to target, "message" to message))
                }
                "open_chat", "chat" -> {
                    val uri = if (isDigits) {
                        Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
                    } else {
                        Uri.parse("whatsapp://send?text=${Uri.encode("")}")
                    }
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Opening WhatsApp chat with $target", mapOf("recipient" to target))
                }
                "call", "voice_call" -> {
                    val intent = if (isDigits) {
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else {
                        context.packageManager.getLaunchIntentForPackage("com.whatsapp")?.apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        } ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.whatsapp")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Opening WhatsApp to call $target", mapOf("recipient" to target))
                }
                else -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                    ToolResult(true, "Opening WhatsApp", mapOf("action" to "open"))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "WhatsApp action failed: ${e.localizedMessage}")
        }
    }
}

/**
 * Specialized Discord Controller:
 * - Send message / open Discord DM or channel
 * - Open Discord server / search
 */
class DiscordControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "discord_control",
        description = "Controls Discord: open Discord, send a message to a user/channel, open a specific server or search.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'open', 'send_message', 'search', 'open_channel'", required = true),
            ToolParameter("target_user_or_channel", "STRING", "User tag, channel name, or server name", required = false),
            ToolParameter("message", "STRING", "Message text to send on Discord", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "open"
        val target = (params["target_user_or_channel"] as? String)?.trim() ?: ""
        val message = (params["message"] as? String)?.trim() ?: ""

        val pm = context.packageManager
        val discordPkg = "com.discord"

        return try {
            when (action) {
                "send_message", "message", "chat" -> {
                    if (message.isNotBlank()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage(discordPkg)
                            putExtra(Intent.EXTRA_TEXT, message)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (shareIntent.resolveActivity(pm) != null) {
                            context.startActivity(shareIntent)
                            return ToolResult(true, "Opening Discord to send message: '$message' ${if (target.isNotBlank()) "to $target" else ""}", mapOf("target" to target, "message" to message))
                        }
                    }
                    val launchIntent = pm.getLaunchIntentForPackage(discordPkg)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/channels/@me")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(launchIntent)
                    ToolResult(true, "Opening Discord for $target", mapOf("target" to target))
                }
                "open_channel", "server", "search" -> {
                    val launchIntent = pm.getLaunchIntentForPackage(discordPkg)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(launchIntent)
                    ToolResult(true, "Opening Discord $target", mapOf("target" to target))
                }
                else -> {
                    val launchIntent = pm.getLaunchIntentForPackage(discordPkg)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(launchIntent)
                    ToolResult(true, "Opening Discord", mapOf("action" to "open"))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "Discord action failed: ${e.localizedMessage}")
        }
    }
}

/**
 * Specialized Facebook Messenger Controller:
 * - Send message to specific user
 * - Open Messenger chat with person
 */
class MessengerControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "messenger_control",
        description = "Controls Facebook Messenger: sends a message or opens chat with a specific user.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'send_message', 'open_chat', 'open'", required = true),
            ToolParameter("target_user", "STRING", "Name or username of the person on Messenger", required = false),
            ToolParameter("message", "STRING", "Message text to send", required = false)
        ),
        requiresConfirmation = true
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "send_message"
        val target = (params["target_user"] as? String)?.trim() ?: ""
        val message = (params["message"] as? String)?.trim() ?: ""

        val pm = context.packageManager
        val messengerPkg = "com.facebook.orca"

        return try {
            when (action) {
                "send_message", "message" -> {
                    if (message.isNotBlank()) {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage(messengerPkg)
                            putExtra(Intent.EXTRA_TEXT, message)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (sendIntent.resolveActivity(pm) != null) {
                            context.startActivity(sendIntent)
                            return ToolResult(true, "Sending Messenger message to $target: '$message'", mapOf("target" to target, "message" to message))
                        }
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("fb-messenger://user/")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(webIntent)
                    }
                    ToolResult(true, "Opening Messenger for $target", mapOf("target" to target))
                }
                else -> {
                    val launchIntent = pm.getLaunchIntentForPackage(messengerPkg)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(launchIntent)
                    ToolResult(true, "Opening Messenger", mapOf("action" to "open"))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "Messenger action failed: ${e.localizedMessage}")
        }
    }
}

/**
 * Specialized TikTok Controller:
 * - Search creators / hashtags / trends
 * - Next / previous video scroll using gestures
 * - Like TikTok video
 */
class TikTokControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "tiktok_control",
        description = "Controls TikTok: search hashtags/creators, scroll to next/previous video, like video, or open TikTok trends.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'search', 'next_video', 'prev_video', 'like', 'open_trends', 'open'", required = true),
            ToolParameter("query", "STRING", "Search query for hashtag, user, or topic on TikTok", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "open"
        val query = (params["query"] as? String)?.trim() ?: ""
        val service = TecnoAccessibilityService.instance

        val pm = context.packageManager
        val tiktokPkg = "com.zhiliaoapp.musically"

        return try {
            when (action) {
                "next_video", "scroll_down", "next" -> {
                    val success = service?.scrollForward() ?: false
                    if (success) {
                        ToolResult(true, "Swiped to next TikTok video", mapOf("action" to "next_video"))
                    } else {
                        ToolResult(false, "Please enable Accessibility to allow gesture scrolling in TikTok.")
                    }
                }
                "prev_video", "scroll_up", "previous" -> {
                    val success = service?.scrollBackward() ?: false
                    if (success) {
                        ToolResult(true, "Swiped to previous TikTok video", mapOf("action" to "prev_video"))
                    } else {
                        ToolResult(false, "Please enable Accessibility to allow gesture scrolling in TikTok.")
                    }
                }
                "like", "like_video" -> {
                    val serviceAcc = TecnoAccessibilityService.instance
                    val success = serviceAcc?.clickElementByTextOrDescription("Like")
                        ?: serviceAcc?.performCoordinateClick(context.resources.displayMetrics.widthPixels * 0.92f, context.resources.displayMetrics.heightPixels * 0.55f)
                        ?: false
                    if (success) {
                        ToolResult(true, "Liked current TikTok video", mapOf("action" to "like"))
                    } else {
                        ToolResult(false, "Could not like video. Ensure TikTok is currently active.")
                    }
                }
                "search", "find" -> {
                    val searchUri = Uri.parse("snssdk1128://search?keyword=${Uri.encode(query)}")
                    val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/tag/${Uri.encode(query)}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(webIntent)
                    }
                    ToolResult(true, "Searching TikTok for '$query'", mapOf("query" to query))
                }
                "open_trends", "trending" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/explore")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Opening TikTok Trends", mapOf("action" to "trends"))
                }
                else -> {
                    val launchIntent = pm.getLaunchIntentForPackage(tiktokPkg)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(launchIntent)
                    ToolResult(true, "Opening TikTok", mapOf("action" to "open"))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "TikTok action failed: ${e.localizedMessage}")
        }
    }
}

/**
 * Specialized Google Ecosystem Controller:
 * - Google Search & Voice Search
 * - Google Maps Directions & Places
 * - Gmail: Compose email to specific contact
 * - Google Photos: Search photos
 */
class GoogleEcosystemTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "google_ecosystem",
        description = "Controls Google services: Google Search, Google Maps navigation, Gmail compose to user, or Google Photos search.",
        parameters = listOf(
            ToolParameter("service", "STRING", "'search', 'maps_directions', 'gmail_compose', 'photos_search'", required = true),
            ToolParameter("query_or_recipient", "STRING", "Search query or email recipient (e.g. 'john@example.com', 'coffee near me', 'photos of dogs')", required = true),
            ToolParameter("subject_or_message", "STRING", "Optional email subject or extra details", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val service = (params["service"] as? String)?.trim()?.lowercase() ?: "search"
        val queryOrRecipient = (params["query_or_recipient"] as? String)?.trim() ?: ""
        val subjectOrMessage = (params["subject_or_message"] as? String)?.trim() ?: ""

        val pm = context.packageManager

        return try {
            when (service) {
                "maps_directions", "maps", "navigation" -> {
                    val uri = Uri.parse("google.navigation:q=${Uri.encode(queryOrRecipient)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (mapIntent.resolveActivity(pm) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(queryOrRecipient)}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(webIntent)
                    }
                    ToolResult(true, "Navigating to $queryOrRecipient on Google Maps", mapOf("destination" to queryOrRecipient))
                }
                "gmail_compose", "gmail", "email" -> {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${Uri.encode(queryOrRecipient)}")
                        putExtra(Intent.EXTRA_SUBJECT, subjectOrMessage.ifBlank { "Message from Tecno AI" })
                        putExtra(Intent.EXTRA_TEXT, "")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(emailIntent)
                    ToolResult(true, "Opening Gmail to compose message to $queryOrRecipient", mapOf("recipient" to queryOrRecipient))
                }
                "photos_search", "photos" -> {
                    val uri = Uri.parse("https://photos.google.com/search/${Uri.encode(queryOrRecipient)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.photos")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(webIntent)
                    }
                    ToolResult(true, "Searching Google Photos for '$queryOrRecipient'", mapOf("query" to queryOrRecipient))
                }
                else -> {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(android.app.SearchManager.QUERY, queryOrRecipient)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                    } else {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(queryOrRecipient)}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    }
                    ToolResult(true, "Searching Google for '$queryOrRecipient'", mapOf("query" to queryOrRecipient))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "Google ecosystem action failed: ${e.localizedMessage}")
        }
    }
}
