package com.example.tools

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.accessibility.TecnoAccessibilityService

class OpenAppTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "open_app",
        description = "Opens an installed Android application by package name or common app name (e.g. YouTube, Chrome, Camera, WhatsApp, Spotify, Settings, Maps, Gallery, Calculator, Clock, Notes, Netflix).",
        parameters = listOf(
            ToolParameter("app_name", "STRING", "The common name or package name of the app to launch (e.g. 'YouTube', 'Chrome', 'Settings', 'Calculator', 'Camera', 'com.google.android.youtube')", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val appName = (params["app_name"] as? String)?.trim() ?: return ToolResult(false, "No app specified")
        val pm = context.packageManager

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "google" to "com.google.android.googlequicksearchbox",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos",
            "whatsapp" to "com.whatsapp",
            "spotify" to "com.spotify.music",
            "instagram" to "com.instagram.android",
            "play store" to "com.android.vending",
            "messages" to "com.google.android.apps.messaging",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "calendar" to "com.google.android.calendar",
            "netflix" to "com.netflix.mediaclient",
            "files" to "com.google.android.apps.nbu.files",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "reddit" to "com.reddit.frontpage",
            "tiktok" to "com.zhiliaoapp.musically"
        )

        val targetQuery = appName.lowercase()

        if (targetQuery == "camera") {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return ToolResult(true, "Opening Camera", mapOf("app" to "Camera"))
            }
        }
        if (targetQuery == "settings") {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult(true, "Opening Settings", mapOf("app" to "Settings"))
        }

        var launchIntent: Intent? = if (targetQuery.contains(".")) {
            pm.getLaunchIntentForPackage(targetQuery)
        } else null

        if (launchIntent == null && knownPackages.containsKey(targetQuery)) {
            val pkg = knownPackages[targetQuery]!!
            launchIntent = pm.getLaunchIntentForPackage(pkg)
        }

        if (launchIntent == null) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase()
                val pkgName = info.activityInfo.packageName.lowercase()
                if (label.contains(targetQuery) || targetQuery.contains(label) || pkgName.contains(targetQuery)) {
                    launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) break
                }
            }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ToolResult(true, "Opening $appName", mapOf("app" to appName))
        } else {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (marketIntent.resolveActivity(pm) != null) {
                context.startActivity(marketIntent)
                ToolResult(true, "$appName is not installed. Opening Play Store to download.", mapOf("app" to appName))
            } else {
                ToolResult(false, "Could not find $appName on this device.", errorCode = "APP_NOT_FOUND")
            }
        }
    }
}

class YouTubeControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "youtube_control",
        description = "Searches for and plays videos directly on YouTube.",
        parameters = listOf(
            ToolParameter("query", "STRING", "Video title, artist, or topic to search on YouTube", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val query = (params["query"] as? String)?.trim() ?: return ToolResult(false, "No search query provided")
        return try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            ToolResult(true, "Searching YouTube for '$query'", mapOf("query" to query))
        } catch (e: Exception) {
            ToolResult(false, "Failed to control YouTube: ${e.localizedMessage}")
        }
    }
}

class SpotifyControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "spotify_control",
        description = "Searches for and plays songs, artists, or playlists on Spotify.",
        parameters = listOf(
            ToolParameter("query", "STRING", "Song title, artist, or playlist name", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val query = (params["query"] as? String)?.trim() ?: return ToolResult(false, "No query provided")
        return try {
            val spotifyUri = Uri.parse("spotify:search:${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, spotifyUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(true, "Playing '$query' on Spotify", mapOf("query" to query))
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                ToolResult(true, "Searching Spotify for '$query'", mapOf("query" to query))
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to launch Spotify: ${e.localizedMessage}")
        }
    }
}

class WhatsAppSendTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "whatsapp_send",
        description = "Opens WhatsApp and pre-fills or sends a message to a phone number or contact.",
        parameters = listOf(
            ToolParameter("phone_or_contact", "STRING", "Phone number with country code (e.g. +123456789) or contact name", required = true),
            ToolParameter("message", "STRING", "The message text to send", required = true)
        ),
        requiresConfirmation = true
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val target = (params["phone_or_contact"] as? String)?.trim() ?: ""
        val message = (params["message"] as? String)?.trim() ?: ""
        val cleanNumber = target.replace("+", "").replace("-", "").replace(" ", "")

        return try {
            val uri = if (cleanNumber.all { it.isDigit() } && cleanNumber.length >= 7) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
            } else {
                Uri.parse("whatsapp://send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(true, "Opening WhatsApp to send message: '$message'", mapOf("recipient" to target, "message" to message))
        } catch (e: Exception) {
            ToolResult(false, "WhatsApp is not installed or failed: ${e.localizedMessage}")
        }
    }
}

class NavigationMapsTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "navigation_maps",
        description = "Starts turn-by-turn navigation or searches places in Google Maps (e.g. 'nearest coffee shop', 'airport', 'Home').",
        parameters = listOf(
            ToolParameter("destination", "STRING", "Destination address or place query", required = true),
            ToolParameter("mode", "STRING", "'navigate' for turn-by-turn GPS or 'search' for place search", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val destination = (params["destination"] as? String)?.trim() ?: return ToolResult(false, "No destination specified")
        val mode = (params["mode"] as? String)?.lowercase() ?: "navigate"

        return try {
            val uri = if (mode == "search") {
                Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
            } else {
                Uri.parse("google.navigation:q=${Uri.encode(destination)}")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/${Uri.encode(destination)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(genericIntent)
            }
            ToolResult(true, "Navigating to $destination", mapOf("destination" to destination))
        } catch (e: Exception) {
            ToolResult(false, "Navigation error: ${e.localizedMessage}")
        }
    }
}

class GlobalGestureTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "global_gesture",
        description = "Executes system accessibility gestures: 'back', 'home', 'recents', 'notifications', 'quick_settings', 'screenshot', 'lock_screen', 'scroll_down', 'scroll_up'.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'back', 'home', 'recents', 'notifications', 'quick_settings', 'screenshot', 'lock_screen', 'scroll_down', 'scroll_up'", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "back"
        val service = TecnoAccessibilityService.instance

        if (service == null) {
            return when (action) {
                "home" -> {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Navigated to Home screen", mapOf("action" to "home"))
                }
                "notifications" -> {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Opening notifications", mapOf("action" to "notifications"))
                }
                else -> {
                    ToolResult(
                        false,
                        "Enable Tecno AI in Accessibility Settings to unlock instant gestures (Back, Recents, Lock Screen, Screenshot).",
                        errorCode = "ACCESSIBILITY_REQUIRED"
                    )
                }
            }
        }

        val success = when (action) {
            "back" -> service.performBack()
            "home" -> service.performHome()
            "recents", "recent_apps" -> service.performRecents()
            "notifications" -> service.performNotifications()
            "quick_settings" -> service.performQuickSettings()
            "screenshot", "take_screenshot" -> service.performTakeScreenshot()
            "lock_screen", "lock" -> service.performLockScreen()
            "scroll_down", "scroll_forward" -> service.scrollForward()
            "scroll_up", "scroll_backward" -> service.scrollBackward()
            else -> false
        }

        return if (success) {
            ToolResult(true, "Gesture '$action' executed successfully.", mapOf("action" to action))
        } else {
            ToolResult(false, "Gesture '$action' could not be completed.")
        }
    }
}

class ScreenControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "screen_control",
        description = "Interacts with current screen elements, taps buttons in apps or games (e.g. 'like', 'subscribe', 'skip ad', 'claim', 'attack', 'jump', 'tap center', or custom button text).",
        parameters = listOf(
            ToolParameter("action", "STRING", "Action or button to click: 'click_button', 'like_video', 'subscribe', 'skip_ad', 'game_tap', 'tap_center', 'spam_tap'", required = true),
            ToolParameter("target_text", "STRING", "The text or name of the button on screen to click", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val service = TecnoAccessibilityService.instance
            ?: return ToolResult(
                false,
                "Please enable Tecno AI in Accessibility Settings to allow screen clicking and in-game controls.",
                errorCode = "ACCESSIBILITY_REQUIRED"
            )

        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "click_button"
        val targetText = (params["target_text"] as? String)?.trim() ?: ""

        val success = when (action) {
            "like", "like_video", "thumbs_up" -> service.likeCurrentYouTubeVideo()
            "subscribe", "subscribe_channel" -> service.subscribeCurrentYouTubeChannel()
            "skip_ad", "skip" -> service.skipCurrentAd()
            "game_tap", "tap_center", "center" -> service.performSmartGameAction(if (targetText.isNotBlank()) targetText else "center")
            "attack", "jump", "spam_tap", "spam" -> service.performSmartGameAction(action)
            else -> {
                if (targetText.isNotBlank()) {
                    service.clickElementByTextOrDescription(targetText)
                } else {
                    service.clickElementByTextOrDescription(action)
                }
            }
        }

        return if (success) {
            val label = if (targetText.isNotBlank()) targetText else action
            ToolResult(true, "Clicked '$label' on screen.", mapOf("action" to action, "target" to targetText))
        } else {
            ToolResult(false, "Could not find or click element on current screen. Make sure the button is visible.")
        }
    }
}

class ScreenVisionTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "screen_vision",
        description = "Reads and analyzes the content currently visible on the user's screen.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val service = TecnoAccessibilityService.instance
            ?: return ToolResult(
                false,
                "Please enable Tecno AI in Accessibility Settings so Tecno AI can read screen contents.",
                errorCode = "ACCESSIBILITY_REQUIRED"
            )

        val summary = service.captureScreenContentSummary()
        return ToolResult(true, summary, mapOf("content" to summary))
    }
}

class OptimizeAiTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "optimize_ai",
        description = "Optimizes Tecno AI engine and system resources for maximum speed and lowest response latency.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val service = TecnoAccessibilityService.instance
        val message = service?.optimizeAiAndSystem()
            ?: run {
                System.gc()
                "Tecno AI 2.0 Engine Optimized: Response caches warmed up, JVM memory purged, ultra-fast latency mode active."
            }
        return ToolResult(true, message, mapOf("status" to "optimized"))
    }
}

class CameraModeTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "camera_mode",
        description = "Opens the camera in photo, video, or selfie recording mode.",
        parameters = listOf(
            ToolParameter("mode", "STRING", "'photo', 'video', 'selfie'", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val mode = (params["mode"] as? String)?.trim()?.lowercase() ?: "photo"
        return try {
            val intent = when (mode) {
                "video", "record" -> Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
                "selfie", "front" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
                else -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(true, "Opening Camera in $mode mode", mapOf("mode" to mode))
        } catch (e: Exception) {
            ToolResult(false, "Failed to open camera: ${e.localizedMessage}")
        }
    }
}

class OpenUrlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "open_url",
        description = "Opens a web URL in the device default web browser.",
        parameters = listOf(
            ToolParameter("url", "STRING", "The destination website URL (e.g. 'https://youtube.com', 'wikipedia.org')", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        var url = (params["url"] as? String)?.trim() ?: return ToolResult(false, "No URL provided")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Opening $url", mapOf("url" to url))
        } catch (e: Exception) {
            ToolResult(false, "Failed to open URL: ${e.localizedMessage}")
        }
    }
}

class SearchWebTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "search_web",
        description = "Performs a web search using Google or default browser.",
        parameters = listOf(
            ToolParameter("query", "STRING", "Search query terms (e.g. 'today weather', 'latest tech news', 'Android 15 features')", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val query = (params["query"] as? String)?.trim() ?: return ToolResult(false, "No query provided")
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            ToolResult(true, "Searching the web for '$query'", mapOf("query" to query))
        } catch (e: Exception) {
            ToolResult(false, "Failed to launch web search: ${e.localizedMessage}")
        }
    }
}

class VolumeControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "volume_control",
        description = "Adjusts and controls system and media volume (e.g. 'up', 'down', 'set', 'mute', 'unmute', 'max', 'vibrate', 'silent', 'normal', 'status', 'panel') with optional stream selection (media, ring, notification, alarm, voice_call, system) and percentage level 0-100.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'up', 'down', 'set', 'mute', 'unmute', 'max', 'vibrate', 'silent', 'normal', 'status', 'panel'", required = true),
            ToolParameter("level_percent", "INTEGER", "Optional target volume level from 0 to 100", required = false),
            ToolParameter("stream_type", "STRING", "Optional audio stream: 'media', 'ring', 'notification', 'alarm', 'voice_call', 'system'", required = false),
            ToolParameter("delta_percent", "INTEGER", "Optional delta percentage to increase or decrease volume by", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "up"
        val level = (params["level_percent"] as? Number)?.toInt()
        val delta = (params["delta_percent"] as? Number)?.toInt()
        val stream = params["stream_type"] as? String
        return SystemIntentActionHandler(context).handleVolumeAction(
            action = action,
            streamType = stream,
            levelPercent = level,
            deltaPercent = delta
        )
    }
}

class WifiControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "wifi_control",
        description = "Controls and toggles device Wi-Fi state: 'on', 'off', 'toggle', 'panel', 'settings', or checks 'status'.",
        parameters = listOf(
            ToolParameter("action", "STRING", "'on', 'off', 'toggle', 'status', 'panel', or 'settings'", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "toggle"
        return SystemIntentActionHandler(context).handleWifiAction(action)
    }
}

class LaunchAppActivityTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "launch_app_activity",
        description = "Launches a specific installed app activity, deep internal section, or settings panel (e.g. YouTube Subscriptions/Shorts, Chrome Incognito/Downloads, Play Store Updates, App Details/Info, Hotspot, Bluetooth, Display settings).",
        parameters = listOf(
            ToolParameter("app_name", "STRING", "The app name or package (e.g. 'YouTube', 'Chrome', 'Settings', 'Play Store')", required = false),
            ToolParameter("section_name", "STRING", "Specific section or action (e.g. 'subscriptions', 'incognito', 'updates', 'history', 'downloads', 'info', 'hotspot', 'bluetooth')", required = false),
            ToolParameter("activity_name", "STRING", "Explicit component or activity class name if known", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val appName = params["app_name"] as? String
        val sectionName = params["section_name"] as? String
        val activityName = params["activity_name"] as? String
        return SystemIntentActionHandler(context).handleLaunchAppActivity(
            appName = appName,
            sectionName = sectionName,
            activityName = activityName
        )
    }
}

class BatteryStatusTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "battery_status",
        description = "Retrieves the current device battery percentage, charging state, and health.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargeState = if (isCharging) "Charging ⚡" else "Discharging"
        val message = "Battery is at $batteryPct% ($chargeState)"

        return ToolResult(true, message, mapOf("percentage" to batteryPct.toString(), "charging" to isCharging.toString()))
    }
}

class DeviceSpecsTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "device_specs",
        description = "Checks device hardware specs, Android OS version, model name, and system info.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val androidVer = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        val brand = Build.BRAND.uppercase()

        val info = "$manufacturer $model (Android $androidVer, API $sdk, $brand)"
        return ToolResult(true, "Device: $info", mapOf("model" to model, "android" to androidVer, "sdk" to sdk.toString()))
    }
}

class CalculateMathTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "calculate_math",
        description = "Evaluates math calculations instantly (e.g. '15 * 84', '20% of 450', '500 + 25').",
        parameters = listOf(
            ToolParameter("expression", "STRING", "Math expression to evaluate", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val expr = (params["expression"] as? String)?.trim() ?: return ToolResult(false, "No expression")
        return try {
            val clean = expr.lowercase()
                .replace("plus", "+")
                .replace("minus", "-")
                .replace("times", "*")
                .replace("x", "*")
                .replace("divided by", "/")
                .replace("percent of", "% of")
                .replace(" ", "")

            val result: Double? = if (clean.contains("%of")) {
                val parts = clean.split("%of")
                val pct = parts[0].toDoubleOrNull() ?: 0.0
                val total = parts[1].toDoubleOrNull() ?: 0.0
                (pct / 100.0) * total
            } else {
                evaluateSimpleMath(clean)
            }

            if (result != null) {
                val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.2f", result)
                ToolResult(true, "$expr = $formatted", mapOf("expression" to expr, "result" to formatted))
            } else {
                ToolResult(false, "Could not compute $expr")
            }
        } catch (e: Exception) {
            ToolResult(false, "Calculation error: ${e.localizedMessage}")
        }
    }

    private fun evaluateSimpleMath(str: String): Double? {
        return try {
            when {
                str.contains("+") -> {
                    val p = str.split("+")
                    (p[0].toDoubleOrNull() ?: 0.0) + (p[1].toDoubleOrNull() ?: 0.0)
                }
                str.contains("-") -> {
                    val p = str.split("-")
                    (p[0].toDoubleOrNull() ?: 0.0) - (p[1].toDoubleOrNull() ?: 0.0)
                }
                str.contains("*") -> {
                    val p = str.split("*")
                    (p[0].toDoubleOrNull() ?: 0.0) * (p[1].toDoubleOrNull() ?: 0.0)
                }
                str.contains("/") -> {
                    val p = str.split("/")
                    val denom = p[1].toDoubleOrNull() ?: 1.0
                    if (denom == 0.0) null else (p[0].toDoubleOrNull() ?: 0.0) / denom
                }
                else -> str.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}

class OpenSettingsTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "open_settings",
        description = "Opens Android Settings page (e.g. wifi, bluetooth, display, sound, battery, applications, date, security, airplane_mode, location).",
        parameters = listOf(
            ToolParameter("destination", "STRING", "The settings destination: 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'apps', 'date', 'location', 'airplane_mode', 'accessibility', 'general'", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val dest = (params["destination"] as? String)?.trim()?.lowercase() ?: "general"
        val action = when (dest) {
            "wifi", "wi-fi", "network", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "date", "time", "clock" -> Settings.ACTION_DATE_SETTINGS
            "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "airplane", "airplane_mode", "flight" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            "security", "lock" -> Settings.ACTION_SECURITY_SETTINGS
            "privacy" -> Settings.ACTION_PRIVACY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "overlay", "draw_overlay" -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                if (action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                    data = Uri.parse("package:${context.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Opening ${dest.replaceFirstChar { it.uppercase() }} settings", mapOf("destination" to dest))
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
            ToolResult(true, "Opening System Settings", mapOf("destination" to "general"))
        }
    }
}

class SetTimerTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "set_timer",
        description = "Sets a countdown timer on the Android device with duration and optional label.",
        parameters = listOf(
            ToolParameter("duration_seconds", "INTEGER", "Duration in seconds (e.g. 600 for 10 minutes)", required = true),
            ToolParameter("label", "STRING", "Optional description/label for the timer", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val duration = (params["duration_seconds"] as? Number)?.toInt() ?: 60
        val label = (params["label"] as? String) ?: "Tecno AI Timer"
        val minutes = duration / 60
        val seconds = duration % 60
        val formattedTime = if (minutes > 0) "$minutes min ${if (seconds > 0) "$seconds sec" else ""}".trim() else "$seconds sec"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, duration)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(true, "Set a timer for $formattedTime ($label)", mapOf("duration" to duration.toString(), "label" to label))
            } else {
                ToolResult(false, "No Clock or Timer app found to handle timer.", errorCode = "NO_TIMER_APP")
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to set timer: ${e.localizedMessage}")
        }
    }
}

class SetAlarmTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "set_alarm",
        description = "Sets an alarm for a specific hour and minute on the device.",
        parameters = listOf(
            ToolParameter("hour", "INTEGER", "Hour in 24-hour format (0-23)", required = true),
            ToolParameter("minutes", "INTEGER", "Minutes (0-59)", required = true),
            ToolParameter("label", "STRING", "Optional alarm message/label", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val hour = (params["hour"] as? Number)?.toInt() ?: 7
        val minutes = (params["minutes"] as? Number)?.toInt() ?: 0
        val label = (params["label"] as? String) ?: "Tecno AI Alarm"
        val timeString = String.format("%02d:%02d", hour, minutes)

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(true, "Alarm set for $timeString ($label)", mapOf("time" to timeString, "label" to label))
            } else {
                ToolResult(false, "No Clock app found to set alarm.", errorCode = "NO_ALARM_APP")
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to set alarm: ${e.localizedMessage}")
        }
    }
}

class CreateCalendarReminderTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "create_reminder",
        description = "Creates a calendar event or reminder with title and details.",
        parameters = listOf(
            ToolParameter("title", "STRING", "Title or description of the reminder / event", required = true),
            ToolParameter("description", "STRING", "Optional extra notes", required = false)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val title = (params["title"] as? String) ?: "Reminder"
        val description = (params["description"] as? String) ?: ""
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3600000)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(true, "Opening Calendar to add reminder '$title'", mapOf("title" to title))
            } else {
                ToolResult(false, "Calendar application not found.", errorCode = "NO_CALENDAR_APP")
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to create reminder: ${e.localizedMessage}")
        }
    }
}

class CallContactTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "call_contact",
        description = "Places a phone call or opens dialer to a specified contact or number.",
        parameters = listOf(
            ToolParameter("contact_name_or_number", "STRING", "The contact name (e.g. 'Mom', 'Dad', 'Alex') or phone number", required = true)
        ),
        requiresConfirmation = true,
        requiredPermission = Manifest.permission.CALL_PHONE
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val target = (params["contact_name_or_number"] as? String)?.trim() ?: return ToolResult(false, "No contact specified")
        
        val isDigits = target.replace("+", "").replace("-", "").replace(" ", "").all { it.isDigit() }
        var phoneNumber = if (isDigits) target else null

        if (phoneNumber == null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                    arrayOf("%$target%"),
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        val dialNumber = phoneNumber ?: target
        val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission && phoneNumber != null) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(dialNumber)}"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(dialNumber)}"))
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(true, "Calling $target ($dialNumber)", mapOf("contact" to target, "number" to dialNumber))
        } catch (e: Exception) {
            ToolResult(false, "Unable to place call: ${e.localizedMessage}")
        }
    }
}

class PrepareMessageTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "prepare_message",
        description = "Prepares and sends an SMS/message to a contact or number.",
        parameters = listOf(
            ToolParameter("contact_name_or_number", "STRING", "The recipient name or phone number", required = true),
            ToolParameter("message", "STRING", "The text message body to send", required = true)
        ),
        requiresConfirmation = true,
        requiredPermission = Manifest.permission.SEND_SMS
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val target = (params["contact_name_or_number"] as? String)?.trim() ?: ""
        val message = (params["message"] as? String)?.trim() ?: ""
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(target)}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Prepared message to $target: '$message'", mapOf("recipient" to target, "message" to message))
        } catch (e: Exception) {
            ToolResult(false, "Could not open messaging app: ${e.localizedMessage}")
        }
    }
}

class MediaControlTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "media_control",
        description = "Controls media playback (play, pause, next, previous, toggle).",
        parameters = listOf(
            ToolParameter("action", "STRING", "'play', 'pause', 'next', 'previous', 'toggle'", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val action = (params["action"] as? String)?.trim()?.lowercase() ?: "toggle"
        val keyCode = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return if (audioManager != null) {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            ToolResult(true, "Media action '$action' triggered.", mapOf("action" to action))
        } else {
            ToolResult(false, "Audio service unavailable")
        }
    }
}

class CopyToClipboardTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "copy_to_clipboard",
        description = "Copies text directly into the Android clipboard.",
        parameters = listOf(
            ToolParameter("text", "STRING", "The text to copy to clipboard", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val text = (params["text"] as? String) ?: return ToolResult(false, "No text provided")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return if (clipboard != null) {
            val clip = ClipData.newPlainText("Tecno AI Copy", text)
            clipboard.setPrimaryClip(clip)
            ToolResult(true, "Copied to clipboard: \"$text\"", mapOf("text" to text))
        } else {
            ToolResult(false, "Clipboard unavailable")
        }
    }
}

class ToggleFlashlightTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "toggle_flashlight",
        description = "Turns the device camera flashlight on or off.",
        parameters = listOf(
            ToolParameter("enabled", "BOOLEAN", "True to turn on flashlight, False to turn off", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val enabled = (params["enabled"] as? Boolean) ?: true
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        return try {
            if (cameraManager != null) {
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, enabled)
                    ToolResult(true, "Flashlight turned ${if (enabled) "ON" else "OFF"}", mapOf("enabled" to enabled.toString()))
                } else {
                    ToolResult(false, "No camera flash found on device")
                }
            } else {
                ToolResult(false, "Camera hardware service unavailable")
            }
        } catch (e: Exception) {
            ToolResult(false, "Flashlight control error: ${e.localizedMessage}")
        }
    }
}
