package com.textvision.alistclient.ui.feature.picker

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.FileCategory
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "Picker Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PickerLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PickerPreviewBody(darkMode = DarkMode.LIGHT)
        }
    }
}

@Preview(
    name = "Picker Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PickerDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PickerPreviewBody(darkMode = DarkMode.DARK)
        }
    }
}

@Preview(name = "Picker Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PickerEmptyPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PickerEmptyBody()
        }
    }
}

@Composable
private fun PickerPreviewBody(darkMode: DarkMode) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "选择目标", subtitle = "移动 3 项到…", onBack = {})
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Breadcrumb chips
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                PreviewChip(AppIcons.home, "根目录")
                Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PreviewChip(AppIcons.folder, "我的资料")
                Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PreviewChip(AppIcons.folder, "工作")
                Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+ 新建", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                text = "可移动到的位置",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            // Current directory row (selected style)
            PreviewFolderRow(name = "当前目录", subtitle = "3 项 · 当前目录", isCurrent = true)
            PreviewFolderRow(name = "备份", subtitle = "文件夹", isCurrent = false)
            PreviewFolderRow(name = "旧文件", subtitle = "文件夹", isCurrent = false)
            Box(modifier = Modifier.height(72.dp))
            ActionButton(
                text = "确认移动到 · 当前目录",
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                variant = ButtonVariant.FILLED,
            )
        }
    }
}

@Composable
private fun PickerEmptyBody() {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "选择目标", subtitle = "移动 3 项到…", onBack = {})
        Text(
            text = "可移动到的位置",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text("（无子目录）", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PreviewChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .padding(end = 4.dp)
            .padding(horizontal = 4.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun PreviewFolderRow(name: String, subtitle: String, isCurrent: Boolean) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        FileTypeIcon(FileCategory.FOLDER)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
