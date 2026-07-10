package com.textvision.alistclient.ui.feature.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.cookie.CookieSites
import com.textvision.alistclient.admin.cookie.WebCookieDialog
import com.textvision.alistclient.admin.form.DynamicFormField
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.admin.storage.StorageEditUiState
import com.textvision.alistclient.admin.storage.StorageEditViewModel
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun StorageEditScreen(
    storageId: Long,
    onBack: () -> Unit,
    viewModel: StorageEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(storageId) { viewModel.load(storageId) }
    LaunchedEffect((state as? StorageEditUiState.Form)?.saved) {
        if ((state as? StorageEditUiState.Form)?.saved == true) onBack()
    }

    AppScaffold(
        topBar = { AppTopBar(title = "编辑存储", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            when (val s = state) {
                is StorageEditUiState.Loading -> {
                    Spacer(Modifier.height(40.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is StorageEditUiState.Error -> {
                    StatusBanner(message = s.message, kind = BannerKind.ERROR)
                }
                is StorageEditUiState.Form -> {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            ListItemRow(
                                title = "挂载路径",
                                subtitle = s.storage.mountPath,
                                leading = {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                            )
                            ListItemRow(
                                title = "驱动",
                                subtitle = s.storage.driver,
                                leading = {
                                    Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.formItems.isNotEmpty()) {
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "驱动参数",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                                s.formItems.forEach { item ->
                                    DynamicFormField(
                                        item = item,
                                        value = s.fieldValues[item.name],
                                        onValueChange = { viewModel.updateField(item.name, it) },
                                        trailing = {
                                            CookieFetchButton(
                                                item = item,
                                                driver = s.storage.driver,
                                                onCaptured = { viewModel.updateField(item.name, it) },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("启用存储", style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = s.enabled,
                                onCheckedChange = { viewModel.setEnabled(it) },
                            )
                        }
                    }
                    s.errorMessage?.let {
                        Spacer(Modifier.height(10.dp))
                        StatusBanner(message = it, kind = BannerKind.ERROR, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    ActionButton(
                        text = if (s.isSaving) "保存中…" else "保存",
                        onClick = { viewModel.save() },
                        enabled = !s.isSaving,
                        isLoading = s.isSaving,
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
private fun CookieFetchButton(item: FormItem, driver: String, onCaptured: (String) -> Unit) {
    if (item.name != "cookie") return
    val site = when (driver) {
        "Quark" -> CookieSites.Quark
        "BaiduNetdisk" -> CookieSites.BaiduNetdisk
        else -> return
    }
    var showDialog by remember { mutableStateOf(false) }
    FilledTonalButton(
        onClick = { showDialog = true },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Cookie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "获取",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
    if (showDialog) {
        WebCookieDialog(
            site = site,
            onDismiss = { showDialog = false },
            onCookieCaptured = { cookie ->
                onCaptured(cookie)
                showDialog = false
            },
        )
    }
}