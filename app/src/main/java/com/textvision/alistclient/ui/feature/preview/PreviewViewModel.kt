package com.textvision.alistclient.ui.feature.preview

import androidx.lifecycle.ViewModel
import com.textvision.alistclient.preview.PreviewTextRepository
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val textRepository: PreviewTextRepository,
    private val transferManager: TransferManager,
) : ViewModel() {
    fun enqueueDownload(path: String, name: String): String =
        transferManager.enqueueDownload(remotePath = path, fileName = name)

    suspend fun fetchText(url: String): String =
        textRepository.fetch(url).getOrElse { "无法读取文件" }
}