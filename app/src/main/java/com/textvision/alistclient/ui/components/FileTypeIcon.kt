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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Candy-style palette per [FileCategory]: a 135° background gradient and a deep icon tint.
 * Colors mirror `prototype/alist-android/index.html` `.file-row .ico.*` definitions.
 */
private data class CandyPalette(
    val gradient: Brush,
    val tint: Color,
)

@Composable
private fun candyPalette(category: FileCategory): CandyPalette = when (category) {
    FileCategory.FOLDER -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFF6FB6FF), Color(0xFF6FB6FF))),
        tint = Color(0xFFFFFFFF),
    )
    FileCategory.IMAGE -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFFFE4ED), Color(0xFFFFD1DD))),
        tint = Color(0xFFC46683),
    )
    FileCategory.VIDEO -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFECE2FF), Color(0xFFDDD0FF))),
        tint = Color(0xFF7C5BC7),
    )
    FileCategory.AUDIO -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFE0F4FF), Color(0xFFCDEEFE))),
        tint = Color(0xFF2D7AB8),
    )
    FileCategory.TEXT -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFFFF4CC), Color(0xFFFFE89B))),
        tint = Color(0xFF8B6A2A),
    )
    FileCategory.CODE -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFDAF6EC), Color(0xFFC6EBDC))),
        tint = Color(0xFF1B6E4F),
    )
    FileCategory.ARCHIVE -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFFFE89B), Color(0xFFFFD96B))),
        tint = Color(0xFF8B5E1A),
    )
    FileCategory.PDF -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFFFE5E8), Color(0xFFFFD2D8))),
        tint = Color(0xFFB8505C),
    )
    FileCategory.DOCUMENT -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFECE2FF), Color(0xFFDDD0FF))),
        tint = Color(0xFF5B3FB5),
    )
    FileCategory.OTHER -> CandyPalette(
        gradient = Brush.linearGradient(listOf(Color(0xFFF1F4FA), Color(0xFFE3E9F4))),
        tint = Color(0xFF6B8AB5),
    )
}

@Composable
fun FileTypeIcon(
    category: FileCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val palette = candyPalette(category)
    val icon = when (category) {
        FileCategory.FOLDER -> AppIcons.folder
        FileCategory.IMAGE -> AppIcons.image
        FileCategory.VIDEO -> AppIcons.video
        FileCategory.AUDIO -> AppIcons.audio
        FileCategory.TEXT -> AppIcons.doc
        FileCategory.CODE -> AppIcons.doc
        FileCategory.ARCHIVE -> AppIcons.archive
        FileCategory.PDF -> Icons.Outlined.PictureAsPdf
        FileCategory.DOCUMENT -> AppIcons.doc
        FileCategory.OTHER -> AppIcons.file
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(palette.gradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
