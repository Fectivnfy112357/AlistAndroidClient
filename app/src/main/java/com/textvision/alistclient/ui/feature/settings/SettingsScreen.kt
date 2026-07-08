package com.textvision.alistclient.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.form.localizedLabel
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.DarkMode

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    onStorageClick: (Long) -> Unit = {},
    onAdvancedSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(Unit) { viewModel.loadAdminData() }

    AppScaffold(
        topBar = { AppTopBar(title = "设置", subtitle = "账号与服务器管理") },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader("主题") }
            item {
                DarkModeSection(
                    current = uiState.darkMode,
                    onSelect = viewModel::setDarkMode,
                )
            }

            if (uiState.storages.isNotEmpty()) {
                item { SectionHeader("存储") }
                items(uiState.storages, key = { it.id ?: 0L }) { storage ->
                    val id = storage.id ?: return@items
                    ListItemRow(
                        leading = {
                            Icon(
                                Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = storage.remark?.takeIf { it.isNotBlank() } ?: storage.mountPath,
                        subtitle = buildString {
                            append(storage.driver)
                            if (storage.disabled) append(" · 已禁用")
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { onStorageClick(id) },
                    )
                }
            }

            if (uiState.quickSettings.isNotEmpty()) {
                item { SectionHeader("快速设置") }
                items(uiState.quickSettings, key = { it.key }) { item ->
                    QuickSettingRow(
                        item = item,
                        onEdit = { viewModel.saveQuickSetting(item.key, it) },
                    )
                }
            }

            item { SectionHeader("维护") }
            item {
                ListItemRow(
                    leading = {
                        Icon(
                            Icons.Outlined.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = "清理临时预览文件",
                    subtitle = "释放本机预览缓存",
                    onClick = { viewModel.clearPreviewFiles() },
                )
            }
            item {
                ListItemRow(
                    leading = {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = "完整设置",
                    subtitle = "所有带表单的设置项",
                    onClick = onAdvancedSettings,
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
            item {
                ActionButton(
                    text = "退出登录",
                    onClick = viewModel::logout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    variant = ButtonVariant.OUTLINED,
                    leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            uiState.errorMessage?.let { msg ->
                item {
                    StatusBanner(
                        kind = BannerKind.ERROR,
                        message = msg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun DarkModeSection(
    current: DarkMode,
    onSelect: (DarkMode) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        DarkMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Brightness6,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(16.dp))
                RadioButton(selected = current == mode, onClick = { onSelect(mode) })
                Spacer(Modifier.width(8.dp))
                Text(
                    text = darkModeLabel(mode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun darkModeLabel(mode: DarkMode): String = when (mode) {
    DarkMode.SYSTEM -> "跟随系统"
    DarkMode.LIGHT -> "浅色"
    DarkMode.DARK -> "深色"
}

@Composable
private fun QuickSettingRow(
    item: SettingItem,
    onEdit: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    ListItemRow(
        leading = {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = displayLabel(item.key),
        subtitle = item.value?.takeIf { it.isNotBlank() } ?: "(未设置)",
        onClick = { editing = true },
    )
    if (editing) {
        var text by remember { mutableStateOf(item.value.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(displayLabel(item.key)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (item.key == "announcement") 3 else 1,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(text)
                    editing = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("取消") }
            },
        )
    }
}

private fun displayLabel(key: String): String = localizedLabel(key)
