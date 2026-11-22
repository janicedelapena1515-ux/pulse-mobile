package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PulseScoreRing(
    score: Int,
    ratingLabel: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 228.dp,
    isScanning: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 900,
            easing = FastOutSlowInEasing
        ),
        label = "score_sweep"
    )

    val pulseScale = if (reducedMotion) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isScanning) 900 else 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_glow"
        )
        pulse
    }

    Box(
        modifier = modifier
            .size(ringSize)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = 18.dp.toPx()
            val radius = (size.minDimension - strokePx) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Ambient outer ruby halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlossyCrimson.copy(alpha = 0.24f),
                        DeepCherry.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (size.minDimension / 2f) * pulseScale
                ),
                radius = (size.minDimension / 2f) * pulseScale,
                center = center
            )

            // Inner glass disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2D121D),
                        CardBackground,
                        Color(0xFF14090E)
                    ),
                    center = center,
                    radius = radius - strokePx * 0.5f
                ),
                radius = radius - strokePx * 0.5f,
                center = center
            )

            // Automotive tick marks
            val tickCount = 36
            for (i in 0 until tickCount) {
                val angleDeg = 135f + (270f / (tickCount - 1)) * i
                val rad = Math.toRadians(angleDeg.toDouble())
                val innerR = radius - strokePx * 0.88f
                val outerR = radius - strokePx * 0.62f
                val start = Offset(
                    center.x + (innerR * cos(rad)).toFloat(),
                    center.y + (innerR * sin(rad)).toFloat()
                )
                val end = Offset(
                    center.x + (outerR * cos(rad)).toFloat(),
                    center.y + (outerR * sin(rad)).toFloat()
                )
                val lit = (i.toFloat() / (tickCount - 1)) * 100f <= animatedScore
                drawLine(
                    color = if (lit) SoftPinkHighlight.copy(alpha = 0.72f) else SecondaryText.copy(alpha = 0.18f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 6 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Track arc
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)
            drawArc(
                color = DeepCherry.copy(alpha = 0.45f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active glossy ruby-to-pink arc
            val sweep = (animatedScore / 100f) * 270f
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            DeepCherry,
                            RubyRed,
                            GlossyCrimson,
                            LipstickHighlight,
                            SoftPinkHighlight,
                            GlossyCrimson
                        ),
                        center = center
                    ),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Specular tip indicator orb
                val endAngleRad = Math.toRadians((135f + sweep).toDouble())
                val tipOffset = Offset(
                    center.x + (radius * cos(endAngleRad)).toFloat(),
                    center.y + (radius * sin(endAngleRad)).toFloat()
                )
                drawCircle(
                    color = SoftPinkHighlight,
                    radius = strokePx * 0.36f,
                    center = tipOffset
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = strokePx * 0.16f,
                    center = tipOffset
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "PULSE SCORE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = SecondaryText
            )
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MainText
            )
            Text(
                text = if (isScanning) "SCANNING..." else ratingLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
                color = SoftPinkHighlight
            )
        }
    }
}
