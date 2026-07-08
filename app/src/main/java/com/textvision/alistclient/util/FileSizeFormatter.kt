package com.textvision.alistclient.util

import java.util.Locale

object FileSizeFormatter {
    fun humanize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024)
        else -> String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024 / 1024)
    }
}
