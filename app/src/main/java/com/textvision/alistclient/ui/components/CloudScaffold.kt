package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudBackground

/**
 * Reserved height for a floating bottom bar (64dp visual bar + 24dp outer padding).
 * Pages that sit above [CloudBottomBar] should use [bottomBarInset] so content
 * never slides behind the bar.
 */
val CloudBottomBarReservedHeight: Dp = 96.dp

/** Convenience: bottom inset that fully clears a floating bottom bar + system gesture bar. */
@Composable
fun bottomBarInset(): Dp {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return navInset + CloudBottomBarReservedHeight
}

@Composable
fun CloudScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    bottomInset: Dp = 0.dp,
    bottomBar: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CloudBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(contentPadding)
                .padding(bottom = bottomInset),
            content = content,
        )
        bottomBar()
    }
}
