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
import kotlinx.coroutines.Job
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

    // P0: load generation + dedicated Job. Earlier, `load` launched a new
    // coroutine on every resume and let late responses overwrite newer state
    // (e.g. tapping the refresh button while the prior request was still in
    // flight produced visible flicker when the older payload landed last). We
    // now cancel the prior job and tag the response so a late return is
    // discarded.
    private var loadJob: Job? = null
    private var loadGeneration: Int = 0

    fun onIntent(intent: FileIntent) {
        when (intent) {
            is FileIntent.Load -> load(intent.path)
            is FileIntent.Search -> _state.update { it.copy(query = intent.query) }
            is FileIntent.Upload -> transferManager.enqueueUpload(intent.uri, _state.value.path)
            is FileIntent.DownloadOne -> downloadOne(intent.path)
            is FileIntent.MultiSelectToggle -> toggleSelect(intent.path)
            is FileIntent.MultiSelectSet -> _state.update {
                it.copy(
                    selection = intent.paths,
                    isMultiSelectMode = intent.paths.isNotEmpty(),
                )
            }
            FileIntent.MultiSelectClear -> _state.update {
                it.copy(selection = emptySet(), isMultiSelectMode = false)
            }
            is FileIntent.MultiSelectDelete -> deleteSelected(intent.paths)
            is FileIntent.MultiSelectDownload -> downloadSelected(intent.paths)
        }
    }

    /**
     * Resume-friendly entry point. Skips the network round-trip when we
     * already have a successful cached listing for [path]; otherwise falls
     * through to a fresh load. Called from [FileScreen]'s `LifecycleResumeEffect`,
     * which previously issued an unconditional Load on every resume (including
     * bottom-tab returns and Compose back-navigation) — paying a full server
     * request + UI refresh for a directory whose contents had not changed.
     */
    fun ensureLoaded(path: String) {
        if (_state.value.lastLoadedForPath == path && !_state.value.isLoading) return
        load(path)
    }

    private fun load(path: String) {
        loadJob?.cancel()
        val generation = ++loadGeneration
        _state.update { it.copy(path = path, isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = fileRepository.list(path)) {
                is ApiResult.Success -> if (generation == loadGeneration) {
                    _state.update {
                        it.copy(
                            path = path,
                            files = result.data,
                            isLoading = false,
                            error = null,
                            selection = emptySet(),
                            isMultiSelectMode = false,
                            lastLoadedForPath = path,
                        )
                    }
                }
                is ApiResult.Failure -> if (generation == loadGeneration) {
                    _state.update {
                        it.copy(isLoading = false, error = result.message.ifBlank { "加载失败 (${result.code})" })
                    }
                }
                is ApiResult.NetworkError -> if (generation == loadGeneration) {
                    _state.update {
                        it.copy(isLoading = false, error = result.cause.message ?: "网络错误")
                    }
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
                    // Mark the cache stale so the upcoming load actually fires;
                    // otherwise `ensureLoaded` would short-circuit on success.
                    _state.update { it.copy(lastLoadedForPath = null) }
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
