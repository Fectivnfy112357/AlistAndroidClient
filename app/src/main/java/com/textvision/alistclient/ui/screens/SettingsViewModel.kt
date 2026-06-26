package com.textvision.alistclient.ui.screens

import androidx.lifecycle.ViewModel
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transferManager: TransferManager,
    private val previewFileStore: PreviewFileStore,
) : ViewModel() {
    fun logout() {
        transferManager.clearAllTasks()
        authRepository.logout()
        previewFileStore.clearPreviewFiles()
    }

    fun clearPreviewFiles(): Int = previewFileStore.clearPreviewFiles()
}
