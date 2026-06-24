package com.textvision.alistclient.network.dto

import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.inferFileType
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlistFsList(
    @SerialName("content") val content: List<AlistFileDto> = emptyList(),
    val total: Int = 0,
    @SerialName("readme") val readme: String? = null,
    val header: String? = null,
)

@Serializable
data class AlistFileDto(
    val name: String,
    val size: Long = 0,
    @SerialName("is_dir") val isDir: Boolean = false,
    val modified: String? = null,
    val created: String? = null,
    val sign: String? = null,
    val thumb: String? = null,
    @SerialName("type") val fileType: Int? = null,
)

@Serializable
data class FsListRequest(val path: String, val page: Int = 1, @SerialName("per_page") val perPage: Int = 0)

@Serializable
data class FsSearchRequest(val path: String, val keywords: String, val page: Int = 1, @SerialName("per_page") val perPage: Int = 50)

@Serializable
data class MkdirRequest(val path: String)

@Serializable
data class RenameRequest(val path: String, val name: String)

@Serializable
data class RemoveRequest(val dir: String, val names: List<String>)

fun AlistFileDto.toFileItem(parentPath: String, baseUrl: String): FileItem {
    val normalizedParent = parentPath.trimEnd('/')
    val fullPath = if (normalizedParent.isEmpty()) "/$name" else "$normalizedParent/$name"
    val directory = isDir || fileType == 0
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it.isNotBlank() && it != name.lowercase() }
    val cleanBase = baseUrl.trimEnd('/')
    return FileItem(
        name = name,
        path = fullPath,
        isDir = directory,
        size = size,
        modifiedAt = modified?.let { runCatching { Instant.parse(it) }.getOrNull() },
        extension = extension,
        type = inferFileType(extension, directory),
        thumbnailUrl = thumb?.let { "$cleanBase/p/$fullPath?sign=$it" },
        downloadUrl = sign?.let { "$cleanBase/d/$fullPath?sign=$it" },
    )
}
