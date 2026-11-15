package com.tecnolegend.tecnoai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tecnolegend.tecnoai.ui.theme.CrimsonBright
import com.tecnolegend.tecnoai.ui.theme.CrimsonDark
import com.tecnolegend.tecnoai.ui.theme.CrimsonPrimary
import com.tecnolegend.tecnoai.ui.theme.CrimsonSubtle
import com.tecnolegend.tecnoai.ui.theme.DarkBackground
import com.tecnolegend.tecnoai.ui.theme.DarkBorder
import com.tecnolegend.tecnoai.ui.theme.DarkSurface
import com.tecnolegend.tecnoai.ui.theme.DarkSurfaceElevated
import com.tecnolegend.tecnoai.ui.theme.SuccessGreen
import com.tecnolegend.tecnoai.ui.theme.TextAccent
import com.tecnolegend.tecnoai.ui.theme.TextMuted
import com.tecnolegend.tecnoai.ui.theme.TextPrimary
import com.tecnolegend.tecnoai.ui.theme.TextSecondary

data class UpdateFeatureNews(
    val title: String,
    val description: String,
    val tag: String,
    val icon: ImageVector
)

val LATEST_UPDATE_NEWS = listOf(
    UpdateFeatureNews(
        title = "System-Wide Siri/Gemini Overlay",
        description = "When you say \"Hey Tecno\" or tap the floating bubble in any app, the assistant response UI opens directly on your screen without leaving what you are doing.",
        tag = "Overlay",
        icon = Icons.Default.Layers
    ),
    UpdateFeatureNews(
        title = "Fixed Voice Model Calibration",
        description = "Complete 3-phrase voice calibration with real-time waveform level monitoring and custom speaker verification.",
        tag = "Voice AI",
        icon = Icons.Default.Mic
    ),
    UpdateFeatureNews(
        title = "Custom Activation Sound Chimes",
        description = "Choose from 4 futuristic activation audio cues (Crimson Cyber, Neural Pulse, Hyper Ascent, Minimal Pop).",
        tag = "Audio",
        icon = Icons.Default.GraphicEq
    ),
    UpdateFeatureNews(
        title = "Default Digital Assistant Permission",
        description = "Set Tecno AI as your system default assistant to trigger with long-press Home or corner navigation gestures.",
        tag = "System",
        icon = Icons.Default.Security
    ),
    UpdateFeatureNews(
        title = "Offline Processing Layer (0-5ms Response)",
        description = "Critical commands like 'open settings', 'stop music', flashlight, and app launches execute instantly on-device with zero delay, even with unstable or disconnected internet.",
        tag = "Offline",
        icon = Icons.Default.Speed
    ),
    UpdateFeatureNews(
        title = "Expanded 1,000+ Prebuilt Intent Powers",
        description = "Sub-5ms instant calculations, deep app launches, in-app navigation gestures, flashlight, volume, battery, and AI vision reasoning.",
        tag = "Speed",
        icon = Icons.Default.Speed
    ),
    UpdateFeatureNews(
        title = "Real-Time Bug Tracker & Diagnostics",
        description = "View real-time error logs, system telemetry, failover key health, and one-tap bug reporting in Settings.",
        tag = "Stability",
        icon = Icons.Default.BugReport
    )
)

@Composable
fun UpdateNewsDialog(
    onDismiss: () -> Unit,
    onExplorePowers: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(CrimsonBright, CrimsonDark, DarkBorder))
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("update_news_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CrimsonBright, CrimsonDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("WHAT'S NEW IN UPDATE 2.0", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                            Text("Tecno AI • Owner: Tecnolegend", fontSize = 11.sp, color = CrimsonBright, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_update_news_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Welcome to Tecno AI 2.0! Here are the latest capabilities and enhancements designed for deep system control and instant responses.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable News Items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(LATEST_UPDATE_NEWS) { item ->
                        UpdateNewsCard(item = item)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onExplorePowers()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("got_it_update_button")
                ) {
                    Text("Got It • Explore Superpowers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun UpdateNewsCard(item: UpdateFeatureNews) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CrimsonSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CrimsonPrimary
                    ) {
                        Text(
                            text = item.tag,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.description,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
