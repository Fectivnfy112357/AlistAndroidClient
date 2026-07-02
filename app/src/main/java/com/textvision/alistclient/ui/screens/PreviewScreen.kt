package com.textvision.alistclient.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.textvision.alistclient.ui.components.CloudPillButton
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudSurfaceMuted
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
import com.textvision.alistclient.ui.theme.cloudClickable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    onBack: () -> Unit = {},
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
                is PreviewMode.TextTooLarge -> PreviewFallback("文件过大", "可以下载或用其他应用打开", onDownload, onExternalOpen)
                is PreviewMode.External -> PreviewFallback("暂不支持内置预览", "可以下载或用其他应用打开", onDownload, onExternalOpen)
                PreviewMode.Unavailable -> PreviewFallback("无法预览", "当前文件没有可用预览链接，请下载后查看", onDownload, onExternalOpen)
            }
        }
    }
}

@Composable
private fun ImagePreview(url: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TextPreview(url: String, textRepository: PreviewTextRepository) {
    var text by remember(url) { mutableStateOf("加载中") }
    LaunchedEffect(url) {
        text = textRepository.fetch(url).getOrElse { "无法读取文件" }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
    )
}

@Composable
private fun AudioPreview(url: String) {
    var isPlaying by remember(url) { mutableStateOf(false) }
    var isPrepared by remember(url) { mutableStateOf(false) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var durationMs by remember(url) { mutableIntStateOf(0) }
    var positionMs by remember(url) { mutableIntStateOf(0) }
    var isSeeking by remember(url) { mutableStateOf(false) }
    var seekTarget by remember(url) { mutableFloatStateOf(0f) }

    val player = remember(url) { MediaPlayer() }
    DisposableEffect(url) {
        runCatching {
            player.setOnPreparedListener {
                durationMs = it.duration
                isPrepared = true
            }
            player.setOnCompletionListener {
                isPlaying = false
                positionMs = durationMs
            }
            player.setOnErrorListener { _, _, _ ->
                error = "播放失败"
                true
            }
            player.setDataSource(url)
            player.prepareAsync()
        }.onFailure { error = "播放失败" }
        onDispose { player.release() }
    }

    // 定时刷新播放进度
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            runCatching { positionMs = player.currentPosition }
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 封面圆盘
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(CloudPrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CloudPrimary,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = error ?: if (!isPrepared) "加载中…" else if (isPlaying) "正在播放" else "已暂停",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error != null) CloudErrorText else CloudTextSecondary,
        )

        Spacer(Modifier.height(20.dp))
        val displayPos = if (isSeeking) seekTarget.toInt() else positionMs
        Slider(
            value = if (durationMs > 0) displayPos.coerceIn(0, durationMs).toFloat() else 0f,
            onValueChange = {
                isSeeking = true
                seekTarget = it
            },
            onValueChangeFinished = {
                runCatching {
                    player.seekTo(seekTarget.toInt())
                    positionMs = seekTarget.toInt()
                }
                isSeeking = false
            },
            valueRange = 0f..(durationMs.coerceAtLeast(1).toFloat()),
            enabled = isPrepared && error == null,
            colors = SliderDefaults.colors(
                thumbColor = CloudPrimary,
                activeTrackColor = CloudPrimary,
                inactiveTrackColor = CloudSurfaceMuted,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(displayPos), style = MaterialTheme.typography.labelMedium, color = CloudTextSecondary)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = CloudTextSecondary)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AudioControlButton(
                icon = Icons.Filled.Replay10,
                contentDescription = "后退10秒",
                enabled = isPrepared && error == null,
                onClick = {
                    runCatching {
                        val target = (player.currentPosition - 10_000).coerceAtLeast(0)
                        player.seekTo(target)
                        positionMs = target
                    }
                },
            )
            // 主播放/暂停按钮
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(CloudPrimary)
                    .cloudClickable(enabled = isPrepared && error == null) {
                        runCatching {
                            if (isPlaying) player.pause() else player.start()
                            isPlaying = !isPlaying
                        }.onFailure { error = "播放失败" }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White,
                )
            }
            AudioControlButton(
                icon = Icons.Filled.Forward10,
                contentDescription = "前进10秒",
                enabled = isPrepared && error == null,
                onClick = {
                    runCatching {
                        val target = (player.currentPosition + 10_000).coerceAtMost(durationMs)
                        player.seekTo(target)
                        positionMs = target
                    }
                },
            )
        }
    }
}

@Composable
private fun AudioControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(CloudSurfaceMuted)
            .cloudClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) CloudTextPrimary else CloudTextSecondary,
        )
    }
}

private fun formatDuration(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun PreviewFallback(title: String, message: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CloudEmptyState(
            title = title,
            message = message,
            action = {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    CloudPillButton(
                        text = "下载",
                        leading = {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        },
                        onClick = onDownload,
                    )
                    CloudPillButton(
                        text = "外部打开",
                        containerColor = com.textvision.alistclient.ui.theme.CloudPrimarySoft,
                        contentColor = CloudPrimary,
                        leading = {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        },
                        onClick = onExternalOpen,
                    )
                }
            },
        )
    }
}
