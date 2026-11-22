package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SportsEsports
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.LocalHapticsEnabled
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

enum class PulseTab(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    HOME("home", "HOME", Icons.Rounded.Home, Icons.Outlined.Home),
    BOOST("boost", "BOOST", Icons.Rounded.Bolt, Icons.Outlined.Bolt),
    GAMES("games", "GAMES", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    MONITOR("monitor", "MONITOR", Icons.AutoMirrored.Rounded.ShowChart, Icons.AutoMirrored.Outlined.ShowChart),
    TOOLS("tools", "TOOLS", Icons.Rounded.Build, Icons.Outlined.Build)
}

@Composable
fun CustomBottomNavigation(
    currentRoute: String?,
    onNavigate: (PulseTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = GlossyCrimson.copy(alpha = 0.35f),
                    spotColor = DeepCherry.copy(alpha = 0.55f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = CardBackground.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, CherryBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2B121C).copy(alpha = 0.92f),
                                CardBackground.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulseTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val scale by animateFloatAsState(
                        targetValue = if (selected && !reducedMotion) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
                        label = "tab_scale"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MainText else SecondaryText,
                        label = "tab_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .defaultMinSize(minHeight = 52.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                DeepCherry.copy(alpha = 0.9f),
                                                RubyRed.copy(alpha = 0.85f),
                                                GlossyCrimson.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = androidx.compose.material3.ripple(color = SoftPinkHighlight),
                                onClick = {
                                    FormatUtils.performPulseHaptic(view, hapticsEnabled)
                                    onNavigate(tab)
                                }
                            )
                            .testTag("nav_${tab.route}")
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.label,
                                tint = if (selected) SoftPinkHighlight else SecondaryText,
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
