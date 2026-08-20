package com.example.wakeword

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.TecnoAiApp
import com.example.ai.GeminiClient
import com.example.assistant.DecisionType
import com.example.assistant.IntentRouter
import com.example.speech.TextToSpeechHelper
import com.example.tools.ToolDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    private var detector: WakeWordDetector? = null
    private var ttsHelper: TextToSpeechHelper? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val intentRouter = IntentRouter()
    private val geminiClient = GeminiClient()

    override fun onCreate() {
        super.onCreate()
        detector = WakeWordDetector(this)
        ttsHelper = TextToSpeechHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val incomingAction = intent?.action
        if (incomingAction == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
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
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        detector?.startDetection { query ->
            handleWakeWordDetected(query)
        }

        return START_STICKY
    }

    private fun handleWakeWordDetected(query: String) {
        val cleanQuery = query.trim()
        Log.i(TAG, "Wake word triggered with query: '$cleanQuery'")

        if (cleanQuery.isBlank()) {
            // Just "Hey Tecno" was spoken: speak back acknowledgement without forcing app redirect
            ttsHelper?.speak("I'm listening. What can I do for you?")
            return
        }

        // Directly process user command in background without redirecting away from current screen
        serviceScope.launch {
            try {
                val decision = intentRouter.route(cleanQuery)
                when {
                    decision.directAnswer != null -> {
                        ttsHelper?.speak(decision.directAnswer)
                    }
                    decision.toolName != null -> {
                        val dispatcher = ToolDispatcher(applicationContext)
                        val result = dispatcher.executeTool(
                            name = decision.toolName,
                            params = decision.parameters,
                            userConfirmed = true,
                            confirmSensitive = false
                        )
                        ttsHelper?.speak(result.message)
                    }
                    decision.type == DecisionType.AI_QUESTION || decision.type == DecisionType.WEB_SEARCH -> {
                        val aiResult = geminiClient.processRequest(
                            userPrompt = cleanQuery,
                            context = applicationContext
                        )
                        ttsHelper?.speak(aiResult.responseText)
                    }
                    else -> {
                        ttsHelper?.speak("Executed $cleanQuery")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing background voice query: ${e.message}")
                ttsHelper?.speak("Processed your request.")
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
        const val ACTION_STOP = "com.example.wakeword.STOP"
    }
}
