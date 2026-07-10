package com.textvision.alistclient.ui.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.form.DynamicFormField
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.admin.settings.AdminSiteSettingsUiState
import com.textvision.alistclient.admin.settings.AdminSiteSettingsViewModel
import com.textvision.alistclient.admin.settings.SettingGroup
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun AdminSiteSettingsScreen(
    onBack: () -> Unit,
    viewModel: AdminSiteSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect((state as? AdminSiteSettingsUiState.Form)?.saved) {
        if ((state as? AdminSiteSettingsUiState.Form)?.saved == true) onBack()
    }

    AppScaffold(
        topBar = { AppTopBar(title = "站点设置", onBack = onBack) },
    ) { innerPadding ->
        when (val s = state) {
            is AdminSiteSettingsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is AdminSiteSettingsUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                StatusBanner(message = s.message, kind = BannerKind.ERROR)
            }
            is AdminSiteSettingsUiState.Form -> AdminFormBody(s, viewModel, innerPadding)
        }
    }
}

@Composable
private fun AdminFormBody(
    s: AdminSiteSettingsUiState.Form,
    viewModel: AdminSiteSettingsViewModel,
    innerPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        // 站点 - title / announcement / icon / hide_announcement
        SiteSettingsSection(
            items = s.groups.flatMap { it.items }
                .filter { it.key in SITE_KEYS },
            values = s.fieldValues,
            onChange = { k, v -> viewModel.updateField(k, v) },
        )
        Spacer(Modifier.height(10.dp))

        // 预览 - preview_enabled / autoplay_video / force_proxy
        PreviewSettingsSection(
            items = s.groups.flatMap { it.items }
                .filter { it.key in PREVIEW_KEYS },
            values = s.fieldValues,
            onChange = { k, v -> viewModel.updateField(k, v) },
        )
        Spacer(Modifier.height(10.dp))

        // 安全 - token有效期 + 签名直链（无就跳过）
        SecuritySettingsSection(
            items = s.groups.flatMap { it.items }
                .filter { it.key in SECURITY_KEYS },
            values = s.fieldValues,
            onChange = { k, v -> viewModel.updateField(k, v) },
        )

        s.errorMessage?.let {
            Spacer(Modifier.height(10.dp))
            StatusBanner(message = it, kind = BannerKind.ERROR, modifier = Modifier.padding(horizontal = 14.dp))
        }
        Spacer(Modifier.height(16.dp))
        ActionButton(
            text = if (s.isSaving) "保存中…" else "保存全部",
            onClick = { viewModel.save() },
            enabled = !s.isSaving,
            isLoading = s.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .navigationBarsPadding(),
            variant = ButtonVariant.FILLED,
        )
        Spacer(Modifier.height(20.dp))
    }
}

private const val SITE_KEYS = "site_title,site_announcement,site_icon,hide_announcement"
private const val PREVIEW_KEYS = "preview_enabled,autoplay_video,force_proxy"
private const val SECURITY_KEYS = "token_validity,enable_sign"

@Composable
private fun SettingsGroupSection(
    title: String,
    items: List<SettingItem>,
    values: Map<String, String?>,
    onChange: (String, String?) -> Unit,
) {
    if (items.isEmpty()) return
    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        padding = PaddingValues(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        items.forEach { item ->
            RenderSettingItem(
                item = item,
                value = values[item.key],
                onChange = { v -> onChange(item.key, v) },
            )
        }
    }
}

@Composable
private fun SiteSettingsSection(
    items: List<SettingItem>,
    values: Map<String, String?>,
    onChange: (String, String?) -> Unit,
) = SettingsGroupSection(title = "站点", items = items, values = values, onChange = onChange)

@Composable
private fun PreviewSettingsSection(
    items: List<SettingItem>,
    values: Map<String, String?>,
    onChange: (String, String?) -> Unit,
) = SettingsGroupSection(title = "预览", items = items, values = values, onChange = onChange)

@Composable
private fun SecuritySettingsSection(
    items: List<SettingItem>,
    values: Map<String, String?>,
    onChange: (String, String?) -> Unit,
) = SettingsGroupSection(title = "安全", items = items, values = values, onChange = onChange)

@Composable
private fun RenderSettingItem(
    item: SettingItem,
    value: String?,
    onChange: (String?) -> Unit,
) {
    val formItem = item.formItems?.map { FormItem.fromConfigItem(it) }?.firstOrNull()
    if (formItem != null) {
        DynamicFormField(
            item = formItem,
            value = value,
            onValueChange = { v -> onChange(v?.toString()) },
        )
    }
}
