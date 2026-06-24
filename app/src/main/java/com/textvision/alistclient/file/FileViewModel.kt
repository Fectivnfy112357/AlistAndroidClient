package com.textvision.alistclient.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.error.ErrorMapper
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileSort
import com.textvision.alistclient.file.model.FileUiState
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
) : ViewModel() {
    constructor(repository: FileRepositoryContract, dispatcher: CoroutineDispatcher) : this(repository) {
        this.dispatcher = dispatcher
    }

    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var loadJob: Job? = null
    private var currentPath: String = "/"
    private var sort: FileSort = FileSort.NameAsc
    private val _uiState = MutableStateFlow<FileUiState>(FileUiState.Loading("/"))
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init { observeSearch() }

    fun load(path: String) {
        currentPath = path
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            _uiState.value = FileUiState.Loading(path)
            when (val result = repository.list(path)) {
                is ApiResult.Success -> _uiState.value = FileUiState.Success(path, applySort(result.data))
                is ApiResult.Failure -> _uiState.value = FileUiState.Error(path, ErrorMapper.mapAlistFailure(result.code, result.message))
                is ApiResult.NetworkError -> _uiState.value = FileUiState.Error(path, ErrorMapper.mapThrowable(result.cause))
            }
        }
    }

    fun refresh() = load(currentPath)

    fun updateSearchQuery(value: String) { _searchQuery.value = value }

    fun setSort(value: FileSort) {
        sort = value
        val current = _uiState.value
        if (current is FileUiState.Success) _uiState.value = current.copy(items = applySort(current.items))
    }

    private fun observeSearch() {
        _searchQuery.debounce(300).distinctUntilChanged().onEach { query ->
            if (query.isBlank()) load(currentPath) else search(query.trim())
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
