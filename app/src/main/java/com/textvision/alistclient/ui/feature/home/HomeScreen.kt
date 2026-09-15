@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.textvision.alistclient.ui.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.ErrorState
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.feature.home.dto.HomeData
import com.textvision.alistclient.ui.feature.home.dto.PublicData
import com.textvision.alistclient.ui.feature.home.dto.SectionResult
import com.textvision.alistclient.ui.feature.home.dto.ServerStatsData
import com.textvision.alistclient.ui.feature.home.dto.SessionData
import com.textvision.alistclient.ui.feature.home.dto.StorageData
import com.textvision.alistclient.ui.feature.home.dto.TaskBucket
import com.textvision.alistclient.ui.feature.home.dto.TaskData
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
    onManageStorage: () -> Unit = {},
    onSearch: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    HomeScreenContent(
        state = state,
        isRefreshing = isRefreshing,
        isOnline = isOnline,
        onStorageClick = onStorageClick,
        onManageStorage = onManageStorage,
        onRetrySection = viewModel::retrySection,
        onRefresh = viewModel::refresh,
        onSearch = onSearch,
    )
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    isRefreshing: Boolean = false,
    isOnline: Boolean = true,
    onStorageClick: (String) -> Unit,
    onManageStorage: () -> Unit = {},
    onRetrySection: (SectionKey) -> Unit,
    onRefresh: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val serverName = (state as? HomeUiState.Success)
        ?.data?.publicSection
        ?.let { (it as? SectionResult.Ok)?.data?.siteTitle }
        ?: "Alist"
    AppScaffold(
        transparentBase = true,
        background = {},
        topBar = {
            HomeGreeting(
                serverTitle = serverName,
                isOnline = isOnline,
                onRefresh = onRefresh,
                onSearch = onSearch,
                modifier = Modifier.testTag("home_topbar"),
            )
        },
    ) { padding ->
        when (state) {
            is HomeUiState.Loading -> LoadingState(
                modifier = Modifier.padding(padding).testTag("home_loading"),
            )

            is HomeUiState.Error -> ErrorState(
                message = state.message,
                onRetry = onRefresh,
                modifier = Modifier.padding(padding),
            )

            is HomeUiState.Success -> DashboardList(
                data = state.data,
                isRefreshing = isRefreshing,
                isOnline = isOnline,
                contentPadding = padding,
                onStorageClick = onStorageClick,
                onManageStorage = onManageStorage,
                onRetrySection = onRetrySection,
                onRefresh = onRefresh,
            )
        }
    }
}

