package com.textvision.alistclient.ui.theme

import androidx.compose.runtime.Composable

@Composable
fun LightThemePreview(content: @Composable () -> Unit) {
    AlistTheme(darkMode = DarkMode.LIGHT) { content() }
}

@Composable
fun DarkThemePreview(content: @Composable () -> Unit) {
    AlistTheme(darkMode = DarkMode.DARK) { content() }
}
