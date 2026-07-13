package com.textvision.alistclient.ui.feature.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.Ink
import com.textvision.alistclient.ui.theme.InkSoft
import com.textvision.alistclient.util.FileSizeFormatter

/**
 * Target directory picker (prototype Screen09 — 1:1 clone of [img_9.png]).
 *
 * Layout:
 *  - Sticky AppTopBar: "选择目标" + "移动 N 项到…" subtitle (green online dot)
 *  - Breadcrumb row of chips: 根目录 / 段 / 段 / [ + 新建 ]
 *  - (Optional) new-folder creation strip
 *  - "可移动到的位置" label
 *  - LazyColumn of folder rows:
 *      - Current directory row (brand-300 wash + ✓ on right, primary color)
 *      - Folder rows with 38dp folder icon + name + meta + radio circle
 *  - Bottom confirm bar: 48dp brand-gradient "确认移动到 · <name>"
 */
@Composable
fun MoveCopyTargetPickerScreen(
    onTargetSelected: (String) -> Unit,
    op: String = "move",
    initialPath: String = "/",
    count: Int = 1,
    viewModel: MoveCopyPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subtitle = when (op) {
        "copy" -> "复制 $count 项到…"
        else -> "移动 $count 项到…"
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "选择目标",
                subtitle = subtitle,
                onBack = { onTargetSelected("") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BreadcrumbRow(
                path = state.currentPath,
                onNavigateRoot = { viewModel.load("/") },
                onNavigateSegment = { i ->
                    val parts = state.currentPath.trim('/').split('/').filter { it.isNotBlank() }
                    val newPath = if (i < 0) "/" else "/" + parts.take(i + 1).joinToString("/")
                    viewModel.load(newPath)
                },
                onCreate = { viewModel.toggleCreate() },
                isCreating = state.isCreatingFolder,
            )

            if (state.isCreatingFolder) {
                NewFolderStrip(
                    name = state.newFolderName,
                    onNameChange = viewModel::updateNewFolderName,
                    onConfirm = {
                        // Prototype: dismiss strip; actual mkdir would call repository here.
                        viewModel.toggleCreate()
                    },
                )
            }

            Text(
                text = "可移动到的位置",
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    item(key = "__current__") {
                        CurrentDirectoryRow(
                            count = count,
                            selected = state.selectedTarget == null,
                            onClick = {
                                viewModel.selectTarget(state.currentPath)
                                onTargetSelected(state.currentPath)
                            },
                        )
                    }
                    items(state.directories, key = { it.path }) { dir ->
                        FolderRowPrototype(
                            dir = dir,
                            selected = state.selectedTarget == dir.path,
                            onClick = {
                                viewModel.load(dir.path)
                                viewModel.selectTarget(dir.path)
                            },
                        )
                    }
                }
            }

            BottomConfirmBar(
                targetName = displayNameFor(state.currentPath, state.selectedTarget),
                onConfirm = {
                    val target = state.selectedTarget ?: state.currentPath
                    onTargetSelected(target)
                },
            )
        }
    }
}

private fun displayNameFor(currentPath: String, selected: String?): String =
    if (selected != null) {
        selected.trim('/').split('/').lastOrNull()?.takeIf { it.isNotBlank() } ?: "当前目录"
    } else {
        currentPath.trim('/').split('/').lastOrNull()?.takeIf { it.isNotBlank() } ?: "当前目录"
    }

// ─────────────────────────────────────────────────────────────────────────
//  Breadcrumb chips
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun BreadcrumbRow(
    path: String,
    onNavigateRoot: () -> Unit,
    onNavigateSegment: (Int) -> Unit,
    onCreate: () -> Unit,
    isCreating: Boolean,
) {
    val parts = path.trim('/').split('/').filter { it.isNotBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChipPill(icon = AppIcons.home, label = "根目录", onClick = onNavigateRoot)
        parts.forEachIndexed { idx, seg ->
            Separator()
            ChipPill(icon = AppIcons.folder, label = seg, onClick = { onNavigateSegment(idx) })
        }
        Spacer(Modifier.width(4.dp))
        Separator()
        ChipPill(
            icon = if (isCreating) Icons.Outlined.Check else Icons.Outlined.Add,
            label = if (isCreating) "收起" else "+ 新建",
            onClick = onCreate,
        )
    }
}

@Composable
private fun ChipPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun Separator() {
    Text(
        text = " / ",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ─────────────────────────────────────────────────────────────────────────
//  New-folder creation strip
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun NewFolderStrip(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(shape)
            .background(Brand300)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) {
                            Text(
                                "新建文件夹",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Brand500, Brand600)))
                .clickable(onClick = onConfirm)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("创建", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Folder rows
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun CurrentDirectoryRow(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brand300)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Brand300, Brand500.copy(alpha = 0.4f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("当前目录", style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(
                "$count 项 · 当前目录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FolderRowPrototype(
    dir: FileItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Brand300 else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Brand300, MaterialTheme.colorScheme.primaryContainer))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dir.name, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(
                subtitleFor(dir),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioCircle(selected = selected)
    }
}

private fun subtitleFor(dir: FileItem): String {
    // Prototype §9.2 placeholder meta — falls back gracefully when sizes are unknown.
    return when {
        dir.size > 0L -> "${dir.size} 项 · ${FileSizeFormatter.humanize(dir.size)}"
        else -> "文件夹"
    }
}

@Composable
private fun RadioCircle(selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Bottom confirm bar
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomConfirmBar(
    targetName: String,
    onConfirm: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            ActionButton(
                text = "确认移动到 · $targetName",
                onClick = onConfirm,
                enabled = true,
                variant = ButtonVariant.FILLED,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )
        }
    }
}