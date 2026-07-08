package com.textvision.alistclient.ui.feature.file

sealed interface FileIntent {
    data class Load(val path: String) : FileIntent
    data class Search(val query: String) : FileIntent
    data class MultiSelectToggle(val path: String) : FileIntent
    data object MultiSelectClear : FileIntent
    data class MultiSelectDelete(val paths: List<String>) : FileIntent
    data class MultiSelectDownload(val paths: List<String>) : FileIntent
}
