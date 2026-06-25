package com.textvision.alistclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.textvision.alistclient.file.model.FileItem

@Composable
fun DirectoryBrowser(
    path: String,
    directories: List<FileItem>,
    onOpen: (String) -> Unit,
    onSelectCurrent: (String) -> Unit,
) {
    Column {
        BreadcrumbBar(path, onOpen)
        Button(onClick = { onSelectCurrent(path) }) { Text("选择当前目录") }
        LazyColumn {
            items(directories, key = { it.path }) { dir ->
                ListItem(
                    headlineContent = { Text(dir.name) },
                    leadingContent = { FileTypeIcon(dir.type) },
                    modifier = Modifier.clickable { onOpen(dir.path) }
                )
            }
        }
    }
}
