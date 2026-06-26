package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.preview.PreviewRouter
import java.io.File

@Composable
fun PreviewScreen(filePath: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    val file = File(filePath)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (file.length() > PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES) {
            Text("文件过大，是否下载或用其他应用打开？")
            Button(onClick = onDownload) { Text("下载") }
            Button(onClick = onExternalOpen) { Text("外部打开") }
        } else {
            // Read on first composition and when filePath changes; updates the visible text state.
            var text by remember(filePath) { mutableStateOf("加载中") }
            LaunchedEffect(filePath) {
                text = runCatching { file.readText() }.getOrElse { "无法读取文件" }
            }
            Text(text, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}
