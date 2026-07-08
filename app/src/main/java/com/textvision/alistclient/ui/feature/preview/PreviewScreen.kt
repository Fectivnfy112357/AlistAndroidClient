package com.textvision.alistclient.ui.feature.preview

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.preview.MimeTypeResolver
import com.textvision.alistclient.preview.PreviewMode
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar

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
    CloudScaffold {
        CloudTopBar(
            title = "文件预览",
            subtitle = name,
            navigationIcon = {
                CloudRoundIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
                Spacer(Modifier.width(10.dp))
            },
            action = {
                CloudRoundIconButton(Icons.Outlined.Download, "下载", onDownload)
                Spacer(Modifier.width(6.dp))
                CloudRoundIconButton(Icons.AutoMirrored.Outlined.OpenInNew, "外部打开", onExternalOpen)
            },
        )
        CloudCard(modifier = Modifier.weight(1f)) {
            when (mode) {
                is PreviewMode.Image -> ImagePreview(mode.url)
                is PreviewMode.Text -> TextPreview(mode.url, viewModel.textRepository)
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