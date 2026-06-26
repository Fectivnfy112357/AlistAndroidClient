package com.textvision.alistclient.ui.screens

import androidx.lifecycle.ViewModel
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transferManager: TransferManager,
    private val previewFileStore: PreviewFileStore,
) : ViewModel() {
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    fun logout() {
        transferManager.clearAllTasks()
        authRepository.logout()
        previewFileStore.clearPreviewFiles()
        _loggedOut.value = true
    }

    fun clearPreviewFiles(): Int = previewFileStore.clearPreviewFiles()
}
