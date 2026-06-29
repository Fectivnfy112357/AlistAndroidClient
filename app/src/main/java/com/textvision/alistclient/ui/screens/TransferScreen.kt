package com.textvision.alistclient.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.TransferProgress
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val EmptyTransferMessage = "暂无传输任务"

data class TransferListUiState(val transfers: List<TransferEntity>) {
    val emptyMessage: String = EmptyTransferMessage
    val shouldShowEmptyState: Boolean = transfers.isEmpty()
    val summaryText: String
        get() {
            if (transfers.isEmpty()) return "上传和下载任务"
            val active = transfers.count { it.status in ActiveTransferStatuses }
            val failed = transfers.count { it.showRetry }
            val completed = transfers.count { it.status == TransferStatus.Success }
            return listOfNotNull(
                active.takeIf { it > 0 }?.let { "$it 个进行中" },
                failed.takeIf { it > 0 }?.let { "$it 个失败" },
                completed.takeIf { it > 0 }?.let { "$it 个完成" },
            ).joinToString(" · ").ifBlank { "暂无进行中的任务" }
        }
}

private val ActiveTransferStatuses = setOf(
    TransferStatus.Waiting,
    TransferStatus.Uploading,
    TransferStatus.Downloading,
)

val TransferEntity.statusText: String
    get() = status.displayName + (failureReason?.let { "：$it" } ?: "")

val TransferEntity.showRetry: Boolean
    get() = status.canRetry

val TransferEntity.retryButtonLabel: String
    get() = status.retryLabel ?: "重试"

val TransferEntity.primaryActionLabel: String?
    get() = when {
        status in ActiveTransferStatuses -> "取消"
        showRetry -> retryButtonLabel
        else -> null
    }

val TransferEntity.isComplete: Boolean
    get() = status == TransferStatus.Success

val TransferEntity.progressText: String
    get() = if (totalBytes > 0L) {
        val percent = (bytesDone.toDouble() / totalBytes.toDouble() * 100.0).coerceIn(0.0, 100.0)
        "${"%.1f".format(percent)}% · ${bytesDone.formatBytes()} / ${totalBytes.formatBytes()}"
    } else {
        "${bytesDone.formatBytes()} / 未知大小"
    }

private fun Long.formatBytes(): String = when {
    this < 1024L -> "$this B"
    this < 1024L * 1024L -> "${"%.1f".format(this / 1024.0)} KB"
    this < 1024L * 1024L * 1024L -> "${"%.1f".format(this / (1024.0 * 1024.0))} MB"
    else -> "${"%.1f".format(this / (1024.0 * 1024.0 * 1024.0))} GB"
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val manager: TransferManager,
) : ViewModel() {
    val transfers = manager.observeTransfers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun cancel(id: String) = manager.cancel(id)
    fun retry(id: String) = manager.retry(id)
}

@Composable
fun TransferScreen(viewModel: TransferViewModel = hiltViewModel()) {
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    TransferScreenContent(
        transfers = transfers,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
    )
}

@Composable
fun TransferScreenContent(
    transfers: List<TransferEntity>,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    val state = TransferListUiState(transfers)
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "传输", subtitle = state.summaryText)
        if (state.shouldShowEmptyState) {
            CloudCard {
                CloudEmptyState(title = state.emptyMessage, message = "上传和下载任务会显示在这里")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transfers, key = { it.id }) { task ->
                    TransferRow(task, onCancel = { onCancel(task.id) }, onRetry = { onRetry(task.id) })
                }
            }
        }
    }
}

@Composable
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .clip(CloudShapes.Control)
            .background(CloudSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    color = CloudTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = task.statusText,
                    color = if (task.showRetry) CloudErrorText else CloudTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TransferAction(task, onCancel, onRetry)
        }

        Spacer(Modifier.height(8.dp))
        TransferProgress(task.bytesDone, task.totalBytes)
        Text(
            text = task.progressText,
            color = CloudPrimary,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun TransferAction(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    val label = task.primaryActionLabel
    when {
        label == "取消" -> TextButton(
            onClick = onCancel,
            modifier = Modifier.widthIn(min = 72.dp),
        ) { Text(label, color = CloudErrorText) }
        label != null -> TextButton(
            onClick = onRetry,
            modifier = Modifier.widthIn(min = 72.dp),
        ) { Text(label, color = CloudPrimary) }
        task.isComplete -> Box(
            modifier = Modifier
                .clip(CloudShapes.Pill)
                .background(CloudPrimarySoft)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("完成", color = CloudPrimary)
        }
    }
}