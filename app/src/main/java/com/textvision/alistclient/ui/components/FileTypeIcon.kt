package com.textvision.alistclient.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.textvision.alistclient.file.model.FileType

@Composable
fun FileTypeIcon(type: FileType) {
    val icon = when (type) {
        FileType.Folder -> Icons.Default.Folder
        FileType.Image -> Icons.Default.Image
        FileType.Text -> Icons.Default.Article
        else -> Icons.Default.InsertDriveFile
    }
    Icon(icon, contentDescription = null)
}
