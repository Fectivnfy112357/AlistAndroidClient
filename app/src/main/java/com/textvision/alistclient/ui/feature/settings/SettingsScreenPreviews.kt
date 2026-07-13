package com.textvision.alistclient.ui.feature.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "SettingsScreen Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsScreenLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        SettingsPreviewBody(DarkMode.LIGHT)
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
        SettingsPreviewBody(DarkMode.DARK)
    }
}

@Composable
private fun SettingsPreviewBody(darkMode: DarkMode) {
    SettingsContent(
        uiState = SettingsUiState(
            darkMode = darkMode,
            storages = listOf(
                StorageInfo(id = 1, mountPath = "/aliyun", driver = "Aliyundrive", remark = "阿里云盘"),
                StorageInfo(id = 2, mountPath = "/quark", driver = "Quark", remark = "夸克网盘"),
                StorageInfo(
                    id = 3,
                    mountPath = "/baidu",
                    driver = "BaiduNetdisk",
                    remark = "百度网盘",
                    disabled = true,
                ),
            ),
            quickSettings = listOf(
                SettingItem(key = "announcement", value = "欢迎来到我的云端小屋 ✨"),
            ),
        ),
        onDarkModeChange = {},
        onStorageClick = {},
        onQuickSettingEdit = { _, _ -> },
        onClearPreviewFiles = {},
        onAdvancedSettings = {},
        onLogout = {},
    )
}
