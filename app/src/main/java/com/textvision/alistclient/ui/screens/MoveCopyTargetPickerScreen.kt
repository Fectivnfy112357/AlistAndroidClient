package com.textvision.alistclient.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.ui.components.DirectoryBrowser

@Composable
fun MoveCopyTargetPickerScreen(
    onTargetSelected: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load("/") }
    val success = state as? FileUiState.Success
    DirectoryBrowser(
        path = success?.path ?: "/",
        directories = success?.items.orEmpty().filter { it.isDir },
        onOpen = viewModel::load,
        onSelectCurrent = onTargetSelected,
    )
}
