package com.textvision.alistclient.file.model

enum class FileType { Folder, Image, Text, Audio, Video, Pdf, Archive, Other }

fun inferFileType(extension: String?, isDir: Boolean): FileType {
    if (isDir) return FileType.Folder
    return when (extension?.lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> FileType.Image
        "txt", "md", "json", "xml", "csv", "log", "kt", "java", "js", "ts", "html", "css" -> FileType.Text
        "mp3", "wav", "flac", "aac", "ogg" -> FileType.Audio
        "mp4", "mkv", "webm", "mov", "avi" -> FileType.Video
        "pdf" -> FileType.Pdf
        "zip", "rar", "7z", "tar", "gz" -> FileType.Archive
        else -> FileType.Other
    }
}