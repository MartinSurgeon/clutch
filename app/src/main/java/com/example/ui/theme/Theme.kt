package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightLedgerColorScheme = lightColorScheme(
    primary = LedgerBlueLight,
    onPrimary = LedgerCardSurfaceLight,
    primaryContainer = LedgerBlueContainerLight,
    onPrimaryContainer = OnLedgerBlueContainerLight,
    secondary = LedgerInkSecondaryLight,
    onSecondary = LedgerCardSurfaceLight,
    secondaryContainer = LedgerCardVariantLight,
    onSecondaryContainer = LedgerInkLight,
    tertiary = LedgerBlueLight,
    onTertiary = LedgerCardSurfaceLight,
    background = LedgerPaperLight,
    onBackground = LedgerInkLight,
    surface = LedgerCardSurfaceLight,
    onSurface = LedgerInkLight,
    surfaceVariant = LedgerCardVariantLight,
    onSurfaceVariant = LedgerInkSecondaryLight,
    outline = LedgerRuleOutlineLight,
    outlineVariant = LedgerRuleOutlineLight.copy(alpha = 0.55f),
    error = StatusRedBorderLight,
    onError = LedgerCardSurfaceLight,
    errorContainer = StatusRedBgLight,
    onErrorContainer = StatusRedTextLight
)

private val DarkLedgerColorScheme = darkColorScheme(
    primary = LedgerBlueDark,
    onPrimary = LedgerPaperDark,
    primaryContainer = LedgerBlueContainerDark,
    onPrimaryContainer = OnLedgerBlueContainerDark,
    secondary = LedgerInkSecondaryDark,
    onSecondary = LedgerPaperDark,
    secondaryContainer = LedgerCardVariantDark,
    onSecondaryContainer = LedgerInkDark,
    tertiary = LedgerBlueDark,
    onTertiary = LedgerPaperDark,
    background = LedgerPaperDark,
    onBackground = LedgerInkDark,
    surface = LedgerCardSurfaceDark,
    onSurface = LedgerInkDark,
    surfaceVariant = LedgerCardVariantDark,
    onSurfaceVariant = LedgerInkSecondaryDark,
    outline = LedgerRuleOutlineDark,
    outlineVariant = LedgerRuleOutlineDark.copy(alpha = 0.55f),
    error = StatusRedBorderDark,
    onError = LedgerPaperDark,
    errorContainer = StatusRedBgDark,
    onErrorContainer = StatusRedTextDark
)

/**
 * Farm ledger theme with dynamic colour strictly turned off as required.
 */
@Composable
fun ClutchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkLedgerColorScheme else LightLedgerColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
