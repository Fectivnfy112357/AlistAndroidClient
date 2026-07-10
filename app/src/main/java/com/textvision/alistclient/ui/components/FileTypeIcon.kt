package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons

enum class FileCategory { FOLDER, IMAGE, VIDEO, AUDIO, TEXT, CODE, ARCHIVE, PDF, DOCUMENT, OTHER }

fun fileCategoryFromMime(mime: String?, name: String? = null): FileCategory {
    if (mime == null && name == null) return FileCategory.OTHER

    val m = mime?.lowercase().orEmpty()
    val n = name?.lowercase().orEmpty()

    // Directory / folder
    if (n.isNotEmpty() && !m.startsWith("text") && n.endsWith("/")) return FileCategory.FOLDER

    // Mime-based
    when {
        m.startsWith("image/") -> return FileCategory.IMAGE
        m.startsWith("video/") -> return FileCategory.VIDEO
        m.startsWith("audio/") -> return FileCategory.AUDIO
        m == "application/pdf" -> return FileCategory.PDF
        m.startsWith("text/") || m == "application/json" || m == "application/xml" -> return FileCategory.TEXT
        m.startsWith("application/") -> when {
            "zip" in m || "tar" in m || "gz" in m || "7z" in m || "rar" in m -> return FileCategory.ARCHIVE
            "javascript" in m || "typescript" in m || "x-sh" in m -> return FileCategory.CODE
            "msword" in m || "wordprocessing" in m || "spreadsheet" in m || "presentation" in m || "officedocument" in m -> return FileCategory.DOCUMENT
        }
    }

    // Extension fallback
    val ext = n.substringAfterLast('.', "")
    return when (ext) {
        // image
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic" -> FileCategory.IMAGE
        // video
        "mp4", "mkv", "mov", "avi", "webm", "flv", "wmv" -> FileCategory.VIDEO
        // audio
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus" -> FileCategory.AUDIO
        // pdf
        "pdf" -> FileCategory.PDF
        // code
        "kt", "java", "py", "js", "ts", "jsx", "tsx", "rs", "go", "c", "cpp", "h", "hpp", "cs", "rb", "php", "sh", "bash", "zsh", "json", "yaml", "yml", "toml", "xml", "html", "css", "scss", "vue", "swift" -> FileCategory.CODE
        // archive
        "zip", "tar", "gz", "tgz", "bz2", "7z", "rar", "xz" -> FileCategory.ARCHIVE
        // document
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "pages", "key", "numbers" -> FileCategory.DOCUMENT
        // text
        "txt", "md", "log", "csv", "ini", "conf" -> FileCategory.TEXT
        else -> FileCategory.OTHER
    }
}

fun com.textvision.alistclient.file.model.FileType.toFileCategory(): FileCategory = when (this) {
    com.textvision.alistclient.file.model.FileType.Folder -> FileCategory.FOLDER
    com.textvision.alistclient.file.model.FileType.Image -> FileCategory.IMAGE
    com.textvision.alistclient.file.model.FileType.Video -> FileCategory.VIDEO
    com.textvision.alistclient.file.model.FileType.Audio -> FileCategory.AUDIO
    com.textvision.alistclient.file.model.FileType.Text -> FileCategory.TEXT
    com.textvision.alistclient.file.model.FileType.Pdf -> FileCategory.PDF
    com.textvision.alistclient.file.model.FileType.Archive -> FileCategory.ARCHIVE
    com.textvision.alistclient.file.model.FileType.Other -> FileCategory.OTHER
}

@Composable
fun FileTypeIcon(
    category: FileCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val (icon, containerColor, contentColor) = when (category) {
        FileCategory.FOLDER -> Triple(AppIcons.folder, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        FileCategory.IMAGE -> Triple(AppIcons.image, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        FileCategory.VIDEO -> Triple(AppIcons.video, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        FileCategory.AUDIO -> Triple(AppIcons.audio, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        FileCategory.TEXT -> Triple(AppIcons.doc, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
        FileCategory.CODE -> Triple(AppIcons.doc, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
        FileCategory.ARCHIVE -> Triple(AppIcons.archive, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        FileCategory.PDF -> Triple(Icons.Outlined.PictureAsPdf, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        FileCategory.DOCUMENT -> Triple(AppIcons.doc, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        FileCategory.OTHER -> Triple(AppIcons.file, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
