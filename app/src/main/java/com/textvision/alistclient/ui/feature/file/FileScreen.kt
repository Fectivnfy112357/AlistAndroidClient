package com.textvision.alistclient.ui.feature.file

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.LocalSnackbarHostState
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.navigation.MoveCopyPickerDest
import com.textvision.alistclient.ui.components.AppAlertDialog
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.SearchField
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.StateWarnFg
import kotlinx.coroutines.launch

/**
 * File browser — prototype Screen03 (1:1 clone of [img_2.png]).
 *
 * Top bar (prototype):
 *   - 32dp round back button on translucent white surface
 *   - title "文件" + subtitle row with mint/offline dot
 *   - ghost refresh + brand-gradient upload icon buttons
 *
 * Offline banner: pale yellow pill with offline icon (prototype §3.2).
 */
@Composable
fun FileScreen(
    initialPath: String = "/",
    onPreview: (FileItem) -> Unit = {},
    onFolderNavigate: (path: String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onMoveSelected: (paths: List<String>, destPath: String) -> Unit = { _, _ -> },
    vm: FileViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            vm.onIntent(FileIntent.Upload(uri))
            scope.launch { snackbar.showSnackbar("已加入传输队列") }
        }
    }

    // Cache-aware resume: only fetch when the ViewModel doesn't already hold
    // a successful listing for this path. Loading on every resume (the previous
    // behaviour) was the main per-tab cost in the tab-cycle hotpath and forced
    // redundant rebinds on bottom-tab returns. Explicit refresh + delete +
    // retry all bypass this gate via FileIntent.Load.
    LifecycleResumeEffect(initialPath) {
        vm.ensureLoaded(initialPath)
        onPauseOrDispose { }
    }

    AppScaffold(
        transparentBase = true,
        background = {},
        topBar = {
            AppTopBar(
                title = "文件",
                subtitle = if (state.isOnline) state.path else "当前离线 · 部分操作不可用",
                subtitleIsOffline = !state.isOnline,
                onBack = onBack,
                actions = {
                    FileTopBarAction(
                        icon = AppIcons.refresh,
                        contentDescription = "刷新",
                        brand = false,
                        onClick = { vm.onIntent(FileIntent.Load(state.path)) },
                    )
                    Spacer(Modifier.size(6.dp))
                    FileTopBarAction(
                        icon = AppIcons.upload,
                        contentDescription = "上传",
                        brand = true,
                        onClick = { uploadLauncher.launch("*/*") },
                    )
                },
            )
        },
        bottomBar = {
            if (state.isMultiSelectMode) {
                FileMultiSelectBar(
                    selectionCount = state.selection.size,
                    onSelectAll = {
                        state.visibleFiles.map { it.path }
                            .filter { it !in state.selection }
                            .forEach { vm.onIntent(FileIntent.MultiSelectToggle(it)) }
                    },
                    onMove = { onMoveSelected(state.selection.toList(), state.path) },
                    onDownload = { vm.onIntent(FileIntent.MultiSelectDownload(state.selection.toList())) },
                    onDelete = { showDeleteConfirm = true },
                    onClear = { vm.onIntent(FileIntent.MultiSelectClear) },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            state.error?.let { msg ->
                StatusBanner(
                    kind = BannerKind.ERROR,
                    message = msg,
                    actionLabel = "重试",
                    onAction = { vm.onIntent(FileIntent.Load(state.path)) },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!state.isOnline) {
                OfflineBanner(message = "离线模式：仅可查看本地缓存")
                Spacer(Modifier.height(8.dp))
            }
            SearchField(
                query = state.query,
                onQueryChange = { vm.onIntent(FileIntent.Search(it)) },
            )
            Spacer(Modifier.height(8.dp))
            if (state.isMultiSelectMode) {
                FileMultiSelectHint(selectionCount = state.selection.size)
                Spacer(Modifier.height(8.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                FileListContent(
                    state = state,
                    onIntent = vm::onIntent,
                    onPreview = onPreview,
                    onFolderNavigate = onFolderNavigate,
                    onShare = { file ->
                        val link = file.downloadUrl
                        if (link.isNullOrBlank()) {
                            scope.launch { snackbar.showSnackbar("该文件无直链") }
                        } else {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, link)
                            }
                            context.startActivity(Intent.createChooser(send, "分享"))
                        }
                    },
                    onCopyLink = { file ->
                        val link = file.downloadUrl
                        if (link.isNullOrBlank()) {
                            scope.launch { snackbar.showSnackbar("该文件无直链") }
                        } else {
                            clipboard.setText(AnnotatedString(link))
                            scope.launch { snackbar.showSnackbar("已复制直链") }
                        }
                    },
                    onDownloadFeedback = {
                        scope.launch { snackbar.showSnackbar("已加入下载队列") }
                    },
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AppAlertDialog(
            title = "删除确认",
            message = "确定删除选中的 ${state.selection.size} 项吗？此操作不可恢复。",
            confirmLabel = "删除",
            onConfirm = {
                vm.onIntent(FileIntent.MultiSelectDelete(state.selection.toList()))
                showDeleteConfirm = false
            },
            dismissLabel = "取消",
            onDismiss = { showDeleteConfirm = false },
            destructive = true,
        )
    }
}

/** Prototype §3.2: pale-yellow pill banner with offline icon. */
@Composable
private fun OfflineBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(com.textvision.alistclient.ui.theme.StateWarnBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.offline,
            contentDescription = null,
            tint = StateWarnFg,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = StateWarnFg,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Prototype top-bar action — translucent white circle, optional brand-gradient fill. */
@Composable
internal fun FileTopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    brand: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .let { base ->
                if (brand) {
                    base.background(Brush.linearGradient(listOf(Brand500, Brand600)))
                } else {
                    base.background(com.textvision.alistclient.ui.theme.Surface.copy(alpha = 0.7f))
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (brand) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
    }
}
