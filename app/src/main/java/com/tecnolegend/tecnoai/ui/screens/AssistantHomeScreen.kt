package com.tecnolegend.tecnoai.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecnolegend.tecnoai.data.PrebuiltTask
import com.tecnolegend.tecnoai.data.PrebuiltTasksRepository
import com.tecnolegend.tecnoai.data.TaskCategory
import com.tecnolegend.tecnoai.data.local.ConversationEntity
import com.tecnolegend.tecnoai.ui.components.RedOrbVisualizer
import com.tecnolegend.tecnoai.ui.components.UpdateNewsDialog
import com.tecnolegend.tecnoai.ui.theme.CrimsonBright
import com.tecnolegend.tecnoai.ui.theme.CrimsonDark
import com.tecnolegend.tecnoai.ui.theme.CrimsonGlow
import com.tecnolegend.tecnoai.ui.theme.CrimsonPrimary
import com.tecnolegend.tecnoai.ui.theme.CrimsonSubtle
import com.tecnolegend.tecnoai.ui.theme.DarkBackground
import com.tecnolegend.tecnoai.ui.theme.DarkBorder
import com.tecnolegend.tecnoai.ui.theme.DarkSurface
import com.tecnolegend.tecnoai.ui.theme.DarkSurfaceElevated
import com.tecnolegend.tecnoai.ui.theme.DarkSurfaceVariant
import com.tecnolegend.tecnoai.ui.theme.SuccessGreen
import com.tecnolegend.tecnoai.ui.theme.TextAccent
import com.tecnolegend.tecnoai.ui.theme.TextMuted
import com.tecnolegend.tecnoai.ui.theme.TextPrimary
import com.tecnolegend.tecnoai.ui.theme.TextSecondary
import com.tecnolegend.tecnoai.ui.theme.WarningAmber
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantUiState
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantHomeScreen(
    viewModel: AssistantViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToVoiceModel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val history by viewModel.conversationHistory.collectAsState()
    val audioLevel by viewModel.audioVolumeMeterHelper.normalizedVolume.collectAsState()
    val voiceProfile by viewModel.voiceProfile.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val lastLatencyMs by viewModel.lastExecutionLatencyMs.collectAsState()
    val isOfflineEngineHandled by viewModel.isOfflineEngineHandled.collectAsState()
    val personalizedSuggestions by viewModel.personalizedSuggestions.collectAsState()

    var taskSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }
    var showUpdateNews by remember { mutableStateOf(false) }

    if (showUpdateNews) {
        UpdateNewsDialog(
            onDismiss = { showUpdateNews = false },
            onExplorePowers = { showUpdateNews = false }
        )
    }

    val filteredTasks = remember(taskSearchQuery, selectedCategory) {
        PrebuiltTasksRepository.searchTasks(taskSearchQuery, selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
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
                    IconButton(onClick = { showUpdateNews = true }, modifier = Modifier.testTag("nav_update_news_button")) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "What's New in Update 2.0", tint = CrimsonBright)
                    }
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
            // Siri/Gemini Dynamic Visualizer Card
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
                            text = "Say it. Tecno controls your device.",
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

            // Voice Recognition Model (Gemini-style Profile Calibration) Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (voiceProfile.isEnrolled) listOf(SuccessGreen, DarkBorder)
                            else listOf(CrimsonBright, CrimsonDark)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToVoiceModel() }
                        .testTag("voice_model_profile_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (voiceProfile.isEnrolled) SuccessGreen.copy(alpha = 0.15f) else CrimsonSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (voiceProfile.isEnrolled) SuccessGreen else CrimsonBright,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (voiceProfile.isEnrolled) "Voice Model Calibrated" else "Train Voice Model",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (voiceProfile.isEnrolled) "Speaker: ${voiceProfile.speakerName} • Tap to view data" else "Take test to recognize your voice like Gemini",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Test Voice",
                            tint = if (voiceProfile.isEnrolled) SuccessGreen else CrimsonBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Offline Processing Engine & Speed Layer Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF00F5D4).copy(alpha = 0.6f), CrimsonDark)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_engine_status_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00F5D4).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Offline Processing Engine",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (networkState.isOnline) "Active • Sub-5ms Local-First Execution" else "⚡ OFFLINE MODE ACTIVE • Critical Powers Ready",
                                        fontSize = 11.sp,
                                        color = if (networkState.isOnline) Color(0xFF00F5D4) else WarningAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (lastLatencyMs != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isOfflineEngineHandled) Color(0xFF00F5D4).copy(alpha = 0.15f) else DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isOfflineEngineHandled) Color(0xFF00F5D4).copy(alpha = 0.4f) else DarkBorder
                                    )
                                ) {
                                    Text(
                                        text = if (isOfflineEngineHandled) "${lastLatencyMs}ms (Instant)" else "${lastLatencyMs}ms",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOfflineEngineHandled) Color(0xFF00F5D4) else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Critical device controls run 100% on-device with zero network latency, ensuring instant response even when internet connection is unstable or offline.",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick offline command triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Open Settings", "Stop Music", "Flashlight", "Volume 80%").forEach { cmd ->
                                Surface(
                                    onClick = {
                                        viewModel.openAssistantOverlay(startVoice = false)
                                        viewModel.submitPrompt(cmd)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = cmd,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Personalized Smart Suggestions Based on Activity & Learned Contacts
            if (personalizedSuggestions.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SUGGESTIONS FOR YOU",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = CrimsonBright
                                )
                            }
                            Text(
                                text = "Learned Habits",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(personalizedSuggestions) { suggestion ->
                                Surface(
                                    onClick = {
                                        viewModel.openAssistantOverlay(startVoice = false)
                                        viewModel.submitPrompt(suggestion.suggestedPrompt)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.testTag("suggestion_chip_${suggestion.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = suggestion.icon,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column {
                                            Text(
                                                text = suggestion.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = suggestion.subtitle,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Prebuilt Tasks Library & In-App Control Hub
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREBUILT POWERS & TASKS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "${filteredTasks.size} Tasks",
                            fontSize = 12.sp,
                            color = CrimsonBright,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Task Search Bar
                    OutlinedTextField(
                        value = taskSearchQuery,
                        onValueChange = { taskSearchQuery = it },
                        placeholder = { Text("Search 1,000+ prebuilt tasks & powers...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (taskSearchQuery.isNotBlank()) {
                                IconButton(onClick = { taskSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonBright,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("search_tasks_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(PrebuiltTasksRepository.categories) { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                onClick = { selectedCategory = category },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) CrimsonBright else DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CrimsonGlow else DarkBorder),
                                modifier = Modifier.testTag("cat_chip_${category.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(category.icon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = category.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Task Cards
            items(filteredTasks, key = { it.id }) { task ->
                PrebuiltTaskCard(
                    task = task,
                    onExecute = {
                        viewModel.openAssistantOverlay(startVoice = false)
                        viewModel.submitPrompt(task.executeIntentText)
                    }
                )
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
                            Text("Try saying \"Hey Tecno, open YouTube\" or tap any task above", color = TextMuted, fontSize = 12.sp)
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
fun PrebuiltTaskCard(
    task: PrebuiltTask,
    onExecute: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, CrimsonDark))),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExecute() }
            .testTag("prebuilt_task_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CrimsonSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForTask(task.iconName),
                    contentDescription = task.title,
                    tint = CrimsonBright,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (task.isDeepControl) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = CrimsonPrimary) {
                            Text("Deep App", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🎤 \"${task.voicePhrase}\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonBright
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = CrimsonBright,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run task", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun getIconForTask(iconName: String): ImageVector {
    return when (iconName) {
        "youtube" -> Icons.Default.PlayArrow
        "spotify" -> Icons.Default.VolumeUp
        "camera" -> Icons.Default.CameraAlt
        "videocam" -> Icons.Default.Videocam
        "flashlight", "flashlight_off" -> Icons.Default.FlashlightOn
        "volume_up", "volume_down", "volume_off" -> Icons.Default.VolumeUp
        "wifi", "bluetooth" -> Icons.Default.Settings
        "arrow_back" -> Icons.Default.ArrowBack
        "home" -> Icons.Default.Home
        "timer" -> Icons.Default.Timer
        "alarm" -> Icons.Default.Alarm
        "navigation", "local_cafe", "local_gas_station" -> Icons.Default.Navigation
        "battery_full", "battery_charging" -> Icons.Default.BatteryChargingFull
        "calculate" -> Icons.Default.AutoAwesome
        else -> Icons.Default.AutoAwesome
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
