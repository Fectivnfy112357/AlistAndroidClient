package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
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
import com.textvision.alistclient.ui.components.TransferProgress
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
    get() = status.name + (failureReason?.let { "：$it" } ?: "")

val TransferEntity.showRetry: Boolean
    get() = status.canRetry

val TransferEntity.retryButtonLabel: String
    get() = status.retryLabel ?: "重试"

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
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("传输")
        if (state.shouldShowEmptyState) Text(state.emptyMessage)
        LazyColumn {
            items(transfers, key = { it.id }) { task ->
                TransferRow(task, onCancel = { onCancel(task.id) }, onRetry = { onRetry(task.id) })
            }
        }
    }
}

@Composable
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    ListItem(
        headlineContent = { Text(task.fileName) },
        supportingContent = {
            Column {
                Text(task.statusText)
                TransferProgress(task.bytesDone, task.totalBytes)
                Row {
                    if (task.status in setOf(TransferStatus.Waiting, TransferStatus.Uploading, TransferStatus.Downloading)) {
                        Button(onClick = onCancel) { Text("取消") }
                    }
                    if (task.showRetry) {
                        Button(onClick = onRetry) { Text(task.retryButtonLabel) }
                    }
                }
            }
        }
    )
}
