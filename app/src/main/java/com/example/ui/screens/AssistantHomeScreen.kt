package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ConversationEntity
import com.example.ui.components.RedOrbVisualizer
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
import com.example.ui.viewmodel.AssistantUiState
import com.example.ui.viewmodel.AssistantViewModel

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val command: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantHomeScreen(
    viewModel: AssistantViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val history by viewModel.conversationHistory.collectAsState()
    val audioLevel by viewModel.speechRecognizerHelper.rmsLevel.collectAsState()

    val quickActions = listOf(
        QuickActionItem("Open YouTube", Icons.Default.PlayArrow, "Open YouTube"),
        QuickActionItem("10m Timer", Icons.Default.Timer, "Set a 10 minute timer"),
        QuickActionItem("Wi-Fi Settings", Icons.Default.Settings, "Open Wi-Fi settings"),
        QuickActionItem("AI News", Icons.Default.Search, "What are the latest AI news today?"),
        QuickActionItem("7 AM Alarm", Icons.Default.Alarm, "Set alarm for 7:00 AM"),
        QuickActionItem("Flashlight", Icons.Default.FlashlightOn, "Turn on flashlight")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CrimsonBright)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "TECNO AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPermissions, modifier = Modifier.testTag("nav_permissions_button")) {
                        Icon(Icons.Default.Security, contentDescription = "Permissions", tint = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToHistory, modifier = Modifier.testTag("nav_history_button")) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("nav_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToAbout, modifier = Modifier.testTag("nav_about_button")) {
                        Icon(Icons.Default.Info, contentDescription = "About", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAssistantOverlay(startVoice = true) },
                containerColor = CrimsonBright,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .border(2.dp, CrimsonGlow, CircleShape)
                    .testTag("main_mic_fab")
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Activate Voice Assistant", modifier = Modifier.size(32.dp))
            }
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Orb & Status Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.radialGradient(listOf(CrimsonDark, DarkBorder))),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { viewModel.openAssistantOverlay(startVoice = false) }
                        .testTag("hero_assistant_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RedOrbVisualizer(
                            state = uiState,
                            audioLevel = audioLevel,
                            size = 150.dp,
                            onClick = { viewModel.openAssistantOverlay(startVoice = true) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (userSettings.isWakeWordEnabled) "\"Hey Tecno\" is Active" else "Tap to Speak",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Say it. Tecno handles it.",
                            fontSize = 13.sp,
                            color = TextAccent
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Wake word status & persistent toggle switch
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (userSettings.isWakeWordEnabled) CrimsonSubtle else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (userSettings.isWakeWordEnabled) CrimsonDark else DarkBorder),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (userSettings.isWakeWordEnabled) SuccessGreen else TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (userSettings.isWakeWordEnabled) "\"Hey Tecno\" Active" else "Wake Word Disabled",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (userSettings.isWakeWordEnabled) "Background service listening" else "Tap switch to enable",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = userSettings.isWakeWordEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.updateWakeWordEnabled(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CrimsonBright,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBorder
                                    ),
                                    modifier = Modifier.testTag("home_wake_word_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Quick Device Actions Grid
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "QUICK ACTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(quickActions) { action ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkBorder, CrimsonDark))),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable {
                                        viewModel.openAssistantOverlay(startVoice = false)
                                        viewModel.submitPrompt(action.command)
                                    }
                                    .testTag("quick_action_${action.title.replace(" ", "_")}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(action.icon, contentDescription = action.title, tint = CrimsonBright, modifier = Modifier.size(20.dp))
                                    }
                                    Text(
                                        text = action.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Commands & Responses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextMuted
                    )
                    if (history.isNotEmpty()) {
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CrimsonBright,
                            modifier = Modifier
                                .clickable { onNavigateToHistory() }
                                .testTag("view_all_history_text")
                        )
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No recent commands yet", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try saying \"Hey Tecno, open YouTube\" or tap the mic", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                val recentItems = history.takeLast(4).reversed()
                items(recentItems, key = { it.id }) { item ->
                    ActivityFeedCard(item = item)
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun ActivityFeedCard(item: ConversationEntity) {
    val isUser = item.role == "USER"
    val isTool = item.role == "TOOL"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) DarkSurfaceVariant else DarkSurfaceElevated
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isUser) listOf(CrimsonDark, DarkBorder) else listOf(DarkBorder, DarkBorder)
            )
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isUser) CrimsonPrimary else DarkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUser) "U" else "T",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else if (isTool) "Device Action" else "Tecno AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) TextPrimary else CrimsonBright
                    )

                    if (item.toolName != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CrimsonSubtle
                        ) {
                            Text(
                                text = item.toolName,
                                fontSize = 10.sp,
                                color = TextAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.content,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
