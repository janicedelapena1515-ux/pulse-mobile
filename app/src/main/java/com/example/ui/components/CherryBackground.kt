package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainBackground
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryBackground

@Composable
fun CherryBackground(
    modifier: Modifier = Modifier,
    showSilkHeaderAccent: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val driftProgress = if (reducedMotion) {
        0.35f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "bg_drift")
        val value by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bg_drift_val"
        )
        value
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MainBackground, SecondaryBackground, MainBackground)
                )
            )
    ) {
        if (showSilkHeaderAccent) {
            Image(
                painter = painterResource(id = R.drawable.img_cherry_silk_banner_1795280814919),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.14f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val topOrbCenter = Offset(
                x = size.width * (0.18f + 0.64f * driftProgress),
                y = size.height * 0.12f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DeepCherry.copy(alpha = 0.42f),
                        RubyRed.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = topOrbCenter,
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = topOrbCenter
            )

            val bottomOrbCenter = Offset(
                x = size.width * (0.85f - 0.55f * driftProgress),
                y = size.height * 0.86f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlossyCrimson.copy(alpha = 0.16f),
                        DeepCherry.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = bottomOrbCenter,
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = bottomOrbCenter
            )
        }

        content()
    }
}
