package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val message = remember { mutableStateOf<String?>(null) }
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "设置", subtitle = "账号与本机缓存")
        CloudCard {
            CloudListItem(
                title = "当前服务器",
                subtitle = "已登录的 Alist 服务",
                leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "清理临时预览文件",
                subtitle = "释放本机预览缓存",
                onClick = {
                    val count = viewModel.clearPreviewFiles()
                    message.value = "已清理 $count 个临时文件"
                },
                leading = { Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = CloudPrimary) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
            CloudListItem(
                title = "退出登录",
                subtitle = "清除当前会话并返回登录页",
                onClick = {
                    viewModel.logout()
                    message.value = "已退出登录"
                },
                leading = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = CloudErrorText) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudStatusBanner(
            text = "多账号、管理员、外部网盘管理不在 MVP 范围内",
            kind = CloudBannerKind.Info,
        )
        message.value?.let {
            Spacer(Modifier.height(10.dp))
            CloudStatusBanner(text = it, kind = CloudBannerKind.Info)
        }
    }
}