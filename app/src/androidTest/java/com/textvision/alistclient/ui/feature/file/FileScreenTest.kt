package com.textvision.alistclient.ui.feature.file

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performLongClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.theme.AlistTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_shows_empty_message() {
        composeRule.setContent {
            AlistTheme {
                Surface {
                    FileListContent(
                        state = FileUiState(),
                        onIntent = {},
                        onPreview = {},
                        onFolderNavigate = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("文件夹为空").assertIsDisplayed()
    }

    @Test
    fun long_press_enters_multi_select_mode() {
        val file = FileItem(
            name = "test.txt",
            path = "/test.txt",
            isDir = false,
            size = 1024L,
            modifiedAt = null,
            extension = "txt",
            type = FileType.Other,
            thumbnailUrl = null,
            downloadUrl = null,
        )
        var captured: FileIntent? = null
        composeRule.setContent {
            AlistTheme {
                Surface {
                    FileListContent(
                        state = FileUiState(path = "/", files = listOf(file)),
                        onIntent = { captured = it },
                        onPreview = {},
                        onFolderNavigate = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("test.txt").performLongClick()
        assertTrue("Expected MultiSelectToggle, got $captured", captured is FileIntent.MultiSelectToggle)
    }
}
