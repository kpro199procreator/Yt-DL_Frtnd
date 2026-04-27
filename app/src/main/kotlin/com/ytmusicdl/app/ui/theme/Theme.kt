package com.ytmusicdl.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta oscura — música se ve mejor en dark
private val DarkColorScheme = darkColorScheme(
    primary         = Color(0xFFCE93D8),   // purple 200
    onPrimary       = Color(0xFF1A001F),
    primaryContainer= Color(0xFF4A148C),
    secondary       = Color(0xFF80CBC4),   // teal 200
    background      = Color(0xFF0E0E0E),
    surface         = Color(0xFF1A1A1A),
    surfaceVariant  = Color(0xFF2A2A2A),
    onBackground    = Color(0xFFEEEEEE),
    onSurface       = Color(0xFFEEEEEE),
)

@Composable
fun YtmusicdlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content,
    )
}
