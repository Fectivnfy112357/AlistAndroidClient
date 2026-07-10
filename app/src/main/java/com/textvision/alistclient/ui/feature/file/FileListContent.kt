package com.textvision.alistclient.ui.feature.file

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.components.EmptyState
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.components.toFileCategory
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.util.FileSizeFormatter

@Composable
fun FileListContent(
    modifier: Modifier = Modifier,
    state: FileUiState,
    onIntent: (FileIntent) -> Unit,
    onPreview: (FileItem) -> Unit,
    onFolderNavigate: (path: String) -> Unit,
    onShare: (FileItem) -> Unit = {},
    onCopyLink: (FileItem) -> Unit = {},
    onDownloadFeedback: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val files = state.visibleFiles
    if (state.isLoading && state.files.isEmpty()) {
        LoadingState(message = "加载中…", modifier = modifier)
        return
    }
    if (files.isEmpty() && !state.isLoading) {
        EmptyState(
            title = "文件夹为空",
            message = state.error ?: "这里没有文件",
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(files, key = { it.path }) { file ->
            val selected = file.path in state.selection
            val rowBackground = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
            ListItemRow(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(rowBackground)
                    .animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
                    ),
                leading = {
                    if (state.isMultiSelectMode) {
                        SelectCheckCircle(
                            selected = selected,
                            onClick = { onIntent(FileIntent.MultiSelectToggle(file.path)) },
                        )
                    } else {
                        FileTypeIcon(file.type.toFileCategory())
                    }
                },
                title = file.name,
                subtitle = if (file.isDir) "文件夹" else FileSizeFormatter.humanize(file.size),
                onClick = {
                    when {
                        state.isMultiSelectMode -> onIntent(FileIntent.MultiSelectToggle(file.path))
                        file.isDir -> onFolderNavigate(file.path)
                        else -> onPreview(file)
                    }
                },
                onLongClick = { onIntent(FileIntent.MultiSelectToggle(file.path)) },
                trailing = if (state.isMultiSelectMode) {
                    {}
                } else if (!file.isDir) {
                    {
                        FileRowMenu(
                            file = file,
                            onIntent = onIntent,
                            onShare = onShare,
                            onCopyLink = onCopyLink,
                            onDownloadFeedback = onDownloadFeedback,
                        )
                    }
                } else {
                    {}
                },
            )
        }
    }
}

/** 22×22 selection check circle used in multi-select mode. */
@Composable
private fun SelectCheckCircle(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(22.dp)) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FileRowMenu(
    file: FileItem,
    onIntent: (FileIntent) -> Unit,
    onShare: (FileItem) -> Unit,
    onCopyLink: (FileItem) -> Unit,
    onDownloadFeedback: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("下载") },
            onClick = {
                expanded = false
                onIntent(FileIntent.DownloadOne(file.path))
                onDownloadFeedback()
            },
        )
        DropdownMenuItem(
            text = { Text("分享") },
            onClick = {
                expanded = false
                onShare(file)
            },
        )
        DropdownMenuItem(
            text = { Text("复制直链") },
            onClick = {
                expanded = false
                onCopyLink(file)
            },
        )
    }
}

// ---------------------------------------------------------------------------
//  Previews
// ---------------------------------------------------------------------------

private val sampleFiles = listOf(
    FileItem(
        name = "Documents", path = "/Documents", isDir = true, size = 0L,
        modifiedAt = null, extension = null, type = FileType.Folder,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "photo.jpg", path = "/photo.jpg", isDir = false, size = 12_400_000L,
        modifiedAt = null, extension = "jpg", type = FileType.Image,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "report.pdf", path = "/report.pdf", isDir = false, size = 340_000L,
        modifiedAt = null, extension = "pdf", type = FileType.Pdf,
        thumbnailUrl = null, downloadUrl = null,
    ),
)

@Preview(name = "FileList Light")
@Composable
private fun FileListContentPreviewLight() {
    AlistTheme {
        Surface {
            FileListContent(
                state = FileUiState(path = "/", files = sampleFiles),
                onIntent = {},
                onPreview = {},
                onFolderNavigate = {},
            )
        }
    }
}

@Preview(name = "FileList Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FileListContentPreviewDark() {
    AlistTheme {
        Surface {
            FileListContent(
                state = FileUiState(
                    path = "/",
                    files = sampleFiles,
                    isMultiSelectMode = true,
                    selection = setOf("/photo.jpg"),
                ),
                onIntent = {},
                onPreview = {},
                onFolderNavigate = {},
            )
        }
    }
}

@Preview(name = "FileList LargeFont", fontScale = 1.6f)
@Composable
private fun FileListContentPreviewLargeFont() {
    AlistTheme {
        Surface {
            FileListContent(
                state = FileUiState(path = "/", files = sampleFiles),
                onIntent = {},
                onPreview = {},
                onFolderNavigate = {},
            )
        }
    }
}
