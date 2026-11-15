package com.tecnolegend.tecnoai.overlay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tecnolegend.tecnoai.ui.components.AssistantOverlaySheet
import com.tecnolegend.tecnoai.ui.theme.TecnoAiTheme
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel

class AssistOverlayActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn on screen and show over keyguard if locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        onBackPressedDispatcher.addCallback(this) {
            viewModel.closeAssistantOverlay()
            finish()
            overridePendingTransition(0, 0)
        }

        handleIntent(intent)

        setContent {
            TecnoAiTheme {
                val isOverlayVisible by viewModel.isOverlayVisible.collectAsState()
                var hasBeenActive by remember { mutableStateOf(false) }

                LaunchedEffect(isOverlayVisible) {
                    if (isOverlayVisible) {
                        hasBeenActive = true
                    } else if (hasBeenActive) {
                        // User dismissed or action completed
                        finish()
                        overridePendingTransition(0, 0)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    AssistantOverlaySheet(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val initialQuery = intent?.getStringExtra(EXTRA_INITIAL_QUERY)
        val autoListen = intent?.getBooleanExtra(EXTRA_AUTO_LISTEN, true) ?: true

        if (!initialQuery.isNullOrBlank()) {
            viewModel.handleWakeWordTrigger(initialQuery)
        } else {
            viewModel.openAssistantOverlay(startVoice = autoListen)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_INITIAL_QUERY = "EXTRA_INITIAL_QUERY"
        const val EXTRA_AUTO_LISTEN = "EXTRA_AUTO_LISTEN"
    }
}

