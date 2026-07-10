package com.textvision.alistclient.ui.feature.file

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme

/**
 * Multi-select bottom action bar (prototype Screen03). 5 slots:
 * 已选 N · 全选（圆形高亮背景） · 移动（primary） · 下载 · 删除（danger） · 取消（×）.
 */
@Composable
fun FileMultiSelectBar(
    selectionCount: Int,
    onSelectAll: () -> Unit,
    onMove: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "已选 $selectionCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 全选 — highlighted circular background
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onSelectAll, modifier = Modifier.size(28.dp)) {
                        Icon(
                            AppIcons.check,
                            contentDescription = "全选",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(2.dp))
                // 移动 — primary tint
                IconButton(onClick = onMove) {
                    Icon(
                        AppIcons.folder,
                        contentDescription = "移动",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                // 下载
                IconButton(onClick = onDownload) {
                    Icon(
                        AppIcons.download,
                        contentDescription = "下载",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // 删除 — danger
                IconButton(onClick = onDelete) {
                    Icon(
                        AppIcons.trash,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                // 取消
                IconButton(onClick = onClear) {
                    Icon(
                        AppIcons.back,
                        contentDescription = "取消",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Blue-tinted hint strip shown at the top of the list while multi-selecting.
 */
@Composable
fun FileMultiSelectHint(
    selectionCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "已选 $selectionCount 项 · 点击 移动 选择目标目录",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Preview(name = "MultiSelectBar Light")
@Composable
private fun FileMultiSelectBarPreviewLight() {
    AlistTheme {
        Column {
            FileMultiSelectHint(selectionCount = 3)
            Spacer(Modifier.size(8.dp))
            FileMultiSelectBar(
                selectionCount = 3,
                onSelectAll = {},
                onMove = {},
                onDownload = {},
                onDelete = {},
                onClear = {},
            )
        }
    }
}
