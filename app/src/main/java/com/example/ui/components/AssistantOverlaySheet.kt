package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSubtle
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantUiState
import com.example.ui.viewmodel.AssistantViewModel

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

    val suggestions = listOf(
        "Open YouTube",
        "Set a 10 min timer",
        "Wi-Fi Settings",
        "Search AI News",
        "What is quantum computing?",
        "Turn on flashlight"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .shadow(24.dp, spotColor = CrimsonGlow)
            .testTag("assistant_overlay_sheet"),
        color = DarkBackground.copy(alpha = 0.97f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle / Title header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (uiState == AssistantUiState.LISTENING) CrimsonBright else CrimsonPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TECNO AI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = CrimsonBright
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.size(36.dp).testTag("stop_speaking_button")
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = "Stop speaking", tint = TextAccent)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.closeAssistantOverlay() },
                        modifier = Modifier.size(36.dp).testTag("close_overlay_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Assistant", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Animated Red Orb in center
            RedOrbVisualizer(
                state = uiState,
                audioLevel = audioLevel,
                size = 120.dp,
                onClick = {
                    if (uiState == AssistantUiState.LISTENING) {
                        viewModel.stopVoiceInput()
                    } else {
                        viewModel.startVoiceInput()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live status & transcription text
            val stateTitle = when (uiState) {
                AssistantUiState.LISTENING -> "Listening to you..."
                AssistantUiState.TRANSCRIBING -> "Transcribing speech..."
                AssistantUiState.THINKING -> "Thinking..."
                AssistantUiState.EXECUTING -> "Running action..."
                AssistantUiState.SPEAKING -> "Speaking response..."
                AssistantUiState.SUCCESS -> "Task completed"
                AssistantUiState.ERROR -> "Notice"
                AssistantUiState.IDLE -> "Say \"Hey Tecno\" or tap mic"
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
                    fontSize = 16.sp,
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
                        fontSize = 14.sp,
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
                            Text("Confirmation Required", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pendingConfirmation?.description ?: "",
                            fontSize = 14.sp,
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

            // Quick suggestion chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { suggestion ->
                    Surface(
                        onClick = { viewModel.submitPrompt(suggestion) },
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.testTag("suggestion_chip_${suggestion.replace(" ", "_")}")
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                    placeholder = { Text("Ask Tecno AI...", color = TextMuted, fontSize = 14.sp) },
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
                            .size(48.dp)
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
                            .size(48.dp)
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
