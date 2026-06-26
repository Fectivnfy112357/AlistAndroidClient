package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem

@Composable
fun DirectoryBrowser(
    path: String,
    directories: List<FileItem>,
    onOpen: (String) -> Unit,
    onSelectCurrent: (String) -> Unit,
) {
    CloudScaffold {
        CloudTopBar(title = "选择目标目录", subtitle = path)
        CloudCard {
            TextButton(onClick = { onSelectCurrent(path) }) { Text("选择当前目录") }
            if (directories.isEmpty()) {
                CloudEmptyState(title = "没有可选子目录", message = "可以直接选择当前目录")
            } else {
                Spacer(Modifier.height(4.dp))
                LazyColumn {
                    items(directories, key = { it.path }) { dir ->
                        CloudListItem(
                            title = dir.name,
                            subtitle = "文件夹",
                            onClick = { onOpen(dir.path) },
                            leading = { FileTypeIcon(dir.type) },
                            trailing = { Text("›") },
                        )
                    }
                }
            }
        }
    }
}
