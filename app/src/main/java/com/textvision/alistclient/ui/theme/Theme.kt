package com.textvision.alistclient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CloudPrimary,
    onPrimary = CloudOnPrimary,
    primaryContainer = CloudPrimarySoft,
    onPrimaryContainer = CloudPrimaryDark,
    background = CloudBackground,
    onBackground = CloudTextPrimary,
    surface = CloudSurface,
    onSurface = CloudTextPrimary,
    surfaceVariant = CloudSurfaceMuted,
    onSurfaceVariant = CloudTextSecondary,
    outline = CloudOutline,
    error = CloudErrorText,
    errorContainer = CloudErrorContainer,
)

@Composable
fun AlistClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CloudTypography,
        content = content,
    )
}
