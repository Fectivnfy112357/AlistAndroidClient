package com.textvision.alistclient.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    onStorageClick: (Long) -> Unit = {},
    onAdvancedSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(Unit) { viewModel.loadAdminData() }

    SettingsContent(
        uiState = uiState,
        onDarkModeChange = viewModel::setDarkMode,
        onStorageClick = onStorageClick,
        onQuickSettingEdit = { item, value -> viewModel.saveQuickSetting(item.key, value) },
        onClearPreviewFiles = { viewModel.clearPreviewFiles() },
        onAdvancedSettings = onAdvancedSettings,
        onLogout = viewModel::logout,
    )
}
