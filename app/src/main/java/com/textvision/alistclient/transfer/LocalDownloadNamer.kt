package com.textvision.alistclient.transfer

import android.os.Environment
import java.security.MessageDigest

object LocalDownloadNamer {
    const val AppDownloadFolder = "alist"
    val publicDownloadsRelativePath: String = "${Environment.DIRECTORY_DOWNLOADS}/$AppDownloadFolder"

    fun fileNameFor(remotePath: String): String {
        val extension = remotePath.substringAfterLast('.', missingDelimiterValue = "").takeIf { it.isNotBlank() && !it.contains('/') }
        val digest = MessageDigest.getInstance("SHA-1").digest(remotePath.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return if (extension == null) digest else "$digest.$extension"
    }
}
