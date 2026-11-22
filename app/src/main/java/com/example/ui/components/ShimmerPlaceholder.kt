package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.LocalReducedMotion

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp
) {
    val reducedMotion = LocalReducedMotion.current
    val shift = if (reducedMotion) 0.4f else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1200f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_x"
        )
        value
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CardBackground,
                        DeepCherry.copy(alpha = 0.55f),
                        CardBackground
                    ),
                    start = Offset(shift - 300f, 0f),
                    end = Offset(shift, 120f)
                )
            )
    )
}
