package com.textvision.alistclient.ui.feature.storage

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.admin.storage.StorageEditUiState
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "StorageEdit Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun StorageEditLightPreview() = StorageEditPreview(DarkMode.LIGHT, true)

@Preview(name = "StorageEdit Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StorageEditDarkPreview() = StorageEditPreview(DarkMode.DARK, true)

@Preview(name = "StorageEdit Disabled", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun StorageEditDisabledPreview() = StorageEditPreview(DarkMode.LIGHT, false)

@Composable
private fun StorageEditPreview(mode: DarkMode, enabled: Boolean) {
    val form = StorageEditUiState.Form(
        storage = StorageInfo(id = 1, mountPath = "/quark", driver = "Quark", remark = "我的夸克网盘"),
        driver = null,
        formItems = listOf(
            FormItem.Text("remark", "备注"),
            FormItem.Text("mount_path", "挂载路径"),
            FormItem.Text("cookie", "Cookie"),
            FormItem.Text("root_folder_path", "根目录路径"),
            FormItem.Select("sort", "排序方式", listOf("modified_desc" to "按修改时间倒序")),
        ),
        fieldValues = mapOf(
            "remark" to "我的夸克网盘",
            "mount_path" to "/quark",
            "cookie" to "ready",
            "root_folder_path" to "/我的资源/",
            "sort" to "modified_desc",
        ),
        enabled = enabled,
    )
    AlistTheme(darkMode = mode) {
        Surface { StorageEditContent(form, {}, { _, _ -> }, {}, {}) }
    }
}
