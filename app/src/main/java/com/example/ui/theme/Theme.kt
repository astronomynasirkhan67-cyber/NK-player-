package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.data.model.AppTheme

@Composable
fun MusicNasirKhanTheme(
    appTheme: AppTheme = AppTheme.IMMERSIVE_UI,
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(appTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
