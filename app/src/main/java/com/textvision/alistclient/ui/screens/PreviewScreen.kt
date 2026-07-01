package com.textvision.alistclient.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import coil3.compose.AsyncImage
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.preview.MimeTypeResolver
import com.textvision.alistclient.preview.PreviewMode
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.preview.PreviewTextRepository
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    val textRepository: PreviewTextRepository,
    private val transferManager: TransferManager,
) : ViewModel() {
    fun enqueueDownload(path: String, name: String): String =
        transferManager.enqueueDownload(remotePath = path, fileName = name)
}

@Composable
fun PreviewScreen(
    name: String,
    path: String,
    type: FileType,
    downloadUrl: String?,
    size: Long,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val mode = remember(name, type, downloadUrl, size) {
        PreviewRouter.route(FileItem(name, path, false, size, null, name.substringAfterLast('.', ""), type, null, downloadUrl))
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
            action = {
                Row {
                    CloudRoundIconButton(Icons.Outlined.Download, "下载", onDownload)
                    Spacer(Modifier.width(6.dp))
                    CloudRoundIconButton(Icons.AutoMirrored.Outlined.OpenInNew, "外部打开", onExternalOpen)
                }
            },
        )
        CloudCard {
            when (mode) {
                is PreviewMode.Image -> ImagePreview(mode.url)
                is PreviewMode.Text -> TextPreview(mode.url, viewModel.textRepository)
                is PreviewMode.Audio -> AudioPreview(mode.url)
                is PreviewMode.TextTooLarge -> PreviewFallback("文件过大", "可以下载或用其他应用打开", onDownload, onExternalOpen)
                is PreviewMode.External -> PreviewFallback("暂不支持内置预览", "可以下载或用其他应用打开", onDownload, onExternalOpen)
                PreviewMode.Unavailable -> PreviewFallback("无法预览", "当前文件没有可用预览链接，请下载后查看", onDownload, onExternalOpen)
            }
        }
    }
}

@Composable
private fun ImagePreview(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TextPreview(url: String, textRepository: PreviewTextRepository) {
    var text by remember(url) { mutableStateOf("加载中") }
    LaunchedEffect(url) {
        text = textRepository.fetch(url).getOrElse { "无法读取文件" }
    }
    Text(
        text = text,
        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
    )
}

@Composable
private fun AudioPreview(url: String) {
    var isPlaying by remember(url) { mutableStateOf(false) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    val player = remember(url) { MediaPlayer() }
    DisposableEffect(url) {
        runCatching {
            player.setDataSource(url)
            player.prepareAsync()
        }.onFailure { error = "播放失败" }
        onDispose { player.release() }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = CloudPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error ?: if (isPlaying) "播放中" else "准备播放",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error != null) CloudErrorText else CloudTextSecondary,
        )
        Spacer(modifier = Modifier.height(20.dp))
        CloudPillButton(
            text = if (isPlaying) "暂停" else "播放",
            leading = {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            onClick = {
                runCatching {
                    if (isPlaying) player.pause() else player.start()
                    isPlaying = !isPlaying
                }.onFailure { error = "播放失败" }
            },
        )
    }
}

@Composable
private fun PreviewFallback(title: String, message: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    CloudEmptyState(
        title = title,
        message = message,
        action = {
            Row {
                TextButton(onClick = onDownload) { Text("下载") }
                TextButton(onClick = onExternalOpen) { Text("外部打开") }
            }
        },
    )
}
