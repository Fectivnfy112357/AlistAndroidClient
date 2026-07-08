package com.textvision.alistclient.admin.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.bottomBarInset

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

    CloudScaffold(bottomInset = bottomBarInset()) {
        CloudTopBar(
            title = "完整设置",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
        )
        when (val s = state) {
            is AdminSiteSettingsUiState.Loading -> {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is AdminSiteSettingsUiState.Error -> {
                CloudStatusBanner(text = s.message, kind = CloudBannerKind.Error)
            }
            is AdminSiteSettingsUiState.Form -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    s.groups.forEach { group ->
                        CloudCard {
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
                        Spacer(Modifier.height(10.dp))
                    }
                    s.errorMessage?.let {
                        CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !s.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (s.isSaving) "保存中…" else "保存")
                    }
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