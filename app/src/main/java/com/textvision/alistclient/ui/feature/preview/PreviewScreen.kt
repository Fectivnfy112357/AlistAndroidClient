package com.textvision.alistclient.ui.feature.preview

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.preview.MimeTypeResolver
import com.textvision.alistclient.preview.PreviewMode
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun PreviewScreen(
    name: String,
    path: String,
    type: FileType,
    downloadUrl: String?,
    size: Long,
    onBack: () -> Unit = {},
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val mode = remember(name, type, downloadUrl, size) {
        PreviewRouter.route(
            FileItem(
                name = name,
                path = path,
                isDir = false,
                size = size,
                modifiedAt = null,
                extension = name.substringAfterLast('.', ""),
                type = type,
                thumbnailUrl = null,
                downloadUrl = downloadUrl,
            ),
        )
    }
    val context = LocalContext.current
    val onDownload: () -> Unit = {
        viewModel.enqueueDownload(path, name)
    }
    val onExternalOpen: () -> Unit = {
        downloadUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.parse(url), MimeTypeResolver.infer(name))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "文件预览",
                subtitle = name,
                onNavigateUp = onBack,
                actions = {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Outlined.Download, contentDescription = "下载")
                    }
                    IconButton(onClick = onExternalOpen) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "外部打开")
                    }
                },
            )
        },
    ) { innerPadding ->
        Card(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (mode) {
                is PreviewMode.Image -> ImagePreview(mode.url)
                is PreviewMode.Text -> TextPreview(mode.url, viewModel::fetchText)
                is PreviewMode.Audio -> AudioPreview(mode.url)
                is PreviewMode.TextTooLarge -> PreviewFallback(
                    title = "文件过大",
                    message = "可以下载或用其他应用打开",
                    onDownload = onDownload,
                    onExternalOpen = onExternalOpen,
                )
                is PreviewMode.External -> PreviewFallback(
                    title = "暂不支持内置预览",
                    message = "可以下载或用其他应用打开",
                    onDownload = onDownload,
                    onExternalOpen = onExternalOpen,
                )
                PreviewMode.Unavailable -> PreviewFallback(
                    title = "无法预览",
                    message = "当前文件没有可用预览链接，请下载后查看",
                    onDownload = onDownload,
                    onExternalOpen = onExternalOpen,
                )
            }
        }
    }
}