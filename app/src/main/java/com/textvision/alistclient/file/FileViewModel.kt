package com.textvision.alistclient.file

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.error.ErrorMapper
import com.textvision.alistclient.common.network.NetworkMonitor
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileSort
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

interface FileRepositoryContract {
    suspend fun list(path: String): ApiResult<List<FileItem>>
    suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>>
}

@OptIn(FlowPreview::class)
@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepositoryContract,
    private val transferManager: TransferManager,
    networkMonitor: NetworkMonitorContract,
) : ViewModel() {
    constructor(
        repository: FileRepositoryContract,
        transferManager: TransferManager,
        dispatcher: CoroutineDispatcher,
    ) : this(repository, transferManager, StubNetworkMonitor()) {
        this.dispatcher = dispatcher
    }

    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var loadJob: Job? = null
    private var currentPath: String = "/"
    private var hasLoadedInitialContent = false
    private var sort: FileSort = FileSort.NameAsc
    private val _uiState = MutableStateFlow<FileUiState>(FileUiState.Loading("/"))
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    init { observeSearch() }

    fun load(path: String) {
        currentPath = path
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            if (_uiState.value !is FileUiState.Success) {
                _uiState.value = FileUiState.Loading(path)
            }
            when (val result = repository.list(path)) {
                is ApiResult.Success -> {
                    hasLoadedInitialContent = true
                    _uiState.value = FileUiState.Success(path, applySort(result.data))
                }
                is ApiResult.Failure -> _uiState.value = FileUiState.Error(path, ErrorMapper.mapAlistFailure(result.code, result.message))
                is ApiResult.NetworkError -> _uiState.value = FileUiState.Error(path, ErrorMapper.mapThrowable(result.cause))
            }
        }
    }

    fun loadIfNeeded(path: String) {
        if (hasLoadedInitialContent && currentPath == path) return
        val current = _uiState.value
        if (current is FileUiState.Success && current.path == path) {
            hasLoadedInitialContent = true
            return
        }
        load(path)
    }

    fun refresh() = load(currentPath)

    fun updateSearchQuery(value: String) {
        if (value == _searchQuery.value) return
        _searchQuery.value = value
    }

    fun setSort(value: FileSort) {
        sort = value
        val current = _uiState.value
        if (current is FileUiState.Success) _uiState.value = current.copy(items = applySort(current.items))
    }

    /**
     * Enqueue a download for the given file item. The current path is the
     * parent directory of [item] in the UI; the file is only a row reference
     * and the manager computes its own remote path.
     */
    fun enqueueDownload(item: FileItem) {
        if (item.isDir) return
        transferManager.enqueueDownload(item.path, item.name)
    }

    /**
     * Enqueue an upload of the picked [uri] into the current directory.
     * The manager is responsible for resolving the display name from the URI.
     */
    fun enqueueUpload(uri: Uri) {
        transferManager.enqueueUpload(uri, currentPath)
    }

    private fun observeSearch() {
        _searchQuery.debounce(300).distinctUntilChanged().onEach { query ->
            if (query.isBlank()) return@onEach
            search(query.trim())
        }.launchIn(viewModelScope)
    }

    private fun search(query: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.search(currentPath, query)) {
                is ApiResult.Success -> _uiState.value = FileUiState.Success(currentPath, applySort(result.data))
                is ApiResult.Failure -> {
                    val current = (_uiState.value as? FileUiState.Success)?.items.orEmpty()
                    _uiState.value = FileUiState.Success(currentPath, current.filter { it.name.contains(query, ignoreCase = true) }, isCurrentDirectoryFilter = true)
                }
                is ApiResult.NetworkError -> {
                    val current = (_uiState.value as? FileUiState.Success)?.items.orEmpty()
                    _uiState.value = FileUiState.Success(currentPath, current.filter { it.name.contains(query, ignoreCase = true) }, isCurrentDirectoryFilter = true)
                }
            }
        }
    }

    private fun applySort(items: List<FileItem>): List<FileItem> = when (sort) {
        FileSort.NameAsc -> items.sortedWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name.lowercase() })
        FileSort.SizeDesc -> items.sortedWith(compareByDescending<FileItem> { it.isDir }.thenByDescending { it.size })
        FileSort.ModifiedDesc -> items.sortedWith(compareByDescending<FileItem> { it.isDir }.thenByDescending { it.modifiedAt })
    }
}

private class StubNetworkMonitor : NetworkMonitorContract {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}
