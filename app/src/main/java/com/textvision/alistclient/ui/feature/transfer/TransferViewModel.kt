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
    val title: String,
    val emptyMessage: String,
    val typeFilter: TransferType? = null,
    val failedOnly: Boolean = false,
) {
    ALL("全部", "暂无传输任务", typeFilter = null),
    UPLOAD("上传", "暂无上传任务", typeFilter = TransferType.Upload),
    DOWNLOAD("下载", "暂无下载任务", typeFilter = TransferType.Download),
    FAILED("失败", "暂无失败任务", failedOnly = true),
    ;

    /** Returns true when [task] belongs to this tab. */
    fun matches(task: TransferEntity): Boolean {
        if (failedOnly) {
            return task.status == TransferStatus.Failed || task.status == TransferStatus.Interrupted
        }
        return typeFilter == null || task.type == typeFilter
    }
}

data class TransferListUiState(
    val all: List<TransferEntity> = emptyList(),
    val tab: TransferTab = TransferTab.ALL,
    val isOnline: Boolean = true,
    // P1: derived fields are computed once per upstream emission instead of on
    // every read. Previously the `summary` getter ran two full-table `count`
    // invocations each time the top bar read it, and `visible` was filtered
    // lazily — every recomposition paid the cost again. Hoisting these into
    // the constructor leaves the Composable side reading plain `val`s.
    val visible: List<TransferEntity> = emptyList(),
    val allCount: Int = 0,
    val uploadCount: Int = 0,
    val downloadCount: Int = 0,
    val failedCount: Int = 0,
    val summary: String = "传输任务",
) {
    companion object {
        fun derive(
            all: List<TransferEntity>,
            tab: TransferTab,
            isOnline: Boolean,
        ): TransferListUiState {
            val visible = all.filter(tab::matches)
            val uploadCount = all.count { it.type == TransferType.Upload }
            val downloadCount = all.count { it.type == TransferType.Download }
            val failedCount = all.count {
                it.status == TransferStatus.Failed || it.status == TransferStatus.Interrupted
            }
            val summary = computeSummary(all, failedCount)
            return TransferListUiState(
                all = all,
                tab = tab,
                isOnline = isOnline,
                visible = visible,
                allCount = all.size,
                uploadCount = uploadCount,
                downloadCount = downloadCount,
                failedCount = failedCount,
                summary = summary,
            )
        }

        private fun computeSummary(
            all: List<TransferEntity>,
            failedCount: Int,
        ): String {
            if (all.isEmpty()) return "传输任务"
            val active = all.count { it.status in TransferStatus.activeStatuses }
            val completed = all.count { it.status == TransferStatus.Success }
            return listOfNotNull(
                active.takeIf { it > 0 }?.let { "$it 进行中" },
                failedCount.takeIf { it > 0 }?.let { "$it 失败" },
                completed.takeIf { it > 0 }?.let { "$it 完成" },
            ).joinToString(" · ").ifBlank { "暂无进行中的任务" }
        }
    }
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val manager: TransferManager,
    private val networkMonitor: NetworkMonitorContract,
) : ViewModel() {

    private val _tab = MutableStateFlow(TransferTab.ALL)
    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val state: StateFlow<TransferListUiState> = combine(
        _tab,
        manager.observeTransfers(),
        networkMonitor.isOnline,
    ) { tab, all, online ->
        // Derive once per emission so the Composable side reads plain fields
        // instead of triggering per-read table scans.
        TransferListUiState.derive(all, tab, online)
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
