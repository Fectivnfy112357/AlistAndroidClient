package com.textvision.alistclient.admin.storage

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
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
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.CloudBannerKind

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

    CloudScaffold(showBottomPadding = true) {
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
                        Text("挂载路径：${s.storage.mountPath}")
                        Text("驱动：${s.storage.driver}")
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.formItems.isNotEmpty()) {
                        CloudCard {
                            Text("驱动参数")
                            s.formItems.forEach { item ->
                                DynamicFormField(
                                    item = item,
                                    value = s.fieldValues[item.name],
                                    onValueChange = { viewModel.updateField(item.name, it) },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    CloudCard {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("启用存储")
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
private fun Column(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(modifier, content = content)
}
