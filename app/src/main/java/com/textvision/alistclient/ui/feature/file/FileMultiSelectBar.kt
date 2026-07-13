package com.textvision.alistclient.ui.feature.file

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.InkSoft
import com.textvision.alistclient.ui.theme.StateError

/**
 * Multi-select action bar (prototype Screen03 §3.3).
 *
 * 5 horizontal slots: 全选 (brand-blue circle w/ check) · 移动 (folder, primary) ·
 * 下载 · 删除 (error) · 取消 (×).
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "已选 $selectionCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp),
            )
            Spacer(Modifier.width(4.dp))
            ActionSlot(
                label = "全选",
                onClick = onSelectAll,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Brand500, Brand600))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            ActionSlot(
                label = "移动",
                tint = MaterialTheme.colorScheme.primary,
                icon = AppIcons.folder,
                onClick = onMove,
            )
            ActionSlot(
                label = "下载",
                tint = MaterialTheme.colorScheme.onSurface,
                icon = AppIcons.download,
                onClick = onDownload,
            )
            ActionSlot(
                label = "删除",
                tint = StateError,
                icon = AppIcons.trash,
                onClick = onDelete,
            )
            ActionSlot(
                label = "取消",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = AppIcons.back,
                onClick = onClear,
            )
        }
    }
}

@Composable
private fun ActionSlot(
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        } else {
            content()
        }
        Spacer(Modifier.size(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

/**
 * Blue-tinted hint strip shown at the top of the list while multi-selecting
 * (prototype §3.2: "已选 N 项 · 点击 移动 选择目标目录").
 */
@Composable
fun FileMultiSelectHint(
    selectionCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Brand300,
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
                text = buildAnnotatedString {
                    append("已选 $selectionCount 项 · 点击 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("移动")
                    }
                    append(" 选择目标目录")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Preview(name = "MultiSelect Light", showBackground = true)
@Composable
private fun FileMultiSelectBarPreviewLight() {
    AlistTheme {
        Column {
            FileMultiSelectHint(selectionCount = 2)
            Spacer(Modifier.size(8.dp))
            FileMultiSelectBar(
                selectionCount = 2,
                onSelectAll = {},
                onMove = {},
                onDownload = {},
                onDelete = {},
                onClear = {},
            )
        }
    }
}