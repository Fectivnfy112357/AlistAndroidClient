package com.textvision.alistclient.ui.feature.storage

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.admin.storage.StorageEditUiState
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageEditContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun prototypeStorageEditorRendersItsCompleteStructure() {
        setContent()

        composeRule.onNodeWithText("编辑存储").assertIsDisplayed()
        composeRule.onAllNodesWithText("夸克网盘").assertCountEquals(2)
        composeRule.onNodeWithText("挂载路径 · /quark").assertIsDisplayed()
        composeRule.onNodeWithText("驱动参数").assertIsDisplayed()
        composeRule.onNodeWithText("备注").assertIsDisplayed()
        composeRule.onNodeWithText("Cookie").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("重新获取").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("启用存储").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("保存修改").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("保存成功后将自动返回").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fieldAndSaveCallbacksRemainConnected() {
        var updatedField: Pair<String, String>? = null
        var saved = false
        setContent(
            onFieldValueChange = { name, value -> updatedField = name to value },
            onSave = { saved = true },
        )

        composeRule.onNodeWithText("我的夸克网盘").performClick().performTextInput(" 2")
        composeRule.onNodeWithText("保存修改").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals("remark" to "我的夸克网盘 2", updatedField)
            assertEquals(true, saved)
        }
    }

    @Test
    fun capturePrototypeStorageEditor() {
        setContent()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/storage_edit_top.png")
    }

    private fun setContent(
        onFieldValueChange: (String, String) -> Unit = { _, _ -> },
        onSave: () -> Unit = {},
    ) {
        val state = StorageEditUiState.Form(
            storage = StorageInfo(id = 1, mountPath = "/quark", driver = "Quark", remark = "我的夸克网盘"),
            driver = null,
            formItems = listOf(
                FormItem.Text("remark", "备注"),
                FormItem.Text("mount_path", "挂载路径"),
                FormItem.Text("cookie", "Cookie"),
                FormItem.Text("root_folder_path", "根目录路径"),
                FormItem.Select("sort", "排序方式", listOf("modified_desc" to "按修改时间倒序")),
            ),
            fieldValues = mapOf(
                "remark" to "我的夸克网盘",
                "mount_path" to "/quark",
                "cookie" to "ready",
                "root_folder_path" to "/我的资源/",
                "sort" to "modified_desc",
            ),
            enabled = true,
        )
        composeRule.setContent {
            AlistTheme(darkMode = DarkMode.LIGHT) {
                Surface {
                    StorageEditContent(
                        form = state,
                        onBack = {},
                        onFieldValueChange = onFieldValueChange,
                        onEnabledChange = {},
                        onSave = onSave,
                    )
                }
            }
        }
    }
}
