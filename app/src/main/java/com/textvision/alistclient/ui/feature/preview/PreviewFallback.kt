package com.textvision.alistclient.ui.feature.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

/**
 * Fallback preview surface for modes that don't have an in-app renderer:
 * - [com.textvision.alistclient.preview.PreviewMode.TextTooLarge]
 * - [com.textvision.alistclient.preview.PreviewMode.External] (Video / Pdf / Archive / Other)
 * - [com.textvision.alistclient.preview.PreviewMode.Unavailable] (Folder / missing URL)
 *
 * Shows a message plus "下载" / "外部打开" actions. Both callbacks are no-ops when
 * the relevant capability is unavailable (handled by callers that null-check `downloadUrl`).
 */
@Composable
internal fun PreviewFallback(
    title: String,
    message: String,
    onDownload: () -> Unit,
    onExternalOpen: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton(
                text = "下载",
                onClick = onDownload,
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.OUTLINED,
                leadingIcon = AppIcons.download,
            )
            ActionButton(
                text = "外部打开",
                onClick = onExternalOpen,
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.OUTLINED,
                leadingIcon = AppIcons.external,
            )
        }
    }
}

@Preview(name = "Fallback External")
@Composable
private fun FallbackExternalPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        PreviewFallback(
            title = "暂不支持内置预览",
            message = "可以下载或用其他应用打开",
            onDownload = {},
            onExternalOpen = {},
        )
    }
}

@Preview(name = "Fallback Too Large")
@Composable
private fun FallbackTooLargePreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        PreviewFallback(
            title = "文件过大",
            message = "可以下载或用其他应用打开",
            onDownload = {},
            onExternalOpen = {},
        )
    }
}

@Preview(name = "Fallback Dark")
@Composable
private fun FallbackDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        PreviewFallback(
            title = "无法预览",
            message = "当前文件没有可用预览链接，请下载后查看",
            onDownload = {},
            onExternalOpen = {},
        )
    }
}