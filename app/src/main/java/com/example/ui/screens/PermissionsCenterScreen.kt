package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.accessibility.TecnoAccessibilityService
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonSubtle
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

data class AdvancedPermissionItem(
    val title: String,
    val description: String,
    val isSystemSpecial: Boolean = false,
    val specialAction: String? = null,
    val permission: String? = null,
    val icon: ImageVector,
    val isCritical: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsCenterScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    val specialPermissions = remember {
        listOf(
            AdvancedPermissionItem(
                title = "Default Digital Assistant App",
                description = "Set Tecno AI as your system-wide default Android assistant for instant activation from power button, corner swipes, or navigation bar gestures.",
                isSystemSpecial = true,
                specialAction = Settings.ACTION_VOICE_INPUT_SETTINGS,
                icon = Icons.Default.AutoAwesome,
                isCritical = true
            ),
            AdvancedPermissionItem(
                title = "Display Over Other Apps (Overlay)",
                description = "Allows the Tecno AI floating bubble and Siri/Gemini dynamic assistant screen to appear on top of other applications.",
                isSystemSpecial = true,
                specialAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                icon = Icons.Default.Layers,
                isCritical = true
            ),
            AdvancedPermissionItem(
                title = "Tecno In-App Accessibility Control",
                description = "Enables hands-free system gestures (Back, Home, Recents, Notifications, Screenshot) and in-app automation.",
                isSystemSpecial = true,
                specialAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
                icon = Icons.Default.Accessibility,
                isCritical = true
            )
        )
    }

    val runtimePermissions = remember {
        listOf(
            AdvancedPermissionItem(
                title = "Microphone (Voice)",
                description = "Required for \"Hey Tecno\" wake phrase detection and speech-to-text recognition.",
                permission = Manifest.permission.RECORD_AUDIO,
                icon = Icons.Default.Mic,
                isCritical = true
            ),
            AdvancedPermissionItem(
                title = "Location",
                description = "Enables location-aware local weather, directions, maps, and nearby searches.",
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                icon = Icons.Default.Info
            ),
            AdvancedPermissionItem(
                title = "Contacts",
                description = "Enables resolving contact names when placing calls or sending messages hands-free.",
                permission = Manifest.permission.READ_CONTACTS,
                icon = Icons.Default.Contacts
            ),
            AdvancedPermissionItem(
                title = "Phone Calls",
                description = "Allows placing phone calls directly upon your voice command confirmation.",
                permission = Manifest.permission.CALL_PHONE,
                icon = Icons.Default.Call
            ),
            AdvancedPermissionItem(
                title = "SMS Messaging",
                description = "Enables hands-free SMS drafting and quick messaging to contacts.",
                permission = Manifest.permission.SEND_SMS,
                icon = Icons.Default.Sms
            ),
            AdvancedPermissionItem(
                title = "Notifications",
                description = "Shows the active background wake-word service status in the notification shade.",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
                icon = Icons.Default.Notifications
            )
        ).filter { it.permission != null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions & Powers", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("permissions_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, CrimsonDark))),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Superpowers & In-App Control",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant permissions below to enable the Siri/Gemini floating overlay on any screen, in-app automation gestures, and instant hands-free voice detection.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Special System Permissions (Overlay + Accessibility + Default Assistant)
            items(specialPermissions) { item ->
                val isGranted = when (item.title) {
                    "Display Over Other Apps (Overlay)" -> Settings.canDrawOverlays(context)
                    "Tecno In-App Accessibility Control" -> TecnoAccessibilityService.instance != null
                    "Default Digital Assistant App" -> {
                        try {
                            val defaultAssist = Settings.Secure.getString(context.contentResolver, "assistant") ?: ""
                            val defaultVoice = Settings.Secure.getString(context.contentResolver, "voice_interaction_service") ?: ""
                            defaultAssist.contains(context.packageName) || defaultVoice.contains(context.packageName)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    else -> false
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isGranted) listOf(DarkBorder, DarkBorder) else listOf(CrimsonDark, DarkBorder)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("perm_card_${item.title.lowercase().replace(" ", "_")}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isGranted) DarkSurface else CrimsonSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.title,
                                        tint = if (isGranted) SuccessGreen else CrimsonBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "System Superpower",
                                        fontSize = 11.sp,
                                        color = CrimsonBright,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGranted) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGranted) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isGranted) Icons.Default.Check else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isGranted) SuccessGreen else WarningAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGranted) "Enabled" else "Required",
                                        fontSize = 11.sp,
                                        color = if (isGranted) SuccessGreen else WarningAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        if (!isGranted && item.specialAction != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(item.specialAction).apply {
                                        if (item.specialAction == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enable_${item.title.take(10).lowercase()}_button")
                            ) {
                                Text("Enable in System Settings", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Standard Runtime Permissions
            items(runtimePermissions) { item ->
                val perm = item.permission ?: return@items
                val isGranted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isGranted) listOf(DarkBorder, DarkBorder) else listOf(CrimsonDark, DarkBorder)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("perm_card_${item.title.lowercase()}")
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isGranted) DarkSurface else CrimsonSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.title,
                                        tint = if (isGranted) SuccessGreen else CrimsonBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (item.isCritical) {
                                        Text(
                                            text = "Required for Voice",
                                            fontSize = 11.sp,
                                            color = CrimsonBright,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGranted) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGranted) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isGranted) Icons.Default.Check else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isGranted) SuccessGreen else WarningAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGranted) "Granted" else "Not Granted",
                                        fontSize = 11.sp,
                                        color = if (isGranted) SuccessGreen else WarningAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        if (!isGranted) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(perm)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonBright),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_${item.title.lowercase()}_button")
                            ) {
                                Text("Allow ${item.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.fillMaxWidth().testTag("open_system_app_settings")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Device App Settings", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
