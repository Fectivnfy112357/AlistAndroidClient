package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class DarkMode { SYSTEM, LIGHT, DARK }

/**
 * Alist UI Theme — Sky Blue + Candy. Static palette (no Material You dynamic color).
 */
@Composable
fun AlistTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT  -> false
        DarkMode.DARK   -> true
    }
    val colors = if (isDark) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
