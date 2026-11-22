package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight

@Composable
fun PulseLogo(
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = true
) {
    val reducedMotion = LocalReducedMotion.current
    val pulsePhase = if (reducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "pulse_logo")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_phase"
        )
        phase
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(DeepCherry, RubyRed, GlossyCrimson)
                    )
                )
                .border(1.2.dp, CherryBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val w = size.width
                val h = size.height
                val midY = h * 0.54f

                // Outer metallic ring glow
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            SoftPinkHighlight.copy(alpha = 0.65f),
                            LipstickHighlight.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    radius = w * 0.44f,
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Heartbeat pulse path
                val path = Path().apply {
                    moveTo(w * 0.08f, midY)
                    lineTo(w * 0.28f, midY)
                    lineTo(w * 0.38f, h * 0.30f)
                    lineTo(w * 0.49f, h * 0.78f)
                    lineTo(w * 0.61f, h * 0.20f)
                    lineTo(w * 0.71f, midY)
                    lineTo(w * 0.92f, midY)
                }

                drawPath(
                    path = path,
                    color = MainText,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Travelling glossy highlight dot
                val dotX = w * (0.08f + 0.84f * pulsePhase)
                drawCircle(
                    color = SoftPinkHighlight.copy(alpha = 0.85f),
                    radius = 2.8.dp.toPx(),
                    center = Offset(dotX, midY)
                )
            }
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PULSE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MainText
                )
                Text(
                    text = "MOBILE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.6.sp
                    ),
                    color = SoftPinkHighlight
                )
            }
            if (showSubtitle) {
                Text(
                    text = "CHERRY EDITION",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                    color = SecondaryText
                )
            }
        }
    }
}
