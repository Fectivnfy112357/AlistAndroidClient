package com.textvision.alistclient.ui.feature.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.FileRepository
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FileViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val transferManager: TransferManager,
    private val networkMonitor: NetworkMonitorContract,
) : ViewModel() {

    private val _state = MutableStateFlow(FileUiState())

    val state: StateFlow<FileUiState> = combine(
        _state,
        networkMonitor.isOnline,
    ) { ui, online ->
        ui.copy(isOnline = online)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FileUiState(),
    )

    fun onIntent(intent: FileIntent) {
        when (intent) {
            is FileIntent.Load -> load(intent.path)
            is FileIntent.Search -> _state.update { it.copy(query = intent.query) }
            is FileIntent.Upload -> transferManager.enqueueUpload(intent.uri, _state.value.path)
            is FileIntent.DownloadOne -> downloadOne(intent.path)
            is FileIntent.MultiSelectToggle -> toggleSelect(intent.path)
            FileIntent.MultiSelectClear -> _state.update {
                it.copy(selection = emptySet(), isMultiSelectMode = false)
            }
            is FileIntent.MultiSelectDelete -> deleteSelected(intent.paths)
            is FileIntent.MultiSelectDownload -> downloadSelected(intent.paths)
        }
    }

    private fun load(path: String) {
        _state.update { it.copy(path = path, isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = fileRepository.list(path)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        path = path,
                        files = result.data,
                        isLoading = false,
                        error = null,
                        selection = emptySet(),
                        isMultiSelectMode = false,
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.message.ifBlank { "加载失败 (${result.code})" })
                }
                is ApiResult.NetworkError -> _state.update {
                    it.copy(isLoading = false, error = result.cause.message ?: "网络错误")
                }
            }
        }
    }

    private fun toggleSelect(path: String) {
        _state.update { current ->
            val newSelection = if (path in current.selection) current.selection - path else current.selection + path
            current.copy(
                selection = newSelection,
                isMultiSelectMode = newSelection.isNotEmpty(),
            )
        }
    }

    private fun deleteSelected(paths: List<String>) {
        if (paths.isEmpty()) return
        viewModelScope.launch {
            when (fileRepository.delete(paths)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(selection = emptySet(), isMultiSelectMode = false) }
                    load(_state.value.path)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = "删除失败 (${paths.size} 项)")
                }
                is ApiResult.NetworkError -> _state.update {
                    it.copy(error = "网络错误，删除失败")
                }
            }
        }
    }

    private fun downloadOne(path: String) {
        val file = _state.value.files.firstOrNull { it.path == path }
        if (file?.isDir == true) return
        val name = file?.name ?: path.substringAfterLast('/')
        transferManager.enqueueDownload(path, name)
    }

    private fun downloadSelected(paths: List<String>) {
        if (paths.isEmpty()) return
        val byPath: Map<String, FileItem> = _state.value.files.associateBy { it.path }
        paths.forEach { p ->
            val name = byPath[p]?.name ?: p.substringAfterLast('/')
            if (byPath[p]?.isDir != true) {
                transferManager.enqueueDownload(p, name)
            }
        }
        _state.update { it.copy(selection = emptySet(), isMultiSelectMode = false) }
    }
}
