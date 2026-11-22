package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight

@Composable
fun StorageProgressBar(
    label: String,
    usedPercent: Float,
    usedText: String,
    totalText: String,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedFraction by animateFloatAsState(
        targetValue = (usedPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 750, easing = FastOutSlowInEasing),
        label = "storage_bar"
    )

    val gradientColors = when {
        usedPercent >= 88f -> listOf(RubyRed, CrimsonAlert, AmberWarm)
        usedPercent >= 75f -> listOf(DeepCherry, GlossyCrimson, AmberWarm)
        else -> listOf(DeepCherry, RubyRed, GlossyCrimson, SoftPinkHighlight)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MainText
            )
            Text(
                text = "$usedText / $totalText",
                style = MaterialTheme.typography.labelMedium,
                color = SoftPinkHighlight
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(DeepCherry.copy(alpha = 0.45f))
                .border(1.dp, CherryBorder, RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(gradientColors))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${usedPercent.toInt().coerceIn(0, 100)}% utilized",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
            Text(
                text = "${(100 - usedPercent.toInt()).coerceIn(0, 100)}% free",
                style = MaterialTheme.typography.bodySmall,
                color = LipstickHighlight
            )
        }
    }
}
