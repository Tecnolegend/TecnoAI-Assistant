package com.tecnolegend.tecnoai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantUiState
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel

private val PanelBg = Color(0xF2121213)
private val PanelBorder = Color(0x26FFFFFF)
private val TextMain = Color(0xFFEDEDED)
private val TextDim = Color(0xFF8E8E93)

/**
 * The "Hey Tecno" assistant: a floating dark panel at the bottom of the screen with
 * a soft white glow that reacts to your voice, like Siri and Gemini.
 * Works on top of any app (AssistOverlayActivity) and inside the app.
 */
@Composable
fun AssistantOverlaySheet(viewModel: AssistantViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val pending by viewModel.pendingConfirmation.collectAsState()
    val level by viewModel.speechRecognizerHelper.rmsLevel.collectAsState()
    val shareScreen by viewModel.shareScreen.collectAsState()
    val screenNotice by viewModel.screenNotice.collectAsState()
    val hidden by viewModel.hideOverlayForCapture.collectAsState()

    val isListening = uiState == AssistantUiState.LISTENING || uiState == AssistantUiState.TRANSCRIBING
    val isWorking = uiState == AssistantUiState.THINKING || uiState == AssistantUiState.EXECUTING
    val isSpeaking = uiState == AssistantUiState.SPEAKING

    // Keep the latest answer on screen until the user asks something new
    var answer by remember { mutableStateOf<String?>(null) }
    var question by remember { mutableStateOf("") }
    LaunchedEffect(uiState, statusMessage) {
        when (uiState) {
            AssistantUiState.LISTENING -> { answer = null; question = "" }
            AssistantUiState.THINKING -> { answer = null; if (liveTranscript.isNotBlank() && question.isBlank()) question = liveTranscript }
            AssistantUiState.SUCCESS, AssistantUiState.SPEAKING, AssistantUiState.ERROR -> {
                statusMessage?.takeIf { it.isNotBlank() && !it.endsWith("...") }?.let { answer = it }
            }
            else -> {}
        }
    }

    var typing by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (hidden) 0f else 1f)
    ) {
        // Tap above the panel to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    viewModel.closeAssistantOverlay()
                }
        )

        EdgeGlow(
            active = isListening || isWorking || isSpeaking,
            level = if (isListening) level else null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(PanelBg)
                .border(1.dp, PanelBorder, RoundedCornerShape(28.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .animateContentSize()
        ) {
            // Status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowOrb(size = 14.dp, level = if (isListening) level else null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        isListening -> "Listening"
                        statusMessage?.startsWith("Looking at your screen") == true -> "Looking at your screen"
                        isWorking -> "Thinking"
                        isSpeaking -> "Speaking"
                        else -> "Tecno AI"
                    },
                    color = TextDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Main text
            Column(
                modifier = Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    isListening -> Text(
                        text = liveTranscript.ifBlank { "What can I do for you?" },
                        color = if (liveTranscript.isBlank()) TextDim else TextMain,
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Medium
                    )
                    isWorking -> {
                        if (question.isNotBlank()) {
                            Text(question, color = TextDim, fontSize = 16.sp, lineHeight = 22.sp)
                            Spacer(Modifier.height(10.dp))
                        }
                        ThinkingIndicator(
                            label = if (statusMessage?.startsWith("Looking at your screen") == true) "Looking at your screen" else "Thinking"
                        )
                    }
                    answer != null -> {
                        if (question.isNotBlank()) {
                            Text(question, color = TextDim, fontSize = 14.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        MarkdownText(answer!!, fontSize = 17)
                    }
                    else -> Text("Say something or type a question.", color = TextDim, fontSize = 18.sp)
                }

                pending?.let { p ->
                    Spacer(Modifier.height(14.dp))
                    Text(p.description, color = TextMain, fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("Cancel", filled = false) { viewModel.cancelPendingAction() }
                        Pill("Confirm", filled = true) { viewModel.confirmPendingAction() }
                    }
                }
            }

            AnimatedVisibility(visible = screenNotice != null, enter = fadeIn(), exit = fadeOut()) {
                Text(screenNotice ?: "", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }

            AnimatedVisibility(visible = typing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        if (typed.isEmpty()) Text("Ask Tecno AI", color = Color(0xFF6E6E73), fontSize = 16.sp)
                        BasicTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            singleLine = true,
                            textStyle = TextStyle(color = TextMain, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (typed.isNotBlank()) { question = typed.trim(); viewModel.submitPrompt(typed.trim()); typed = "" }
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CircleIcon(Icons.Rounded.ArrowUpward, "Send", filled = true) {
                        if (typed.isNotBlank()) { question = typed.trim(); viewModel.submitPrompt(typed.trim()); typed = "" }
                    }
                }
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Surface(
                    onClick = { viewModel.toggleShareScreen() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (shareScreen) Color(0xFFEDEDED) else Color.Transparent,
                    border = BorderStroke(1.dp, if (shareScreen) Color(0xFFEDEDED) else Color(0xFF2E2E31))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Icon(Icons.Outlined.PhoneAndroid, null, tint = if (shareScreen) Color.Black else Color(0xFFB4B4B8), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (shareScreen) "Sharing screen" else "Share screen", color = if (shareScreen) Color.Black else Color(0xFFB4B4B8), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                CircleIcon(Icons.Outlined.Keyboard, "Type", filled = false) { typing = !typing }
                Spacer(Modifier.width(10.dp))
                when {
                    isListening -> CircleIcon(Icons.Rounded.Stop, "Stop", filled = true) { viewModel.stopVoiceInput() }
                    isSpeaking -> CircleIcon(Icons.Rounded.GraphicEq, "Stop speaking", filled = true) { viewModel.stopSpeaking() }
                    else -> CircleIcon(Icons.Rounded.Mic, "Talk", filled = true) { typing = false; viewModel.startVoiceInput() }
                }
                Spacer(Modifier.width(10.dp))
                CircleIcon(Icons.Rounded.Close, "Close", filled = false) { viewModel.closeAssistantOverlay() }
            }
        }
    }
}

/** Soft white light rising from the bottom edge; it swells with your voice. */
@Composable
private fun EdgeGlow(active: Boolean, level: Float?, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "edge")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val voice by animateFloatAsState(targetValue = level ?: 0f, label = "voice")
    val strength by animateFloatAsState(targetValue = if (active) 1f else 0.35f, animationSpec = tween(500), label = "strength")

    Canvas(modifier = modifier.fillMaxWidth().height(260.dp)) {
        val w = size.width
        val h = size.height
        val intensity = (0.22f + voice * 0.45f) * strength
        // Two drifting light sources for a living, Siri-like glow
        listOf(0.25f + drift * 0.2f, 0.75f - drift * 0.2f).forEach { x ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = intensity), Color(0xFFBFD4FF).copy(alpha = intensity * 0.35f), Color.Transparent),
                    center = Offset(w * x, h * 1.05f),
                    radius = h * (0.95f + voice * 0.3f)
                ),
                radius = h * (0.95f + voice * 0.3f),
                center = Offset(w * x, h * 1.05f)
            )
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.10f * strength)),
                startY = h * 0.55f,
                endY = h
            )
        )
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, description: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (filled) Color(0xFFEDEDED) else Color(0xFF232325))
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = if (filled) Color.Black else Color(0xFFEDEDED), modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun Pill(label: String, filled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (filled) Color(0xFFEDEDED) else Color(0xFF232325)) {
        Text(
            label,
            color = if (filled) Color.Black else TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
        )
    }
}
