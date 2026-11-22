package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.LocalHapticsEnabled
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun GlossyRedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "btn_scale"
    )

    val sheenOffset = if (reducedMotion || !enabled) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "btn_sheen")
        val offset by transition.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sheen_pos"
        )
        offset
    }

    val shape = RoundedCornerShape(22.dp)
    val baseBrush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(DeepCherry, RubyRed, GlossyCrimson, LipstickHighlight),
            start = Offset(0f, 0f),
            end = Offset(900f, 200f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(DeepCherry.copy(alpha = 0.4f), RubyRed.copy(alpha = 0.4f))
        )
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 56.dp)
            .shadow(
                elevation = if (enabled) 18.dp else 2.dp,
                shape = shape,
                ambientColor = GlossyCrimson.copy(alpha = 0.5f),
                spotColor = LipstickHighlight.copy(alpha = 0.6f)
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = SoftPinkHighlight),
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = {
                    FormatUtils.performPulseHaptic(view, hapticsEnabled, heavy = true)
                    onClick()
                }
            ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    SoftPinkHighlight.copy(alpha = 0.65f),
                    LipstickHighlight.copy(alpha = 0.35f),
                    DeepCherry
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseBrush)
        ) {
            // Top glossy specular reflection band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Animated horizontal gloss sweep
            if (enabled && !reducedMotion) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SoftPinkHighlight.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                start = Offset(sheenOffset * 600f - 180f, 0f),
                                end = Offset(sheenOffset * 600f + 180f, 180f)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MainText,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MainText
                )
            }
        }
    }
}
