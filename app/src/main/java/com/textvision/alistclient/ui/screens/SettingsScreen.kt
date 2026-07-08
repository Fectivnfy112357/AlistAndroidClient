package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.storage.StorageRowItem
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    onStorageClick: (Long) -> Unit = {},
    onAdvancedSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message = remember { mutableStateOf<String?>(null) }
    var editingSetting by remember { mutableStateOf<SettingItem?>(null) }

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(Unit) { viewModel.loadAdminData() }

    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "设置", subtitle = "账号与服务器管理")
        CloudCard {
            CloudListItem(
                title = "当前服务器",
                subtitle = "已登录的 Alist 服务",
                leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "清理临时预览文件",
                subtitle = "释放本机预览缓存",
                onClick = {
                    val count = viewModel.clearPreviewFiles()
                    message.value = "已清理 $count 个临时文件"
                },
                leading = { Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = CloudPrimary) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
            CloudListItem(
                title = "退出登录",
                subtitle = "清除当前会话并返回登录页",
                onClick = {
                    viewModel.logout()
                    message.value = "已退出登录"
                },
                leading = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = CloudErrorText) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "存储",
                subtitle = "${uiState.storages.size} 个存储",
                leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary) },
            )
            uiState.storages.forEach { s ->
                StorageRowItem(
                    storage = s,
                    onClick = { s.id?.let(onStorageClick) },
                    onToggle = { checked -> s.id?.let { viewModel.toggleStorage(it, checked) } },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "站点设置",
                subtitle = "${uiState.quickSettings.size} 个常用项",
                leading = { Icon(Icons.Outlined.Language, contentDescription = null, tint = CloudPrimary) },
            )
            uiState.quickSettings.forEach { item ->
                CloudListItem(
                    title = displayLabel(item.key),
                    subtitle = item.value?.takeIf { it.isNotBlank() } ?: "(未设置)",
                    onClick = { editingSetting = item },
                    trailing = { Text("›", color = CloudTextSecondary) },
                )
            }
            CloudListItem(
                title = "完整设置",
                subtitle = "所有带表单的设置项",
                onClick = onAdvancedSettings,
                leading = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = CloudPrimary) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        uiState.errorMessage?.let {
            CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
        }
        message.value?.let {
            CloudStatusBanner(text = it, kind = CloudBannerKind.Info)
        }
    }

    editingSetting?.let { item ->
        QuickSettingDialog(
            item = item,
            onDismiss = { editingSetting = null },
            onSave = { newValue ->
                viewModel.saveQuickSetting(item.key, newValue)
                editingSetting = null
            },
        )
    }
}

@Composable
private fun QuickSettingDialog(
    item: SettingItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(item.value.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(displayLabel(item.key)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (item.key == "announcement") 3 else 1,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun displayLabel(key: String): String = when (key) {
    "site_title" -> "站点标题"
    "logo" -> "Logo URL"
    "login_background" -> "登录页背景图"
    "announcement" -> "登录页公告"
    else -> key
}