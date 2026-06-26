package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.theme.FolderIconTint
import com.textvision.alistclient.ui.theme.FolderTint
import com.textvision.alistclient.ui.theme.GenericFileIconTint
import com.textvision.alistclient.ui.theme.GenericFileTint
import com.textvision.alistclient.ui.theme.ImageIconTint
import com.textvision.alistclient.ui.theme.ImageTint
import com.textvision.alistclient.ui.theme.TextIconTint
import com.textvision.alistclient.ui.theme.TextTint
import com.textvision.alistclient.ui.theme.CloudShapes

@Composable
fun FileTypeIcon(
    type: FileType,
    modifier: Modifier = Modifier,
    tiled: Boolean = true,
) {
    val style = when (type) {
        FileType.Folder -> FileIconStyle(Icons.Default.Folder, FolderTint, FolderIconTint)
        FileType.Image -> FileIconStyle(Icons.Default.Image, ImageTint, ImageIconTint)
        FileType.Text -> FileIconStyle(Icons.Default.Article, TextTint, TextIconTint)
        else -> FileIconStyle(Icons.Default.InsertDriveFile, GenericFileTint, GenericFileIconTint)
    }
    if (tiled) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(CloudShapes.IconTile)
                .background(style.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(style.icon, contentDescription = null, tint = style.content)
        }
    } else {
        Icon(style.icon, contentDescription = null, tint = style.content, modifier = modifier)
    }
}

private data class FileIconStyle(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)