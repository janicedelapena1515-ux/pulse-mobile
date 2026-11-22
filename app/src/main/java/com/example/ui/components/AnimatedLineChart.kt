package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.ui.theme.SubtleGlassBorder

@Composable
fun AnimatedLineChart(
    title: String,
    currentValueText: String,
    samples: List<Float>,
    modifier: Modifier = Modifier,
    maxScale: Float = 100f,
    minScale: Float = 0f,
    lineColor: Color = LipstickHighlight,
    chartHeight: Dp = 110.dp,
    unavailableMessage: String? = null
) {
    GlassMetricCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SecondaryText
            )
            Text(
                text = currentValueText,
                style = MaterialTheme.typography.labelLarge,
                color = MainText
            )
        }

        if (unavailableMessage != null && samples.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = unavailableMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 12.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val usableH = (h - topPad - bottomPad).coerceAtLeast(1f)

                // Subtle horizontal reference grid
                for (i in 0..2) {
                    val y = topPad + (usableH * i / 2f)
                    drawLine(
                        color = SubtleGlassBorder,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (samples.isEmpty()) return@Canvas

                val effectiveMax = maxOf(maxScale, samples.maxOrNull() ?: maxScale).coerceAtLeast(minScale + 1f)
                val range = (effectiveMax - minScale).coerceAtLeast(1f)
                val pts = if (samples.size == 1) {
                    val y = topPad + usableH * (1f - ((samples[0] - minScale) / range).coerceIn(0f, 1f))
                    listOf(Offset(0f, y), Offset(w, y))
                } else {
                    samples.mapIndexed { index, value ->
                        val x = (index.toFloat() / (samples.size - 1)) * w
                        val norm = ((value - minScale) / range).coerceIn(0f, 1f)
                        val y = topPad + usableH * (1f - norm)
                        Offset(x, y)
                    }
                }

                val strokePath = Path()
                val fillPath = Path()
                pts.forEachIndexed { idx, pt ->
                    if (idx == 0) {
                        strokePath.moveTo(pt.x, pt.y)
                        fillPath.moveTo(pt.x, h)
                        fillPath.lineTo(pt.x, pt.y)
                    } else {
                        val prev = pts[idx - 1]
                        val midX = (prev.x + pt.x) / 2f
                        strokePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                        fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                    }
                }
                fillPath.lineTo(pts.last().x, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.34f),
                            GlossyCrimson.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GlossyCrimson, lineColor, SoftPinkHighlight)
                    ),
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val last = pts.last()
                drawCircle(
                    color = lineColor.copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = last
                )
                drawCircle(
                    color = SoftPinkHighlight,
                    radius = 3.5.dp.toPx(),
                    center = last
                )
            }
        }
    }
}
