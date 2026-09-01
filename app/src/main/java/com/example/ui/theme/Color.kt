package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme

fun getThemeColorScheme(appTheme: AppTheme): ColorScheme {
    val primary = Color(appTheme.primaryHex)
    val secondary = Color(appTheme.secondaryHex)
    val tertiary = Color(appTheme.accentHex)
    val background = Color(appTheme.backgroundHex)
    val surface = Color(appTheme.surfaceHex)

    return darkColorScheme(
        primary = primary,
        onPrimary = Color.Black,
        primaryContainer = primary.copy(alpha = 0.25f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.Black,
        secondaryContainer = secondary.copy(alpha = 0.25f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color.Black,
        background = background,
        onBackground = Color(0xFFF1F1F6),
        surface = surface,
        onSurface = Color(0xFFEAEAEE),
        surfaceVariant = surface.copy(alpha = 0.7f),
        onSurfaceVariant = Color(0xFFB0B0C0),
        outline = primary.copy(alpha = 0.35f)
    )
}
