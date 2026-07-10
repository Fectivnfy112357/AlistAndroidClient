package com.textvision.alistclient.snapshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.EmptyState
import com.textvision.alistclient.ui.components.ErrorState
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.components.FileCategory
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentSnapshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Composable
    private fun Sample(darkTheme: Boolean = false, content: @Composable () -> Unit) {
        AlistTheme(darkMode = if (darkTheme) DarkMode.DARK else DarkMode.LIGHT) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(16.dp)) { content() }
            }
        }
    }

    @Test
    fun status_banners() {
        composeRule.setContent {
            Sample {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusBanner(kind = BannerKind.INFO, message = "这是一条信息提示")
                    StatusBanner(kind = BannerKind.WARNING, message = "这是一条警告提示")
                    StatusBanner(kind = BannerKind.ERROR, message = "这是一条错误提示", actionLabel = "重试", onAction = {})
                    StatusBanner(kind = BannerKind.SUCCESS, message = "操作成功完成")
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/status_banners.png")
    }

    @Test
    fun empty_state() {
        composeRule.setContent {
            Sample {
                EmptyState(
                    title = "文件夹为空",
                    icon = Icons.Outlined.Folder,
                    message = "这里没有文件",
                    actionLabel = "刷新",
                    onAction = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/empty_state.png")
    }

    @Test
    fun error_state() {
        composeRule.setContent {
            Sample { ErrorState(message = "网络连接失败", onRetry = {}) }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/error_state.png")
    }

    @Test
    fun loading_state() {
        composeRule.setContent {
            Sample { LoadingState(message = "加载中…") }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/loading_state.png")
    }

    @Test
    fun action_buttons() {
        composeRule.setContent {
            Sample {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Filled", onClick = {}, variant = ButtonVariant.FILLED)
                    ActionButton("Tonal", onClick = {}, variant = ButtonVariant.TONAL)
                    ActionButton("Outlined", onClick = {}, variant = ButtonVariant.OUTLINED)
                    ActionButton("Text", onClick = {}, variant = ButtonVariant.TEXT)
                    ActionButton("Disabled", onClick = {}, enabled = false)
                    ActionButton("Loading", onClick = {}, loading = true)
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/action_buttons.png")
    }

    @Test
    fun list_item_rows() {
        composeRule.setContent {
            Sample {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ListItemRow(
                        leading = { FileTypeIcon(FileCategory.FOLDER) },
                        title = "Documents",
                        subtitle = "文件夹",
                    )
                    ListItemRow(
                        leading = { FileTypeIcon(FileCategory.IMAGE) },
                        title = "photo.jpg",
                        subtitle = "1.2 MB",
                    )
                    ListItemRow(
                        leading = { FileTypeIcon(FileCategory.PDF) },
                        title = "report.pdf",
                        subtitle = "820 KB",
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/list_item_rows.png")
    }

    @Test
    fun file_type_icons() {
        composeRule.setContent {
            Sample {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileCategory.entries.forEach { category ->
                        FileTypeIcon(category)
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/file_type_icons.png")
    }
}
