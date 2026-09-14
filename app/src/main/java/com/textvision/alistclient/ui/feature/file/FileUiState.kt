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
    val isOnline: Boolean = true,
    /**
     * The last path that successfully resolved from the server. Drives the
     * "re-load on resume" optimization: when [FileScreen]'s LifecycleResumeEffect
     * re-fires for the same path, the ViewModel skips the network round-trip
     * and renders the cached data immediately. Cleared on delete / refresh /
     * path change so the next Load is forced.
     */
    val lastLoadedForPath: String? = null,
) {
    /**
     * P0: was a `get()` that re-ran `filter` on every read. With a 1000-item
     * directory and two readers (`FileListContent` and the multi-select bar's
     * `isAllSelected`), each keystroke fired two full O(n) scans. Declaring it
     * as a `val` makes the data class compute it once per instance during
     * `copy` — readers now pay nothing.
     */
    val visibleFiles: List<FileItem> = if (query.isBlank()) files
        else files.filter { it.name.contains(query, ignoreCase = true) }

    val isSelectionEmpty: Boolean
        get() = selection.isEmpty()

    val isAllSelected: Boolean
        get() = visibleFiles.isNotEmpty() && visibleFiles.all { it.path in selection }
}
