package com.textvision.alistclient.ui.feature.file

import androidx.compose.runtime.Immutable
import com.textvision.alistclient.file.model.FileItem

@Immutable
data class FileUiState(
    val path: String = "/",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val selection: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
) {
    val visibleFiles: List<FileItem>
        get() = if (query.isBlank()) files
        else files.filter { it.name.contains(query, ignoreCase = true) }

    val isSelectionEmpty: Boolean
        get() = selection.isEmpty()

    val isAllSelected: Boolean
        get() = files.isNotEmpty() && selection.size == files.size
}
