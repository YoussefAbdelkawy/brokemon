package com.joecode.brokemon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DexColorScheme = darkColorScheme(
    primary = DexColors.DexRed,
    onPrimary = DexColors.Text,
    primaryContainer = DexColors.DexRedDark,
    onPrimaryContainer = DexColors.Text,
    secondary = DexColors.LedBlue,
    onSecondary = DexColors.Background,
    tertiary = DexColors.LedYellow,
    background = DexColors.Background,
    onBackground = DexColors.Text,
    surface = DexColors.Surface,
    onSurface = DexColors.Text,
    surfaceVariant = DexColors.SurfaceHigh,
    onSurfaceVariant = DexColors.TextMuted,
    surfaceContainer = DexColors.Surface,
    surfaceContainerHigh = DexColors.SurfaceHigh,
    surfaceContainerHighest = DexColors.SurfaceHigh,
    outline = DexColors.Outline,
    error = DexColors.LedRed,
)

// Chamfered corners read as "pixel" without looking jagged.
private val DexShapes = Shapes(
    extraSmall = DexShape(2.dp),
    small = DexShape(4.dp),
    medium = DexShape(6.dp),
    large = DexShape(8.dp),
    extraLarge = DexShape(12.dp),
)

/** Dark mode only, by design. */
@Composable
fun BrokemonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DexColorScheme,
        typography = BrokemonTypography,
        shapes = DexShapes,
        content = content,
    )
}
