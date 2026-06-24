package com.textvision.alistclient.file.model

import com.textvision.alistclient.common.error.AppError

sealed interface FileUiState {
    data class Loading(val path: String) : FileUiState
    data class Success(
        val path: String,
        val items: List<FileItem>,
        val selectedItems: Set<String> = emptySet(),
        val isMultiSelectMode: Boolean = false,
        val isCurrentDirectoryFilter: Boolean = false,
    ) : FileUiState
    data class Error(val path: String, val error: AppError) : FileUiState
}