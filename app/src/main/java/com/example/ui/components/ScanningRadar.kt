package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight

@Composable
fun ScanningRadar(
    statusText: String,
    progress: Float,
    modifier: Modifier = Modifier,
    radarSize: Dp = 156.dp
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "radar")

    val sweepAngle = if (reducedMotion) 45f else {
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radar_angle"
        )
        angle
    }

    val rippleProgress = if (reducedMotion) 0.5f else {
        val ripple by transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radar_ripple"
        )
        ripple
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(radarSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(radarSize)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f

                // Concentric rings
                listOf(0.32f, 0.64f, 0.94f).forEach { factor ->
                    drawCircle(
                        color = CherryBorder,
                        radius = maxR * factor,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Liquid expanding pulse ring
                drawCircle(
                    color = SoftPinkHighlight.copy(alpha = (1f - rippleProgress) * 0.55f),
                    radius = maxR * rippleProgress,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Radar sweep beam
                rotate(degrees = sweepAngle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.72f to Color.Transparent,
                            0.94f to GlossyCrimson.copy(alpha = 0.42f),
                            1.0f to SoftPinkHighlight.copy(alpha = 0.72f),
                            center = center
                        ),
                        radius = maxR * 0.94f,
                        center = center
                    )
                    drawLine(
                        color = SoftPinkHighlight,
                        start = center,
                        end = Offset(center.x + maxR * 0.94f, center.y),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Center ruby core
                drawCircle(
                    color = LipstickHighlight,
                    radius = 7.dp.toPx(),
                    center = center
                )
            }
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.titleSmall,
            color = MainText
        )

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = LipstickHighlight,
            trackColor = DeepCherry.copy(alpha = 0.5f)
        )

        Text(
            text = "${(progress * 100).toInt().coerceIn(0, 100)}% VERIFIED",
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryText
        )
    }
}
