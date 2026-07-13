package com.textvision.alistclient.ui.feature.settings

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SettingsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val state = SettingsUiState(
        darkMode = DarkMode.LIGHT,
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
            SettingItem(key = "site_title", value = "我的云盘"),
            SettingItem(key = "announcement", value = "欢迎来到我的云端小屋 ✨"),
        ),
    )

    @Test
    fun prototypeSectionsRenderInOrderAndOnlyAnnouncementIsQuickSetting() {
        setContent()

        composeRule.onNodeWithText("设置").assertIsDisplayed()
        composeRule.onNodeWithText("管理你的小窝").assertIsDisplayed()
        composeRule.onNodeWithText("柚子 · admin").assertIsDisplayed()
        composeRule.onNodeWithText("外观主题").assertIsDisplayed()
        composeRule.onNodeWithText("浅色").assertIsDisplayed()
        composeRule.onNodeWithText("深色").assertIsDisplayed()
        composeRule.onNodeWithText("跟随").assertIsDisplayed()
        composeRule.onNodeWithText("存储源").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("/baidu · 已禁用").assertIsDisplayed()
        composeRule.onNodeWithText("站点公告").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("快速设置").assertIsDisplayed()
        composeRule.onAllNodesWithText("站点标题").assertCountEquals(0)
        composeRule.onNodeWithText("维护").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(6)
        composeRule.onNodeWithText("退出登录").assertIsDisplayed()
        composeRule.onNodeWithText("Alist Client · v1.0.0 · made with 💙").assertIsDisplayed()
    }

    @Test
    fun themeAndStorageActionsRemainConnected() {
        var selectedMode: DarkMode? = null
        var selectedStorage: Long? = null
        setContent(
            onDarkModeChange = { selectedMode = it },
            onStorageClick = { selectedStorage = it },
        )

        composeRule.onNodeWithText("深色").performClick()
        composeRule.onNodeWithText("夸克网盘").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(DarkMode.DARK, selectedMode)
            assertEquals(2L, selectedStorage)
        }
    }

    @Test
    fun capturePrototypeTopAndBottom() {
        setContent()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/settings_top.png")

        composeRule.onNode(hasScrollAction()).performScrollToIndex(6)
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/settings_bottom.png")
    }

    private fun setContent(
        onDarkModeChange: (DarkMode) -> Unit = {},
        onStorageClick: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            AlistTheme(darkMode = DarkMode.LIGHT) {
                Surface {
                    SettingsContent(
                        uiState = state,
                        onDarkModeChange = onDarkModeChange,
                        onStorageClick = onStorageClick,
                        onQuickSettingEdit = { _: SettingItem, _: String -> },
                        onClearPreviewFiles = {},
                        onAdvancedSettings = {},
                        onLogout = {},
                    )
                }
            }
        }
    }
}
