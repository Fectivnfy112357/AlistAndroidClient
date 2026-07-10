package com.textvision.alistclient.ui.feature.admin

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.textvision.alistclient.admin.settings.SettingGroup
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.network.dto.ConfigItem
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "AdminSite Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AdminSiteLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AdminPreviewBody(darkMode = DarkMode.LIGHT)
        }
    }
}

@Preview(
    name = "AdminSite Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdminSiteDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AdminPreviewBody(darkMode = DarkMode.DARK)
        }
    }
}

@Preview(name = "AdminSite Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AdminSiteEmptyPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AppTopBar(title = "站点设置", onBack = {})
                Box(modifier = Modifier.padding(24.dp)) {
                    Text("等待加载", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AdminPreviewBody(darkMode: DarkMode) {
    val sampleItem = SettingItem(
        key = "site_title",
        value = "我的 Alist",
        type = "string",
        help = null,
        options = null,
        formItems = listOf(ConfigItem(name = "site_title", type = "string", default = kotlinx.serialization.json.JsonPrimitive("我的 Alist"), options = null, label = "站点标题")),
    )
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        AppTopBar(title = "站点设置", onBack = {})
        PreviewGroup("站点", listOf(sampleItem), mapOf("site_title" to "我的 Alist"))
        PreviewGroup("预览", emptyList(), emptyMap())
        PreviewGroup("安全", emptyList(), emptyMap())
        ActionButton(
            text = "保存全部",
            onClick = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            variant = ButtonVariant.FILLED,
        )
    }
}

@Composable
private fun PreviewGroup(title: String, items: List<SettingItem>, values: Map<String, String?>) {
    SectionCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        padding = PaddingValues(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        items.forEach { item ->
            val formItem = item.formItems?.map { FormItem.fromConfigItem(it) }?.firstOrNull()
            Text(
                text = "${item.key}：${values[item.key] ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            formItem
        }
    }
}
