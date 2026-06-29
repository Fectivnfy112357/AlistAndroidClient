package com.textvision.alistclient.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.common.error.ErrorMessageMapper
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudLoadingState
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudSearchBar
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun FileScreen(viewModel: FileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.loadIfNeeded("/") }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.enqueueUpload(uri)
    }

    val currentPath = when (val s = state) {
        is FileUiState.Loading -> s.path
        is FileUiState.Success -> s.path
        is FileUiState.Error -> s.path
    }

    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(
            title = "我的文件",
            subtitle = currentPath,
            action = {
                CloudRoundIconButton(
                    icon = Icons.Default.UploadFile,
                    contentDescription = "上传",
                    onClick = { uploadLauncher.launch("*/*") },
                    modifier = Modifier.testTag("upload_button"),
                )
            },
        )
        CloudSearchBar(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = "搜索",
        )
        Spacer(Modifier.height(10.dp))
        if (!isOnline) {
            CloudStatusBanner(text = "当前无网络", kind = CloudBannerKind.Warning)
            Spacer(Modifier.height(10.dp))
        }
        when (val s = state) {
            is FileUiState.Loading -> CloudCard { CloudLoadingState() }
            is FileUiState.Error -> CloudCard {
                CloudEmptyState(
                    title = ErrorMessageMapper.toUserMessage(s.error),
                    action = {
                        androidx.compose.material3.TextButton(onClick = viewModel::refresh) {
                            Text("重试")
                        }
                    },
                )
            }
            is FileUiState.Success -> {
                if (s.isCurrentDirectoryFilter) {
                    CloudStatusBanner(text = "当前目录搜索结果")
                    Spacer(Modifier.height(10.dp))
                }
                if (s.items.isEmpty()) {
                    CloudCard {
                        CloudEmptyState(
                            title = "这里还没有文件",
                            message = "可以通过右上角上传文件",
                        )
                    }
                } else {
                    CloudCard {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            items(s.items, key = { it.path }) { item ->
                                FileRow(
                                    item = item,
                                    onOpenDir = { viewModel.load(item.path) },
                                    onDownload = { viewModel.enqueueDownload(item) },
                                    onShare = {
                                        val intent = PreviewRouter.shareLinkIntent(item.path)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    onOpenDir: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    CloudListItem(
        title = item.name,
        subtitle = item.subtitleText(),
        onClick = if (item.isDir) onOpenDir else null,
        leading = { FileTypeIcon(item.type) },
        trailing = {
            if (item.isDir) {
                Text("›", color = CloudTextSecondary, style = MaterialTheme.typography.titleLarge)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.testTag("share_button")) {
                        Icon(Icons.Default.Share, contentDescription = "分享链接", tint = CloudPrimary)
                    }
                    IconButton(onClick = onDownload, modifier = Modifier.testTag("download_button")) {
                        Icon(Icons.Default.Download, contentDescription = "下载", tint = CloudPrimary)
                    }
                }
            }
        },
    )
}

private fun FileItem.subtitleText(): String = when {
    isDir -> "文件夹"
    size == 0L -> "未知大小"
    size < 1024L -> "$size B"
    size < 1024L * 1024L -> "${size / 1024L} KB"
    size < 1024L * 1024L * 1024L -> "${size / (1024L * 1024L)} MB"
    else -> "${size / (1024L * 1024L * 1024L)} GB"
}