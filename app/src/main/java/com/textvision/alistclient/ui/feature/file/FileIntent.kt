package com.textvision.alistclient.ui.feature.file

import android.net.Uri

sealed interface FileIntent {
    data class Load(val path: String) : FileIntent
    data class Search(val query: String) : FileIntent
    data class Upload(val uri: Uri) : FileIntent
    data class DownloadOne(val path: String) : FileIntent
    data class MultiSelectToggle(val path: String) : FileIntent
    /**
     * Replace the current selection wholesale. Used by "全选" to avoid the
     * O(N²) cost of emitting a [MultiSelectToggle] per item, which would
     * rebuild a `Set<String>` and re-emit `FileUiState` once per row.
     */
    data class MultiSelectSet(val paths: Set<String>) : FileIntent
    data object MultiSelectClear : FileIntent
    data class MultiSelectDelete(val paths: List<String>) : FileIntent
    data class MultiSelectDownload(val paths: List<String>) : FileIntent
}
