package com.tecnolegend.tecnoai.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecnolegend.tecnoai.R
import com.tecnolegend.tecnoai.data.local.ConversationEntity
import com.tecnolegend.tecnoai.ui.components.GlowOrb
import com.tecnolegend.tecnoai.ui.components.MarkdownText
import com.tecnolegend.tecnoai.ui.components.ThinkingIndicator
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantUiState
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel
import com.tecnolegend.tecnoai.ui.viewmodel.PendingConfirmation
import kotlinx.coroutines.launch
import java.util.Calendar

// Local palette for the chat surface
private val Bg = Color(0xFF0B0B0C)
private val Bubble = Color(0xFF1E1E20)
private val Field = Color(0xFF161618)
private val FieldBorder = Color(0xFF2A2A2D)
private val TextMain = Color(0xFFEDEDED)
private val TextDim = Color(0xFF8E8E93)

@Composable
fun ChatScreen(
    viewModel: AssistantViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToVoiceModel: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val pending by viewModel.pendingConfirmation.collectAsState()
    val shareScreen by viewModel.shareScreen.collectAsState()
    val screenNotice by viewModel.screenNotice.collectAsState()
    val level by viewModel.speechRecognizerHelper.rmsLevel.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isListening = uiState == AssistantUiState.LISTENING || uiState == AssistantUiState.TRANSCRIBING
    val isWorking = uiState == AssistantUiState.THINKING || uiState == AssistantUiState.EXECUTING

    // Keep the newest message in view
    LaunchedEffect(messages.size, isWorking) {
        val count = messages.size + (if (isWorking) 1 else 0) + (if (pending != null) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF111112),
                drawerContentColor = TextMain,
                modifier = Modifier.width(300.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("Tecno AI", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                }
                DrawerLink("New chat", Icons.Outlined.Edit) {
                    viewModel.newChat(); scope.launch { drawerState.close() }
                }
                DrawerLink("History", Icons.Outlined.History) {
                    scope.launch { drawerState.close() }; onNavigateToHistory()
                }
                HorizontalDivider(color = Color(0xFF222224), modifier = Modifier.padding(vertical = 8.dp, horizontal = 20.dp))
                DrawerLink("Settings", Icons.Outlined.Settings) {
                    scope.launch { drawerState.close() }; onNavigateToSettings()
                }
                DrawerLink("Voice training", Icons.Outlined.RecordVoiceOver) {
                    scope.launch { drawerState.close() }; onNavigateToVoiceModel()
                }
                DrawerLink("Permissions", Icons.Outlined.Security) {
                    scope.launch { drawerState.close() }; onNavigateToPermissions()
                }
                DrawerLink("About", Icons.Outlined.Info) {
                    scope.launch { drawerState.close() }; onNavigateToAbout()
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = TextMain)
                }
                Text(
                    "Tecno AI",
                    color = TextMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.newChat() }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "New chat", tint = TextMain)
                }
            }

            // Conversation
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && !isWorking && !isListening) {
                    EmptyState(onSuggestion = { viewModel.submitTypedPrompt(it) })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(messages, key = { it.id }) { msg -> MessageItem(msg) }
                        if (isWorking) {
                            item(key = "thinking") {
                                ThinkingIndicator(
                                    label = when {
                                        statusMessage?.startsWith("Looking at your screen") == true -> "Looking at your screen"
                                        uiState == AssistantUiState.EXECUTING -> "Working on it"
                                        else -> "Thinking"
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        pending?.let { p ->
                            item(key = "confirm") {
                                ConfirmCard(p, onConfirm = { viewModel.confirmPendingAction() }, onCancel = { viewModel.cancelPendingAction() })
                            }
                        }
                    }
                }
            }

            // Screen-access hint
            AnimatedVisibility(visible = screenNotice != null, enter = fadeIn(), exit = fadeOut()) {
                val context = LocalContext.current
                Text(
                    text = (screenNotice ?: "") + "  Open settings",
                    color = TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            Composer(
                isListening = isListening,
                liveTranscript = liveTranscript,
                level = level,
                shareScreen = shareScreen,
                isSpeaking = uiState == AssistantUiState.SPEAKING,
                onSend = { viewModel.submitTypedPrompt(it) },
                onMic = { viewModel.startVoiceInput() },
                onStopListening = { viewModel.stopVoiceInput() },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onToggleScreen = { viewModel.toggleShareScreen() }
            )
        }
    }
}

@Composable
private fun DrawerLink(label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, fontSize = 15.sp) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = TextMain,
            unselectedIconColor = Color(0xFFB4B4B8)
        ),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello, night owl"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(onSuggestion: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Text(greeting(), color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.size(6.dp))
        Text("How can I help you today?", color = TextDim, fontSize = 16.sp)
        Spacer(Modifier.size(28.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "What's on my screen?",
                "Set a timer for 10 minutes",
                "Open YouTube",
                "Explain black holes simply"
            ).forEach { s ->
                Surface(
                    onClick = { onSuggestion(s) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF151517),
                    border = BorderStroke(1.dp, FieldBorder)
                ) {
                    Text(s, color = Color(0xFFCFCFD3), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageItem(msg: ConversationEntity) {
    val clipboard = LocalClipboardManager.current
    when {
        msg.role.equals("USER", ignoreCase = true) -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = msg.content,
                    color = TextMain,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                        .background(Bubble)
                        .padding(horizontal = 15.dp, vertical = 10.dp)
                )
            }
        }
        msg.toolName != null -> {
            // Phone actions show as a small status line, not a big card
            val ok = msg.isToolSuccess != false && !msg.isCanceled
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (ok) Color(0xFF1C2A1F) else Color(0xFF2A1C1C))
                ) {
                    Icon(
                        if (ok) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        tint = if (ok) Color(0xFF3FB950) else Color(0xFFFF6B6B),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                MarkdownText(msg.content, color = Color(0xFFCFCFD3), fontSize = 15)
            }
        }
        else -> {
            Column {
                MarkdownText(msg.content)
                if (!msg.searchSources.isNullOrBlank()) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Sources: " + msg.searchSources.split(" | ").joinToString(" · ") { it.substringBefore(" (") },
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.size(6.dp))
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(msg.content)) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color(0xFF6E6E73), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ConfirmCard(p: PendingConfirmation, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF151517),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Confirm action", color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.size(6.dp))
            Text(p.description, color = TextMain, fontSize = 15.sp, lineHeight = 21.sp)
            Spacer(Modifier.size(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = onCancel, shape = RoundedCornerShape(12.dp), color = Color(0xFF232325)) {
                    Text("Cancel", color = TextMain, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                Surface(onClick = onConfirm, shape = RoundedCornerShape(12.dp), color = Color(0xFFEDEDED)) {
                    Text("Confirm", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun Composer(
    isListening: Boolean,
    liveTranscript: String,
    level: Float,
    shareScreen: Boolean,
    isSpeaking: Boolean,
    onSend: (String) -> Unit,
    onMic: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleScreen: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Field)
            .border(1.dp, FieldBorder, RoundedCornerShape(26.dp))
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
    ) {
        if (isListening) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 28.dp)) {
                GlowOrb(size = 12.dp, level = level)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = liveTranscript.ifBlank { "Listening..." },
                    color = if (liveTranscript.isBlank()) TextDim else TextMain,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            }
        } else {
            Box(modifier = Modifier.heightIn(min = 28.dp, max = 160.dp)) {
                if (text.isEmpty()) {
                    Text("Message Tecno AI", color = Color(0xFF6E6E73), fontSize = 16.sp)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = TextMain, fontSize = 16.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Share-screen toggle (Gemini-style "ask about my screen")
            Surface(
                onClick = onToggleScreen,
                shape = RoundedCornerShape(16.dp),
                color = if (shareScreen) Color(0xFFEDEDED) else Color.Transparent,
                border = BorderStroke(1.dp, if (shareScreen) Color(0xFFEDEDED) else FieldBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        tint = if (shareScreen) Color.Black else Color(0xFFB4B4B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (shareScreen) "Sharing screen" else "Screen",
                        color = if (shareScreen) Color.Black else Color(0xFFB4B4B8),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            when {
                isListening -> RoundButton(Icons.Rounded.Stop, "Stop", filled = true, onClick = onStopListening)
                isSpeaking -> RoundButton(Icons.Rounded.GraphicEq, "Stop speaking", filled = true, onClick = onStopSpeaking)
                text.isNotBlank() -> RoundButton(Icons.Rounded.ArrowUpward, "Send", filled = true) {
                    onSend(text.trim())
                    text = ""
                }
                else -> RoundButton(Icons.Rounded.Mic, "Talk", filled = false, onClick = onMic)
            }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (filled) Color(0xFFEDEDED) else Color(0xFF242426))
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = if (filled) Color.Black else Color(0xFFEDEDED), modifier = Modifier.size(20.dp))
    }
}
