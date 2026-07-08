package com.textvision.alistclient.ui.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransferTab(
    val type: TransferType,
    val title: String,
    val emptyMessage: String,
) {
    UPLOAD(TransferType.Upload, "上传", "暂无上传任务"),
    DOWNLOAD(TransferType.Download, "下载", "暂无下载任务"),
}

data class TransferListUiState(
    val all: List<TransferEntity> = emptyList(),
    val tab: TransferTab = TransferTab.UPLOAD,
    val isOnline: Boolean = true,
) {
    val visible: List<TransferEntity> = all.filter { it.type == tab.type }
    val summary: String
        get() {
            if (all.isEmpty()) return "传输任务"
            val active = visible.count { it.status in ActiveTransferStatuses }
            val failed = visible.count { it.status == TransferStatus.Failed || it.status == TransferStatus.Interrupted }
            val completed = visible.count { it.status == TransferStatus.Success }
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

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val manager: TransferManager,
    private val networkMonitor: NetworkMonitorContract,
) : ViewModel() {

    private val _tab = MutableStateFlow(TransferTab.UPLOAD)
    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val state: StateFlow<TransferListUiState> = combine(
        _tab,
        manager.observeTransfers(),
        networkMonitor.isOnline,
    ) { tab, all, online ->
        TransferListUiState(all = all, tab = tab, isOnline = online)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TransferListUiState(),
    )

    fun selectTab(tab: TransferTab) {
        _tab.value = tab
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _isRefreshing.value = false
        }
    }

    fun cancel(id: String) = manager.cancel(id)
    fun retry(id: String) = manager.retry(id)
    fun delete(id: String) = manager.delete(id)
}
