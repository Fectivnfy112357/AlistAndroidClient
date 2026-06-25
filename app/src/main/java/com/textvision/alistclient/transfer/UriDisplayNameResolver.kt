package com.textvision.alistclient.transfer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

object UriDisplayNameResolver {
    fun resolve(contentResolver: ContentResolver, uri: Uri, nowMillis: Long): String {
        val queried = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
        return queried ?: fallbackName(uri, nowMillis)
    }

    fun fallbackName(uri: Uri, nowMillis: Long): String = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "upload-$nowMillis"
}
