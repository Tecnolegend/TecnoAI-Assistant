package com.tecnolegend.tecnoai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tecnolegend.tecnoai.wakeword.WakeWordService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tecnolegend.tecnoai.ui.components.AssistantOverlaySheet
import com.tecnolegend.tecnoai.ui.screens.AboutScreen
import com.tecnolegend.tecnoai.ui.screens.AssistantHomeScreen
import com.tecnolegend.tecnoai.ui.screens.HistoryScreen
import com.tecnolegend.tecnoai.ui.screens.OnboardingScreen
import com.tecnolegend.tecnoai.ui.screens.PermissionsCenterScreen
import com.tecnolegend.tecnoai.ui.screens.SettingsScreen
import com.tecnolegend.tecnoai.ui.screens.VoiceEnrollmentScreen
import com.tecnolegend.tecnoai.ui.theme.DarkBackground
import com.tecnolegend.tecnoai.ui.theme.TecnoAiTheme
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val overlayBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WakeWordService.ACTION_OPEN_OVERLAY_BROADCAST) {
                val query = intent.getStringExtra(com.tecnolegend.tecnoai.overlay.AssistOverlayActivity.EXTRA_INITIAL_QUERY)
                viewModel.handleWakeWordTrigger(query)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter(WakeWordService.ACTION_OPEN_OVERLAY_BROADCAST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(overlayBroadcastReceiver, filter)
        }

        handleIntent(intent)

        setContent {
            TecnoAiTheme {
                TecnoAiAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.ensureWakeWordServiceRunning()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(overlayBroadcastReceiver)
        } catch (e: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val initialQuery = intent?.getStringExtra("EXTRA_INITIAL_QUERY")
            ?: intent?.getStringExtra(com.tecnolegend.tecnoai.overlay.AssistOverlayActivity.EXTRA_INITIAL_QUERY)
        val isVoiceTrigger = intent?.getBooleanExtra("EXTRA_START_LISTENING", false) == true ||
                             intent?.getBooleanExtra("EXTRA_START_ASSISTANT_OVERLAY", false) == true ||
                             intent?.action == Intent.ACTION_VOICE_COMMAND ||
                             intent?.action == Intent.ACTION_ASSIST

        if (!initialQuery.isNullOrBlank()) {
            viewModel.handleWakeWordTrigger(initialQuery)
        } else if (isVoiceTrigger) {
            viewModel.handleWakeWordTrigger(null)
        }
    }
}

@Composable
fun TecnoAiAppContent(viewModel: AssistantViewModel) {
    val navController = rememberNavController()
    val isOverlayVisible by viewModel.isOverlayVisible.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val startDestination = if (userSettings.hasCompletedOnboarding) "home" else "onboarding"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                AssistantHomeScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToPermissions = { navController.navigate("permissions") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToVoiceModel = { navController.navigate("voice_model") }
                )
            }

            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("permissions") {
                PermissionsCenterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVoiceModel = { navController.navigate("voice_model") }
                )
            }

            composable("voice_model") {
                VoiceEnrollmentScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Global Animated Assistant Overlay Sheet
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            // Dark Scrim background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.closeAssistantOverlay() }
                    )
            )
        }

        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AssistantOverlaySheet(viewModel = viewModel)
        }
    }
}
