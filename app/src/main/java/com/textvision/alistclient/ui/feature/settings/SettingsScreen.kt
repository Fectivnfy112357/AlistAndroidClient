package com.textvision.alistclient.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.components.UserAvatarCard
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
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
        topBar = {
            AppTopBar(title = "设置", subtitle = "管理你的小窝", onBack = null)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    UserAvatarCard(
                        name = "访客",
                        role = "VIP",
                        serverName = "我的云端小屋 · 在线",
                        status = "VIP",
                    )
                }
            }

            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("外观主题")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ThemeCard(
                                label = "浅色",
                                icon = AppIcons.sun,
                                selected = uiState.darkMode == DarkMode.LIGHT,
                                onClick = { viewModel.setDarkMode(DarkMode.LIGHT) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeCard(
                                label = "深色",
                                icon = AppIcons.moon,
                                selected = uiState.darkMode == DarkMode.DARK,
                                onClick = { viewModel.setDarkMode(DarkMode.DARK) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeCard(
                                label = "自动",
                                icon = AppIcons.auto,
                                selected = uiState.darkMode == DarkMode.SYSTEM,
                                onClick = { viewModel.setDarkMode(DarkMode.SYSTEM) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (uiState.storages.isNotEmpty()) {
                item {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            SectionTitle("存储源")
                            uiState.storages.take(3).forEachIndexed { index, storage ->
                                val id = storage.id ?: return@forEachIndexed
                                if (index > 0) Divider()
                                StorageSourceRow(
                                    title = storage.remark?.takeIf { it.isNotBlank() }
                                        ?: storage.mountPath.substringAfterLast('/').ifBlank { storage.mountPath },
                                    subtitle = storage.driver,
                                    driver = storage.driver,
                                    onClick = { onStorageClick(id) },
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.quickSettings.isNotEmpty()) {
                item {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            SectionTitle("快速设置")
                            uiState.quickSettings.take(2).forEachIndexed { index, setting ->
                                if (index > 0) Divider()
                                QuickSettingRow(
                                    item = setting,
                                    onEdit = { viewModel.saveQuickSetting(setting.key, it) },
                                )
                            }
                            Divider()
                            PrivacyPasswordRow()
                        }
                    }
                }
            }

            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionTitle("维护")
                        ListItemRow(
                            leading = {
                                Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            title = "清理临时预览文件",
                            subtitle = "释放本机预览缓存",
                            trailing = {
                                Icon(AppIcons.chevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = { viewModel.clearPreviewFiles() },
                        )
                        Divider()
                        ListItemRow(
                            leading = {
                                Icon(Icons.Outlined.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            title = "完整设置",
                            subtitle = "所有带表单的设置项",
                            trailing = {
                                Icon(AppIcons.chevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = onAdvancedSettings,
                        )
                    }
                }
            }

            uiState.errorMessage?.let { msg ->
                item {
                    StatusBanner(
                        kind = BannerKind.ERROR,
                        message = msg,
                        actionLabel = "重试",
                        onAction = { viewModel.loadAdminData() },
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                LogoutButton(onClick = viewModel::logout)
                Spacer(Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "Alist Client · v1.0.0 · made with 💙",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
