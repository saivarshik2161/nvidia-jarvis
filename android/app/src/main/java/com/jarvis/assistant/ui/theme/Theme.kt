package com.jarvis.assistant.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF88),      // Green
    onPrimary = Color.Black,
    secondary = Color(0xFF00FFFF),    // Cyan
    onSecondary = Color.Black,
    tertiary = Color(0xFF4488FF),     // Blue
    onTertiary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xCCFFFFFF),
    outline = Color(0xFF3D3D3D),
    error = Color(0xFFFF4444),
    onError = Color.White,
)

@Composable
fun JARVISTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}