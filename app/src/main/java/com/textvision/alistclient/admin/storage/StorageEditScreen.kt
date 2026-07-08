package com.textvision.alistclient.admin.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.bottomBarInset
import com.textvision.alistclient.ui.theme.CloudPrimary

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

    CloudScaffold(bottomInset = bottomBarInset()) {
        CloudTopBar(
            title = "编辑存储",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
        )
        when (val s = state) {
            is StorageEditUiState.Loading -> {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is StorageEditUiState.Error -> {
                CloudStatusBanner(text = s.message, kind = CloudBannerKind.Error)
            }
            is StorageEditUiState.Form -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CloudCard {
                        CloudListItem(
                            title = "挂载路径",
                            subtitle = s.storage.mountPath,
                            leading = {
                                Icon(Icons.Outlined.Folder, contentDescription = null, tint = CloudPrimary)
                            },
                        )
                        CloudListItem(
                            title = "驱动",
                            subtitle = s.storage.driver,
                            leading = {
                                Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary)
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.formItems.isNotEmpty()) {
                        CloudCard {
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
                        Spacer(Modifier.height(10.dp))
                    }
                    CloudCard {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
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
                        CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
                    }
                    Spacer(Modifier.height(20.dp))
                    androidx.compose.material3.Button(
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
private fun Column(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(modifier, content = content)
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
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Cookie,
                contentDescription = null,
                tint = CloudPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "获取",
                style = MaterialTheme.typography.labelLarge,
                color = CloudPrimary,
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
