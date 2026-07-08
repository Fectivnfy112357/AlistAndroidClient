package com.textvision.alistclient.snapshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.feature.file.FileListContent
import com.textvision.alistclient.ui.feature.file.FileUiState
import com.textvision.alistclient.ui.theme.AlistClientTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class FileScreenSnapshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun makeFile(
        name: String,
        path: String,
        isDir: Boolean,
        type: FileType,
        ext: String?,
        size: Long = if (isDir) 0L else 1024L,
    ): FileItem = FileItem(
        name = name,
        path = path,
        isDir = isDir,
        size = size,
        modifiedAt = null,
        extension = ext,
        type = type,
        thumbnailUrl = null,
        downloadUrl = null,
    )

    @Composable
    private fun FileListSample(
        state: FileUiState,
        darkTheme: Boolean = false,
    ) {
        AlistClientTheme(darkTheme = darkTheme, dynamicColor = false) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    FileListContent(
                        state = state,
                        onIntent = {},
                        onPreview = {},
                        onFolderNavigate = {},
                    )
                }
            }
        }
    }

    @Test
    fun file_screen_empty() {
        composeRule.setContent { FileListSample(FileUiState(path = "/")) }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/file_screen_empty.png")
    }

    @Test
    fun file_screen_success() {
        val files = listOf(
            makeFile("Documents", "/Documents", true, FileType.Folder, null),
            makeFile("photo.jpg", "/photo.jpg", false, FileType.Image, "jpg"),
            makeFile("report.pdf", "/report.pdf", false, FileType.Pdf, "pdf"),
            makeFile("notes.md", "/notes.md", false, FileType.Text, "md"),
        )
        composeRule.setContent { FileListSample(FileUiState(path = "/", files = files)) }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/file_screen_success.png")
    }

    @Test
    fun file_screen_dark() {
        val files = listOf(
            makeFile("Documents", "/Documents", true, FileType.Folder, null),
            makeFile("photo.jpg", "/photo.jpg", false, FileType.Image, "jpg"),
            makeFile("report.pdf", "/report.pdf", false, FileType.Pdf, "pdf"),
        )
        composeRule.setContent { FileListSample(FileUiState(path = "/", files = files), darkTheme = true) }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/file_screen_dark.png")
    }
}
