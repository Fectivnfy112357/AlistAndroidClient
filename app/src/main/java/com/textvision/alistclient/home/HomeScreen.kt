@file:OptIn(ExperimentalMaterial3Api::class)

package com.textvision.alistclient.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.ui.components.ErrorState
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
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
    onRetrySection: (SectionKey) -> Unit,
    onRefresh: () -> Unit = {},
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "首页",
                subtitle = if (isOnline) "在线" else "离线",
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
                contentPadding = padding,
                onStorageClick = onStorageClick,
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
    contentPadding: PaddingValues,
    onStorageClick: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { HeroCard(data.publicSection) }
            item {
                KpiRow(
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
            item {
                StorageSection(
                    storageSection = data.storageSection,
                    onStorageClick = onStorageClick,
                    onRetry = { onRetrySection(SectionKey.Storage) },
                )
            }
        }
    }
}
