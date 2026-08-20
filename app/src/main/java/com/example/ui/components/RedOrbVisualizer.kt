package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.viewmodel.AssistantUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RedOrbVisualizer(
    state: AssistantUiState,
    audioLevel: Float = 0f,
    size: Dp = 160.dp,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantUiState.LISTENING) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("red_orb_visualizer")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 3.2f

            // Dynamic scale factor based on audio amplitude or state
            val dynamicScale = when (state) {
                AssistantUiState.LISTENING -> (1f + (audioLevel * 0.45f)) * pulseScale
                AssistantUiState.THINKING, AssistantUiState.EXECUTING -> pulseScale * 1.05f
                AssistantUiState.SPEAKING -> (1f + (pulseScale * 0.1f))
                AssistantUiState.ERROR -> 0.95f
                else -> pulseScale
            }

            val currentRadius = baseRadius * dynamicScale

            // 1. Outer Ambient Glow Rings
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CrimsonGlow, Color.Transparent),
                    center = center,
                    radius = currentRadius * 1.8f
                ),
                radius = currentRadius * 1.8f,
                center = center
            )

            // 2. Rotating Cybernetic Ring Accents
            val ringRadius = currentRadius * 1.35f
            val numDots = 8
            for (i in 0 until numDots) {
                val angle = Math.toRadians((rotationAngle + (i * 360f / numDots)).toDouble())
                val dotCenter = Offset(
                    (center.x + ringRadius * cos(angle)).toFloat(),
                    (center.y + ringRadius * sin(angle)).toFloat()
                )
                val dotAlpha = if (state == AssistantUiState.LISTENING) 0.8f else 0.4f
                drawCircle(
                    color = CrimsonBright.copy(alpha = dotAlpha),
                    radius = 3.5f * (if (i % 2 == 0) 1.4f else 0.9f),
                    center = dotCenter
                )
            }

            // Outer subtle boundary ring
            drawCircle(
                color = CrimsonPrimary.copy(alpha = 0.35f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.8f)
            )

            // 3. Core Neural Orb Gradient
            val coreColors = when (state) {
                AssistantUiState.ERROR -> listOf(Color(0xFFFF334B), Color(0xFF66000B))
                AssistantUiState.SUCCESS -> listOf(Color(0xFFFF5277), CrimsonPrimary, CrimsonDark)
                else -> listOf(Color.White, CrimsonBright, CrimsonPrimary, CrimsonDark)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = Offset(center.x - currentRadius * 0.25f, center.y - currentRadius * 0.25f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // 4. Subtle Inner Highlight / Lens Refraction
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f),
                    radius = currentRadius * 0.5f
                ),
                radius = currentRadius * 0.5f,
                center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f)
            )
        }
    }
}
