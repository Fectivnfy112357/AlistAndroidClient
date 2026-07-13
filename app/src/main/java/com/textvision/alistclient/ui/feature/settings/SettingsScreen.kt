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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ─── 1) 用户卡 ────────────────────────────────────────────────
            item {
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = PaddingValues(14.dp),
                ) {
                    UserAvatarCard(
                        name = "柚子",
                        role = "admin",
                        serverName = "我的云端小屋 · 在线",
                        status = "VIP",
                    )
                }
            }

            // ─── 2) 外观主题 ──────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SectionTitle("外观主题")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeCard(
                            label = "浅色",
                            icon = AppIcons.sun,
                            selected = uiState.darkMode == DarkMode.LIGHT,
                            swatchStops = LightSwatch,
                            onClick = { viewModel.setDarkMode(DarkMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeCard(
                            label = "深色",
                            icon = AppIcons.moon,
                            selected = uiState.darkMode == DarkMode.DARK,
                            swatchStops = DarkSwatch,
                            onClick = { viewModel.setDarkMode(DarkMode.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeCard(
                            label = "跟随",
                            icon = AppIcons.auto,
                            selected = uiState.darkMode == DarkMode.SYSTEM,
                            swatchStops = SystemSwatch,
                            onClick = { viewModel.setDarkMode(DarkMode.SYSTEM) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ─── 3) 存储源 ────────────────────────────────────────────────
            if (uiState.storages.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        SectionTitle("存储源")
                        SectionCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                        ) {
                            Column {
                                uiState.storages.take(3).forEachIndexed { index, storage ->
                                    val id = storage.id ?: return@forEachIndexed
                                    if (index > 0) Divider()
                                    StorageSourceRow(
                                        title = storage.remark?.takeIf { it.isNotBlank() }
                                            ?: storage.mountPath.substringAfterLast('/').ifBlank { storage.mountPath },
                                        subtitle = "${storage.mountPath} · ${storage.driver}",
                                        driver = storage.driver,
                                        onClick = { onStorageClick(id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── 4) 快速设置 ──────────────────────────────────────────────
            if (uiState.quickSettings.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        SectionTitle("快速设置")
                        SectionCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                        ) {
                            Column {
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
            }

            // ─── 5) 维护 ──────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SectionTitle("维护")
                    SectionCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                    ) {
                        Column {
                            CleanPreviewRow(onClick = { viewModel.clearPreviewFiles() })
                            Divider()
                            AdvancedSettingsRow(onClick = onAdvancedSettings)
                        }
                    }
                }
            }

            uiState.errorMessage?.let { msg ->
                item {
                    Spacer(Modifier.height(8.dp))
                    StatusBanner(message = msg, kind = com.textvision.alistclient.ui.components.BannerKind.ERROR)
                }
            }

            // ─── 6) 退出登录 + Footer ─────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                LogoutButton(onClick = viewModel::logout)
                Spacer(Modifier.height(8.dp))
            }
            item {
                Text(
                    text = "Alist Client · v1.0.0 · made with 💙",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}