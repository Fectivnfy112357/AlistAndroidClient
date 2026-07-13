package com.textvision.alistclient.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.InkMute

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onDarkModeChange: (DarkMode) -> Unit,
    onStorageClick: (Long) -> Unit,
    onQuickSettingEdit: (SettingItem, String) -> Unit,
    onClearPreviewFiles: () -> Unit,
    onAdvancedSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader()
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "profile", contentType = "card") {
                    SectionCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(14.dp),
                        solid = true,
                    ) {
                        SettingsUserCard()
                    }
                }

                item(key = "theme", contentType = "section") {
                    SettingsSection(title = "外观主题", cardPadding = PaddingValues(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeCard(
                                label = "浅色",
                                icon = AppIcons.sun,
                                selected = uiState.darkMode == DarkMode.LIGHT,
                                swatchStops = LightSwatch,
                                onClick = { onDarkModeChange(DarkMode.LIGHT) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeCard(
                                label = "深色",
                                icon = AppIcons.moon,
                                selected = uiState.darkMode == DarkMode.DARK,
                                swatchStops = DarkSwatch,
                                onClick = { onDarkModeChange(DarkMode.DARK) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeCard(
                                label = "跟随",
                                icon = AppIcons.auto,
                                selected = uiState.darkMode == DarkMode.SYSTEM,
                                swatchStops = SystemSwatch,
                                onClick = { onDarkModeChange(DarkMode.SYSTEM) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (uiState.storages.isNotEmpty()) {
                    item(key = "storages", contentType = "section") {
                        SettingsSection(title = "存储源") {
                            Column {
                                uiState.storages.take(3).forEachIndexed { index, storage ->
                                    val id = storage.id ?: return@forEachIndexed
                                    if (index > 0) Divider()
                                    StorageSourceRow(
                                        title = storage.remark?.takeIf(String::isNotBlank)
                                            ?: storage.mountPath.substringAfterLast('/').ifBlank { storage.mountPath },
                                        subtitle = if (storage.disabled || storage.status == "disabled") {
                                            "${storage.mountPath} · 已禁用"
                                        } else {
                                            "${storage.mountPath} · ${storage.driver}"
                                        },
                                        driver = storage.driver,
                                        onClick = { onStorageClick(id) },
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.quickSettings.firstOrNull { it.key == "announcement" }?.let { announcement ->
                    item(key = "quick", contentType = "section") {
                        SettingsSection(title = "快速设置") {
                            Column {
                                QuickSettingRow(
                                    item = announcement,
                                    onEdit = { onQuickSettingEdit(announcement, it) },
                                )
                                Divider()
                                PrivacyPasswordRow()
                            }
                        }
                    }
                }

                item(key = "maintenance", contentType = "section") {
                    SettingsSection(title = "维护") {
                        Column {
                            CleanPreviewRow(onClick = onClearPreviewFiles)
                            Divider()
                            AdvancedSettingsRow(onClick = onAdvancedSettings)
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    item(key = "error", contentType = "banner") {
                        StatusBanner(message = message, kind = BannerKind.ERROR)
                    }
                }

                item(key = "logout", contentType = "action") {
                    LogoutButton(onClick = onLogout)
                }

                item(key = "footer", contentType = "footer") {
                    Text(
                        text = "Alist Client · v1.0.0 · made with 💙",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMute,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    cardPadding: PaddingValues = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        SectionTitle(title)
        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            padding = cardPadding,
            solid = true,
            content = content,
        )
    }
}
