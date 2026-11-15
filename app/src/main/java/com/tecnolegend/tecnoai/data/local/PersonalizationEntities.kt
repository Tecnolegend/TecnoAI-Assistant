package com.tecnolegend.tecnoai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores learned human relationships & contact aliases.
 * (e.g. father -> "John Doe", mother -> "Sarah", mom -> "Sarah", boss -> "Michael", best friend -> "Alex")
 */
@Entity(tableName = "user_relationships")
data class UserRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val relationKey: String, // "father", "mother", "mom", "dad", "brother", "sister", "wife", "husband", "best_friend", "boss", "friend"
    val displayName: String,
    val phoneNumber: String? = null,
    val nickname: String? = null,
    val appPreference: String? = "whatsapp", // "whatsapp", "call", "sms", "messenger", "discord"
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Stores learned user preferences & favorite things.
 * (e.g. favorite_music -> "synthwave / The Weeknd", favorite_app -> "Spotify", favorite_game -> "PUBG", morning_routine -> "check weather and news")
 */
@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey
    val prefKey: String, // "favorite_music", "favorite_app", "favorite_artist", "favorite_game", "favorite_hobby", "home_city", "morning_routine"
    val prefValue: String,
    val category: String = "general", // "music", "apps", "interests", "routine", "personal"
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Logs user interaction frequency and time-of-day habits for smart personalized suggestions.
 */
@Entity(tableName = "user_activity_logs")
data class UserActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val activityType: String, // "app_launch", "tool_execution", "voice_query", "messaging", "media_play"
    val target: String, // "whatsapp", "spotify", "youtube", "discord", "tiktok", "dad", "mom"
    val actionDetail: String? = null,
    val hourOfDay: Int = 0, // 0..23
    val dayOfWeek: Int = 1, // Calendar.DAY_OF_WEEK
    val timestamp: Long = System.currentTimeMillis()
)
