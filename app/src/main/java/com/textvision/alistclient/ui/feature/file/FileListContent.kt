package com.textvision.alistclient.ui.feature.file

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.components.EmptyState
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.CandyLilacBg
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLemonBg
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyMintBg
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.CandyPinkBg
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.InkSoft
import com.textvision.alistclient.util.DateFormatter
import com.textvision.alistclient.util.FileSizeFormatter

/**
 * File list — prototype §3 (1:1 clone of [img_2.png]).
 *
 * Each row: 38×38 candy-gradient icon box · name + size/date subtitle · trailing slot.
 * Selected variant uses brand-soft [Brand300] wash per prototype §3.4.
 */
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
            FileRowPrototype(
                // P2: drop the `MediumBouncy` placement animation — it applied
                // a spring spec to every row on every directory swap and was the
                // main per-row cost during `home-scroll`/`file-tab` reflows.
                // Cross-fading rows on enter/leave is still cheap and is
                // preserved via the default fade specs.
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
                    fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
                ),
                file = file,
                selected = selected,
                showCheck = state.isMultiSelectMode,
                onIntent = onIntent,
                onPreview = onPreview,
                onFolderNavigate = onFolderNavigate,
                onShare = onShare,
                onCopyLink = onCopyLink,
                onDownloadFeedback = onDownloadFeedback,
            )
        }
    }
}

@Composable
private fun FileRowPrototype(
    file: FileItem,
    selected: Boolean,
    showCheck: Boolean,
    onIntent: (FileIntent) -> Unit,
    onPreview: (FileItem) -> Unit,
    onFolderNavigate: (path: String) -> Unit,
    onShare: (FileItem) -> Unit,
    onCopyLink: (FileItem) -> Unit,
    onDownloadFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBg = if (selected) Brand300 else Color.Transparent
    // P2: cache subtitle per (size, modifiedAt, isDir). The previous version
    // recomputed the relative-date string on every recomposition — for a busy
    // directory refresh (selection toggle, scroll, online flip) that produced
    // hundreds of identical DateFormatter calls per frame.
    val subtitle = remember(file.size, file.modifiedAt, file.isDir) { buildSubtitle(file) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .clickable {
                when {
                    showCheck -> onIntent(FileIntent.MultiSelectToggle(file.path))
                    file.isDir -> onFolderNavigate(file.path)
                    else -> onPreview(file)
                }
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) {
            SelectCheckCircle(
                selected = selected,
                onClick = { onIntent(FileIntent.MultiSelectToggle(file.path)) },
            )
            Spacer(Modifier.width(10.dp))
        } else {
            FileIconBox(file.type)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                )
            }
        }
        if (!showCheck) {
            if (!file.isDir) {
                FileRowMenu(
                    file = file,
                    onIntent = onIntent,
                    onShare = onShare,
                    onCopyLink = onCopyLink,
                    onDownloadFeedback = onDownloadFeedback,
                )
            } else {
                Icon(
                    imageVector = AppIcons.chevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FileIconBox(type: FileType) {
    // P2: cache the gradient brush + foreground pair per file type. The two
    // have only 7 distinct values across the prototype palette, so memoising
    // keeps the row composition from re-allocating `Brush.linearGradient`
    // on every recomposition (selection flip, scroll, online state tick).
    val (brush, fg) = remember(type) { iconBoxAppearance(type) }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconForType(type),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Pure appearance table for [FileIconBox] — extracted so it can be `remember`-ed
 * once per file type instead of recomputed for every recomposing row.
 */
private fun iconBoxAppearance(type: FileType): Pair<Brush, Color> {
    val (grad, fg) = when (type) {
        FileType.Folder  -> listOf(Brand500, Brand500) to Color.White
        FileType.Image   -> listOf(CandyPinkBg, CandyPink) to Color(0xFFC46683)
        FileType.Video   -> listOf(CandyLilacBg, Color(0xFFDDD0FF)) to Color(0xFF7C5BC7)
        FileType.Audio   -> listOf(CandyMintBg, CandyMint) to Color(0xFF2D9B7C)
        FileType.Text    -> listOf(CandyLemonBg, CandyLemon) to Color(0xFF9C7A1F)
        FileType.Pdf     -> listOf(CandyLemonBg, CandyLemon) to Color(0xFF9C7A1F)
        FileType.Archive -> listOf(Color(0xFFE8EFF8), Color(0xFFD7E1F0)) to InkSoft
        FileType.Other   -> listOf(Color(0xFFEAF0F8), Color(0xFFD7E1F0)) to InkSoft
    }
    return Brush.linearGradient(grad) to fg
}

private fun iconForType(type: FileType): ImageVector = when (type) {
    FileType.Folder  -> AppIcons.folder
    FileType.Image   -> AppIcons.image
    FileType.Video   -> AppIcons.video
    FileType.Audio   -> AppIcons.audio
    FileType.Text    -> AppIcons.doc
    FileType.Pdf     -> AppIcons.doc
    FileType.Archive -> AppIcons.archive
    FileType.Other   -> AppIcons.file
}

/** Builds prototype §3 subtitle: "<size> · <relative date>" — e.g. "3.2 MB · 今天 14:21". */
private fun buildSubtitle(file: FileItem): String? {
    val parts = mutableListOf<String>()
    if (file.isDir) {
        parts += "文件夹"
    } else if (file.size > 0) {
        parts += FileSizeFormatter.humanize(file.size)
    }
    val dateText = file.modifiedAt?.let { DateFormatter.relativeDateTime(it) }
    if (dateText != null) parts += dateText
    return if (parts.isEmpty()) null else parts.joinToString(" · ")
}

/** 22×22 selection check circle used in multi-select mode. */
@Composable
private fun SelectCheckCircle(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .let { base ->
                if (selected) base.background(Brush.linearGradient(listOf(Brand500, Brand600)))
                else base.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
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
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable { expanded = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = "更多操作",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
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
        name = "旅行日记 · 京都樱花.jpg", path = "/travel", isDir = false, size = 3_300_000L,
        modifiedAt = null, extension = "jpg", type = FileType.Image,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "2024 春季合集", path = "/2024spring", isDir = true, size = 0L,
        modifiedAt = null, extension = null, type = FileType.Folder,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "vlog_04.mp4", path = "/vlog", isDir = false, size = 134_000_000L,
        modifiedAt = null, extension = "mp4", type = FileType.Video,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "攻略笔记.pdf", path = "/note", isDir = false, size = 1_800_000L,
        modifiedAt = null, extension = "pdf", type = FileType.Pdf,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "晚安白噪音.mp3", path = "/noise", isDir = false, size = 8_800_000L,
        modifiedAt = null, extension = "mp3", type = FileType.Audio,
        thumbnailUrl = null, downloadUrl = null,
    ),
    FileItem(
        name = "raw 照片.zip", path = "/raw", isDir = false, size = 268_000_000L,
        modifiedAt = null, extension = "zip", type = FileType.Archive,
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
                    selection = setOf("/travel", "/2024spring"),
                ),
                onIntent = {},
                onPreview = {},
                onFolderNavigate = {},
            )
        }
    }
}
