package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speech.VoiceTestStage
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
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantUiState
import com.example.ui.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEnrollmentScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceProfile by viewModel.voiceProfile.collectAsState()
    val testState by viewModel.voiceTrainer.testState.collectAsState()
    val animatedProgress by animateFloatAsState(targetValue = testState.progressPercent, label = "enrollment_progress")

    var speakerNameInput by remember { mutableStateOf(voiceProfile.speakerName) }
    val isTesting = testState.stage != VoiceTestStage.READY && testState.stage != VoiceTestStage.COMPLETED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Voice Recognition Model",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Visualizer & Status Orb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceElevated)
                    .border(
                        1.5.dp,
                        Brush.radialGradient(listOf(CrimsonBright.copy(alpha = 0.6f), DarkBorder)),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RedOrbVisualizer(
                        state = if (isTesting) AssistantUiState.LISTENING else AssistantUiState.IDLE,
                        audioLevel = testState.liveVolume,
                        size = 140.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (voiceProfile.isEnrolled) "Voice Model Enrolled" else "No Voice Model Enrolled",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (voiceProfile.isEnrolled) SuccessGreen else WarningAmber
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (voiceProfile.isEnrolled) {
                            "Calibrated for: ${voiceProfile.speakerName}"
                        } else {
                            "Take a voice calibration test to train Tecno AI on your voice profile like Gemini."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Active Voice Test Calibration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CrimsonBright, CrimsonDark))
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_test_container_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = CrimsonBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Calibration Test",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (isTesting) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CrimsonBright
                            ) {
                                Text(
                                    text = "RECORDING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isTesting && testState.stage != VoiceTestStage.COMPLETED) {
                        OutlinedTextField(
                            value = speakerNameInput,
                            onValueChange = { speakerNameInput = it },
                            label = { Text("Your Name / Speaker Label", color = TextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonBright,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("speaker_name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.startVoiceTest(speakerNameInput.ifBlank { "User" })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_voice_test_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Begin Voice Test (3 Phrases)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    } else {
                        // Live phrase prompt
                        Text(
                            text = testState.statusMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CrimsonBright
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testState.promptPhrase,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = CrimsonBright,
                            trackColor = DarkBorder
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Samples: ${testState.capturedSamplesCount}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "Pitch: ${testState.livePitchHz.toInt()} Hz",
                                fontSize = 11.sp,
                                color = TextAccent
                            )
                        }

                        if (testState.stage == VoiceTestStage.COMPLETED) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.resetVoiceTrainer()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("finish_voice_test_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Completed & Saved", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Saved Voice Data Summary (Gemini-style Profile)
            if (voiceProfile.isEnrolled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, CrimsonDark))),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_voice_data_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saved Acoustic Voiceprint", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            }

                            IconButton(
                                onClick = { viewModel.resetEnrolledVoice() },
                                modifier = Modifier.size(32.dp).testTag("delete_voice_profile_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = WarningAmber, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        VoiceDataMetricRow(label = "Speaker Name", value = voiceProfile.speakerName)
                        VoiceDataMetricRow(label = "Fundamental Pitch (F0)", value = "${voiceProfile.averagePitchHz.toInt()} Hz")
                        VoiceDataMetricRow(label = "Spectral Centroid (Timbre)", value = "${voiceProfile.spectralCentroid.toInt()} Hz")
                        VoiceDataMetricRow(label = "Energy Signature", value = "${voiceProfile.averageEnergy.toInt()} dB RMS")
                        VoiceDataMetricRow(label = "Confidence Gate", value = "${(voiceProfile.confidenceThreshold * 100).toInt()}% match")
                        val formattedDate = remember(voiceProfile.enrollmentDateTimestamp) {
                            if (voiceProfile.enrollmentDateTimestamp > 0L) {
                                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(voiceProfile.enrollmentDateTimestamp))
                            } else "Active"
                        }
                        VoiceDataMetricRow(label = "Enrollment Date", value = formattedDate)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceDataMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