@Composable
private fun DashboardList(
    data: HomeData,
    isRefreshing: Boolean,
    isOnline: Boolean,
    contentPadding: PaddingValues,
    onStorageClick: (String) -> Unit,
    onManageStorage: () -> Unit,
    onRetrySection: (SectionKey) -> Unit,
    onRefresh: () -> Unit,
) {
    // P3: stabilise the parent-supplied retry callback. The previous
    // `onRetryServerStats = { onRetrySection(SectionKey.ServerStats) }` style
    // rebuilt a fresh lambda on every recomposition, defeating the new
    // @Immutable DTOs and forcing MetricRow / TaskSection to recompose along
    // with the entire dashboard even when their actual data hadn't changed.
    val onRetrySectionState by rememberUpdatedState(onRetrySection)
    val onRetryServerStats: () -> Unit = remember(onRetrySectionState) {
        { onRetrySectionState(SectionKey.ServerStats) }
    }
    val onRetrySession: () -> Unit = remember(onRetrySectionState) {
        { onRetrySectionState(SectionKey.Session) }
    }
    val onRetryTask: () -> Unit = remember(onRetrySectionState) {
        { onRetrySectionState(SectionKey.Task) }
    }
    val onRetryStorage: () -> Unit = remember(onRetrySectionState) {
        { onRetrySectionState(SectionKey.Storage) }
    }
    val onStorageClickState by rememberUpdatedState(onStorageClick)

    // The dashboard is deliberately short. During fast fling reversals it
    // repeatedly reaches an edge, where stretch overscroll consumes the next
    // drag before the list can move in the opposite direction. Keep the
    // dashboard directly manipulable at its bounds rather than stretching it.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        item(key = "hero", contentType = "hero") {
            HeroServerCard(data.publicSection, online = isOnline)
        }
        item(key = "metrics", contentType = "metrics") {
            MetricRow(
                serverStats = data.serverStatsSection,
                session = data.sessionSection,
                onRetryServerStats = onRetryServerStats,
                onRetrySession = onRetrySession,
            )
        }
        item(key = "tasks", contentType = "tasks") {
            Column(modifier = Modifier.testTag("home_task_section")) {
                TaskHeader()
                Spacer(Modifier.height(8.dp))
                TaskSection(
                    task = data.taskSection,
                    onRetry = onRetryTask,
                )
            }
        }
        item(key = "storage-header", contentType = "header") {
            StorageHeader(onManage = onManageStorage)
        }
        val storages = data.storages
        if (storages.isEmpty()) {
            item(key = "storage-empty", contentType = "storage-state") {
                StorageEmptyOrFailed(
                    storageSection = data.storageSection,
                    onRetry = onRetryStorage,
                )
            }
        } else {
            items(
                items = storages,
                key = { it.mountPath },
                contentType = { "storage" },
            ) { storage ->
                // P3: per-row stable click lambda. The previous version logged
                // `mountPath` to logcat on every recomposition of every card
                // (i.e. every scroll frame, every online-state flip, every
                // refresh tick), which is exactly the churn we saw on the
                // dashboard during continuous scrolling.
                val onClick = remember(storage.mountPath) {
                    { onStorageClickState(storage.mountPath) }
                }
                StorageCard(storage = storage, onClick = onClick)
            }
        }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Previews
// ═══════════════════════════════════════════════════════════════════════════

private fun previewData() = HomeData(
    publicSection = SectionResult.Ok(
        PublicData("我的云端小屋", "v3.41.0", null, null, null, null, false),
    ),
    storageSection = SectionResult.Ok(
        StorageData(
            listOf(
                StorageInfo(mountPath = "/aliyun", driver = "Aliyundrive", status = "work"),
                StorageInfo(mountPath = "/quark", driver = "Quark", status = "work"),
                StorageInfo(mountPath = "/baidu", driver = "BaiduNetDisk", status = "disabled"),
                StorageInfo(mountPath = "/local", driver = "Local", status = "work"),
            ),
        ),
    ),
    serverStatsSection = SectionResult.Ok(ServerStatsData(userCount = 12, roleCount = 3, disabledUserCount = 0)),
    sessionSection = SectionResult.Ok(SessionData(totalCount = 5, activeCount = 5)),
    taskSection = SectionResult.Ok(
        TaskData(
            runningCount = 2,
            finishedCount = 12,
            failedBucketIds = emptyList(),
            buckets = listOf(
                TaskBucket("upload", 1),
                TaskBucket("decompress", 1),
                TaskBucket("copy", 0),
                TaskBucket("offline_download", 0),
                TaskBucket("s3_transition", 0),
            ),
        ),
    ),
)

@Preview(name = "Home Light", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun HomeLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        HomeScreenContent(
            state = HomeUiState.Success(previewData()),
            onStorageClick = {},
            onRetrySection = {},
        )
    }
}

@Preview(
    name = "Home Dark",
    showBackground = true,
    widthDp = 380,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        HomeScreenContent(
            state = HomeUiState.Success(previewData()),
            onStorageClick = {},
            onRetrySection = {},
        )
    }
}

@Preview(name = "Home LargeFont", showBackground = true, widthDp = 380, heightDp = 900, fontScale = 1.5f)
@Composable
private fun HomeLargeFontPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        HomeScreenContent(
            state = HomeUiState.Success(previewData()),
            onStorageClick = {},
            onRetrySection = {},
        )
    }
}
