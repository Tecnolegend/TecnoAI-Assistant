package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantUiState
import com.example.ui.viewmodel.AssistantViewModel
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantOverlaySheet(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val audioLevel by viewModel.speechRecognizerHelper.rmsLevel.collectAsState()
    val isSpeaking by viewModel.textToSpeechHelper.isSpeaking.collectAsState()

    var textInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "siri_wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val suggestions = listOf(
        "Who is the owner?",
        "Battery status",
        "Volume 80%",
        "Open YouTube",
        "Set 5 min timer",
        "Calculate 25 * 84",
        "Wi-Fi Settings"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(CrimsonBright.copy(alpha = 0.8f), DarkBorder)),
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .shadow(32.dp, spotColor = CrimsonGlow)
            .testTag("assistant_overlay_sheet"),
        color = DarkBackground.copy(alpha = 0.96f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient Red Gradient Fluid Waves at bottom
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val width = size.width
                val height = size.height
                val path = Path()
                path.moveTo(0f, height)

                for (x in 0..width.toInt() step 8) {
                    val progress = x / width
                    val y = height * 0.45f + (sin(progress * 4 * Math.PI + waveOffset).toFloat() * (12f + audioLevel * 18f))
                    path.lineTo(x.toFloat(), y)
                }
                path.lineTo(width, height)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CrimsonBright.copy(alpha = if (uiState == AssistantUiState.LISTENING) 0.35f else 0.18f),
                            CrimsonDark.copy(alpha = 0.05f)
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, CrimsonDark, RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState == AssistantUiState.LISTENING) CrimsonBright else CrimsonPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TECNO AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = CrimsonBright
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSpeaking) {
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier.size(34.dp).testTag("stop_speaking_button")
                            ) {
                                Icon(Icons.Default.VolumeOff, contentDescription = "Stop speaking", tint = TextAccent)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.closeAssistantOverlay() },
                            modifier = Modifier.size(34.dp).testTag("close_overlay_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Assistant", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Siri-Style Pulsing Red Orb
                RedOrbVisualizer(
                    state = uiState,
                    audioLevel = audioLevel,
                    size = 110.dp,
                    onClick = {
                        if (uiState == AssistantUiState.LISTENING) {
                            viewModel.stopVoiceInput()
                        } else {
                            viewModel.startVoiceInput()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Live status text
                val stateTitle = when (uiState) {
                    AssistantUiState.LISTENING -> "Listening..."
                    AssistantUiState.TRANSCRIBING -> "Transcribing speech..."
                    AssistantUiState.THINKING -> "Consulting Gemini AI..."
                    AssistantUiState.EXECUTING -> "Executing device power..."
                    AssistantUiState.SPEAKING -> "Speaking response..."
                    AssistantUiState.SUCCESS -> "Task completed"
                    AssistantUiState.ERROR -> "Notice"
                    AssistantUiState.IDLE -> "Say \"Hey Tecno\" or tap to speak"
                }

                Text(
                    text = stateTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState == AssistantUiState.LISTENING) CrimsonBright else TextMuted
                )

                if (liveTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$liveTranscript\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                if (statusMessage != null && statusMessage != liveTranscript) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, CrimsonDark))),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                // Sensitive Action Confirmation Card
                if (pendingConfirmation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimsonBright, CrimsonDark))),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("confirmation_dialog_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Permission Required", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pendingConfirmation?.description ?: "",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelPendingAction() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                    modifier = Modifier.testTag("cancel_action_button")
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.confirmPendingAction() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                                    modifier = Modifier.testTag("confirm_action_button")
                                ) {
                                    Text("Proceed", color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Suggestion chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        Surface(
                            onClick = { viewModel.submitPrompt(suggestion) },
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.testTag("suggestion_chip_${suggestion.replace(" ", "_").replace("?", "")}")
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Input & Mic Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask or command Tecno AI...", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.submitPrompt(textInput)
                                textInput = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonBright,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CrimsonBright,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_text_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.submitPrompt(textInput)
                                textInput = ""
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CrimsonBright)
                                .testTag("send_prompt_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (uiState == AssistantUiState.LISTENING) {
                                    viewModel.stopVoiceInput()
                                } else {
                                    viewModel.startVoiceInput()
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (uiState == AssistantUiState.LISTENING) CrimsonPrimary else DarkSurfaceElevated)
                                .border(1.dp, if (uiState == AssistantUiState.LISTENING) CrimsonBright else DarkBorder, CircleShape)
                                .testTag("mic_toggle_button")
                        ) {
                            Icon(
                                if (uiState == AssistantUiState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (uiState == AssistantUiState.LISTENING) Color.White else CrimsonBright
                            )
                        }
                    }
                }
            }
        }
    }
}
