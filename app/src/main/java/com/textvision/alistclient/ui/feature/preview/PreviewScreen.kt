package com.textvision.alistclient.ui.feature.preview

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.preview.MimeTypeResolver
import com.textvision.alistclient.preview.PreviewMode
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.KeyValueRow
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

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
    val clipboard = LocalClipboardManager.current

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
    val onShare: () -> Unit = {
        downloadUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val intent = PreviewRouter.shareLinkIntent(url)
            runCatching { context.startActivity(Intent.createChooser(intent, "分享")) }
        }
    }
    val onCopyLink: () -> Unit = {
        downloadUrl?.takeIf { it.isNotBlank() }?.let { url ->
            clipboard.setText(AnnotatedString(url))
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "文件预览",
                subtitle = path,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(AppIcons.share, contentDescription = "分享")
                    }
                    IconButton(onClick = onDownload) {
                        Icon(AppIcons.download, contentDescription = "下载")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewBody(
                mode = mode,
                name = name,
                size = size,
                onDownload = onDownload,
                onExternalOpen = onExternalOpen,
                fetchText = viewModel::fetchText,
            )
            ActionRow(
                onShare = onShare,
                onCopyLink = onCopyLink,
                onExternalOpen = onExternalOpen,
            )
            DetailsCard(
                name = name,
                path = path,
                type = type,
                size = size,
            )
        }
    }
}

@Composable
private fun PreviewBody(
    mode: PreviewMode,
    name: String,
    size: Long,
    onDownload: () -> Unit,
    onExternalOpen: () -> Unit,
    fetchText: suspend (String) -> String,
) {
    when (mode) {
        is PreviewMode.Image -> ImagePreview(
            url = mode.url,
            title = name,
            subtitle = formatImageSubtitle(size),
        )
        is PreviewMode.Text -> TextPreview(mode.url, fetchText)
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

@Composable
private fun ActionRow(
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onExternalOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionButton(
            text = "分享",
            onClick = onShare,
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.OUTLINED,
            leadingIcon = AppIcons.share,
        )
        ActionButton(
            text = "复制直链",
            onClick = onCopyLink,
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.OUTLINED,
            leadingIcon = AppIcons.link,
        )
        ActionButton(
            text = "其他应用",
            onClick = onExternalOpen,
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.OUTLINED,
            leadingIcon = AppIcons.external,
        )
    }
}

@Composable
private fun DetailsCard(
    name: String,
    path: String,
    type: FileType,
    size: Long,
) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            KeyValueRow(label = "类型", value = formatFileTypeLabel(name, type))
            KeyValueRow(label = "尺寸", value = formatSize(size))
            KeyValueRow(label = "修改时间", value = "—")
            KeyValueRow(label = "位置", value = path)
        }
    }
}

private fun formatFileTypeLabel(name: String, type: FileType): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    val mime = MimeTypeResolver.infer(name)
    return when {
        ext.isNotEmpty() -> ext.uppercase() + " · " + mime
        else -> mime
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIdx = 0
    while (value >= 1024.0 && unitIdx < units.lastIndex) {
        value /= 1024.0
        unitIdx++
    }
    return if (value >= 100) "%.0f %s".format(value, units[unitIdx])
    else "%.1f %s".format(value, units[unitIdx])
}

private fun formatImageSubtitle(size: Long): String = formatSize(size)

// ---------------------------------------------------------------------------
//  Previews
//
//  PreviewScreen wires Hilt + navigation backstack, so we preview the layout
//  sections in isolation (AppTopBar + ActionRow + DetailsCard) under
//  AlistTheme — three themes per prototype success criterion §1.3.7.
// ---------------------------------------------------------------------------

@Preview(name = "TopBar + ActionRow + Details (Light)")
@Composable
private fun PreviewTopBarLight() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "文件预览",
                subtitle = "/d/photos/海岸线日落.jpg",
                onBack = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(AppIcons.share, contentDescription = "分享")
                    }
                    IconButton(onClick = {}) {
                        Icon(AppIcons.download, contentDescription = "下载")
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActionRow(onShare = {}, onCopyLink = {}, onExternalOpen = {})
                DetailsCard(
                    name = "海岸线日落.jpg",
                    path = "/d/photos/海岸线日落.jpg",
                    type = FileType.Image,
                    size = 2_516_582,
                )
            }
        }
    }
}

@Preview(name = "TopBar + ActionRow + Details (Dark)")
@Composable
private fun PreviewTopBarDark() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "文件预览",
                subtitle = "/d/notes/Rustdesk密钥.md",
                onBack = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(AppIcons.share, contentDescription = "分享")
                    }
                    IconButton(onClick = {}) {
                        Icon(AppIcons.download, contentDescription = "下载")
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActionRow(onShare = {}, onCopyLink = {}, onExternalOpen = {})
                DetailsCard(
                    name = "Rustdesk密钥.md",
                    path = "/d/notes/Rustdesk密钥.md",
                    type = FileType.Text,
                    size = 4_096,
                )
            }
        }
    }
}

@Preview(name = "TopBar + ActionRow + Details (Audio)")
@Composable
private fun PreviewTopBarAudio() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "文件预览",
                subtitle = "/music/lofi/lofi-chill.mp3",
                onBack = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(AppIcons.share, contentDescription = "分享")
                    }
                    IconButton(onClick = {}) {
                        Icon(AppIcons.download, contentDescription = "下载")
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActionRow(onShare = {}, onCopyLink = {}, onExternalOpen = {})
                DetailsCard(
                    name = "lofi-chill.mp3",
                    path = "/music/lofi/lofi-chill.mp3",
                    type = FileType.Audio,
                    size = 6_582_144,
                )
            }
        }
    }
}