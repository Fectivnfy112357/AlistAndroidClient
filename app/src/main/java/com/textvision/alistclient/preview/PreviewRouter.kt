package com.textvision.alistclient.preview

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import java.io.File

sealed interface PreviewAction {
    data class InAppText(val file: File) : PreviewAction
    data class InAppImage(val uriString: String) : PreviewAction
    data class ExternalOpen(val intent: Intent) : PreviewAction
    data class TooLargeText(val file: File) : PreviewAction
}

object PreviewRouter {
    const val TEXT_PREVIEW_LIMIT_BYTES = 2L * 1024L * 1024L

    fun route(context: Context, item: FileItem, localFile: File): PreviewAction {
        return when (item.type) {
            FileType.Text -> if (localFile.length() <= TEXT_PREVIEW_LIMIT_BYTES) PreviewAction.InAppText(localFile) else PreviewAction.TooLargeText(localFile)
            FileType.Image -> PreviewAction.InAppImage(localFile.toURI().toString())
            else -> PreviewAction.ExternalOpen(openIntent(context, localFile, item.name))
        }
    }

    fun openIntent(context: Context, file: File, name: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MimeTypeResolver.infer(name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    const val LINK_SHARE_WARNING = "此链接需要登录 Alist 账号才能访问。如果服务器在内网，对方可能无法打开。"

    fun shareFileIntent(context: Context, file: File, name: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = MimeTypeResolver.infer(name)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun shareLinkIntent(link: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$link\n\n$LINK_SHARE_WARNING")
    }
}
