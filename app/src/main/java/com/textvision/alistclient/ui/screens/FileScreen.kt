package com.textvision.alistclient.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.common.error.ErrorMessageMapper
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.ui.components.BreadcrumbBar
import com.textvision.alistclient.ui.components.FileTypeIcon

@Composable
fun FileScreen(viewModel: FileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load("/") }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.enqueueUpload(uri)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        val currentPath = when (val s = state) {
            is FileUiState.Loading -> s.path
            is FileUiState.Success -> s.path
            is FileUiState.Error -> s.path
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BreadcrumbBar(currentPath, viewModel::load, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { uploadLauncher.launch("*/*") },
                modifier = Modifier.testTag("upload_button"),
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "上传")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("搜索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        when (val s = state) {
            is FileUiState.Loading -> CircularProgressIndicator()
            is FileUiState.Error -> Column {
                Text(ErrorMessageMapper.toUserMessage(s.error))
                Button(onClick = viewModel::refresh) { Text("重试") }
            }
            is FileUiState.Success -> {
                if (s.isCurrentDirectoryFilter) Text("当前目录搜索结果")
                if (s.items.isEmpty()) Text("这里没有文件")
                LazyColumn {
                    items(s.items, key = { it.path }) { item ->
                        FileRow(
                            item = item,
                            onOpenDir = { viewModel.load(item.path) },
                            onDownload = { viewModel.enqueueDownload(item) },
                        )
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
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            Text(
                if (!item.isDir && item.size == 0L) "未知大小"
                else if (item.isDir) "文件夹"
                else "${item.size} B"
            )
        },
        leadingContent = { FileTypeIcon(item.type) },
        trailingContent = {
            if (!item.isDir) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.testTag("download_button"),
                ) {
                    Icon(Icons.Default.Download, contentDescription = "下载")
                }
            } else {
                TextButton(onClick = onOpenDir) { Text("打开") }
            }
        },
        modifier = Modifier.clickable(enabled = item.isDir) { onOpenDir() }
    )
}
