package com.textvision.alistclient.ui.feature.storage

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.network.dto.DriverInfo
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

/** Static body mirroring the wired screen — uses fake StorageInfo/DriverInfo. */
@Composable
private fun StorageEditPreviewBody(enabled: Boolean) {
    val storage = StorageInfo(
        id = 1,
        mountPath = "/我的阿里云",
        driver = "AliyunDrive",
        order = 0,
        status = if (enabled) "work" else "disabled",
    )
    val driver = DriverInfo(
        common = emptyList(),
        additional = listOf(
            com.textvision.alistclient.network.dto.ConfigItem(
                name = "root_folder_id",
                type = "string",
                default = kotlinx.serialization.json.JsonPrimitive("root"),
                options = null,
                label = "根目录路径",
            ),
            com.textvision.alistclient.network.dto.ConfigItem(
                name = "order_by",
                type = "select",
                default = kotlinx.serialization.json.JsonPrimitive("name"),
                options = null,
                label = "排序方式",
            ),
        ),
    )
    val formItems = driver.additional.orEmpty().map { FormItem.fromConfigItem(it) }
    val fieldValues: Map<String, Any?> = mapOf(
        "root_folder_id" to "root",
        "order_by" to "name",
        "remark" to "我的私人云盘",
        "mount_path" to "/我的阿里云",
    )

    Column {
        AppTopBar(
            title = "编辑存储",
            subtitle = storage.mountPath,
            onBack = {},
        )
        StorageInfoCard(
            storageName = storage.mountPath,
            mountPath = storage.mountPath,
            enabled = enabled,
        )
        Spacer(Modifier.height(10.dp))
        SectionCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            padding = PaddingValues(14.dp),
        ) {
            Text(
                text = "驱动参数",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            formItems.forEach { item ->
                Text(
                    text = "${item.label}：${(fieldValues[item.name] ?: "").toString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            variant = ButtonVariant.FILLED,
        )
    }
}
