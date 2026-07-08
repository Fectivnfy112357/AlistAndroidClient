package com.textvision.alistclient.ui.feature.file

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.ui.components.EmptyState
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.components.toFileCategory
import com.textvision.alistclient.util.FileSizeFormatter

@Composable
fun FileListContent(
    state: FileUiState,
    onIntent: (FileIntent) -> Unit,
    onPreview: (FileItem) -> Unit,
    onFolderNavigate: (path: String) -> Unit,
    onShare: (FileItem) -> Unit = {},
    onCopyLink: (FileItem) -> Unit = {},
    onDownloadFeedback: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val files = state.visibleFiles
    if (state.isLoading && state.files.isEmpty()) {
        LoadingState(message = "加载中…", modifier = modifier)
        return
    }
    if (files.isEmpty() && !state.isLoading) {
        EmptyState(
            title = "文件夹为空",
            message = state.error ?: "这里没有文件",
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(files, key = { it.path }) { file ->
            val selected = file.path in state.selection
            ListItemRow(
                leading = { FileTypeIcon(file.type.toFileCategory()) },
                title = file.name,
                subtitle = if (file.isDir) "文件夹" else FileSizeFormatter.humanize(file.size),
                onClick = {
                    when {
                        state.isMultiSelectMode -> onIntent(FileIntent.MultiSelectToggle(file.path))
                        file.isDir -> onFolderNavigate(file.path)
                        else -> onPreview(file)
                    }
                },
                onLongClick = { onIntent(FileIntent.MultiSelectToggle(file.path)) },
                trailing = if (state.isMultiSelectMode) {
                    {
                        IconButton(onClick = { onIntent(FileIntent.MultiSelectToggle(file.path)) }) {
                            Icon(
                                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (selected) "已选中" else "未选中",
                            )
                        }
                    }
                } else if (!file.isDir) {
                    {
                        FileRowMenu(
                            file = file,
                            onIntent = onIntent,
                            onShare = onShare,
                            onCopyLink = onCopyLink,
                            onDownloadFeedback = onDownloadFeedback,
                        )
                    }
                } else {
                    {}
                },
            )
        }
    }
}

@Composable
private fun FileRowMenu(
    file: FileItem,
    onIntent: (FileIntent) -> Unit,
    onShare: (FileItem) -> Unit,
    onCopyLink: (FileItem) -> Unit,
    onDownloadFeedback: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("下载") },
            onClick = {
                expanded = false
                onIntent(FileIntent.DownloadOne(file.path))
                onDownloadFeedback()
            },
        )
        DropdownMenuItem(
            text = { Text("分享") },
            onClick = {
                expanded = false
                onShare(file)
            },
        )
        DropdownMenuItem(
            text = { Text("复制直链") },
            onClick = {
                expanded = false
                onCopyLink(file)
            },
        )
    }
}
