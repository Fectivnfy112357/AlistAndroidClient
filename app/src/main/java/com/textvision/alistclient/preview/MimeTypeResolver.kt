package com.textvision.alistclient.preview

import android.webkit.MimeTypeMap

object MimeTypeResolver {
    fun infer(name: String): String {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (ext.isBlank()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
