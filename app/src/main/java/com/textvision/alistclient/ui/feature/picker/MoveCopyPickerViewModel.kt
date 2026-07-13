package com.textvision.alistclient.ui.feature.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.FileRepositoryContract
import com.textvision.alistclient.file.model.FileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Move/Copy target picker — drives the directory browser used by the file
 * screen's "移动" action. Independent from `ui.feature.file.FileViewModel`
 * to keep picker concerns (single-path navigation + creation) isolated.
 */
@HiltViewModel
class MoveCopyPickerViewModel @Inject constructor(
    private val repository: FileRepositoryContract,
) : ViewModel() {

    data class State(
        val currentPath: String = "/",
        val directories: List<FileItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedTarget: String? = null,
        val isCreatingFolder: Boolean = false,
        val newFolderName: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { load("/") }

    fun load(path: String) {
        _state.update { it.copy(currentPath = path, isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.list(path)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        currentPath = path,
                        directories = result.data.filter { f -> f.isDir },
                        isLoading = false,
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

    fun selectTarget(path: String) {
        _state.update { it.copy(selectedTarget = path) }
    }

    fun toggleCreate() {
        _state.update { it.copy(isCreatingFolder = !it.isCreatingFolder, newFolderName = "") }
    }

    fun updateNewFolderName(name: String) {
        _state.update { it.copy(newFolderName = name) }
    }
}