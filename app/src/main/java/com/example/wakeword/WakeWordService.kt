package com.example.wakeword

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.TecnoAiApp
import com.example.data.local.UserSettingsManager
import com.example.diagnostics.BugTracker
import com.example.overlay.AssistOverlayActivity
import com.example.speech.SoundCueHelper
import com.example.speech.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    private var detector: WakeWordDetector? = null
    private var ttsHelper: TextToSpeechHelper? = null
    private var soundCueHelper: SoundCueHelper? = null
    private var userSettingsManager: UserSettingsManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        detector = WakeWordDetector(this)
        ttsHelper = TextToSpeechHelper(this)
        soundCueHelper = SoundCueHelper()
        userSettingsManager = UserSettingsManager(this)
        Log.i(TAG, "WakeWordService initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val incomingAction = intent?.action
        if (incomingAction == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val openAppIntent = Intent(this, AssistOverlayActivity::class.java).apply {
            putExtra(AssistOverlayActivity.EXTRA_AUTO_LISTEN, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, TecnoAiApp.WAKE_WORD_CHANNEL_ID)
            .setContentTitle("Tecno AI Active")
            .setContentText("Listening for 'Hey Tecno' in background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .addAction(0, "Open Assistant", pendingOpen)
            .addAction(0, "Stop", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            BugTracker.logError(TAG, "Foreground service start failed: ${e.message}")
        }

        detector?.startDetection { query ->
            handleWakeWordDetected(query)
        }

        return START_STICKY
    }

    private fun handleWakeWordDetected(query: String) {
        val cleanQuery = query.trim()
        Log.i(TAG, "Wake word triggered with query: '$cleanQuery'")
        BugTracker.logInfo(TAG, "Wake word triggered: '$cleanQuery'")

        serviceScope.launch {
            // 1. Play activation sound cue based on user settings
            try {
                val settings = userSettingsManager?.userSettingsFlow?.firstOrNull()
                val themeName = settings?.soundTheme ?: SoundCueHelper.SoundTheme.CRIMSON_CYBER.name
                val theme = try {
                    SoundCueHelper.SoundTheme.valueOf(themeName)
                } catch (e: Exception) {
                    SoundCueHelper.SoundTheme.CRIMSON_CYBER
                }
                soundCueHelper?.playActivationSound(theme)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play sound cue: ${e.message}")
            }

            // 2. Prepare Overlay Activity Intent
            val overlayIntent = Intent(this@WakeWordService, AssistOverlayActivity::class.java).apply {
                if (cleanQuery.isNotBlank()) {
                    putExtra(AssistOverlayActivity.EXTRA_INITIAL_QUERY, cleanQuery)
                    putExtra(AssistOverlayActivity.EXTRA_AUTO_LISTEN, false)
                } else {
                    putExtra(AssistOverlayActivity.EXTRA_AUTO_LISTEN, true)
                }
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }

            val pendingIntent = PendingIntent.getActivity(
                this@WakeWordService,
                (System.currentTimeMillis() % 10000).toInt(),
                overlayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Method A: Direct startActivity
            try {
                startActivity(overlayIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Direct startActivity from service failed: ${e.message}")
            }

            // Method B: Send PendingIntent
            try {
                pendingIntent.send()
            } catch (e: Exception) {
                Log.w(TAG, "PendingIntent send failed: ${e.message}")
            }

            // Method C: Post High-Priority FullScreenIntent Notification (Guaranteed popup on Android 10-15)
            try {
                val alertNotification = NotificationCompat.Builder(this@WakeWordService, TecnoAiApp.ALERT_CHANNEL_ID)
                    .setContentTitle("Tecno AI Activated")
                    .setContentText(if (cleanQuery.isNotBlank()) cleanQuery else "Listening...")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pendingIntent, true)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setTimeoutAfter(6000L)
                    .build()

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify(ALERT_NOTIFICATION_ID, alertNotification)
            } catch (e: Exception) {
                Log.w(TAG, "Alert notification post failed: ${e.message}")
            }

            // Method D: Send broadcast to foreground activity if active
            try {
                val broadcastIntent = Intent(ACTION_OPEN_OVERLAY_BROADCAST).apply {
                    setPackage(packageName)
                    if (cleanQuery.isNotBlank()) {
                        putExtra(AssistOverlayActivity.EXTRA_INITIAL_QUERY, cleanQuery)
                    }
                    putExtra(AssistOverlayActivity.EXTRA_AUTO_LISTEN, cleanQuery.isBlank())
                }
                sendBroadcast(broadcastIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Broadcast send failed: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        detector?.stopDetection()
        ttsHelper?.shutdown()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "WakeWordService"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val ACTION_STOP = "com.example.wakeword.STOP"
        const val ACTION_OPEN_OVERLAY_BROADCAST = "com.example.ACTION_OPEN_OVERLAY"
    }
}

