package com.textvision.alistclient.file.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant

@Immutable
data class FileItem(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modifiedAt: Instant?,
    val extension: String?,
    val type: FileType,
    val thumbnailUrl: String?,
    val downloadUrl: String?,
)
