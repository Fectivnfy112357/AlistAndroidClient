package com.textvision.alistclient.ui.feature.picker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.Ink
import com.textvision.alistclient.ui.theme.InkSoft

@Preview(name = "Picker Light", showBackground = true, widthDp = 360, heightDp = 820)
@Composable
private fun PickerLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PickerPreviewBody()
        }
    }
}

@Preview(
    name = "Picker Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PickerDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PickerPreviewBody()
        }
    }
}

@Preview(name = "Picker Creating", showBackground = true, widthDp = 360, heightDp = 820)
@Composable
private fun PickerCreatingPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PickerCreatingBody()
        }
    }
}

@Composable
private fun PickerPreviewBody() {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "选择目标", subtitle = "移动 2 项到…", onBack = {})
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewChip(AppIcons.home, "根目录")
            Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreviewChip(AppIcons.folder, "我的资料")
            Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreviewChip(AppIcons.folder, "2024 春季合集")
            Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreviewChip(Icons.Outlined.Add, "+ 新建")
        }
        Text(
            text = "可移动到的位置",
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp),
        )
        // 当前目录 row (selected)
        CurrentFolderRowPrototype(
            name = "当前目录",
            subtitle = "2 项 · 当前目录",
            selected = false,
            isCurrent = true,
        )
        // 子目录 rows
        PreviewFolderRow(name = "备份目录", subtitle = "8 项 · 1.2 GB", selected = false)
        PreviewFolderRow(name = "工作文档", subtitle = "23 项", selected = false)
        PreviewFolderRow(name = "2024 春季合集", subtitle = "12 项 · 当前目录", selected = true)
        PreviewFolderRow(name = "家庭相册", subtitle = "56 项", selected = false)
        PreviewFolderRow(name = "下载缓存", subtitle = "4 项 · 340 MB", selected = false)
        Box(modifier = Modifier.height(72.dp))
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                ActionButton(
                    text = "确认移动到 · 2024 春季合集",
                    onClick = {},
                    variant = ButtonVariant.FILLED,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
        }
    }
}

@Composable
private fun PickerCreatingBody() {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "选择目标", subtitle = "移动 2 项到…", onBack = {})
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewChip(AppIcons.home, "根目录")
            Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreviewChip(AppIcons.folder, "我的资料")
            Text(" / ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreviewChip(Icons.Outlined.Check, "收起")
        }
        // 创建文件夹 strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brand300)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("樱花合集_v2", style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Brand500, Brand600)))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                Text("创建", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text(
            text = "可移动到的位置",
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp),
        )
        PreviewFolderRow(name = "备份目录", subtitle = "8 项 · 1.2 GB", selected = false)
        PreviewFolderRow(name = "工作文档", subtitle = "23 项", selected = false)
        PreviewFolderRow(name = "家庭相册", subtitle = "56 项", selected = false)
    }
}

@Composable
private fun PreviewChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(13.dp))
        Text(
            label,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun CurrentFolderRowPrototype(
    name: String,
    subtitle: String,
    selected: Boolean,
    isCurrent: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brand300)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Brand300, Brand500.copy(alpha = 0.4f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PreviewFolderRow(name: String, subtitle: String, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Brand300 else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Brand300, MaterialTheme.colorScheme.primaryContainer))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Brand500, Brand600))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Transparent),
            )
        }
    }
}