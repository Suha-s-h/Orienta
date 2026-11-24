package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ScreenDeckDarkScheme = darkColorScheme(
    primary = SignalAmber,
    onPrimary = OnSignalAmber,
    primaryContainer = SignalAmberContainer,
    onPrimaryContainer = SignalAmber,
    secondary = LaserCyan,
    onSecondary = OnLaserCyan,
    secondaryContainer = LaserCyanContainer,
    onSecondaryContainer = LaserCyan,
    tertiary = EmeraldReady,
    onTertiary = OnEmeraldReady,
    tertiaryContainer = EmeraldReadyContainer,
    onTertiaryContainer = EmeraldReady,
    error = SignalCoral,
    errorContainer = SignalCoralContainer,
    onErrorContainer = SignalCoral,
    background = ObsidianChassis,
    onBackground = PrimaryInk,
    surface = AnodizedGraphite,
    onSurface = PrimaryInk,
    surfaceVariant = MachinedSlate,
    onSurfaceVariant = SecondaryTelemetryInk,
    surfaceContainerLowest = ObsidianChassis,
    surfaceContainerLow = RecessedWell,
    surfaceContainer = AnodizedGraphite,
    surfaceContainerHigh = MachinedSlate,
    surfaceContainerHighest = SubtleSteel,
    outline = SubtleSteel,
    outlineVariant = HairlineSteel
)

private val ScreenDeckShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ScreenDeckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScreenDeckDarkScheme,
        typography = ScreenDeckTypography,
        shapes = ScreenDeckShapes,
        content = content
    )
}
