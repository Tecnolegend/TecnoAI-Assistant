package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    onNavigateToVoiceModel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val voiceProfile by viewModel.voiceProfile.collectAsState()
    val authUser = rememberUserAuth()
    val coroutineScope = rememberCoroutineScope()

    val models = listOf(
        "gemini-2.5-flash" to "Tecno Neural Flash 2.5 (Fast & Responsive - Recommended)",
        "gemini-flash-latest" to "Tecno Neural Flash Ultra (Sub-second Latency)"
    )

    val soundThemes = listOf(
        SoundCueHelper.SoundTheme.CRIMSON_CYBER,
        SoundCueHelper.SoundTheme.NEURAL_PULSE,
        SoundCueHelper.SoundTheme.HYPER_ASCENT,
        SoundCueHelper.SoundTheme.MINIMAL_POP,
        SoundCueHelper.SoundTheme.MUTE
    )

    val languages = listOf(
        "default" to "Default Device Language",
        "en-US" to "English (United States)",
        "en-GB" to "English (United Kingdom)",
        "es-ES" to "Spanish (Español)",
        "fr-FR" to "French (Français)",
        "de-DE" to "German (Deutsch)",
        "hi-IN" to "Hindi (हिन्दी)",
        "zh-CN" to "Chinese (Mandarin)"
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
                                Text("TECNO AI 2.0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Owner & Creator: Tecnolegend", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CrimsonBright)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Next-generation intelligent Android voice assistant with deep device powers, instant in-app screen controls, sub-5ms instant responses, and 5-key neural failover.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 5-KEY FAILOVER POOL SECTION
            item {
                SettingsSectionHeader(title = "TECNO AI 5-KEY FAILOVER POOL")
            }

            item {
                SettingsCard {
                    Text(
                        text = "5-Key Automatic Failover System",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Configure up to 5 API keys. If one key reaches rate limits or encounters an error, Tecno AI instantly switches to the next available key without interruption.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val keys = listOf(
                        Triple(1, "Primary Key (Slot 1)", settings.apiKey1),
                        Triple(2, "Backup Key 1 (Slot 2)", settings.apiKey2),
                        Triple(3, "Backup Key 2 (Slot 3)", settings.apiKey3),
                        Triple(4, "Backup Key 3 (Slot 4)", settings.apiKey4),
                        Triple(5, "Backup Key 5 (Slot 5)", settings.apiKey5)
                    )

                    keys.forEach { (index, label, currentKey) ->
                        ApiKeySlotInput(
                            index = index,
                            label = label,
                            savedKey = currentKey,
                            onSaveKey = { newKey ->
                                viewModel.updateApiKey(index, newKey)
                            }
                        )
                        if (index < 5) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Floating Overlay Superpower
            item {
                SettingsSectionHeader(title = "SYSTEM OVERLAY & FLOATING BUBBLE")
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Floating Bubble Over Other Apps",
                        subtitle = "Shows a floating quick-access red badge that stays over other apps for instant one-tap activation.",
                        icon = Icons.Default.Layers,
                        checked = settings.isOverlayBubbleEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            } else {
                                viewModel.updateOverlayBubbleEnabled(enabled)
                            }
                        },
                        tag = "toggle_floating_bubble"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Auto-Listen on Overlay Open",
                        subtitle = "Immediately activate microphone when assistant is opened.",
                        icon = Icons.Default.Mic,
                        checked = settings.autoListenOnOverlayOpen,
                        onCheckedChange = { viewModel.updateAutoListenOnOpen(it) },
                        tag = "toggle_auto_listen"
                    )
                }
            }

            // Voice Recognition & Calibration
            item {
                SettingsSectionHeader(title = "VOICE RECOGNITION & CALIBRATION")
            }

            item {
                SettingsCard {
                    Surface(
                        onClick = onNavigateToVoiceModel,
                        shape = RoundedCornerShape(12.dp),
                        color = if (voiceProfile.isEnrolled) SuccessGreen.copy(alpha = 0.12f) else CrimsonSubtle,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (voiceProfile.isEnrolled) SuccessGreen.copy(alpha = 0.4f) else CrimsonBright.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("voice_calibration_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (voiceProfile.isEnrolled) SuccessGreen.copy(alpha = 0.2f) else CrimsonSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (voiceProfile.isEnrolled) SuccessGreen else CrimsonBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (voiceProfile.isEnrolled) "Voice Model: Enrolled (${voiceProfile.speakerName})" else "Voice Model: Not Calibrated",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (voiceProfile.isEnrolled) "Tap to test or re-calibrate your voice profile" else "Take test to save your unique voice profile in Tecno AI",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (voiceProfile.isEnrolled) SuccessGreen else CrimsonBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Microphone Volume Sensitivity Gain: ${(settings.micSensitivityGain * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Boost this if the AI has trouble picking up your voice in noisy or quiet environments.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Slider(
                        value = settings.micSensitivityGain,
                        onValueChange = { viewModel.updateMicSensitivityGain(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonBright,
                            activeTrackColor = CrimsonPrimary,
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier.testTag("slider_mic_gain")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Speech Recognition Language",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    languages.forEach { (code, label) ->
                        val isSelected = settings.speechLanguage == code
                        Surface(
                            onClick = { viewModel.updateSpeechLanguage(code) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CrimsonSubtle else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CrimsonBright else DarkBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, fontSize = 12.sp, color = if (isSelected) TextPrimary else TextSecondary)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
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

            // Neural Model Selection
            item {
                SettingsSectionHeader(title = "TECNO AI NEURAL MODEL ENGINE")
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
                    DiagnosticRow("Registered Mobile Powers", "${ToolRegistry.allTools.size} Superpowers Active")
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Owner & Developer", "Tecnolegend")
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Tecno AI Neural Engine", "Active (5-Key Failover Enabled)")
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
fun ApiKeySlotInput(
    index: Int,
    label: String,
    savedKey: String,
    onSaveKey: (String) -> Unit
) {
    var textValue by remember(savedKey) { mutableStateOf(savedKey) }
    var isVisible by remember { mutableStateOf(false) }
    val isConfigured = savedKey.isNotBlank()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isConfigured) CrimsonBright.copy(alpha = 0.5f) else DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = if (isConfigured) CrimsonBright else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConfigured) TextPrimary else TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isConfigured) SuccessGreen.copy(alpha = 0.15f) else DarkSurfaceVariant
                ) {
                    Text(
                        text = if (isConfigured) "● Active" else "Empty",
                        fontSize = 10.sp,
                        color = if (isConfigured) SuccessGreen else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = { Text("Paste Key #$index here", fontSize = 12.sp, color = TextMuted) },
                    visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonBright,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSaveKey(textValue) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (textValue != savedKey) CrimsonBright else DarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_key_$index")
                ) {
                    Text("Save", fontSize = 12.sp, color = if (textValue != savedKey) Color.White else TextMuted)
                }
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
