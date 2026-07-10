package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Standard Scaffold wrapper — replaces Material3 Scaffold, integrates AppTopBar + AppBottomBar.
 *
 * Slots (prototype §9.2):
 * - [topBar] — usually [AppTopBar]
 * - [bottomBar] — usually [AppBottomBar]
 * - [background] — optional decorative background. Defaults to
 *   [SkyBlueBackground] + [CloudDecor] so every screen shares the sky-blue base.
 *   Drawn behind [content], covers the full container. Pass `{}` to opt out and
 *   fall back to the flat theme background color.
 * - [content] — main content, receives Scaffold's inner PaddingValues.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    background: @Composable () -> Unit = {
        SkyBlueBackground()
        CloudDecor()
    },
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        background()
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.systemBars,
            content = content,
        )
    }
}
