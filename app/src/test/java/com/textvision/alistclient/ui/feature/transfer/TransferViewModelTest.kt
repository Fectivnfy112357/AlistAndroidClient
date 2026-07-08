package com.textvision.alistclient.ui.feature.transfer

import app.cash.turbine.test
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

    private val manager: TransferManager = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitorContract = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { manager.observeTransfers() } returns flowOf(emptyList())
        every { networkMonitor.isOnline } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun task(
        id: String,
        type: TransferType,
        status: TransferStatus,
    ) = TransferEntity(
        id = id,
        fileName = "$id.bin",
        remotePath = "/$id",
        localPath = null,
        sourceUri = null,
        bytesDone = 0,
        totalBytes = 100,
        type = type,
        status = status,
        failureReason = null,
        createdAtMillis = 0,
        updatedAtMillis = 0,
    )

    @Test
    fun selectTab_switches_tab_on_real_vm() = runTest {
        val vm = TransferViewModel(manager, networkMonitor)

        assertEquals(TransferTab.UPLOAD, vm.state.value.tab)

        vm.selectTab(TransferTab.DOWNLOAD)
        assertEquals(TransferTab.DOWNLOAD, vm.state.value.tab)

        vm.selectTab(TransferTab.UPLOAD)
        assertEquals(TransferTab.UPLOAD, vm.state.value.tab)
    }

    @Test
    fun visible_filters_by_tab_type() = runTest {
        every { manager.observeTransfers() } returns flowOf(
            listOf(
                task("u1", TransferType.Upload, TransferStatus.Uploading),
                task("u2", TransferType.Upload, TransferStatus.Success),
                task("d1", TransferType.Download, TransferStatus.Downloading),
            )
        )
        val vm = TransferViewModel(manager, networkMonitor)

        vm.state.test {
            // UPLOAD tab -> 2 uploads visible
            assertEquals(2, awaitItem().visible.size)

            vm.selectTab(TransferTab.DOWNLOAD)
            val download = awaitItem()
            assertEquals(1, download.visible.size)
            assertEquals("d1", download.visible.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun summary_counts_active_failed_completed() = runTest {
        every { manager.observeTransfers() } returns flowOf(
            listOf(
                task("u1", TransferType.Upload, TransferStatus.Uploading),
                task("u2", TransferType.Upload, TransferStatus.Failed),
                task("u3", TransferType.Upload, TransferStatus.Success),
            )
        )
        val vm = TransferViewModel(manager, networkMonitor)

        val summary = vm.state.value.summary
        assertTrue(summary, summary.contains("1 个进行中"))
        assertTrue(summary, summary.contains("1 个失败"))
        assertTrue(summary, summary.contains("1 个完成"))
    }

    @Test
    fun cancel_delegates_to_manager() = runTest {
        val viewModel = TransferViewModel(manager, networkMonitor)

        viewModel.cancel("id1")
        verify { manager.cancel("id1") }
    }

    @Test
    fun retry_delegates_to_manager() = runTest {
        val viewModel = TransferViewModel(manager, networkMonitor)

        viewModel.retry("id2")
        verify { manager.retry("id2") }
    }

    @Test
    fun delete_delegates_to_manager() = runTest {
        val viewModel = TransferViewModel(manager, networkMonitor)

        viewModel.delete("id3")
        verify { manager.delete("id3") }
    }
}
