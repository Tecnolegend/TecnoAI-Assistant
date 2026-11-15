package com.tecnolegend.tecnoai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.tecnolegend.tecnoai.data.local.TecnoDatabase
import com.tecnolegend.tecnoai.data.local.UserSettingsManager
import com.tecnolegend.tecnoai.data.repository.AssistantRepository

class TecnoAiApp : Application() {

    val database: TecnoDatabase by lazy { TecnoDatabase.getDatabase(this) }
    val userSettingsManager: UserSettingsManager by lazy { UserSettingsManager(this) }
    val repository: AssistantRepository by lazy {
        AssistantRepository(database.conversationDao(), userSettingsManager, this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val wakeChannel = NotificationChannel(
                WAKE_WORD_CHANNEL_ID,
                "Tecno AI Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active background listening for Hey Tecno wake phrase"
                setShowBadge(false)
            }
            manager.createNotificationChannel(wakeChannel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Tecno AI Instant Assistant Activation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alert and instant screen activation when saying Hey Tecno"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val WAKE_WORD_CHANNEL_ID = "tecno_wake_word_channel"
        const val ALERT_CHANNEL_ID = "tecno_wake_alert_channel"
    }
}
