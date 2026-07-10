@file:OptIn(ExperimentalMaterial3Api::class)

package com.textvision.alistclient.ui.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.foundation.CloudDecor
import com.textvision.alistclient.ui.foundation.SkyBlueBackground
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
    onManageStorage: () -> Unit = {},
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
) {
    val serverName = (state as? HomeUiState.Success)
        ?.data?.publicSection
        ?.let { (it as? SectionResult.Ok)?.data?.siteTitle }
    AppScaffold(
        background = {
            SkyBlueBackground()
            CloudDecor()
        },
        topBar = {
            AppTopBar(
                title = "早上好 ✨",
                subtitle = if (isOnline) {
                    "已连接" + (serverName?.let { " · $it" } ?: "")
                } else {
                    "离线"
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(AppIcons.refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = {}) {
                        Icon(AppIcons.search, contentDescription = "搜索")
                    }
                },
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
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HeroServerCard(data.publicSection, online = isOnline) }
            item {
                MetricRow(
                    serverStats = data.serverStatsSection,
                    session = data.sessionSection,
                    onRetryServerStats = { onRetrySection(SectionKey.ServerStats) },
                    onRetrySession = { onRetrySection(SectionKey.Session) },
                )
            }
            item {
                TaskSection(
                    task = data.taskSection,
                    onRetry = { onRetrySection(SectionKey.Task) },
                )
            }
            item { StorageHeader(onManage = onManageStorage) }
            val storages = data.storages
            if (storages.isEmpty()) {
                item {
                    StorageEmptyOrFailed(
                        storageSection = data.storageSection,
                        onRetry = { onRetrySection(SectionKey.Storage) },
                    )
                }
            } else {
                items(storages) { storage ->
                    StorageCard(storage = storage) { onStorageClick(storage.mountPath) }
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
                StorageInfo(mountPath = "/local", driver = "Local", status = "disabled"),
            ),
        ),
    ),
    serverStatsSection = SectionResult.Ok(ServerStatsData(userCount = 5, roleCount = 3, disabledUserCount = 0)),
    sessionSection = SectionResult.Ok(SessionData(totalCount = 4, activeCount = 2)),
    taskSection = SectionResult.Ok(
        TaskData(
            runningCount = 3,
            finishedCount = 12,
            failedBucketIds = emptyList(),
            buckets = listOf(
                TaskBucket("upload", 2),
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
