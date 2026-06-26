package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import java.io.File

@Composable
fun PreviewScreen(filePath: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    val file = File(filePath)
    CloudScaffold {
        CloudTopBar(
            title = "文件预览",
            subtitle = file.name,
            action = {
                Row {
                    CloudRoundIconButton(Icons.Outlined.Download, "下载", onDownload)
                    Spacer(Modifier.width(6.dp))
                    CloudRoundIconButton(Icons.AutoMirrored.Outlined.OpenInNew, "外部打开", onExternalOpen)
                }
            },
        )
        CloudCard {
            if (file.length() > PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES) {
                CloudEmptyState(
                    title = "文件过大",
                    message = "可以下载或用其他应用打开",
                    action = {
                        Row {
                            TextButton(onClick = onDownload) { Text("下载") }
                            TextButton(onClick = onExternalOpen) { Text("外部打开") }
                        }
                    },
                )
            } else {
                var text by remember(filePath) { mutableStateOf("加载中") }
                LaunchedEffect(filePath) {
                    text = runCatching { file.readText() }.getOrElse { "无法读取文件" }
                }
                Text(
                    text = text,
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
