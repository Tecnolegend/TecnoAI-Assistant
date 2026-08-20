package com.example.personalization

import android.content.Context
import com.example.data.local.TargetCount
import com.example.data.local.UserActivityLogDao
import com.example.data.local.UserActivityLogEntity
import com.example.data.local.UserPreferenceDao
import com.example.data.local.UserPreferenceEntity
import com.example.data.local.UserRelationshipDao
import com.example.data.local.UserRelationshipEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.regex.Pattern

data class PersonalizedSuggestion(
    val id: String,
    val title: String,
    val subtitle: String,
    val suggestedPrompt: String,
    val icon: String, // Emoji or icon indicator
    val category: String, // "whatsapp", "spotify", "youtube", "discord", "tiktok", "phone", "routine", "device"
    val badge: String? = null
)

sealed class PersonalizationParseResult {
    data class Learned(val confirmationMessage: String) : PersonalizationParseResult()
    data class QueryResponse(val responseMessage: String) : PersonalizationParseResult()
    data class ResolvedCommand(val modifiedCommand: String, val resolvedEntities: Map<String, String>) : PersonalizationParseResult()
    object NotApplicable : PersonalizationParseResult()
}

class PersonalizationEngine(
    private val context: Context,
    private val relationshipDao: UserRelationshipDao,
    private val preferenceDao: UserPreferenceDao,
    private val activityLogDao: UserActivityLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val allRelationships: Flow<List<UserRelationshipEntity>> = relationshipDao.getAllRelationships()
    val allPreferences: Flow<List<UserPreferenceEntity>> = preferenceDao.getAllPreferences()

    suspend fun learnRelationship(
        relation: String,
        name: String,
        phone: String? = null,
        appPreference: String = "whatsapp"
    ) = withContext(Dispatchers.IO) {
        val cleanRelation = relation.trim().lowercase()
        val existing = relationshipDao.getRelationshipByKey(cleanRelation)
        val entity = UserRelationshipEntity(
            id = existing?.id ?: 0,
            relationKey = cleanRelation,
            displayName = name.trim(),
            phoneNumber = phone?.trim() ?: existing?.phoneNumber,
            appPreference = appPreference,
            updatedAt = System.currentTimeMillis()
        )
        relationshipDao.insertRelationship(entity)
    }

    suspend fun deleteRelationship(id: Int) = withContext(Dispatchers.IO) {
        relationshipDao.deleteRelationshipById(id)
    }

    suspend fun deletePreference(key: String) = withContext(Dispatchers.IO) {
        preferenceDao.deletePreference(key.trim().lowercase())
    }

    suspend fun getRelationship(relation: String): UserRelationshipEntity? = withContext(Dispatchers.IO) {
        val clean = relation.trim().lowercase()
        relationshipDao.getRelationshipByKey(clean) ?: when (clean) {
            "dad", "father", "daddy", "papa", "pops" -> relationshipDao.getRelationshipByKey("father") ?: relationshipDao.getRelationshipByKey("dad")
            "mom", "mother", "mommy", "mama", "mum" -> relationshipDao.getRelationshipByKey("mother") ?: relationshipDao.getRelationshipByKey("mom")
            "wife", "wifey", "spouse" -> relationshipDao.getRelationshipByKey("wife")
            "husband", "hubby" -> relationshipDao.getRelationshipByKey("husband")
            "brother", "bro" -> relationshipDao.getRelationshipByKey("brother")
            "sister", "sis" -> relationshipDao.getRelationshipByKey("sister")
            "best friend", "bestfriend", "bssf", "homie" -> relationshipDao.getRelationshipByKey("best_friend") ?: relationshipDao.getRelationshipByKey("best friend")
            "boss", "manager" -> relationshipDao.getRelationshipByKey("boss")
            else -> relationshipDao.getRelationshipByKey(clean)
        }
    }

    suspend fun learnPreference(key: String, value: String, category: String = "general") = withContext(Dispatchers.IO) {
        val entity = UserPreferenceEntity(
            prefKey = key.trim().lowercase(),
            prefValue = value.trim(),
            category = category,
            updatedAt = System.currentTimeMillis()
        )
        preferenceDao.insertPreference(entity)
    }

    suspend fun getPreference(key: String): String? = withContext(Dispatchers.IO) {
        preferenceDao.getPreference(key.trim().lowercase())?.prefValue
    }

    fun logActivity(type: String, target: String, detail: String? = null) {
        scope.launch {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val day = cal.get(Calendar.DAY_OF_WEEK)
            val log = UserActivityLogEntity(
                activityType = type,
                target = target.lowercase(),
                actionDetail = detail,
                hourOfDay = hour,
                dayOfWeek = day,
                timestamp = System.currentTimeMillis()
            )
            activityLogDao.insertActivity(log)
        }
    }

    /**
     * Inspects input prompt for learning sentences or queries about relationships / preferences.
     */
    suspend fun processPersonalization(prompt: String): PersonalizationParseResult = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // 1. Learning relationships: "My dad is John", "My father's name is John", "My mom is Sarah"
        val relationshipPatterns = listOf(
            Pattern.compile("^(?:remember that|note that)?\\s*my\\s+(father|dad|mother|mom|brother|sister|wife|husband|best friend|boss|son|daughter)(?:'s)?\\s+(?:name\\s+is|is|called)\\s+([a-zA-Z0-9\\s+]+)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^([a-zA-Z0-9\\s]+)\\s+is\\s+my\\s+(father|dad|mother|mom|brother|sister|wife|husband|best friend|boss|son|daughter)$", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in relationshipPatterns) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                val rel: String
                val name: String
                if (matcher.groupCount() >= 2) {
                    if (matcher.group(1).lowercase().matches(Regex("father|dad|mother|mom|brother|sister|wife|husband|best friend|boss|son|daughter"))) {
                        rel = matcher.group(1).lowercase()
                        name = matcher.group(2).trim()
                    } else {
                        name = matcher.group(1).trim()
                        rel = matcher.group(2).lowercase()
                    }
                    val normalizedRel = when (rel) {
                        "dad", "father", "daddy", "papa" -> "father"
                        "mom", "mother", "mommy", "mama", "mum" -> "mother"
                        "best friend", "bestfriend" -> "best_friend"
                        else -> rel
                    }
                    learnRelationship(normalizedRel, name)
                    val titleRel = normalizedRel.replace("_", " ").replaceFirstChar { it.uppercase() }
                    return@withContext PersonalizationParseResult.Learned(
                        "I've learned that your $titleRel is $name! I'll remember this whenever you ask me to message, call, or contact them."
                    )
                }
            }
        }

        // Phone number learning: "My dad's number is +123456789", "Mom's phone number is 9876543210"
        val phonePattern = Pattern.compile("^(?:my\\s+)?(father|dad|mother|mom|brother|sister|wife|husband|best friend|boss)(?:'s)?\\s+(?:phone\\s+number|number|phone)\\s+is\\s+([+0-9\\s-]+)$", Pattern.CASE_INSENSITIVE)
        val phoneMatcher = phonePattern.matcher(trimmed)
        if (phoneMatcher.find()) {
            val rel = phoneMatcher.group(1).lowercase()
            val phone = phoneMatcher.group(2).replace(" ", "").trim()
            val normalizedRel = when (rel) {
                "dad", "father", "daddy" -> "father"
                "mom", "mother", "mommy" -> "mother"
                "best friend", "bestfriend" -> "best_friend"
                else -> rel
            }
            val existing = getRelationship(normalizedRel)
            val name = existing?.displayName ?: normalizedRel.replaceFirstChar { it.uppercase() }
            learnRelationship(normalizedRel, name, phone)
            return@withContext PersonalizationParseResult.Learned(
                "Got it! Saved phone number $phone for your ${normalizedRel.replace('_', ' ')} ($name)."
            )
        }

        // 2. Learning preferences: "My favorite music is Synthwave", "My favorite song is Starboy", "My favorite food is Pizza"
        val prefPatterns = listOf(
            Pattern.compile("^(?:remember that|note that)?\\s*my\\s+favorite\\s+(music|song|artist|band|app|game|food|hobby|color|movie|sport)\\s+is\\s+(.+)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^i\\s+love\\s+(?:listening to|playing|using)?\\s*(.+)$", Pattern.CASE_INSENSITIVE)
        )

        val prefMatcher = prefPatterns[0].matcher(trimmed)
        if (prefMatcher.find()) {
            val key = "favorite_" + prefMatcher.group(1).lowercase().trim()
            val value = prefMatcher.group(2).trim()
            val cat = when (prefMatcher.group(1).lowercase()) {
                "music", "song", "artist", "band" -> "music"
                "app", "game" -> "apps"
                else -> "personal"
            }
            learnPreference(key, value, cat)
            val cleanKeyName = prefMatcher.group(1).lowercase()
            return@withContext PersonalizationParseResult.Learned(
                "Noted! I've saved that your favorite $cleanKeyName is \"$value\". I'll tailor music and recommendations accordingly."
            )
        }

        // 3. Asking about memory: "Who is my father?", "Who is my dad?", "Who is my mother?", "What is my favorite music?"
        val queryRelPattern = Pattern.compile("^(?:who\\s+is|who's|tell\\s+me)\\s+my\\s+(father|dad|mother|mom|brother|sister|wife|husband|best friend|boss|family)\\??$", Pattern.CASE_INSENSITIVE)
        val queryRelMatcher = queryRelPattern.matcher(trimmed)
        if (queryRelMatcher.find()) {
            val targetRel = queryRelMatcher.group(1).lowercase()
            val found = getRelationship(targetRel)
            return@withContext if (found != null) {
                val phoneInfo = if (found.phoneNumber != null) " (Phone: ${found.phoneNumber})" else ""
                PersonalizationParseResult.QueryResponse("Your ${targetRel.replace('_', ' ')} is ${found.displayName}$phoneInfo.")
            } else {
                PersonalizationParseResult.QueryResponse("You haven't told me who your $targetRel is yet. You can say \"My $targetRel is [Name]\" to teach me!")
            }
        }

        val queryPrefPattern = Pattern.compile("^(?:what\\s+is|what's)\\s+my\\s+favorite\\s+(music|song|artist|app|game|food|hobby|color)\\??$", Pattern.CASE_INSENSITIVE)
        val queryPrefMatcher = queryPrefPattern.matcher(trimmed)
        if (queryPrefMatcher.find()) {
            val prefType = queryPrefMatcher.group(1).lowercase()
            val value = getPreference("favorite_$prefType")
            return@withContext if (value != null) {
                PersonalizationParseResult.QueryResponse("Your favorite $prefType is \"$value\".")
            } else {
                PersonalizationParseResult.QueryResponse("I don't know your favorite $prefType yet. Tell me \"My favorite $prefType is [Something]\" anytime!")
            }
        }

        // 4. Resolving relationship pronouns inside action commands:
        // "Send WhatsApp to my dad: Hello", "Call my mom", "Send message to my father on whatsapp"
        var resolvedCommand = trimmed
        val resolvedMap = mutableMapOf<String, String>()

        val relKeywords = listOf("my father", "my dad", "my mother", "my mom", "my wife", "my husband", "my brother", "my sister", "my best friend", "my boss")
        for (kw in relKeywords) {
            if (lower.contains(kw)) {
                val cleanKey = kw.removePrefix("my ").trim()
                val found = getRelationship(cleanKey)
                if (found != null) {
                    resolvedCommand = resolvedCommand.replace(Regex("(?i)\\b$kw\\b"), found.displayName)
                    resolvedMap[kw] = found.displayName
                }
            }
        }

        if (resolvedMap.isNotEmpty()) {
            return@withContext PersonalizationParseResult.ResolvedCommand(
                modifiedCommand = resolvedCommand,
                resolvedEntities = resolvedMap
            )
        }

        PersonalizationParseResult.NotApplicable
    }

    /**
     * Generates real-time, context-aware personalized suggestion chips based on:
     * - Current time of day
     * - User's saved favorites
     * - Learned family / contacts
     * - Frequent activity patterns
     */
    fun getPersonalizedSuggestionsFlow(): Flow<List<PersonalizedSuggestion>> = flow {
        val suggestions = mutableListOf<PersonalizedSuggestion>()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        // 1. Time-of-Day Adaptive Suggestion
        when (hour) {
            in 5..11 -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "morning_brief",
                        title = "Morning Briefing",
                        subtitle = "Check battery, weather & alarm",
                        suggestedPrompt = "What is my battery level and status?",
                        icon = "☀️",
                        category = "routine",
                        badge = "Morning"
                    )
                )
            }
            in 12..17 -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "afternoon_focus",
                        title = "Quick Discord & Work",
                        subtitle = "Check latest server messages",
                        suggestedPrompt = "Open Discord",
                        icon = "💬",
                        category = "discord",
                        badge = "Productivity"
                    )
                )
            }
            in 18..22 -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "evening_relax",
                        title = "Evening Chill",
                        subtitle = "Watch trending YouTube & Spotify",
                        suggestedPrompt = "Open YouTube",
                        icon = "📺",
                        category = "youtube",
                        badge = "Relax"
                    )
                )
            }
            else -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "night_mode",
                        title = "Night Rest",
                        subtitle = "Turn off flashlight & set alarm",
                        suggestedPrompt = "Turn off flashlight and set timer for 8 hours",
                        icon = "🌙",
                        category = "device",
                        badge = "Night"
                    )
                )
            }
        }

        // 2. Favorite Music Preference Integration
        val favMusic = getPreference("favorite_music") ?: getPreference("favorite_song") ?: getPreference("favorite_artist")
        if (favMusic != null) {
            suggestions.add(
                PersonalizedSuggestion(
                    id = "fav_music",
                    title = "Play $favMusic",
                    subtitle = "Your favorite music on Spotify",
                    suggestedPrompt = "Play $favMusic on Spotify",
                    icon = "🎵",
                    category = "spotify",
                    badge = "Favorite"
                )
            )
        } else {
            suggestions.add(
                PersonalizedSuggestion(
                    id = "spotify_top",
                    title = "Play Top Hits",
                    subtitle = "Stream music on Spotify",
                    suggestedPrompt = "Play music on Spotify",
                    icon = "🎵",
                    category = "spotify"
                )
            )
        }

        // 3. Learned Contact Shortcuts (Dad, Mom, Wife, Best Friend)
        val dad = getRelationship("father") ?: getRelationship("dad")
        val mom = getRelationship("mother") ?: getRelationship("mom")
        val wife = getRelationship("wife")
        val bestFriend = getRelationship("best_friend")

        when {
            dad != null -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "contact_dad",
                        title = "WhatsApp ${dad.displayName}",
                        subtitle = "Quick message to your Dad",
                        suggestedPrompt = "Send WhatsApp to ${dad.displayName}: Hello Dad",
                        icon = "💚",
                        category = "whatsapp",
                        badge = "Family"
                    )
                )
            }
            mom != null -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "contact_mom",
                        title = "Call Mom (${mom.displayName})",
                        subtitle = "Stay connected with family",
                        suggestedPrompt = "Call ${mom.displayName}",
                        icon = "📞",
                        category = "phone",
                        badge = "Family"
                    )
                )
            }
            wife != null -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "contact_wife",
                        title = "Message ${wife.displayName}",
                        subtitle = "Send WhatsApp message to your wife",
                        suggestedPrompt = "Send WhatsApp to ${wife.displayName}: Love you",
                        icon = "❤️",
                        category = "whatsapp",
                        badge = "Family"
                    )
                )
            }
            bestFriend != null -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "contact_bff",
                        title = "Chat with ${bestFriend.displayName}",
                        subtitle = "Send a message to your best friend",
                        suggestedPrompt = "Send WhatsApp to ${bestFriend.displayName}: What's up?",
                        icon = "🔥",
                        category = "whatsapp",
                        badge = "Friend"
                    )
                )
            }
            else -> {
                suggestions.add(
                    PersonalizedSuggestion(
                        id = "whatsapp_quick",
                        title = "Send WhatsApp Message",
                        subtitle = "Message any contact instantly",
                        suggestedPrompt = "Open WhatsApp",
                        icon = "💬",
                        category = "whatsapp"
                    )
                )
            }
        }

        // 4. Specialized Apps (YouTube Shorts, Discord, TikTok, Google)
        suggestions.add(
            PersonalizedSuggestion(
                id = "youtube_shorts",
                title = "Watch YouTube Shorts",
                subtitle = "Browse popular video shorts",
                suggestedPrompt = "Open YouTube Shorts",
                icon = "▶️",
                category = "youtube"
            )
        )
        suggestions.add(
            PersonalizedSuggestion(
                id = "tiktok_explore",
                title = "Explore TikTok Trends",
                subtitle = "Open TikTok feed and scroll videos",
                suggestedPrompt = "Open TikTok",
                icon = "📱",
                category = "tiktok"
            )
        )
        suggestions.add(
            PersonalizedSuggestion(
                id = "discord_servers",
                title = "Open Discord",
                subtitle = "Check server channels and direct messages",
                suggestedPrompt = "Open Discord",
                icon = "🎮",
                category = "discord"
            )
        )

        emit(suggestions)
    }.flowOn(Dispatchers.IO)
}
