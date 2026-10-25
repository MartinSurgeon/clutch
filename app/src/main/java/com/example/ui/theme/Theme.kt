package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightFarmColorScheme = lightColorScheme(
    primary = FarmGreenLight,
    onPrimary = CardSurfaceLight,
    primaryContainer = FarmGreenContainerLight,
    onPrimaryContainer = OnFarmGreenContainerLight,
    secondary = YolkGoldTextLight,
    onSecondary = CardSurfaceLight,
    secondaryContainer = YolkGoldContainerLight,
    onSecondaryContainer = OnYolkGoldContainerLight,
    tertiary = YolkGoldTextLight,
    onTertiary = CardSurfaceLight,
    background = FieldBackgroundLight,
    onBackground = InkLight,
    surface = CardSurfaceLight,
    onSurface = InkLight,
    surfaceVariant = CardVariantLight,
    onSurfaceVariant = InkSecondaryLight,
    surfaceContainer = CardSurfaceLight,
    surfaceContainerLow = FieldBackgroundLight,
    surfaceContainerHigh = CardVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight.copy(alpha = 0.6f),
    error = StatusRedBorderLight,
    onError = CardSurfaceLight,
    errorContainer = StatusRedBgLight,
    onErrorContainer = StatusRedTextLight
)

private val DarkFarmColorScheme = darkColorScheme(
    primary = FarmGreenDark,
    onPrimary = OnFarmGreenDark,
    primaryContainer = FarmGreenContainerDark,
    onPrimaryContainer = OnFarmGreenContainerDark,
    secondary = YolkGoldTextDark,
    onSecondary = FieldBackgroundDark,
    secondaryContainer = YolkGoldContainerDark,
    onSecondaryContainer = OnYolkGoldContainerDark,
    tertiary = YolkGoldTextDark,
    onTertiary = FieldBackgroundDark,
    background = FieldBackgroundDark,
    onBackground = InkDark,
    surface = CardSurfaceDark,
    onSurface = InkDark,
    surfaceVariant = CardVariantDark,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainer = CardSurfaceDark,
    surfaceContainerLow = FieldBackgroundDark,
    surfaceContainerHigh = CardVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark.copy(alpha = 0.6f),
    error = StatusRedBorderDark,
    onError = FieldBackgroundDark,
    errorContainer = StatusRedBgDark,
    onErrorContainer = StatusRedTextDark
)

private val ClutchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Farm Green theme with dynamic colour strictly turned off so the brand stays consistent.
 */
@Composable
fun ClutchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkFarmColorScheme else LightFarmColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ClutchShapes,
        content = content
    )
}
