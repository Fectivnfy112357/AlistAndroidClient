package com.textvision.alistclient.ui.feature.file

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.ui.components.AppAlertDialog
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.SearchField
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun FileScreen(
    initialPath: String = "/",
    onPreview: (FileItem) -> Unit = {},
    onFolderNavigate: (path: String) -> Unit = {},
    vm: FileViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(initialPath) {
        if (state.path != initialPath) {
            vm.onIntent(FileIntent.Load(initialPath))
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "文件",
                subtitle = state.path,
                actions = {
                    IconButton(onClick = { vm.onIntent(FileIntent.Load(state.path)) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                query = state.query,
                onQueryChange = { vm.onIntent(FileIntent.Search(it)) },
            )
            Spacer(Modifier.height(8.dp))
            state.error?.let { msg ->
                StatusBanner(
                    kind = BannerKind.ERROR,
                    message = msg,
                    actionLabel = "重试",
                    onAction = { vm.onIntent(FileIntent.Load(state.path)) },
                )
            }
            if (!state.isOnline) {
                StatusBanner(
                    kind = BannerKind.WARNING,
                    message = "当前离线，部分操作不可用",
                )
            }
            if (state.isMultiSelectMode) {
                FileMultiSelectBar(
                    selectionCount = state.selection.size,
                    onSelectAll = {
                        state.visibleFiles.map { it.path }
                            .filter { it !in state.selection }
                            .forEach { vm.onIntent(FileIntent.MultiSelectToggle(it)) }
                    },
                    onDelete = { showDeleteConfirm = true },
                    onDownload = { vm.onIntent(FileIntent.MultiSelectDownload(state.selection.toList())) },
                    onClear = { vm.onIntent(FileIntent.MultiSelectClear) },
                )
            }
            FileListContent(
                state = state,
                onIntent = vm::onIntent,
                onPreview = onPreview,
                onFolderNavigate = onFolderNavigate,
            )
        }
    }

    if (showDeleteConfirm) {
        AppAlertDialog(
            title = "删除确认",
            message = "确定删除选中的 ${state.selection.size} 项吗？此操作不可恢复。",
            confirmLabel = "删除",
            onConfirm = {
                vm.onIntent(FileIntent.MultiSelectDelete(state.selection.toList()))
                showDeleteConfirm = false
            },
            dismissLabel = "取消",
            onDismiss = { showDeleteConfirm = false },
            destructive = true,
        )
    }
}
