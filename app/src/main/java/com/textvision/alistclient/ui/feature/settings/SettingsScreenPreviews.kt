package com.textvision.alistclient.ui.feature.settings

import android.content.res.Configuration
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.StorageInfo
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

@Composable
private fun SettingsPreviewBody() {
    val storages = listOf(
        StorageInfo(id = 1, mountPath = "/aliyun", driver = "AliyunDrive", order = 0, status = "work"),
        StorageInfo(id = 2, mountPath = "/quark", driver = "Quark", order = 1, status = "work"),
        StorageInfo(id = 3, mountPath = "/baidu", driver = "BaiduNetdisk", order = 2, status = "disabled"),
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                SectionTitle("外观主题")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeCard("浅色", AppIcons.sun, selected = true, onClick = {}, swatchStops = LightSwatch, modifier = Modifier.weight(1f))
                    ThemeCard("深色", AppIcons.moon, selected = false, onClick = {}, swatchStops = DarkSwatch, modifier = Modifier.weight(1f))
                    ThemeCard("跟随", AppIcons.auto, selected = false, onClick = {}, swatchStops = SystemSwatch, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                SectionTitle("存储源")
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                ) {
                    Column {
                        StorageSourceRow("阿里云盘", "/aliyun · AliyunDrive", "AliyunDrive", onClick = {})
                        Divider()
                        StorageSourceRow("夸克网盘", "/quark · Quark", "Quark", onClick = {})
                        Divider()
                        StorageSourceRow("百度网盘", "/baidu · 已禁用", "BaiduNetdisk", onClick = {})
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                SectionTitle("快速设置")
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                ) {
                    Column {
                        QuickSettingRow(
                            item = com.textvision.alistclient.network.dto.SettingItem(
                                key = "announcement", value = "欢迎来到我的云端小屋 ✨", type = "string",
                                options = null, help = "", group = 1,
                            ),
                            onEdit = {},
                        )
                        Divider()
                        PrivacyPasswordRow()
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                SectionTitle("维护")
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = PaddingValues(vertical = 6.dp, horizontal = 14.dp),
                ) {
                    Column {
                        CleanPreviewRow(onClick = {})
                        Divider()
                        AdvancedSettingsRow(onClick = {})
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            LogoutButton(onClick = {})
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