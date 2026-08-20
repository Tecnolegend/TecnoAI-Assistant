package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiNetworkClient
import com.example.speech.SoundCueHelper
import com.example.speech.VoicePresets
import com.example.tools.ToolRegistry
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDark
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
import com.example.ui.viewmodel.AssistantViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val authUser = rememberUserAuth()
    val coroutineScope = rememberCoroutineScope()

    val models = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Recommended - Ultra Fast)",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Deep AI Consulting)",
        "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash Lite (Low Latency)"
    )

    val soundThemes = listOf(
        SoundCueHelper.SoundTheme.CRIMSON_CYBER,
        SoundCueHelper.SoundTheme.NEURAL_PULSE,
        SoundCueHelper.SoundTheme.HYPER_ASCENT,
        SoundCueHelper.SoundTheme.MINIMAL_POP,
        SoundCueHelper.SoundTheme.MUTE
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Superpowers", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // About & Owner Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CrimsonBright, CrimsonDark))
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("owner_info_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("TECNO AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Owner & Creator: Tecnolegend", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CrimsonBright)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Next-gen intelligent mobile assistant with full Android device powers, instant local execution, Siri-style bottom activation, and deep Gemini AI consultation.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Voice & Activation Section
            item {
                SettingsSectionHeader(title = "VOICE & ACTIVATION")
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = "\"Hey Tecno\" Wake Word",
                        subtitle = "Runs background acoustic detection for the wake phrase.",
                        icon = Icons.Default.Mic,
                        checked = settings.isWakeWordEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateWakeWordEnabled(enabled)
                        },
                        tag = "toggle_wake_word"
                    )

                    if (settings.isWakeWordEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Wake Word Sensitivity: ${(settings.wakeWordSensitivity * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Slider(
                            value = settings.wakeWordSensitivity,
                            onValueChange = { viewModel.updateSensitivity(it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CrimsonBright,
                                activeTrackColor = CrimsonPrimary,
                                inactiveTrackColor = DarkBorder
                            ),
                            modifier = Modifier.testTag("slider_sensitivity")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Voice Speech Output (TTS)",
                        subtitle = "Speak answers and tool execution results out loud.",
                        icon = Icons.Default.VolumeUp,
                        checked = settings.isTtsEnabled,
                        onCheckedChange = { viewModel.updateTtsEnabled(it) },
                        tag = "toggle_tts"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Haptic Feedback",
                        subtitle = "Vibrate subtly when wake word triggers or mic starts.",
                        icon = Icons.Default.Vibration,
                        checked = settings.isHapticEnabled,
                        onCheckedChange = { viewModel.updateHapticEnabled(it) },
                        tag = "toggle_haptic"
                    )
                }
            }

            // Custom Voice Profiles Selection
            item {
                SettingsSectionHeader(title = "AI VOICE PROFILES (7 VOICES)")
            }

            item {
                SettingsCard {
                    VoicePresets.allVoices.forEach { voice ->
                        val isSelected = settings.voiceId == voice.id
                        Surface(
                            onClick = { viewModel.updateVoiceId(voice.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CrimsonSubtle else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CrimsonBright else DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .testTag("voice_option_${voice.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = voice.name,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DarkSurfaceElevated
                                        ) {
                                            Text(
                                                text = voice.gender,
                                                fontSize = 10.sp,
                                                color = CrimsonBright,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = voice.description,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            viewModel.textToSpeechHelper.setVoiceProfile(voice.id)
                                            viewModel.textToSpeechHelper.speak("Hello, I am ${voice.name}, powered by Tecno AI.")
                                        },
                                        modifier = Modifier.size(32.dp).testTag("preview_voice_${voice.id}")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview voice", tint = CrimsonBright)
                                    }

                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CrimsonBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sound Activation Chime Selection
            item {
                SettingsSectionHeader(title = "ACTIVATION SOUND CHIMES")
            }

            item {
                SettingsCard {
                    soundThemes.forEach { soundTheme ->
                        val isSelected = settings.soundTheme == soundTheme.name
                        Surface(
                            onClick = {
                                viewModel.updateSoundTheme(soundTheme.name)
                                viewModel.soundCueHelper.playActivationSound(soundTheme)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CrimsonSubtle else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CrimsonBright else DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .testTag("sound_theme_${soundTheme.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = soundTheme.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.soundCueHelper.playActivationSound(soundTheme) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = "Play sound", tint = CrimsonBright)
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CrimsonBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Security & Safety Section
            item {
                SettingsSectionHeader(title = "SECURITY & PERMISSIONS")
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Confirm Sensitive Actions",
                        subtitle = "Require one-tap confirmation before making phone calls or sending SMS.",
                        icon = Icons.Default.Security,
                        checked = settings.confirmSensitiveActions,
                        onCheckedChange = { viewModel.updateConfirmSensitive(it) },
                        tag = "toggle_confirm_sensitive"
                    )
                }
            }

            // Gemini Model Selection
            item {
                SettingsSectionHeader(title = "AI INTELLIGENCE & CONSULTING MODEL")
            }

            item {
                SettingsCard {
                    models.forEach { (modelId, label) ->
                        val isSelected = settings.selectedModel == modelId
                        Surface(
                            onClick = { viewModel.updateSelectedModel(modelId) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CrimsonSubtle else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CrimsonBright else DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("model_select_$modelId")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = CrimsonBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cloud & Firebase Account Status
            item {
                SettingsSectionHeader(title = "CLOUD PERSISTENCE & ACCOUNT")
            }

            item {
                val authState by viewModel.authManager.authState.collectAsState()
                val activity = context as? android.app.Activity

                SettingsCard {
                    when (val state = authState) {
                        is com.example.auth.AuthState.Authenticated -> {
                            val user = state.user
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(CrimsonSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = CrimsonBright,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName ?: user.email ?: "Google Account",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = user.email ?: "",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "● Cloud Firestore Sync Active",
                                            fontSize = 10.sp,
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.authManager.signOut()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                            ) {
                                Text("Sign Out of Google", fontSize = 13.sp)
                            }
                        }

                        is com.example.auth.AuthState.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = CrimsonBright,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Signing in with Google...", color = TextSecondary, fontSize = 13.sp)
                            }
                        }

                        else -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Offline / Local Mode",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Sign in to backup conversations to Firebase Firestore.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            coroutineScope.launch {
                                                viewModel.authManager.signInWithGoogle(activity)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button")
                                ) {
                                    Text("Sign In with Google", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // System Diagnostics
            item {
                SettingsSectionHeader(title = "DIAGNOSTICS & SYSTEM POWERS")
            }

            item {
                SettingsCard {
                    val apiKey = GeminiNetworkClient.getApiKey()
                    val hasKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

                    DiagnosticRow("Registered Mobile Powers", "${ToolRegistry.allTools.size} Powers Active")
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Owner & Developer", "Tecnolegend")
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Gemini AI Intelligence", if (hasKey) "Online & Connected" else "Direct Tool Mode")
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Wake Word Service", if (settings.isWakeWordEnabled) "Background Listening Active" else "Disabled")
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun rememberUserAuth(): String? {
    return try {
        FirebaseAuth.getInstance().currentUser?.email
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder))),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (checked) CrimsonSubtle else DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) CrimsonBright else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CrimsonBright,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurface
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
