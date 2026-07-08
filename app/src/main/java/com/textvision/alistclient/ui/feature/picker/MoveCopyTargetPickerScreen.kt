package com.textvision.alistclient.ui.feature.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.ui.components.DirectoryBrowser
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun MoveCopyTargetPickerScreen(
    onTargetSelected: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load("/") }
    val success = state as? FileUiState.Success
    AppScaffold(
        topBar = { AppTopBar(title = "选择目标") },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DirectoryBrowser(
                path = success?.path ?: "/",
                directories = success?.items.orEmpty().filter { it.isDir },
                onOpen = viewModel::load,
                onSelectCurrent = onTargetSelected,
            )
        }
    }
}