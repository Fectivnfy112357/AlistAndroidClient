package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.AlistTheme

/**
 * Transfer screen — segmented switcher (全部 / 上传 / 下载 / 失败) + 4-state task cards.
 *
 * Uses [TransferListUiState] as the single source of truth (see [TransferViewModel]).
 * 4-segmented tabs carry per-tab badges (upload=candy-pink, download=candy-mint) per
 * prototype spec §5.2.5.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(viewModel: TransferViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    AppScaffold(
        transparentBase = true,
        background = {},
        topBar = {
            AppTopBar(
                title = "传输",
                subtitle = state.summary,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TransferSegmentedTabs(
                selected = state.tab,
                uploadCount = state.uploadCount,
                downloadCount = state.downloadCount,
                failedCount = state.failedCount,
                onSelect = viewModel::selectTab,
            )

            if (!state.isOnline) {
                StatusBanner(
                    kind = BannerKind.WARNING,
                    message = "当前离线，传输操作已暂停",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = state.tab,
                    transitionSpec = {
                        fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessMedium))
                    },
                    label = "tab",
                ) { tab ->
                    TransferListContent(
                        rows = state.visible,
                        onCancel = viewModel::cancel,
                        onRetry = viewModel::retry,
                        onDelete = viewModel::delete,
                        emptyTitle = tab.emptyMessage,
                        emptyMessage = "对应类型的传输任务会显示在这里",
                        enabled = state.isOnline,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  4-segmented tabs with badges
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransferSegmentedTabs(
    selected: TransferTab,
    uploadCount: Int,
    downloadCount: Int,
    failedCount: Int,
    onSelect: (TransferTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val tabs = TransferTab.entries
        tabs.forEachIndexed { index, tab ->
            val badgeCount = when (tab) {
                TransferTab.ALL -> null
                TransferTab.UPLOAD -> uploadCount
                TransferTab.DOWNLOAD -> downloadCount
                TransferTab.FAILED -> failedCount
            }
            val badgeColor: Color? = when (tab) {
                TransferTab.UPLOAD -> MaterialTheme.colorScheme.tertiary
                TransferTab.DOWNLOAD -> MaterialTheme.colorScheme.secondary
                TransferTab.FAILED -> MaterialTheme.colorScheme.error
                TransferTab.ALL -> null
            }
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = tab.title)
                    if (badgeCount != null && badgeCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Badge(count = badgeCount, color = badgeColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(count: Int, color: Color?) {
    val bg = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// ---------------------------------------------------------------------------
//  Previews
// ---------------------------------------------------------------------------

private val previewTabs = TransferTab.entries

@Preview(name = "SegmentedTabs Light")
@Composable
private fun SegmentedTabsLightPreview() {
    AlistTheme {
        Surface {
            TransferSegmentedTabs(
                selected = TransferTab.UPLOAD,
                uploadCount = 3,
                downloadCount = 2,
                failedCount = 1,
                onSelect = {},
                modifier = Modifier.padding(PaddingValues(16.dp)),
            )
        }
    }
}

@Preview(name = "SegmentedTabs Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SegmentedTabsDarkPreview() {
    AlistTheme {
        Surface {
            TransferSegmentedTabs(
                selected = TransferTab.FAILED,
                uploadCount = 0,
                downloadCount = 5,
                failedCount = 4,
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

// Local Surface re-export — TransferScreen only references AppScaffold so this
// is a lightweight Material3 Surface wrapper for previews.
@Composable
private fun Surface(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth()) { content() }
}
