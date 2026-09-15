package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    // P3: stabilise parent callbacks; `trailing` and `onClick` lambdas per row
    // were reconstructed on every recomposition. With ~tens of directories
    // and frequent state ticks (path loading), the row closures were a
    // measurable share of the picker recomposition budget.
    val onOpenState by rememberUpdatedState(onOpen)
    val onSelectCurrentState by rememberUpdatedState(onSelectCurrent)
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = path,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        TextButton(
            onClick = { onSelectCurrentState(path) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text("选择当前目录")
        }
        if (directories.isEmpty()) {
            EmptyState(title = "没有可选子目录", message = "可以直接选择当前目录")
        } else {
            LazyColumn {
                items(directories, key = { it.path }) { dir ->
                    val onClick = remember(dir.path) { { onOpenState(dir.path) } }
                    ListItemRow(
                        leading = { FileTypeIcon(dir.type.toFileCategory()) },
                        title = dir.name,
                        subtitle = "文件夹",
                        onClick = onClick,
                        // Trailing is a no-capture composable lambda so its
                        // recreation cost is negligible; the meaningful win
                        // here is the per-row stable onClick above.
                        trailing = { Text("›") },
                    )
                }
            }
        }
    }
}
