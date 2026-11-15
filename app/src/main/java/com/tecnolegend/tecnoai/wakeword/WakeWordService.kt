package com.tecnolegend.tecnoai.wakeword

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
import com.tecnolegend.tecnoai.R
import com.tecnolegend.tecnoai.TecnoAiApp
import com.tecnolegend.tecnoai.data.local.UserSettingsManager
import com.tecnolegend.tecnoai.diagnostics.BugTracker
import com.tecnolegend.tecnoai.overlay.AssistOverlayActivity
import com.tecnolegend.tecnoai.speech.SoundCueHelper
import com.tecnolegend.tecnoai.speech.TextToSpeechHelper
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
    private var isDetecting = false

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
        // The app's own mic button needs the microphone: step aside for a while
        if (incomingAction == ACTION_PAUSE) {
            detector?.pauseDetection(intent?.getLongExtra(EXTRA_PAUSE_MS, 8000L) ?: 8000L)
            return START_STICKY
        }
        if (incomingAction == ACTION_RESUME) {
            detector?.resumeDetection()
            return START_STICKY
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
            .setSmallIcon(R.drawable.ic_stat_tecno)
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
            // Without foreground status Android would kill (and crash) the service: stop cleanly
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            BugTracker.logError(TAG, "Foreground service start failed: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start listening only once; repeated start requests must not restart the mic
        if (!isDetecting) {
            isDetecting = true
            isRunning = true
            detector?.startDetection { query ->
                handleWakeWordDetected(query)
            }
        }

        return START_STICKY
    }

    private fun handleWakeWordDetected(query: String) {
        val cleanQuery = query.trim()
        Log.i(TAG, "Wake word triggered with query: '$cleanQuery'")
        BugTracker.logInfo(TAG, "Wake word triggered: '$cleanQuery'")

        serviceScope.launch {
            // The assistant window plays the activation chime itself (only once)
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

            // Open the assistant exactly once, using the best method Android allows right now
            val canOpenDirectly = android.provider.Settings.canDrawOverlays(this@WakeWordService) || isAppInForeground()
            if (canOpenDirectly) {
                try {
                    startActivity(overlayIntent)
                    return@launch
                } catch (e: Exception) {
                    Log.w(TAG, "Direct startActivity from service failed: ${e.message}")
                }
            }

            // Background: show a full-screen / heads-up notification the user can tap
            try {
                val alertNotification = NotificationCompat.Builder(this@WakeWordService, TecnoAiApp.ALERT_CHANNEL_ID)
                    .setContentTitle("Tecno AI")
                    .setContentText(if (cleanQuery.isNotBlank()) cleanQuery else "I'm listening. Tap to talk.")
                    .setSmallIcon(R.drawable.ic_stat_tecno)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pendingIntent, true)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setTimeoutAfter(8000L)
                    .build()

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify(ALERT_NOTIFICATION_ID, alertNotification)
            } catch (e: Exception) {
                Log.w(TAG, "Alert notification post failed: ${e.message}")
            }
        }
    }

    private fun isAppInForeground(): Boolean {
        val info = android.app.ActivityManager.RunningAppProcessInfo()
        android.app.ActivityManager.getMyMemoryState(info)
        return info.importance <= android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    override fun onDestroy() {
        isDetecting = false
        isRunning = false
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
        const val ACTION_STOP = "com.tecnolegend.tecnoai.wakeword.STOP"
        const val ACTION_PAUSE = "com.tecnolegend.tecnoai.wakeword.PAUSE"
        const val ACTION_RESUME = "com.tecnolegend.tecnoai.wakeword.RESUME"
        const val EXTRA_PAUSE_MS = "pause_ms"

        /** True while the wake word service is up and listening. */
        @Volatile
        var isRunning = false
            private set
        const val ACTION_OPEN_OVERLAY_BROADCAST = "com.tecnolegend.tecnoai.ACTION_OPEN_OVERLAY"
    }
}

