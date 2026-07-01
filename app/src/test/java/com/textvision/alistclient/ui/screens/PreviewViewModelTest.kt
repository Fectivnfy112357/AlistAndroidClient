package com.textvision.alistclient.ui.screens

import com.textvision.alistclient.transfer.TransferManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PreviewViewModelTest {
    private val transferManager: TransferManager = mockk(relaxed = true)

    @Test fun enqueueDownloadForwardsRemotePathAndFileNameToTransferManager() {
        val viewModel = PreviewViewModel(textRepository = mockk(relaxed = true), transferManager = transferManager)

        viewModel.enqueueDownload(path = "/d/notes/Rustdesk密钥.md", name = "Rustdesk密钥.md")

        verify(exactly = 1) {
            transferManager.enqueueDownload(remotePath = "/d/notes/Rustdesk密钥.md", fileName = "Rustdesk密钥.md")
        }
    }
}