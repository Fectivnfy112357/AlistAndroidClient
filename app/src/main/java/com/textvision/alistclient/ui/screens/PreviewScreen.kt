package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PreviewScreen(filePath: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    val file = File(filePath)
    val text = produceState(initialValue = "加载中", filePath) {
        value = runCatching { file.readText() }.getOrElse { "无法读取文件" }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (file.length() > 2L * 1024L * 1024L) {
            Text("文件过大，是否下载或用其他应用打开？")
            Button(onClick = onDownload) { Text("下载") }
            Button(onClick = onExternalOpen) { Text("外部打开") }
        } else {
            Text(text.value, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}