package com.textvision.alistclient.ui.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.feature.music.MusicSettingsSection
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.theme.InkMute

// Settings is a vertically scrolling group of solid cards. Its cards retain
// their fill, rounded shape, and hairline border without moving drop shadows.
private val SettingsCardShadowElevation = 0.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    musicRoot: String,
    musicCacheSize: Long,
    onStorageClick: (Long) -> Unit,
    onQuickSettingEdit: (SettingItem, String) -> Unit,
    onClearPreviewFiles: () -> Unit,
    onAdvancedSettings: () -> Unit,
    onLogout: () -> Unit,
    onMusicRootChange: (String) -> Unit,
    onClearMusicCache: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleStorages = uiState.storages.take(3)
    val announcement = uiState.quickSettings.firstOrNull { it.key == "announcement" }
    // P3: stabilise all parent callbacks. Settings list is short but every
    // section uses these (3 storage rows + 1 quick-setting row + 4
    // maintenance rows), so any unstable lambda forced the whole list
    // subtree to recompose on every parent tick.
    val onStorageClickState by rememberUpdatedState(onStorageClick)
    val onQuickSettingEditState by rememberUpdatedState(onQuickSettingEdit)
    // Settings is also a finite card stack. Removing only its edge stretch
    // avoids an elastic rebound consuming a subsequent reverse drag.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        AppScaffold(
            modifier = modifier,
            transparentBase = true,
            background = {},
        ) { _ ->
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
                        shadowElevation = SettingsCardShadowElevation,
                    ) {
                        SettingsUserCard()
                    }
                }

                if (visibleStorages.isNotEmpty()) {
                    item(key = "storages", contentType = "section") {
                        SettingsSection(title = "存储源") {
                            Column {
                                visibleStorages.forEachIndexed { index, storage ->
                                    val id = storage.id ?: return@forEachIndexed
                                    if (index > 0) Divider()
                                    // P3: per-row stable click. Each iteration
                                    // rebuilt the lambda before; with this fix
                                    // only mount/unmount of a row recreates it.
                                    val onClick = remember(id) { { onStorageClickState(id) } }
                                    StorageSourceRow(
                                        title = storage.remark?.takeIf(String::isNotBlank)
                                            ?: storage.mountPath.substringAfterLast('/').ifBlank { storage.mountPath },
                                        subtitle = if (storage.disabled || storage.status == "disabled") {
                                            "${storage.mountPath} · 已禁用"
                                        } else {
                                            "${storage.mountPath} · ${storage.driver}"
                                        },
                                        driver = storage.driver,
                                        onClick = onClick,
                                    )
                                }
                            }
                        }
                    }
                }

                announcement?.let {
                    item(key = "quick", contentType = "section") {
                        SettingsSection(title = "快速设置") {
                            Column {
                                // P3: same trick for the quick-setting row —
                                // captures [announcement] by identity, so the
                                // remember key keeps it referentially stable.
                                val onEdit = remember(announcement) {
                                    { value: String -> onQuickSettingEditState(announcement, value) }
                                }
                                QuickSettingRow(
                                    item = announcement,
                                    onEdit = onEdit,
                                )
                                Divider()
                                PrivacyPasswordRow()
                            }
                        }
                    }
                }

                item(key = "music", contentType = "section") {
                    MusicSettingsSection(
                        currentRoot = musicRoot,
                        cacheSizeBytes = musicCacheSize,
                        onRootChange = onMusicRootChange,
                        onClearCache = onClearMusicCache,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
            shadowElevation = SettingsCardShadowElevation,
            content = content,
        )
    }
}
