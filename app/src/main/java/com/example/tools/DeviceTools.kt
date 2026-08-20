package com.example.tools

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import android.media.AudioManager
import androidx.core.content.ContextCompat

class OpenAppTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "open_app",
        description = "Opens an installed Android application by package name or common app name (e.g. YouTube, Chrome, Camera, WhatsApp, Spotify, Settings).",
        parameters = listOf(
            ToolParameter("app_name", "STRING", "The common name or package name of the app to launch (e.g. 'YouTube', 'Chrome', 'Settings', 'com.google.android.youtube')", required = true)
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
            "whatsapp" to "com.whatsapp",
            "spotify" to "com.spotify.music",
            "instagram" to "com.instagram.android",
            "play store" to "com.android.vending",
            "messages" to "com.google.android.apps.messaging"
        )

        val targetQuery = appName.lowercase()

        if (targetQuery == "camera") {
            val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
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
        description = "Performs a web search using Google or browser.",
        parameters = listOf(
            ToolParameter("query", "STRING", "Search query terms (e.g. 'today weather', 'latest tech news')", required = true)
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

class OpenSettingsTool : DeviceTool {
    override val definition = ToolDefinition(
        name = "open_settings",
        description = "Opens Android Settings page (e.g. wifi, bluetooth, display, sound, battery, applications, date, security, airplane_mode, location).",
        parameters = listOf(
            ToolParameter("destination", "STRING", "The settings destination: 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'apps', 'date', 'location', 'airplane_mode', 'general'", required = true)
        )
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val dest = (params["destination"] as? String)?.trim()?.lowercase() ?: "general"
        val action = when (dest) {
            "wifi", "wi-fi", "network" -> Settings.ACTION_WIFI_SETTINGS
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
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
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
            ToolResult(true, "Copied text to clipboard: \"$text\"", mapOf("text" to text))
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
