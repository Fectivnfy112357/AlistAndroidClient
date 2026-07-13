package com.textvision.alistclient.ui.feature.storage

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "StorageEdit Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun StorageEditLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StorageEditPreviewBody(enabled = true)
        }
    }
}

@Preview(
    name = "StorageEdit Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StorageEditDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StorageEditPreviewBody(enabled = true)
        }
    }
}

@Preview(name = "StorageEdit Disabled", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun StorageEditDisabledPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StorageEditPreviewBody(enabled = false)
        }
    }
}

/** Static body mirroring the wired screen — fake StorageInfo only. */
@Composable
private fun StorageEditPreviewBody(enabled: Boolean) {
    val storage = StorageInfo(
        id = 1,
        mountPath = "/quark",
        driver = "Quark",
        order = 0,
        status = if (enabled) "work" else "disabled",
    )

    Column {
        AppTopBar(
            title = "编辑存储",
            subtitle = storage.mountPath,
            onBack = {},
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            StorageInfoCard(
                storageName = "夸克网盘",
                mountPath = storage.mountPath,
                enabled = enabled,
            )
            Spacer(Modifier.height(16.dp))
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(16.dp),
            ) {
                Column {
                    Text(
                        text = "驱动参数",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text("备注", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("我的夸克网盘", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    Text("挂载路径", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("/quark", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    Text("Cookie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("已设置 · 30 天前更新 [重新获取]", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "启用存储",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (enabled) "开" else "关",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ActionButton(
                text = "保存修改",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.FILLED,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "保存成功后将自动返回",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}