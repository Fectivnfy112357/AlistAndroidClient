package com.textvision.alistclient.ui.feature.preview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.preview.MimeTypeResolver
import com.textvision.alistclient.preview.PreviewMode
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.KeyValueRow
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600

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

    val onDownload: () -> Unit = { viewModel.enqueueDownload(path, name) }
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
                subtitle = name,
                onBack = onBack,
                actions = {
                    PreviewTopBarAction(
                        icon = AppIcons.share,
                        contentDescription = "分享",
                        brand = false,
                        onClick = onShare,
                    )
                    Spacer(Modifier.size(6.dp))
                    PreviewTopBarAction(
                        icon = AppIcons.download,
                        contentDescription = "下载",
                        brand = true,
                        onClick = onDownload,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
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

/** Top-bar circle button — translucent white or brand-gradient fill. */
@Composable
private fun PreviewTopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    brand: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .let { base ->
                if (brand) base.background(Brush.linearGradient(listOf(Brand500, Brand600)))
                else base.background(com.textvision.alistclient.ui.theme.Surface.copy(alpha = 0.7f))
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (brand) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
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
            title = formatImageTitle(name),
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

/** Three ghost-pill action buttons with leading icon — matches prototype §5.2.4. */
@Composable
private fun ActionRow(
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onExternalOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewActionPill(
            label = "分享",
            icon = AppIcons.share,
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
        PreviewActionPill(
            label = "复制直链",
            icon = AppIcons.link,
            onClick = onCopyLink,
            modifier = Modifier.weight(1f),
        )
        PreviewActionPill(
            label = "其他应用",
            icon = AppIcons.external,
            onClick = onExternalOpen,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PreviewActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(com.textvision.alistclient.ui.theme.Surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .height(38.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(com.textvision.alistclient.ui.theme.Surface.copy(alpha = 0.7f))
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                modifier = Modifier.padding(bottom = 10.dp),
                textAlign = TextAlign.Start,
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

private fun formatImageTitle(name: String): String = name
private fun formatImageSubtitle(size: Long): String = formatSize(size)