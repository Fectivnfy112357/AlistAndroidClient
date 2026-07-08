package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    AppBackground {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars,
            content = content,
        )
    }
}
