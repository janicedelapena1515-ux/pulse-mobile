package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

private val CherryLuxuryColorScheme = darkColorScheme(
    primary = GlossyCrimson,
    onPrimary = MainText,
    primaryContainer = DeepCherry,
    onPrimaryContainer = SoftPinkHighlight,
    secondary = LipstickHighlight,
    onSecondary = MainBackground,
    secondaryContainer = RubyRed,
    onSecondaryContainer = MainText,
    tertiary = SoftPinkHighlight,
    onTertiary = MainBackground,
    tertiaryContainer = CardBackground,
    onTertiaryContainer = SoftPinkHighlight,
    background = MainBackground,
    onBackground = MainText,
    surface = SecondaryBackground,
    onSurface = MainText,
    surfaceVariant = CardBackground,
    onSurfaceVariant = SecondaryText,
    surfaceContainerLowest = MainBackground,
    surfaceContainerLow = SecondaryBackground,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = ElevatedGlass,
    outline = CherryBorder,
    outlineVariant = SubtleGlassBorder,
    error = CrimsonAlert,
    onError = MainText
)

@Composable
fun PulseMobileTheme(
    reducedMotion: Boolean = false,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalHapticsEnabled provides hapticsEnabled
    ) {
        MaterialTheme(
            colorScheme = CherryLuxuryColorScheme,
            typography = PulseTypography,
            shapes = PulseShapes,
            content = content
        )
    }
}
