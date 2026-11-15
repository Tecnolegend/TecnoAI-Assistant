package com.tecnolegend.tecnoai.tools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

class SystemIntentActionHandler(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val packageManager = context.packageManager

    // ==========================================
    // 1. WI-FI CONTROL & INTENT OPERATIONS
    // ==========================================

    @Suppress("DEPRECATION")
    fun handleWifiAction(action: String): ToolResult {
        val normalizedAction = action.trim().lowercase()
        return try {
            when (normalizedAction) {
                "on", "enable", "turn_on" -> {
                    var directSuccess = false
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager != null) {
                        try {
                            directSuccess = wifiManager.setWifiEnabled(true)
                        } catch (e: Exception) {}
                    }
                    if (directSuccess) {
                        ToolResult(true, "Wi-Fi turned ON", mapOf("status" to "enabled"))
                    } else {
                        launchWifiPanelOrSettings()
                        ToolResult(true, "Opening Wi-Fi control panel to enable Wi-Fi", mapOf("status" to "panel_opened"))
                    }
                }
                "off", "disable", "turn_off" -> {
                    var directSuccess = false
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager != null) {
                        try {
                            directSuccess = wifiManager.setWifiEnabled(false)
                        } catch (e: Exception) {}
                    }
                    if (directSuccess) {
                        ToolResult(true, "Wi-Fi turned OFF", mapOf("status" to "disabled"))
                    } else {
                        launchWifiPanelOrSettings()
                        ToolResult(true, "Opening Wi-Fi control panel to disable Wi-Fi", mapOf("status" to "panel_opened"))
                    }
                }
                "toggle", "switch" -> {
                    val isEnabled = wifiManager?.isWifiEnabled ?: false
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager != null) {
                        wifiManager.setWifiEnabled(!isEnabled)
                        ToolResult(true, "Wi-Fi toggled to ${if (!isEnabled) "ON" else "OFF"}", mapOf("status" to (!isEnabled).toString()))
                    } else {
                        launchWifiPanelOrSettings()
                        ToolResult(true, "Opening Wi-Fi control panel (current status: ${if (isEnabled) "ON" else "OFF"})", mapOf("status" to isEnabled.toString()))
                    }
                }
                "status", "check", "info" -> {
                    val isEnabled = wifiManager?.isWifiEnabled ?: false
                    val connectionInfo = wifiManager?.connectionInfo
                    val ssid = connectionInfo?.ssid?.replace("\"", "") ?: "None"
                    val ip = formatIpAddress(connectionInfo?.ipAddress ?: 0)
                    val statusText = if (isEnabled) {
                        if (ssid != "<unknown ssid>" && ssid != "None" && ssid.isNotBlank()) {
                            "Wi-Fi is ON and connected to \"$ssid\" (IP: $ip)"
                        } else {
                            "Wi-Fi is ON (not connected to a network)"
                        }
                    } else {
                        "Wi-Fi is currently turned OFF"
                    }
                    ToolResult(true, statusText, mapOf("enabled" to isEnabled.toString(), "ssid" to ssid, "ip" to ip))
                }
                "panel", "settings", "open" -> {
                    launchWifiPanelOrSettings()
                    ToolResult(true, "Opened Wi-Fi settings panel", mapOf("action" to "settings_opened"))
                }
                else -> {
                    launchWifiPanelOrSettings()
                    ToolResult(true, "Opened Wi-Fi settings", mapOf("action" to "settings_opened"))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to execute Wi-Fi action: ${e.localizedMessage}")
        }
    }

    private fun launchWifiPanelOrSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                return
            } catch (e: Exception) {}
        }
        val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(settingsIntent)
    }

    private fun formatIpAddress(ip: Int): String {
        return if (ip == 0) "0.0.0.0" else "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }

    // ==========================================
    // 2. DEEP VOLUME & AUDIO STREAM OPERATIONS
    // ==========================================

    fun handleVolumeAction(
        action: String,
        streamType: String? = "media",
        levelPercent: Int? = null,
        deltaPercent: Int? = null
    ): ToolResult {
        if (audioManager == null) return ToolResult(false, "Audio service unavailable")

        val stream = when (streamType?.lowercase()?.trim()) {
            "ring", "ringer", "call_ring", "phone" -> AudioManager.STREAM_RING
            "notification", "notifications", "alerts", "alert" -> AudioManager.STREAM_NOTIFICATION
            "alarm", "alarms" -> AudioManager.STREAM_ALARM
            "voice_call", "call", "voice", "incall" -> AudioManager.STREAM_VOICE_CALL
            "system", "ui" -> AudioManager.STREAM_SYSTEM
            else -> AudioManager.STREAM_MUSIC
        }
        val streamName = when (stream) {
            AudioManager.STREAM_RING -> "Ring"
            AudioManager.STREAM_NOTIFICATION -> "Notification"
            AudioManager.STREAM_ALARM -> "Alarm"
            AudioManager.STREAM_VOICE_CALL -> "Voice Call"
            AudioManager.STREAM_SYSTEM -> "System"
            else -> "Media"
        }

        val maxVol = audioManager.getStreamMaxVolume(stream)
        val currentVol = audioManager.getStreamVolume(stream)
        val currentPercent = if (maxVol > 0) ((currentVol.toFloat() / maxVol) * 100).toInt() else 0

        return try {
            when (action.lowercase().trim()) {
                "set" -> {
                    val targetPct = (levelPercent ?: 50).coerceIn(0, 100)
                    val targetVol = ((targetPct / 100f) * maxVol).toInt().coerceIn(0, maxVol)
                    audioManager.setStreamVolume(stream, targetVol, AudioManager.FLAG_SHOW_UI)
                    ToolResult(
                        true,
                        "$streamName volume set to $targetPct% (level $targetVol/$maxVol)",
                        mapOf("stream" to streamName, "level" to targetPct.toString())
                    )
                }
                "up", "raise", "increase" -> {
                    if (deltaPercent != null && deltaPercent > 0) {
                        val newPct = (currentPercent + deltaPercent).coerceIn(0, 100)
                        val targetVol = ((newPct / 100f) * maxVol).toInt().coerceIn(0, maxVol)
                        audioManager.setStreamVolume(stream, targetVol, AudioManager.FLAG_SHOW_UI)
                        ToolResult(
                            true,
                            "$streamName volume increased to $newPct%",
                            mapOf("stream" to streamName, "level" to newPct.toString())
                        )
                    } else {
                        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        val newVol = audioManager.getStreamVolume(stream)
                        val newPct = if (maxVol > 0) ((newVol.toFloat() / maxVol) * 100).toInt() else 0
                        ToolResult(
                            true,
                            "$streamName volume increased to $newPct%",
                            mapOf("stream" to streamName, "level" to newPct.toString())
                        )
                    }
                }
                "down", "lower", "decrease" -> {
                    if (deltaPercent != null && deltaPercent > 0) {
                        val newPct = (currentPercent - deltaPercent).coerceIn(0, 100)
                        val targetVol = ((newPct / 100f) * maxVol).toInt().coerceIn(0, maxVol)
                        audioManager.setStreamVolume(stream, targetVol, AudioManager.FLAG_SHOW_UI)
                        ToolResult(
                            true,
                            "$streamName volume lowered to $newPct%",
                            mapOf("stream" to streamName, "level" to newPct.toString())
                        )
                    } else {
                        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        val newVol = audioManager.getStreamVolume(stream)
                        val newPct = if (maxVol > 0) ((newVol.toFloat() / maxVol) * 100).toInt() else 0
                        ToolResult(
                            true,
                            "$streamName volume lowered to $newPct%",
                            mapOf("stream" to streamName, "level" to newPct.toString())
                        )
                    }
                }
                "mute", "silence" -> {
                    audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                    ToolResult(true, "$streamName volume muted", mapOf("stream" to streamName, "level" to "0"))
                }
                "unmute" -> {
                    val restoreVol = (maxVol * 0.5f).toInt().coerceAtLeast(1)
                    audioManager.setStreamVolume(stream, restoreVol, AudioManager.FLAG_SHOW_UI)
                    ToolResult(true, "$streamName volume restored to 50%", mapOf("stream" to streamName, "level" to "50"))
                }
                "max", "full" -> {
                    audioManager.setStreamVolume(stream, maxVol, AudioManager.FLAG_SHOW_UI)
                    ToolResult(true, "$streamName volume set to maximum (100%)", mapOf("stream" to streamName, "level" to "100"))
                }
                "vibrate", "vibration" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    ToolResult(true, "Phone set to Vibrate mode", mapOf("ringer_mode" to "vibrate"))
                }
                "silent_mode", "silent" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    ToolResult(true, "Phone set to Silent mode", mapOf("ringer_mode" to "silent"))
                }
                "normal_mode", "normal" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    ToolResult(true, "Phone set to Normal sound mode", mapOf("ringer_mode" to "normal"))
                }
                "status", "check" -> {
                    val mediaPct = ((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) * 100).toInt()
                    val ringPct = ((audioManager.getStreamVolume(AudioManager.STREAM_RING).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)) * 100).toInt()
                    val alarmPct = ((audioManager.getStreamVolume(AudioManager.STREAM_ALARM).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)) * 100).toInt()
                    val ringerModeName = when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                        AudioManager.RINGER_MODE_SILENT -> "Silent"
                        else -> "Normal"
                    }
                    val summary = "Volume Levels: Media $mediaPct%, Ring $ringPct%, Alarm $alarmPct% • Ringer Mode: $ringerModeName"
                    ToolResult(true, summary, mapOf("media" to "$mediaPct%", "ring" to "$ringPct%", "alarm" to "$alarmPct%", "ringer" to ringerModeName))
                }
                "panel" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val intent = Intent(Settings.Panel.ACTION_VOLUME).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                            return ToolResult(true, "Opening volume control panel", mapOf("action" to "panel_opened"))
                        } catch (e: Exception) {}
                    }
                    val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                    ToolResult(true, "Opening Sound & Vibration settings", mapOf("action" to "settings_opened"))
                }
                else -> ToolResult(false, "Unknown volume operation: $action")
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to adjust volume: ${e.localizedMessage}")
        }
    }

    // =========================================================
    // 3. LAUNCH SPECIFIC INSTALLED APP ACTIVITIES VIA INTENT
    // =========================================================

    fun handleLaunchAppActivity(
        appName: String? = null,
        activityName: String? = null,
        actionName: String? = null,
        sectionName: String? = null,
        extraData: Map<String, String>? = null
    ): ToolResult {
        val targetApp = appName?.trim()?.lowercase() ?: ""
        val targetSection = sectionName?.trim()?.lowercase() ?: ""
        val targetActivity = activityName?.trim() ?: ""

        // 1. Direct Component Name execution if explicit activity class is supplied
        if (targetActivity.isNotBlank() && targetActivity.contains("/")) {
            try {
                val parts = targetActivity.split("/")
                val pkg = parts[0]
                val act = if (parts[1].startsWith(".")) "$pkg${parts[1]}" else parts[1]
                val intent = Intent().apply {
                    component = ComponentName(pkg, act)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    extraData?.forEach { (k, v) -> putExtra(k, v) }
                }
                context.startActivity(intent)
                return ToolResult(true, "Launched activity $targetActivity", mapOf("component" to targetActivity))
            } catch (e: Exception) {
                // Fallback to query matching below
            }
        }

        // 2. Pre-mapped deep app activity intent shortcuts
        val shortcutResult = executePredefinedAppShortcut(targetApp, targetSection, actionName, extraData)
        if (shortcutResult != null) return shortcutResult

        // 3. Deep App Info / App Details Settings
        if (targetSection.contains("info") || targetSection.contains("details") || targetSection.contains("permissions") || targetSection.contains("storage")) {
            val pkg = resolvePackageName(targetApp)
            if (pkg != null) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$pkg")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult(true, "Opening App Info & Permissions for $targetApp", mapOf("app" to targetApp, "package" to pkg))
            }
        }

        // 4. Dynamic Package & Activity Intent Search
        val dynamicResult = executeDynamicActivityMatch(targetApp, targetSection, targetActivity)
        if (dynamicResult != null) return dynamicResult

        // 5. Fallback: Launch the main app launcher intent
        val pkg = resolvePackageName(targetApp)
        if (pkg != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return ToolResult(true, "Opening $targetApp", mapOf("app" to targetApp, "package" to pkg))
            }
        }

        return ToolResult(false, "Could not find activity for '$appName' ${if (sectionName != null) "($sectionName)" else ""}.", errorCode = "ACTIVITY_NOT_FOUND")
    }

    private fun executePredefinedAppShortcut(
        targetApp: String,
        targetSection: String,
        actionName: String?,
        extraData: Map<String, String>?
    ): ToolResult? {
        try {
            // YouTube Deep Sections
            if (targetApp.contains("youtube")) {
                when {
                    targetSection.contains("subscription") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/subscriptions")).apply {
                            setPackage("com.google.android.youtube")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening YouTube Subscriptions", mapOf("section" to "subscriptions"))
                        }
                    }
                    targetSection.contains("history") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/history")).apply {
                            setPackage("com.google.android.youtube")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening YouTube Watch History", mapOf("section" to "history"))
                        }
                    }
                    targetSection.contains("library") || targetSection.contains("playlist") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/library")).apply {
                            setPackage("com.google.android.youtube")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening YouTube Library", mapOf("section" to "library"))
                        }
                    }
                    targetSection.contains("short") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/shorts")).apply {
                            setPackage("com.google.android.youtube")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening YouTube Shorts", mapOf("section" to "shorts"))
                        }
                    }
                }
            }

            // Chrome Deep Activities
            if (targetApp.contains("chrome") || targetApp.contains("browser")) {
                when {
                    targetSection.contains("incognito") || targetSection.contains("private") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                            setPackage("com.android.chrome")
                            putExtra("com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB", true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening Chrome Incognito Tab", mapOf("section" to "incognito"))
                        }
                    }
                    targetSection.contains("download") -> {
                        val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        return ToolResult(true, "Opening Downloads", mapOf("section" to "downloads"))
                    }
                    targetSection.contains("history") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("chrome://history")).apply {
                            setPackage("com.android.chrome")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening Chrome History", mapOf("section" to "history"))
                        }
                    }
                    targetSection.contains("bookmark") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("chrome://bookmarks")).apply {
                            setPackage("com.android.chrome")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening Chrome Bookmarks", mapOf("section" to "bookmarks"))
                        }
                    }
                }
            }

            // Play Store Deep Activities
            if (targetApp.contains("play store") || targetApp.contains("market") || targetApp.contains("google play")) {
                when {
                    targetSection.contains("update") || targetSection.contains("my app") || targetSection.contains("installed") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/apps")).apply {
                            setPackage("com.android.vending")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening My Apps & Updates in Play Store", mapOf("section" to "updates"))
                        }
                    }
                    targetSection.contains("subscription") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")).apply {
                            setPackage("com.android.vending")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                            return ToolResult(true, "Opening Subscriptions in Play Store", mapOf("section" to "subscriptions"))
                        }
                    }
                }
            }

            // System Deep Settings Sub-activities
            if (targetApp.contains("setting") || targetSection.isNotBlank()) {
                val action = when {
                    targetSection.contains("hotspot") || targetSection.contains("tethering") -> Settings.ACTION_WIRELESS_SETTINGS
                    targetSection.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
                    targetSection.contains("display") || targetSection.contains("brightness") -> Settings.ACTION_DISPLAY_SETTINGS
                    targetSection.contains("sound") || targetSection.contains("volume") || targetSection.contains("vibration") -> Settings.ACTION_SOUND_SETTINGS
                    targetSection.contains("battery") || targetSection.contains("power") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                    targetSection.contains("storage") || targetSection.contains("memory") -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                    targetSection.contains("nfc") -> Settings.ACTION_NFC_SETTINGS
                    targetSection.contains("developer") -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                    targetSection.contains("vpn") -> Settings.ACTION_VPN_SETTINGS
                    targetSection.contains("notification") -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                    targetSection.contains("accessibility") -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                    targetSection.contains("location") || targetSection.contains("gps") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    targetSection.contains("date") || targetSection.contains("time") -> Settings.ACTION_DATE_SETTINGS
                    targetSection.contains("security") -> Settings.ACTION_SECURITY_SETTINGS
                    targetSection.contains("privacy") -> Settings.ACTION_PRIVACY_SETTINGS
                    targetSection.contains("airplane") || targetSection.contains("flight") -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
                    targetSection.contains("default app") -> Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
                    else -> null
                }
                if (action != null) {
                    val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    if (intent.resolveActivity(packageManager) != null) {
                        context.startActivity(intent)
                        return ToolResult(true, "Opening ${targetSection.replaceFirstChar { it.uppercase() }} Settings", mapOf("section" to targetSection))
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun executeDynamicActivityMatch(
        targetApp: String,
        targetSection: String,
        targetActivity: String
    ): ToolResult? {
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            val activities = packageManager.queryIntentActivities(mainIntent, 0)

            for (info in activities) {
                val actPkg = info.activityInfo.packageName.lowercase()
                val actName = info.activityInfo.name.lowercase()
                val actLabel = info.loadLabel(packageManager).toString().lowercase()

                val appMatches = targetApp.isEmpty() || actPkg.contains(targetApp) || actLabel.contains(targetApp)
                val sectionMatches = targetSection.isEmpty() || actName.contains(targetSection) || actLabel.contains(targetSection)
                val explicitMatches = targetActivity.isNotEmpty() && actName.contains(targetActivity.lowercase())

                if ((appMatches && sectionMatches) || explicitMatches) {
                    val launchIntent = Intent().apply {
                        component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(launchIntent)
                    return ToolResult(
                        true,
                        "Launched ${info.loadLabel(packageManager)} (${info.activityInfo.name})",
                        mapOf("package" to info.activityInfo.packageName, "activity" to info.activityInfo.name)
                    )
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun resolvePackageName(appName: String): String? {
        val clean = appName.lowercase().trim()
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
            "settings" to "com.android.settings"
        )
        if (knownPackages.containsKey(clean)) return knownPackages[clean]
        if (clean.contains(".")) return clean

        // Search installed packages
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps = packageManager.queryIntentActivities(mainIntent, 0)
            for (app in apps) {
                val label = app.loadLabel(packageManager).toString().lowercase()
                if (label.contains(clean) || clean.contains(label)) {
                    return app.activityInfo.packageName
                }
            }
        } catch (e: Exception) {}
        return null
    }
}
