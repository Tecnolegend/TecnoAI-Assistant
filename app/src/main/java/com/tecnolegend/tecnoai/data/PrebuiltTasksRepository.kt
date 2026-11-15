package com.tecnolegend.tecnoai.data

data class PrebuiltTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val voicePhrase: String,
    val description: String,
    val iconName: String,
    val isDeepControl: Boolean = false,
    val requiresAccessibility: Boolean = false,
    val executeIntentText: String
)

enum class TaskCategory(val displayName: String, val icon: String) {
    ALL("All", "⭐"),
    DEEP_APPS("App Control", "📱"),
    DEVICE_POWERS("Device Superpowers", "⚡"),
    GESTURES("Global Gestures", "🎮"),
    TIME_PRODUCTIVITY("Time & Alarms", "⏰"),
    AI_INTELLIGENCE("AI Consulting", "🧠"),
    MUSIC_MEDIA("Music & Video", "🎵"),
    NAVIGATION_PLACES("Maps & Navigation", "🗺️"),
    SYSTEM_DIAGNOSTICS("System Specs", "🛡️")
}

object PrebuiltTasksRepository {
    val categories = TaskCategory.values().toList()

    val tasks: List<PrebuiltTask> = listOf(
        // App Control & Deep Navigation
        PrebuiltTask(
            id = "yt_lofi",
            title = "Play Lo-Fi Beats on YouTube",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Play lofi hip hop on YouTube",
            description = "Directly launches YouTube app and streams chill lofi study beats.",
            iconName = "youtube",
            isDeepControl = true,
            executeIntentText = "play lofi hip hop on youtube"
        ),
        PrebuiltTask(
            id = "sp_workout",
            title = "Play Workout Hits on Spotify",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Play workout pump on Spotify",
            description = "Launches Spotify directly and loads high-energy workout playlist.",
            iconName = "spotify",
            isDeepControl = true,
            executeIntentText = "play workout pump on spotify"
        ),
        PrebuiltTask(
            id = "wa_eta",
            title = "WhatsApp: \"Running 5 mins late\"",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "WhatsApp Mom saying I'll be there in 5 mins",
            description = "Pre-fills a WhatsApp message to your contact without typing.",
            iconName = "whatsapp",
            isDeepControl = true,
            executeIntentText = "whatsapp Mom saying I will be there in 5 minutes"
        ),
        PrebuiltTask(
            id = "cam_selfie",
            title = "Open Front Selfie Camera",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Take a selfie",
            description = "Instantly switches to front-facing camera lens for quick photo.",
            iconName = "camera",
            isDeepControl = true,
            executeIntentText = "take a selfie"
        ),
        PrebuiltTask(
            id = "cam_video",
            title = "Open 4K Video Recorder",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Record a video",
            description = "Launches camera directly into video recording viewfinder mode.",
            iconName = "videocam",
            isDeepControl = true,
            executeIntentText = "record a video"
        ),
        PrebuiltTask(
            id = "search_tech",
            title = "Search Tech Headlines",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Search the web for Android 16 features",
            description = "Queries Google and summarizes latest mobile technology updates.",
            iconName = "public",
            executeIntentText = "search the web for Android 16 features"
        ),

        // Device Superpowers
        PrebuiltTask(
            id = "wifi_toggle",
            title = "Toggle Wi-Fi On / Off",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Toggle Wi-Fi",
            description = "Toggles Wi-Fi state or opens quick connectivity panel.",
            iconName = "wifi",
            isDeepControl = true,
            executeIntentText = "toggle wifi"
        ),
        PrebuiltTask(
            id = "wifi_status",
            title = "Check Wi-Fi Status & SSID",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Check Wi-Fi status",
            description = "Reports connection state, connected Wi-Fi network SSID, and IP.",
            iconName = "wifi",
            executeIntentText = "wifi status"
        ),
        PrebuiltTask(
            id = "phone_vibrate",
            title = "Switch Phone to Vibrate Mode",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Put phone on vibrate",
            description = "Switches system ringer profile to vibration mode instantly.",
            iconName = "volume_down",
            executeIntentText = "vibrate"
        ),
        PrebuiltTask(
            id = "vol_media_80",
            title = "Set Media Volume to 80%",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Set media volume to 80%",
            description = "Adjusts specific media playback stream to 80% volume.",
            iconName = "volume_up",
            executeIntentText = "set media volume to 80%"
        ),
        PrebuiltTask(
            id = "deep_chrome_incognito",
            title = "Open Chrome Incognito Tab",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Open incognito in Chrome",
            description = "Launches Google Chrome directly in private incognito browsing mode.",
            iconName = "public",
            isDeepControl = true,
            executeIntentText = "open incognito in chrome"
        ),
        PrebuiltTask(
            id = "deep_yt_subs",
            title = "Open YouTube Subscriptions",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Open subscriptions in YouTube",
            description = "Jumps straight to your active channel subscription feed in YouTube.",
            iconName = "youtube",
            isDeepControl = true,
            executeIntentText = "open subscriptions in youtube"
        ),
        PrebuiltTask(
            id = "deep_playstore_updates",
            title = "Check Play Store App Updates",
            category = TaskCategory.DEEP_APPS,
            voicePhrase = "Open updates in Play Store",
            description = "Opens Google Play Store direct to My Apps & Updates page.",
            iconName = "apps",
            isDeepControl = true,
            executeIntentText = "open updates in play store"
        ),
        PrebuiltTask(
            id = "torch_on",
            title = "Turn Flashlight ON",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Turn on flashlight",
            description = "Fires device LED camera flash instantly in dark environments.",
            iconName = "flashlight",
            executeIntentText = "turn on flashlight"
        ),
        PrebuiltTask(
            id = "torch_off",
            title = "Turn Flashlight OFF",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Flashlight off",
            description = "Turns off the camera LED light immediately.",
            iconName = "flashlight_off",
            executeIntentText = "flashlight off"
        ),
        PrebuiltTask(
            id = "vol_max",
            title = "Set Volume to 100% (Max)",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Max volume",
            description = "Boosts media audio stream to 100% full capacity.",
            iconName = "volume_up",
            executeIntentText = "max volume"
        ),
        PrebuiltTask(
            id = "vol_mute",
            title = "Mute Media Volume",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Mute",
            description = "Silences all media playback streams instantly.",
            iconName = "volume_off",
            executeIntentText = "mute"
        ),
        PrebuiltTask(
            id = "vol_75",
            title = "Set Volume to 75%",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Set volume to 75%",
            description = "Calibrates audio output stream to comfortable 75% level.",
            iconName = "volume_down",
            executeIntentText = "set volume to 75%"
        ),
        PrebuiltTask(
            id = "wifi_settings",
            title = "Open Wi-Fi Network Panel",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Open Wi-Fi settings",
            description = "Jumps straight to Wi-Fi connection and network manager.",
            iconName = "wifi",
            executeIntentText = "open wifi settings"
        ),
        PrebuiltTask(
            id = "bt_settings",
            title = "Open Bluetooth Settings",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Open Bluetooth settings",
            description = "Opens device Bluetooth pairing and peripheral connection screen.",
            iconName = "bluetooth",
            executeIntentText = "open bluetooth settings"
        ),
        PrebuiltTask(
            id = "battery_saver",
            title = "Open Battery Saver Settings",
            category = TaskCategory.DEVICE_POWERS,
            voicePhrase = "Open battery settings",
            description = "Directly accesses system power management and saver mode.",
            iconName = "battery_charging",
            executeIntentText = "open battery settings"
        ),

        // Global Gestures
        PrebuiltTask(
            id = "gest_back",
            title = "Perform Back Gesture",
            category = TaskCategory.GESTURES,
            voicePhrase = "Go back",
            description = "Performs global Android back navigation across any app.",
            iconName = "arrow_back",
            requiresAccessibility = true,
            executeIntentText = "go back"
        ),
        PrebuiltTask(
            id = "gest_home",
            title = "Navigate to Home Screen",
            category = TaskCategory.GESTURES,
            voicePhrase = "Go home",
            description = "Minimizes active applications and returns to Android home desktop.",
            iconName = "home",
            requiresAccessibility = true,
            executeIntentText = "go home"
        ),
        PrebuiltTask(
            id = "gest_recents",
            title = "Open App Switcher (Recents)",
            category = TaskCategory.GESTURES,
            voicePhrase = "Recent apps",
            description = "Opens multi-tasking overview showing recently used apps.",
            iconName = "view_agenda",
            requiresAccessibility = true,
            executeIntentText = "recent apps"
        ),
        PrebuiltTask(
            id = "gest_notifs",
            title = "Pull Down Notifications Shade",
            category = TaskCategory.GESTURES,
            voicePhrase = "Open notifications",
            description = "Pulls down status bar to display pending notifications.",
            iconName = "notifications",
            requiresAccessibility = true,
            executeIntentText = "open notifications"
        ),
        PrebuiltTask(
            id = "gest_screen",
            title = "Capture Screenshot",
            category = TaskCategory.GESTURES,
            voicePhrase = "Take screenshot",
            description = "Takes high-resolution snapshot of the active screen.",
            iconName = "camera_alt",
            requiresAccessibility = true,
            executeIntentText = "take screenshot"
        ),
        PrebuiltTask(
            id = "gest_lock",
            title = "Lock Device Screen",
            category = TaskCategory.GESTURES,
            voicePhrase = "Lock screen",
            description = "Secures phone and powers off display instantly.",
            iconName = "lock",
            requiresAccessibility = true,
            executeIntentText = "lock screen"
        ),

        // Time & Alarms
        PrebuiltTask(
            id = "timer_pomodoro",
            title = "25 Min Pomodoro Focus Timer",
            category = TaskCategory.TIME_PRODUCTIVITY,
            voicePhrase = "Set timer for 25 minutes labeled Focus",
            description = "Countdown timer calibrated for deep focus and studying.",
            iconName = "timer",
            executeIntentText = "set timer for 25 minutes for Focus"
        ),
        PrebuiltTask(
            id = "timer_nap",
            title = "15 Min Power Nap Timer",
            category = TaskCategory.TIME_PRODUCTIVITY,
            voicePhrase = "Set timer for 15 minutes",
            description = "Quick restorative snooze timer with wake chime.",
            iconName = "timer",
            executeIntentText = "set timer for 15 minutes"
        ),
        PrebuiltTask(
            id = "alarm_morning",
            title = "7:00 AM Morning Rise Alarm",
            category = TaskCategory.TIME_PRODUCTIVITY,
            voicePhrase = "Set an alarm for 7:00 AM",
            description = "Schedules morning wake-up alarm in device clock.",
            iconName = "alarm",
            executeIntentText = "set an alarm for 7:00 am"
        ),
        PrebuiltTask(
            id = "cal_meeting",
            title = "Schedule Meeting Reminder",
            category = TaskCategory.TIME_PRODUCTIVITY,
            voicePhrase = "Add reminder Project Review Meeting",
            description = "Creates calendar entry with alert notifications.",
            iconName = "event",
            executeIntentText = "create reminder Project Review Meeting"
        ),

        // AI Intelligence
        PrebuiltTask(
            id = "ai_owner",
            title = "Ask: \"Who is your owner and creator?\"",
            category = TaskCategory.AI_INTELLIGENCE,
            voicePhrase = "Who created Tecno AI?",
            description = "Confirms Tecnolegend as the architect and owner of Tecno AI.",
            iconName = "psychology",
            executeIntentText = "who created tecno ai?"
        ),
        PrebuiltTask(
            id = "ai_math",
            title = "Calculate Tip (18% of $142)",
            category = TaskCategory.AI_INTELLIGENCE,
            voicePhrase = "What is 18% of 142?",
            description = "Instant mathematical computation without calculator app.",
            iconName = "calculate",
            executeIntentText = "what is 18% of 142"
        ),
        PrebuiltTask(
            id = "ai_code",
            title = "Explain Quantum Computing Simply",
            category = TaskCategory.AI_INTELLIGENCE,
            voicePhrase = "Explain quantum computing in 2 sentences",
            description = "Synthesizes complex computer science using Gemini AI.",
            iconName = "auto_awesome",
            executeIntentText = "explain quantum computing in 2 sentences"
        ),

        // Maps & Navigation
        PrebuiltTask(
            id = "nav_coffee",
            title = "Find Nearest Coffee Shops",
            category = TaskCategory.NAVIGATION_PLACES,
            voicePhrase = "Find coffee near me",
            description = "Launches Maps with nearby top-rated coffee ratings.",
            iconName = "local_cafe",
            isDeepControl = true,
            executeIntentText = "find coffee near me"
        ),
        PrebuiltTask(
            id = "nav_gas",
            title = "Find Nearest Gas Station",
            category = TaskCategory.NAVIGATION_PLACES,
            voicePhrase = "Find gas stations near me",
            description = "Shows live fuel prices and route directions.",
            iconName = "local_gas_station",
            isDeepControl = true,
            executeIntentText = "find gas stations near me"
        ),
        PrebuiltTask(
            id = "nav_airport",
            title = "Turn-by-Turn GPS to Airport",
            category = TaskCategory.NAVIGATION_PLACES,
            voicePhrase = "Navigate to Airport",
            description = "Starts live voice GPS guidance with real-time traffic.",
            iconName = "navigation",
            isDeepControl = true,
            executeIntentText = "navigate to Airport"
        ),

        // Music & Media
        PrebuiltTask(
            id = "media_pause",
            title = "Pause Music Playback",
            category = TaskCategory.MUSIC_MEDIA,
            voicePhrase = "Pause music",
            description = "Dispatches universal media key event to pause current audio.",
            iconName = "pause",
            executeIntentText = "pause music"
        ),
        PrebuiltTask(
            id = "media_next",
            title = "Skip to Next Song",
            category = TaskCategory.MUSIC_MEDIA,
            voicePhrase = "Next song",
            description = "Skips to the next track on Spotify, YouTube Music, or active player.",
            iconName = "skip_next",
            executeIntentText = "next song"
        ),

        // System Diagnostics
        PrebuiltTask(
            id = "diag_battery",
            title = "Check Battery & Charging Telemetry",
            category = TaskCategory.SYSTEM_DIAGNOSTICS,
            voicePhrase = "Check battery",
            description = "Queries real-time battery percentage, charging state, and health.",
            iconName = "battery_full",
            executeIntentText = "check battery"
        ),
        PrebuiltTask(
            id = "diag_specs",
            title = "Query Hardware & Android OS Specs",
            category = TaskCategory.SYSTEM_DIAGNOSTICS,
            voicePhrase = "Phone specs",
            description = "Fetches device manufacturer, model, Android OS, and API level.",
            iconName = "memory",
            executeIntentText = "phone specs"
        )
    )

    fun searchTasks(query: String, selectedCategory: TaskCategory = TaskCategory.ALL): List<PrebuiltTask> {
        val q = query.trim().lowercase()
        return tasks.filter { task ->
            val matchesCategory = selectedCategory == TaskCategory.ALL || task.category == selectedCategory
            val matchesQuery = q.isEmpty() ||
                    task.title.lowercase().contains(q) ||
                    task.description.lowercase().contains(q) ||
                    task.voicePhrase.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }
}
