package com.tecnolegend.tecnoai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tecnolegend.tecnoai.ui.theme.CrimsonBright
import com.tecnolegend.tecnoai.ui.theme.CrimsonDark
import com.tecnolegend.tecnoai.ui.theme.CrimsonGlow
import com.tecnolegend.tecnoai.ui.theme.CrimsonPrimary
import com.tecnolegend.tecnoai.ui.viewmodel.AssistantUiState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Red Gradient Response Ball Visualizer
 * - Multi-layered fiery crimson & scarlet radial gradients
 * - Fluid morphing inner light flames (Ruby, Bright Red, Coral, Warm Sunset Amber, Wine)
 * - 3D glossy specular glass highlights and voice-reactive volumetric pulse
 */
@Composable
fun RedOrbVisualizer(
    state: AssistantUiState,
    audioLevel: Float = 0f,
    size: Dp = 130.dp,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val entranceScale = remember { Animatable(0.2f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "red_orb_filaments")

    // Rotation of inner swirling red light ribbons
    val filamentRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantUiState.LISTENING) 2000 else 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "red_rot_1"
    )

    val filamentRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantUiState.LISTENING) 2800 else 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "red_rot_2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantUiState.LISTENING) 500 else 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_pulse"
    )

    val coreGlowPhase by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_glow_phase"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("red_orb_visualizer")
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.3f) * entranceScale.value
            val dynamicAmp = when (state) {
                AssistantUiState.LISTENING -> (audioLevel * 0.65f).coerceIn(0.05f, 0.85f)
                AssistantUiState.THINKING, AssistantUiState.EXECUTING -> 0.35f
                AssistantUiState.SPEAKING -> 0.45f
                else -> 0.05f
            }

            val orbRadius = baseRadius * (pulseScale + dynamicAmp * 0.28f)

            // 1. Outer Dark Red Soft Drop Shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x99000000), Color.Transparent),
                    center = Offset(center.x, center.y + orbRadius * 0.35f),
                    radius = orbRadius * 1.45f
                ),
                radius = orbRadius * 1.4f,
                center = Offset(center.x, center.y + orbRadius * 0.3f)
            )

            // 2. Ambient Red Glow Halo (Deep Crimson & Scarlet aura)
            val glowRadius = orbRadius * (1.4f + dynamicAmp * 0.55f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrimsonBright.copy(alpha = 0.7f),
                        CrimsonPrimary.copy(alpha = 0.45f),
                        Color(0xFFFF1744).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 3. Dark Wine Base Sphere (Glossy Deep Crimson Body)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B000C),
                        Color(0xFF220005),
                        Color(0xFF0F0003)
                    ),
                    center = Offset(center.x, center.y + orbRadius * 0.1f),
                    radius = orbRadius
                ),
                radius = orbRadius,
                center = center
            )

            // 4. Fiery Crimson & Scarlet Inner Swirling Ribbon Waves
            // Layer A: Vibrant Scarlet / Coral Flame Wave
            val redPath1 = Path()
            for (i in 0..360 step 8) {
                val rad = Math.toRadians(i.toDouble())
                val waveOffset = sin(rad * 3 + Math.toRadians(filamentRotation1.toDouble())) * (orbRadius * (0.3f + dynamicAmp * 0.4f))
                val r = (orbRadius * 0.75f) + waveOffset.toFloat()
                val x = (center.x + r * cos(rad)).toFloat()
                val y = (center.y + r * sin(rad)).toFloat()
                if (i == 0) redPath1.moveTo(x, y) else redPath1.lineTo(x, y)
            }
            redPath1.close()

            drawPath(
                path = redPath1,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF1744).copy(alpha = 0.9f),
                        Color(0xFFFF5252).copy(alpha = 0.75f),
                        Color(0xFFFF6D00).copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - orbRadius * 0.2f, center.y - orbRadius * 0.1f),
                    radius = orbRadius * 0.82f
                ),
                style = Fill
            )

            // Layer B: Deep Crimson / Ruby Flame Wave
            val redPath2 = Path()
            for (i in 0..360 step 8) {
                val rad = Math.toRadians(i.toDouble())
                val waveOffset = cos(rad * 4 + Math.toRadians(filamentRotation2.toDouble())) * (orbRadius * (0.34f + dynamicAmp * 0.45f))
                val r = (orbRadius * 0.78f) + waveOffset.toFloat()
                val x = (center.x + r * cos(rad)).toFloat()
                val y = (center.y + r * sin(rad)).toFloat()
                if (i == 0) redPath2.moveTo(x, y) else redPath2.lineTo(x, y)
            }
            redPath2.close()

            drawPath(
                path = redPath2,
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrimsonBright.copy(alpha = 0.95f),
                        CrimsonPrimary.copy(alpha = 0.85f),
                        Color(0xFFD50000).copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(center.x + orbRadius * 0.2f, center.y + orbRadius * 0.1f),
                    radius = orbRadius * 0.88f
                ),
                style = Fill
            )

            // Layer C: Radiant White-Hot / Pink-White Core Energy
            val coreSize = orbRadius * (0.42f + dynamicAmp * 0.3f) * coreGlowPhase
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color(0xFFFFEBEE).copy(alpha = 0.92f),
                        Color(0xFFFF5252).copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreSize
                ),
                radius = coreSize,
                center = center
            )

            // 5. Specular 3D Glass Highlights
            val specularOffset = Offset(center.x - orbRadius * 0.22f, center.y - orbRadius * 0.38f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = specularOffset,
                    radius = orbRadius * 0.5f
                ),
                topLeft = Offset(specularOffset.x - orbRadius * 0.32f, specularOffset.y - orbRadius * 0.16f),
                size = Size(orbRadius * 0.64f, orbRadius * 0.32f)
            )

            // Outer Crimson Edge Rim Stroke
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0x88FFFFFF),
                        CrimsonBright.copy(alpha = 0.9f),
                        Color(0xFFFF5252).copy(alpha = 0.8f),
                        Color(0x88FFFFFF)
                    ),
                    center = center
                ),
                radius = orbRadius - 1f,
                center = center,
                style = Stroke(width = 2.0f)
            )
        }
    }
}
