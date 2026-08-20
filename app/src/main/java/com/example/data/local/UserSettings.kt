package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tecno_user_settings")

data class UserSettings(
    val isWakeWordEnabled: Boolean = true,
    val isTtsEnabled: Boolean = true,
    val isHapticEnabled: Boolean = true,
    val confirmSensitiveActions: Boolean = true,
    val selectedModel: String = "gemini-3.5-flash",
    val wakeWordSensitivity: Float = 0.5f,
    val hasCompletedOnboarding: Boolean = false,
    val autoCloseOverlayOnSuccess: Boolean = false,
    val userCustomName: String = "User"
)

class UserSettingsManager(private val context: Context) {
    companion object {
        val KEY_WAKE_WORD = booleanPreferencesKey("wake_word_enabled")
        val KEY_TTS = booleanPreferencesKey("tts_enabled")
        val KEY_HAPTIC = booleanPreferencesKey("haptic_enabled")
        val KEY_CONFIRM_SENSITIVE = booleanPreferencesKey("confirm_sensitive_actions")
        val KEY_MODEL = stringPreferencesKey("gemini_model")
        val KEY_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
        val KEY_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val KEY_AUTO_CLOSE = booleanPreferencesKey("auto_close_overlay")
        val KEY_USER_NAME = stringPreferencesKey("user_custom_name")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            isWakeWordEnabled = preferences[KEY_WAKE_WORD] ?: true,
            isTtsEnabled = preferences[KEY_TTS] ?: true,
            isHapticEnabled = preferences[KEY_HAPTIC] ?: true,
            confirmSensitiveActions = preferences[KEY_CONFIRM_SENSITIVE] ?: true,
            selectedModel = preferences[KEY_MODEL] ?: "gemini-3.5-flash",
            wakeWordSensitivity = preferences[KEY_SENSITIVITY] ?: 0.5f,
            hasCompletedOnboarding = preferences[KEY_ONBOARDING] ?: false,
            autoCloseOverlayOnSuccess = preferences[KEY_AUTO_CLOSE] ?: false,
            userCustomName = preferences[KEY_USER_NAME] ?: "User"
        )
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD] = enabled }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TTS] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC] = enabled }
    }

    suspend fun setConfirmSensitiveActions(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CONFIRM_SENSITIVE] = enabled }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { it[KEY_MODEL] = model }
    }

    suspend fun setWakeWordSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[KEY_SENSITIVITY] = sensitivity }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = completed }
    }

    suspend fun setAutoCloseOverlay(autoClose: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CLOSE] = autoClose }
    }

    suspend fun setUserCustomName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }
}
