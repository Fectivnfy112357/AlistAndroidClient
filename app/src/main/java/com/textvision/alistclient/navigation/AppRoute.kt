package com.textvision.alistclient.navigation

import com.textvision.alistclient.file.model.FileType
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PreviewArgs(
    val name: String,
    val path: String,
    val type: FileType,
    val downloadUrl: String?,
    val size: Long,
)

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Files : AppRoute("files")
    data object Transfers : AppRoute("transfers")
    data object Settings : AppRoute("settings")
    data object MoveCopyPicker : AppRoute("copy_move_picker")
    data object Preview : AppRoute("preview/{payload}") {
        fun create(name: String, path: String, type: FileType, downloadUrl: String?, size: Long): String {
            val raw = listOf(
                encode(name),
                encode(path),
                type.name,
                encode(downloadUrl.orEmpty()),
                size.toString(),
            ).joinToString("|")
            return "preview/${encode(raw)}"
        }

        fun decode(payload: String): PreviewArgs {
            val raw = decodeValue(payload)
            val parts = raw.split("|", limit = 5)
            return PreviewArgs(
                name = decodeValue(parts.getOrElse(0) { "" }),
                path = decodeValue(parts.getOrElse(1) { "" }),
                type = runCatching { FileType.valueOf(parts.getOrElse(2) { FileType.Other.name }) }.getOrDefault(FileType.Other),
                downloadUrl = decodeValue(parts.getOrElse(3) { "" }).takeIf { it.isNotBlank() },
                size = parts.getOrElse(4) { "0" }.toLongOrNull() ?: 0L,
            )
        }

        private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        private fun decodeValue(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
