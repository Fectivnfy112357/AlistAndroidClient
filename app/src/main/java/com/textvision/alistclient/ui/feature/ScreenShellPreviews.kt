package com.textvision.alistclient.ui.feature

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.EmptyState
import com.textvision.alistclient.ui.components.LoadingState
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.AlistClientTheme

// Screen-level composables depend on Hilt-injected ViewModels and cannot render
// in @Preview. These shells reconstruct each screen's empty/loading state from
// stateless building blocks so designers can inspect the scaffold + content.

@Preview(name = "Login shell", showBackground = true)
@Composable
private fun LoginScreenShellPreview() {
    AlistClientTheme(dynamicColor = false) {
        AppScaffold(topBar = { AppTopBar(title = "登录") }) { padding ->
            LoadingState(message = "连接服务器…", modifier = Modifier.padding(padding))
        }
    }
}

@Preview(name = "File shell (empty)", showBackground = true)
@Composable
private fun FileScreenShellPreview() {
    AlistClientTheme(dynamicColor = false) {
        AppScaffold(topBar = { AppTopBar(title = "文件", subtitle = "/") }) { padding ->
            EmptyState(
                title = "此目录为空",
                icon = Icons.Outlined.Inbox,
                message = "上传文件后会显示在这里",
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "Home shell (loading)", showBackground = true)
@Composable
private fun HomeScreenShellPreview() {
    AlistClientTheme(dynamicColor = false) {
        AppScaffold(topBar = { AppTopBar(title = "首页") }) { padding ->
            LoadingState(message = "加载仪表盘…", modifier = Modifier.padding(padding))
        }
    }
}

@Preview(
    name = "Settings shell Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsScreenShellPreview() {
    AlistClientTheme(darkTheme = true, dynamicColor = false) {
        AppScaffold(topBar = { AppTopBar(title = "设置") }) { padding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                LoadingState(message = "读取偏好…", modifier = Modifier.padding(padding))
            }
        }
    }
}

@Preview(name = "Transfer shell (empty)", showBackground = true)
@Composable
private fun TransferScreenShellPreview() {
    AlistClientTheme(dynamicColor = false) {
        AppScaffold(topBar = { AppTopBar(title = "传输") }) { padding ->
            EmptyState(
                title = "暂无任务",
                icon = Icons.Outlined.CloudUpload,
                message = "下载 / 上传记录会显示在这里",
                modifier = Modifier.padding(padding),
            )
        }
    }
}
