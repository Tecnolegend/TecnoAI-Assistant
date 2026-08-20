package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.UpdateNewsDialog
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSubtle
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUpdateNews by remember { mutableStateOf(false) }

    if (showUpdateNews) {
        UpdateNewsDialog(
            onDismiss = { showUpdateNews = false },
            onExplorePowers = { showUpdateNews = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Tecno AI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("about_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showUpdateNews = true }, modifier = Modifier.testTag("about_update_news_button")) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "What's New", tint = CrimsonBright)
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
            // Brand Hero Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CrimsonBright, CrimsonDark))),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CrimsonBright, CrimsonDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TECNO AI 2.0",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = TextPrimary
                        )

                        Text(
                            text = "Created & Owned by Tecnolegend",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonBright
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Version 2.0.0 (Production Release)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "\"Say it. Tecno handles it.\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextAccent
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showUpdateNews = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("view_release_notes_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("What's New in Update 2.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Owner Profile Card
            item {
                Text("CREATOR & OWNERSHIP", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = TextMuted)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimsonDark, DarkBorder))),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CrimsonSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Owner: Tecnolegend", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Architect & Creator of the Tecno AI Assistant ecosystem", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Core Pillars
            item {
                Text("CORE CAPABILITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = TextMuted)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder))),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FeatureRow("Ultra-Fast Device Superpowers", "Controls volume, battery, device specs, timer, flashlight, Wi-Fi, apps, and phone calls instantly.")
                        FeatureRow("Tecno Neural AI Consulting & Vision", "Deep reasoning, in-app screen perception, and fast answers powered by Tecno AI.")
                        FeatureRow("Siri-Style Bottom Wave", "Fluid crimson gradient bottom sheet that springs up instantly upon activation.")
                        FeatureRow("Multi-Voice Synthesis (7 Voices)", "Select from 7 distinct AI voices and customized acoustic activation chimes.")
                        FeatureRow("Continuous \"Hey Tecno\" Wake Word", "Background acoustic detector listens for the wake phrase with configurable sensitivity.")
                    }
                }
            }

            // Security & Privacy
            item {
                Text("SAFETY & PERMISSIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = TextMuted)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder))),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Explicit Confirmation for Sensitive Actions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Actions that initiate phone calls or send messages prompt for your one-tap approval before dispatching.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun FeatureRow(title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(CrimsonBright)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = description, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
