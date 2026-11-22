package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight

@Composable
fun BatteryIndicator(
    level: Int,
    tempC: Float?,
    statusLabel: String,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val pulseAlpha = if (isCharging && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "battery_charge")
        val alpha by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "charge_alpha"
        )
        alpha
    } else 1f

    val fillGradient = when {
        level <= 15 && !isCharging -> listOf(CrimsonAlert, GlossyCrimson)
        isCharging -> listOf(GlossyCrimson, EmeraldOptimal.copy(alpha = pulseAlpha))
        else -> listOf(DeepCherry, GlossyCrimson, LipstickHighlight)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Battery visual shell
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepCherry.copy(alpha = 0.45f))
                        .border(1.dp, CherryBorder, RoundedCornerShape(8.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((level.coerceIn(4, 100) / 100f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(Brush.horizontalGradient(fillGradient))
                    )
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Charging",
                            tint = MainText,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .width(3.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SoftPinkHighlight.copy(alpha = 0.7f))
                )
            }

            Text(
                text = "$level%",
                style = MaterialTheme.typography.headlineSmall,
                color = MainText
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
            if (tempC != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = "Battery Temperature",
                        tint = if (tempC >= 40f) AmberWarm else SoftPinkHighlight,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${String.format("%.1f", tempC)}°C",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (tempC >= 40f) AmberWarm else SoftPinkHighlight
                    )
                }
            }
        }
    }
}
