package com.tecnolegend.tecnoai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A calm "thinking" indicator: a white orb that breathes inside a soft white glow.
 */
@Composable
fun GlowOrb(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    level: Float? = null, // 0..1 voice level; null = gentle breathing
    color: Color = Color.White
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val breath by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val glowPulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val voice by animateFloatAsState(
        targetValue = level?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "voice"
    )
    val scale = if (level != null) 0.78f + voice * 0.35f else breath
    val glow = if (level != null) 0.35f + voice * 0.6f else glowPulse

    Canvas(modifier = modifier.size(size * 2.4f)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val core = size.toPx() / 2f * scale
        // Outer soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.55f * glow), color.copy(alpha = 0.12f * glow), Color.Transparent),
                center = c,
                radius = core * 2.6f
            ),
            radius = core * 2.6f,
            center = c
        )
        // Bright core with a slight gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, color.copy(alpha = 0.92f), color.copy(alpha = 0.75f)),
                center = Offset(c.x - core * 0.25f, c.y - core * 0.25f),
                radius = core * 1.3f
            ),
            radius = core,
            center = c
        )
    }
}

/** Orb plus a softly shimmering label, e.g. "Thinking". */
@Composable
fun ThinkingIndicator(
    label: String = "Thinking",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "shift"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlowOrb(size = 12.dp)
        Text(
            text = label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6E6E73), Color.White, Color(0xFF6E6E73)),
                    start = Offset(shift * 300f - 150f, 0f),
                    end = Offset(shift * 300f + 150f, 0f)
                )
            )
        )
    }
}
