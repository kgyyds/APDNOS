package com.apdnos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HackerColorScheme = darkColorScheme(
    primary = Color(0xFF00FF7F),
    onPrimary = Color.Black,
    secondary = Color(0xFF00C853),
    onSecondary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFE0FFE0),
    surface = Color(0xFF0A0F0A),
    onSurface = Color(0xFFE0FFE0),
    tertiary = Color(0xFF00FFB2)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HackerColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
