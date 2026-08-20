package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var isBubbleAttached = false

    companion object {
        private const val CHANNEL_ID = "tecno_overlay_channel"
        private const val NOTIFICATION_ID = 2002
        const val ACTION_START = "com.example.overlay.START"
        const val ACTION_STOP = "com.example.overlay.STOP"
        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        isRunning = true

        if (Settings.canDrawOverlays(this)) {
            showFloatingBubble()
        }

        return START_STICKY
    }

    private fun showFloatingBubble() {
        if (isBubbleAttached) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }

        val bubble = ImageView(this).apply {
            val size = (56 * resources.displayMetrics.density).toInt()
            layoutParams = WindowManager.LayoutParams(size, size)
            val shape = GradientDrawable().apply {
                this.shape = GradientDrawable.OVAL
                setColor(0xFFE50914.toInt()) // Crimson Red
                setStroke((2 * resources.displayMetrics.density).toInt(), 0xFFFF4D4D.toInt())
            }
            background = shape
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(0xFFFFFFFF.toInt())
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 16f
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isClick = false
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager?.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        openAssistantApp()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(bubble, params)
            floatingBubbleView = bubble
            isBubbleAttached = true
        } catch (e: Exception) {
            // Permission or window manager issue
        }
    }

    private fun openAssistantApp() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VOICE_COMMAND
            putExtra("EXTRA_START_ASSISTANT_OVERLAY", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(launchIntent)
    }

    private fun buildForegroundNotification(): Notification {
        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_START_ASSISTANT_OVERLAY", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(this, 1, openIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tecno AI Overlay Active")
            .setContentText("Tap floating icon anywhere or say 'Hey Tecno'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hide Overlay", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tecno AI Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows quick access floating bubble over other apps"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (isBubbleAttached && floatingBubbleView != null) {
            try {
                windowManager?.removeView(floatingBubbleView)
            } catch (e: Exception) {}
            isBubbleAttached = false
            floatingBubbleView = null
        }
    }
}
