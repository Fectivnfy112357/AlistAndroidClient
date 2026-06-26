package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    Column(Modifier.padding(16.dp)) {
        Text("设置")
        Button(onClick = { viewModel.logout(); message.value = "已退出登录" }) { Text("退出登录") }
        Button(onClick = { val count = viewModel.clearPreviewFiles(); message.value = "已清理 $count 个临时文件" }) { Text("清理临时预览文件") }
        Text("多账号、管理员、外部网盘管理不在 MVP 范围内")
        message.value?.let { Text(it) }
    }
}
