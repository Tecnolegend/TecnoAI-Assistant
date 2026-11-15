package com.tecnolegend.tecnoai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecnolegend.tecnoai.ui.theme.CrimsonBright
import com.tecnolegend.tecnoai.ui.theme.CrimsonDark
import com.tecnolegend.tecnoai.ui.theme.CrimsonGlow
import com.tecnolegend.tecnoai.ui.theme.DarkBackground
import com.tecnolegend.tecnoai.ui.theme.DarkBorder
import com.tecnolegend.tecnoai.ui.theme.DarkSurface
import com.tecnolegend.tecnoai.ui.theme.DarkSurfaceElevated
import com.tecnolegend.tecnoai.ui.theme.DarkSurfaceVariant
import com.tecnolegend.tecnoai.ui.theme.TextAccent
import com.tecnolegend.tecnoai.ui.theme.TextMuted
import com.tecnolegend.tecnoai.ui.theme.TextPrimary
import com.tecnolegend.tecnoai.ui.theme.TextSecondary
import com.tecnolegend.tecnoai.ui.theme.WarningAmber
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantUiState
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel

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
    val networkState by viewModel.networkState.collectAsState()
    val lastLatencyMs by viewModel.lastExecutionLatencyMs.collectAsState()
    val isOfflineEngineHandled by viewModel.isOfflineEngineHandled.collectAsState()

    var showTextInput by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val activeDisplayMessage = when {
        !statusMessage.isNullOrBlank() -> statusMessage ?: ""
        liveTranscript.isNotBlank() -> liveTranscript
        uiState == AssistantUiState.LISTENING -> "Listening..."
        uiState == AssistantUiState.THINKING -> "Tecno AI Thinking..."
        uiState == AssistantUiState.EXECUTING -> "Executing request..."
        else -> "How can I help you?"
    }

    val suggestions = listOf(
        "Who is the owner?",
        "Battery status",
        "Volume 80%",
        "Open YouTube",
        "Set 5 min timer",
        "Wi-Fi Settings"
    )

    // Fullscreen translucent backdrop dismissable on tap
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { viewModel.closeAssistantOverlay() }
            )
            .testTag("assistant_overlay_sheet"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Stop tap propagation */ }
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Top Controls Bar (Stop speaking, Keyboard toggle, Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState == AssistantUiState.LISTENING) CrimsonBright else Color(0xFF00F5D4))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TECNO AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Offline / Latency badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOfflineEngineHandled) Color(0xFF00F5D4).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOfflineEngineHandled) Color(0xFF00F5D4).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (!networkState.isOnline) "⚡ Offline Mode"
                                else if (isOfflineEngineHandled && lastLatencyMs != null) "⚡ ${lastLatencyMs}ms Local"
                                else "⚡ Offline Ready",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOfflineEngineHandled) Color(0xFF00F5D4) else Color(0xFFE0E0E0)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("stop_speaking_button")
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = "Stop speaking", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = { showTextInput = !showTextInput },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (showTextInput) CrimsonBright else Color.Black.copy(alpha = 0.5f))
                            .testTag("toggle_keyboard_button")
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Type command", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.closeAssistantOverlay() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("close_overlay_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Assistant", tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Sensitive Action Confirmation Card
            if (pendingConfirmation != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimsonBright, CrimsonDark))),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("confirmation_dialog_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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

            // Reference UI: Elegant Floating Rounded Frosted Speech Bubble Card
            // Modeled after the screenshot: Frosted warm peach-cream translucent card with dark indigo/violet text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF6E8DF).copy(alpha = 0.94f)
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color(0x66000000)
                    )
                    .testTag("siri_speech_response_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = activeDisplayMessage,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 25.sp,
                        color = Color(0xFF241C38),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional search sources or follow-up note
                    if (uiState == AssistantUiState.LISTENING) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap orb or say your command...",
                            fontSize = 12.sp,
                            color = Color(0xFF6C5E78)
                        )
                    }
                }
            }

            // Quick suggestion chips
            AnimatedVisibility(
                visible = !showTextInput && uiState != AssistantUiState.THINKING,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        Surface(
                            onClick = { viewModel.submitPrompt(suggestion) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .testTag("suggestion_chip_${suggestion.replace(" ", "_").replace("?", "")}")
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Text input row if toggled
            AnimatedVisibility(
                visible = showTextInput,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask or command Tecno AI...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.submitPrompt(textInput)
                                textInput = ""
                                showTextInput = false
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonBright,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CrimsonBright,
                            focusedContainerColor = Color.Black.copy(alpha = 0.7f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_text_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.submitPrompt(textInput)
                                textInput = ""
                                showTextInput = false
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CrimsonBright)
                            .testTag("send_prompt_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reference UI: Iconic 3D Glowing Iridescent Sphere Orb
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

            // Reference UI: Bottom White Home Pill Indicator Bar
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .testTag("bottom_home_indicator_pill")
            )
            
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

