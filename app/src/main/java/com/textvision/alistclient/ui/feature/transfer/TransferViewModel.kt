package com.textvision.alistclient.ui.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
) : ViewModel() {

    private val _tab = MutableStateFlow(TransferTab.UPLOAD)

    val state: StateFlow<TransferListUiState> = combine(
        _tab,
        manager.observeTransfers(),
    ) { tab, all ->
        TransferListUiState(all = all, tab = tab)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransferListUiState(),
    )

    fun selectTab(tab: TransferTab) {
        _tab.value = tab
    }

    fun cancel(id: String) = manager.cancel(id)
    fun retry(id: String) = manager.retry(id)
    fun delete(id: String) = manager.delete(id)
}
