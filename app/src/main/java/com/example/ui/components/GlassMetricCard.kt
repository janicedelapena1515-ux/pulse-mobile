package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LipstickHighlight
import com.example.ui.theme.SoftPinkHighlight
import com.example.ui.theme.SubtleGlassBorder

@Composable
fun GlassMetricCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                SoftPinkHighlight.copy(alpha = 0.65f),
                GlossyCrimson.copy(alpha = 0.50f),
                CherryBorder
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                SoftPinkHighlight.copy(alpha = 0.24f),
                SubtleGlassBorder,
                CherryBorder.copy(alpha = 0.35f)
            )
        )
    }

    val containerBrush = if (highlighted) {
        Brush.verticalGradient(
            colors = listOf(
                DeepCherry.copy(alpha = 0.58f),
                CardBackground.copy(alpha = 0.94f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2D131D).copy(alpha = 0.86f),
                CardBackground.copy(alpha = 0.92f)
            )
        )
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = if (highlighted) 14.dp else 6.dp,
                shape = shape,
                ambientColor = GlossyCrimson.copy(alpha = 0.32f),
                spotColor = LipstickHighlight.copy(alpha = 0.36f)
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
        ) {
            // Subtle glossy specular rim along top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                SoftPinkHighlight.copy(alpha = 0.38f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}
