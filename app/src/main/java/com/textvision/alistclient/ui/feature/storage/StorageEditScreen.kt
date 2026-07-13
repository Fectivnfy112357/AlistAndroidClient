package com.textvision.alistclient.ui.feature.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.storage.StorageEditUiState
import com.textvision.alistclient.admin.storage.StorageEditViewModel
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.theme.Brand50

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

    when (val screenState = state) {
        StorageEditUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize().background(Brand50),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { CircularProgressIndicator() }
        is StorageEditUiState.Error -> Box(
            modifier = Modifier.fillMaxSize().background(Brand50).padding(horizontal = 16.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { StatusBanner(message = screenState.message, kind = BannerKind.ERROR) }
        is StorageEditUiState.Form -> StorageEditContent(
            form = screenState,
            onBack = onBack,
            onFieldValueChange = viewModel::updateField,
            onEnabledChange = viewModel::setEnabled,
            onSave = viewModel::save,
        )
    }
}
