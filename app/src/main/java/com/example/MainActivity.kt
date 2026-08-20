package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.ui.components.AssistantOverlaySheet
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AssistantHomeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TecnoAiTheme
import com.example.ui.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            TecnoAiTheme {
                TecnoAiAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val initialQuery = intent?.getStringExtra("EXTRA_INITIAL_QUERY")
        if (!initialQuery.isNullOrBlank()) {
            viewModel.openAssistantOverlay(startVoice = false)
            viewModel.submitPrompt(initialQuery)
        } else if (intent?.getBooleanExtra("EXTRA_START_LISTENING", false) == true) {
            viewModel.openAssistantOverlay(startVoice = true)
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
                    onNavigateToAbout = { navController.navigate("about") }
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
