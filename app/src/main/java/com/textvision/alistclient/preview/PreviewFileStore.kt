package com.textvision.alistclient.preview

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun previewDir(): File = File(context.cacheDir, "preview").also { it.mkdirs() }

    fun clearPreviewFiles(): Int {
        val files = previewDir().listFiles().orEmpty()
        files.forEach { it.deleteRecursively() }
        return files.size
    }
}
