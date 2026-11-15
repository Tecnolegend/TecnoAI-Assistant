package com.tecnolegend.tecnoai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecnolegend.tecnoai.ui.components.RedOrbVisualizer
import com.tecnolegend.tecnoai.ui.theme.CrimsonBright
import com.tecnolegend.tecnoai.ui.theme.CrimsonDark
import com.tecnolegend.tecnoai.ui.theme.CrimsonGlow
import com.tecnolegend.tecnoai.ui.theme.CrimsonPrimary
import com.tecnolegend.tecnoai.ui.theme.DarkBackground
import com.tecnolegend.tecnoai.ui.theme.DarkBorder
import com.tecnolegend.tecnoai.ui.theme.DarkSurface
import com.tecnolegend.tecnoai.ui.theme.DarkSurfaceElevated
import com.tecnolegend.tecnoai.ui.theme.TextAccent
import com.tecnolegend.tecnoai.ui.theme.TextMuted
import com.tecnolegend.tecnoai.ui.theme.TextPrimary
import com.tecnolegend.tecnoai.ui.theme.TextSecondary
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantUiState
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel

data class OnboardingStep(
    val stepNumber: Int,
    val title: String,
    val headline: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    viewModel: AssistantViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableStateOf(0) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (currentStepIndex < 3) {
            currentStepIndex++
        } else {
            viewModel.completeOnboarding()
            onComplete()
        }
    }

    val steps = listOf(
        OnboardingStep(
            stepNumber = 1,
            title = "WELCOME",
            headline = "Say it. Tecno handles it.",
            description = "Tecno AI is your high-speed action assistant built with native Android tool orchestration and advanced neural intelligence.",
            icon = Icons.Default.SmartToy
        ),
        OnboardingStep(
            stepNumber = 2,
            title = "WAKE PHRASE",
            headline = "\"Hey Tecno\"",
            description = "Activate your assistant instantly with hands-free wake word recognition, even while multitasking or in the car.",
            icon = Icons.Default.Mic
        ),
        OnboardingStep(
            stepNumber = 3,
            title = "LOCAL SPEED",
            headline = "Zero-Latency Device Tools",
            description = "Open apps, adjust system settings, set alarms, control media, and toggle device flashlight with deterministic speed.",
            icon = Icons.Default.FlashOn
        ),
        OnboardingStep(
            stepNumber = 4,
            title = "PERMISSION SETUP",
            headline = "Enable Microphone Access",
            description = "Grant audio recording access to enable seamless voice recognition and wake-word detection.",
            icon = Icons.Default.Security
        )
    )

    val currentStep = steps[currentStepIndex]

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(4.dp)
                            .width(if (index == currentStepIndex) 32.dp else 12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index == currentStepIndex) CrimsonBright else DarkBorder)
                    )
                }
            }

            // Center Visual & Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                RedOrbVisualizer(
                    state = if (currentStepIndex == 1) AssistantUiState.LISTENING else AssistantUiState.IDLE,
                    size = 180.dp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonPrimary.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonDark)
                ) {
                    Text(
                        text = currentStep.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = CrimsonBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentStep.headline,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentStep.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Bottom Navigation Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (currentStepIndex == 3) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            currentStepIndex++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_primary_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentStepIndex == 3) "Allow Microphone & Start" else "Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (currentStepIndex == 3) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                if (currentStepIndex < 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.completeOnboarding()
                            onComplete()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text("Skip", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
