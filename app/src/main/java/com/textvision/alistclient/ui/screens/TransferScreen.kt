package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.TransferProgress
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val EmptyTransferMessage = "暂无传输任务"

data class TransferListUiState(val transfers: List<TransferEntity>) {
    val emptyMessage: String = EmptyTransferMessage
    val shouldShowEmptyState: Boolean = transfers.isEmpty()
}

val TransferEntity.statusText: String
    get() = status.displayName + (failureReason?.let { "：$it" } ?: "")

val TransferEntity.showRetry: Boolean
    get() = status.canRetry

val TransferEntity.retryButtonLabel: String
    get() = status.retryLabel ?: "重试"

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
        CloudTopBar(title = "传输", subtitle = "上传与下载任务")
        if (state.shouldShowEmptyState) {
            CloudCard {
                CloudEmptyState(title = state.emptyMessage, message = "上传和下载任务会显示在这里")
            }
        } else {
            CloudCard {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(transfers, key = { it.id }) { task ->
                        TransferRow(task, onCancel = { onCancel(task.id) }, onRetry = { onRetry(task.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    CloudListItem(
        title = task.fileName,
        subtitle = task.statusText,
        trailing = {},
    )
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        TransferProgress(task.bytesDone, task.totalBytes)
        Text(
            text = task.progressText,
            color = CloudPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Row {
            if (task.status in setOf(TransferStatus.Waiting, TransferStatus.Uploading, TransferStatus.Downloading)) {
                TextButton(onClick = onCancel) { Text("取消", color = CloudErrorText) }
            }
            if (task.showRetry) {
                TextButton(onClick = onRetry) { Text(task.retryButtonLabel, color = CloudPrimary) }
            }
        }
    }
}