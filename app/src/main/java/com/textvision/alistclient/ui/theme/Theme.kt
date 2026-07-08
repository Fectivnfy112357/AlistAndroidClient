package com.textvision.alistclient.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors: ColorScheme = lightColorScheme(
    primary = LightColorScheme.primary,
    onPrimary = LightColorScheme.onPrimary,
    primaryContainer = LightColorScheme.primaryContainer,
    onPrimaryContainer = LightColorScheme.onPrimaryContainer,
    secondary = LightColorScheme.secondary,
    onSecondary = LightColorScheme.onSecondary,
    secondaryContainer = LightColorScheme.secondaryContainer,
    onSecondaryContainer = LightColorScheme.onSecondaryContainer,
    tertiary = LightColorScheme.tertiary,
    onTertiary = LightColorScheme.onTertiary,
    tertiaryContainer = LightColorScheme.tertiaryContainer,
    onTertiaryContainer = LightColorScheme.onTertiaryContainer,
    error = LightColorScheme.error,
    onError = LightColorScheme.onError,
    errorContainer = LightColorScheme.errorContainer,
    onErrorContainer = LightColorScheme.onErrorContainer,
    background = LightColorScheme.background,
    onBackground = LightColorScheme.onBackground,
    surface = LightColorScheme.surface,
    onSurface = LightColorScheme.onSurface,
    surfaceVariant = LightColorScheme.surfaceVariant,
    onSurfaceVariant = LightColorScheme.onSurfaceVariant,
    surfaceContainerLowest = LightColorScheme.surfaceContainerLowest,
    surfaceContainerLow = LightColorScheme.surfaceContainerLow,
    surfaceContainer = LightColorScheme.surfaceContainer,
    surfaceContainerHigh = LightColorScheme.surfaceContainerHigh,
    surfaceContainerHighest = LightColorScheme.surfaceContainerHighest,
    outline = LightColorScheme.outline,
    outlineVariant = LightColorScheme.outlineVariant,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = DarkColorScheme.primary,
    onPrimary = DarkColorScheme.onPrimary,
    primaryContainer = DarkColorScheme.primaryContainer,
    onPrimaryContainer = DarkColorScheme.onPrimaryContainer,
    secondary = DarkColorScheme.secondary,
    onSecondary = DarkColorScheme.onSecondary,
    secondaryContainer = DarkColorScheme.secondaryContainer,
    onSecondaryContainer = DarkColorScheme.onSecondaryContainer,
    tertiary = DarkColorScheme.tertiary,
    onTertiary = DarkColorScheme.onTertiary,
    tertiaryContainer = DarkColorScheme.tertiaryContainer,
    onTertiaryContainer = DarkColorScheme.onTertiaryContainer,
    error = DarkColorScheme.error,
    onError = DarkColorScheme.onError,
    errorContainer = DarkColorScheme.errorContainer,
    onErrorContainer = DarkColorScheme.onErrorContainer,
    background = DarkColorScheme.background,
    onBackground = DarkColorScheme.onBackground,
    surface = DarkColorScheme.surface,
    onSurface = DarkColorScheme.onSurface,
    surfaceVariant = DarkColorScheme.surfaceVariant,
    onSurfaceVariant = DarkColorScheme.onSurfaceVariant,
    surfaceContainerLowest = DarkColorScheme.surfaceContainerLowest,
    surfaceContainerLow = DarkColorScheme.surfaceContainerLow,
    surfaceContainer = DarkColorScheme.surfaceContainer,
    surfaceContainerHigh = DarkColorScheme.surfaceContainerHigh,
    surfaceContainerHighest = DarkColorScheme.surfaceContainerHighest,
    outline = DarkColorScheme.outline,
    outlineVariant = DarkColorScheme.outlineVariant,
)

@Composable
fun AlistClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
