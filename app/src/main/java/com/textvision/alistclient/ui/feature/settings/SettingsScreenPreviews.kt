package com.textvision.alistclient.ui.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.UserAvatarCard
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "SettingsScreen Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsScreenLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AppTopBar(title = "设置", subtitle = "管理你的小窝")
                SettingsPreviewBody()
            }
        }
    }
}

@Preview(
    name = "SettingsScreen Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsScreenDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AppTopBar(title = "设置", subtitle = "管理你的小窝")
                SettingsPreviewBody()
            }
        }
    }
}

@Preview(name = "SettingsScreen Large Font", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SettingsScreenLargeFontPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AppTopBar(title = "设置", subtitle = "管理你的小窝")
                SettingsPreviewBody()
            }
        }
    }
}

@Composable
private fun SettingsPreviewBody() {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                UserAvatarCard(
                    name = "晓源",
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
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ThemeCard("浅色", AppIcons.sun, selected = true, onClick = {}, modifier = Modifier.weight(1f))
                        ThemeCard("深色", AppIcons.moon, selected = false, onClick = {}, modifier = Modifier.weight(1f))
                        ThemeCard("自动", AppIcons.auto, selected = false, onClick = {}, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionTitle("存储源")
                    StorageSourceRow("本地存储", "Local", "Local", onClick = {})
                    Divider()
                    StorageSourceRow("阿里云盘", "AliyunDrive", "AliyunDrive", onClick = {})
                    Divider()
                    StorageSourceRow("Google Drive", "GoogleDrive", "GoogleDrive", onClick = {})
                }
            }
        }
        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionTitle("快速设置")
                    ListItemRow(
                        leading = {
                            Icon(AppIcons.sparkle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        title = "站点公告",
                        subtitle = "欢迎使用 Alist",
                        trailing = {
                            Icon(
                                AppIcons.chevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {},
                    )
                    Divider()
                    PrivacyPasswordRow()
                }
            }
        }
        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionTitle("维护")
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
                        trailing = {
                            Icon(
                                AppIcons.chevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {},
                    )
                    Divider()
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
                        trailing = {
                            Icon(
                                AppIcons.chevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {},
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            LogoutButton(onClick = {})
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
