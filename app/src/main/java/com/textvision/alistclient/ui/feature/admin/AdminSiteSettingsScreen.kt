package com.textvision.alistclient.ui.feature.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
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
        topBar = { AppTopBar(title = "完整设置", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            when (val s = state) {
                is AdminSiteSettingsUiState.Loading -> {
                    Spacer(Modifier.height(40.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is AdminSiteSettingsUiState.Error -> {
                    StatusBanner(message = s.message, kind = BannerKind.ERROR, modifier = Modifier.padding(horizontal = 16.dp))
                }
                is AdminSiteSettingsUiState.Form -> {
                    s.groups.forEach { group ->
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "分组：${group.key}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                                group.items.forEach { item ->
                                    RenderSettingItem(
                                        item = item,
                                        value = s.fieldValues[item.key],
                                        onChange = { viewModel.updateField(item.key, it) },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    s.errorMessage?.let {
                        StatusBanner(message = it, kind = BannerKind.ERROR, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    ActionButton(
                        text = if (s.isSaving) "保存中…" else "保存",
                        onClick = { viewModel.save() },
                        enabled = !s.isSaving,
                        loading = s.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        variant = ButtonVariant.FILLED,
                    )
                }
            }
        }
    }
}

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