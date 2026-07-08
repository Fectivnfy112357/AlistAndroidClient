package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(viewModel: TransferViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    AppScaffold(
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                val tabs = TransferTab.entries
                tabs.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    ) {
                        Text(text = tab.title)
                    }
                }
            }
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
                TransferListContent(
                    rows = state.visible,
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onDelete = viewModel::delete,
                    emptyTitle = state.tab.emptyMessage,
                    emptyMessage = "对应类型的传输任务会显示在这里",
                    enabled = state.isOnline,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
