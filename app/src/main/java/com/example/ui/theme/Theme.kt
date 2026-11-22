package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = StudioObsidian,
    primaryContainer = Color(0xFF004754),
    onPrimaryContainer = StudioCyan,
    secondary = StudioAmber,
    onSecondary = StudioObsidian,
    secondaryContainer = Color(0xFF473300),
    onSecondaryContainer = StudioAmber,
    tertiary = StudioCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4F1625),
    onTertiaryContainer = Color(0xFFFFB3C1),
    background = StudioObsidian,
    onBackground = StudioTextPrimary,
    surface = StudioDeepSlate,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioPanel,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioDeepSlate,
    surfaceContainer = StudioPanel,
    surfaceContainerHigh = StudioCard,
    surfaceContainerHighest = StudioCardHover,
    outline = StudioBorder,
    outlineVariant = Color(0xFF202B44),
    error = StudioCoral,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = Color(0xFF007B8F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F7FF),
    onPrimaryContainer = Color(0xFF003B45),
    secondary = Color(0xFFB46900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7C2),
    onSecondaryContainer = Color(0xFF422400),
    tertiary = Color(0xFFC92A54),
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightPanel,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightPanel,
    surfaceContainerHigh = LightCard,
    outline = LightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ToonMotionTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
