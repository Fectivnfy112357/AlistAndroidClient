package com.textvision.alistclient.ui.feature.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.FileCategory
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600

@Composable
fun MoveCopyTargetPickerScreen(
    onTargetSelected: (String) -> Unit,
    op: String = "move",
    initialPath: String = "/",
    count: Int = 1,
    viewModel: FileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialPath) { viewModel.load(initialPath) }

    var currentPath by rememberSaveable { mutableStateOf(initialPath) }
    var selectedTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateFolder by rememberSaveable { mutableStateOf(false) }
    var newFolderName by rememberSaveable { mutableStateOf("") }

    val success = state as? FileUiState.Success
    val directories = success?.items.orEmpty().filter { it.isDir }
    val subtitle = when (op) {
        "copy" -> "复制 $count 项到…"
        else -> "移动 $count 项到…"
    }

    LaunchedEffect(success?.path) {
        if (success != null) currentPath = success.path
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
                path = currentPath,
                onNavigateRoot = { viewModel.load("/"); currentPath = "/" },
                onNavigateSegment = { i ->
                    val parts = currentPath.trim('/').split('/').filter { it.isNotBlank() }
                    if (i < 0) currentPath = "/"
                    else currentPath = "/" + parts.take(i + 1).joinToString("/")
                    viewModel.load(currentPath)
                },
                onCreate = { showCreateFolder = !showCreateFolder },
            )

            if (showCreateFolder) {
                NewFolderCard(
                    name = newFolderName,
                    onNameChange = { newFolderName = it },
                    onConfirm = {
                        if (newFolderName.isNotBlank()) {
                            showCreateFolder = false
                            newFolderName = ""
                        }
                    },
                )
            }

            Text(
                text = "可移动到的位置",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    item(key = "__current__") {
                        FolderRow(
                            name = "当前目录",
                            subtitle = "$count 项 · 当前目录",
                            selected = true,
                            showCurrentStyle = true,
                            onClick = { onTargetSelected(currentPath) },
                        )
                    }
                    items(directories, key = { it.path }) { dir ->
                        FolderRow(
                            name = dir.name,
                            subtitle = "文件夹",
                            selected = selectedTarget == dir.path,
                            showCurrentStyle = false,
                            onClick = {
                                currentPath = dir.path
                                viewModel.load(dir.path)
                                selectedTarget = dir.path
                            },
                        )
                    }
                }
            }

            BottomConfirmBar(
                targetName = if (selectedTarget != null) {
                    currentPath.trim('/').split('/').lastOrNull() ?: "当前目录"
                } else "当前目录",
                enabled = true,
                onConfirm = {
                    val target = selectedTarget ?: currentPath
                    onTargetSelected(target)
                },
            )
        }
    }
}

@Composable
private fun BreadcrumbRow(
    path: String,
    onNavigateRoot: () -> Unit,
    onNavigateSegment: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    val parts = path.trim('/').split('/').filter { it.isNotBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(icon = AppIcons.home, label = "根目录", onClick = onNavigateRoot)
        parts.forEachIndexed { idx, seg ->
            Separator()
            Chip(
                icon = AppIcons.folder,
                label = seg,
                onClick = { onNavigateSegment(idx) },
            )
        }
        Spacer(Modifier.width(4.dp))
        Separator()
        PillButton(label = "+ 新建", onClick = onCreate)
    }
}

@Composable
private fun Chip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
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

@Composable
private fun NewFolderCard(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val brandShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(brandShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, brandShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("新建文件夹", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onConfirm) {
            Text("✓ 创建", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    showCurrentStyle: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (showCurrentStyle) MaterialTheme.colorScheme.primaryContainer
             else if (selected) MaterialTheme.colorScheme.primaryContainer
             else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileTypeIcon(FileCategory.FOLDER, size = 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showCurrentStyle) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            RadioCircle(selected = selected)
        }
    }
}

@Composable
private fun RadioCircle(selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun BottomConfirmBar(targetName: String, enabled: Boolean, onConfirm: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            ActionButton(
                text = "确认移动到 · $targetName",
                onClick = onConfirm,
                enabled = enabled,
                variant = ButtonVariant.FILLED,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
