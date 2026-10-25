package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Sora, bundled as four static weights (cut from the variable font) so every weight
 * renders correctly on all supported Android versions, offline.
 */
val SoraFontFamily = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold)
)

private fun sora(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = SoraFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

// Sora is a wide face, so sizes sit a step below Material defaults to keep lines short on phones.
val Typography = Typography(
    displayLarge = sora(FontWeight.Bold, 44, 52, -1.0),
    displayMedium = sora(FontWeight.Bold, 36, 44, -0.8),
    displaySmall = sora(FontWeight.Bold, 30, 38, -0.5),
    headlineLarge = sora(FontWeight.Bold, 28, 36, -0.4),
    headlineMedium = sora(FontWeight.SemiBold, 24, 32, -0.3),
    headlineSmall = sora(FontWeight.SemiBold, 20, 28, -0.2),
    titleLarge = sora(FontWeight.SemiBold, 18, 26),
    titleMedium = sora(FontWeight.SemiBold, 15, 22),
    titleSmall = sora(FontWeight.SemiBold, 13, 20),
    bodyLarge = sora(FontWeight.Normal, 15, 23),
    bodyMedium = sora(FontWeight.Normal, 14, 21),
    bodySmall = sora(FontWeight.Normal, 12, 18),
    labelLarge = sora(FontWeight.SemiBold, 14, 20),
    labelMedium = sora(FontWeight.Medium, 12, 16),
    labelSmall = sora(FontWeight.Medium, 11, 16, 0.2)
)
