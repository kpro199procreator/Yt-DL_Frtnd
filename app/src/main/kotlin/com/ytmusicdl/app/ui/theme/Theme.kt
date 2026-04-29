package com.ytmusicdl.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary          = Color(0xFFCE93D8),
    onPrimary        = Color(0xFF1A001F),
    primaryContainer = Color(0xFF4A148C),
    secondary        = Color(0xFF80CBC4),
    background       = Color(0xFF0E0E0E),
    surface          = Color(0xFF1A1A1A),
    surfaceVariant   = Color(0xFF2A2A2A),
    onBackground     = Color(0xFFEEEEEE),
    onSurface        = Color(0xFFEEEEEE),
)

@Composable
fun YtmusicdlTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = DarkColors.surface.toArgb()
            window.navigationBarColor = DarkColors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = DarkColors, content = content)
}
