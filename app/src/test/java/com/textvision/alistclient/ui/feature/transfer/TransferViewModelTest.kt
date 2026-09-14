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
import kotlinx.coroutines.flow.first
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
        every { manager.observeTransfers() } returns flowOf(
            listOf(
                task("u1", TransferType.Upload, TransferStatus.Uploading),
                task("d1", TransferType.Download, TransferStatus.Downloading),
            )
        )
        val vm = TransferViewModel(manager, networkMonitor)

        // The production VM now uses `stateIn(WhileSubscribed(5_000))` to
        // avoid keeping Room's `dao.observeAll()` hot when no UI is collecting.
        // Reading `state.value` without a subscriber returns the initial empty
        // value, so we wait for the first real emission via `first { ... }`.
        val initial = vm.state.first { it.allCount > 0 }
        assertEquals(TransferTab.ALL, initial.tab)

        vm.selectTab(TransferTab.DOWNLOAD)
        val afterDownload = vm.state.first { it.tab == TransferTab.DOWNLOAD }
        assertEquals(TransferTab.DOWNLOAD, afterDownload.tab)

        vm.selectTab(TransferTab.UPLOAD)
        val afterUpload = vm.state.first { it.tab == TransferTab.UPLOAD }
        assertEquals(TransferTab.UPLOAD, afterUpload.tab)
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
            // ALL tab -> 3 visible
            val all = awaitItem()
            assertEquals(3, all.visible.size)
            assertEquals(TransferTab.ALL, all.tab)

            vm.selectTab(TransferTab.UPLOAD)
            val upload = awaitItem()
            assertEquals(2, upload.visible.size)
            assertEquals(TransferTab.UPLOAD, upload.tab)

            vm.selectTab(TransferTab.DOWNLOAD)
            val download = awaitItem()
            assertEquals(1, download.visible.size)
            assertEquals("d1", download.visible.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun visible_filters_failed_tab() = runTest {
        every { manager.observeTransfers() } returns flowOf(
            listOf(
                task("u1", TransferType.Upload, TransferStatus.Uploading),
                task("u2", TransferType.Upload, TransferStatus.Failed),
                task("u3", TransferType.Download, TransferStatus.Interrupted),
                task("d1", TransferType.Download, TransferStatus.Success),
            )
        )
        val vm = TransferViewModel(manager, networkMonitor)

        vm.state.test {
            // Drain the initial ALL emission
            val all = awaitItem()
            assertEquals(4, all.visible.size)
            assertEquals(TransferTab.ALL, all.tab)

            // FAILED tab -> 2 (u2 + u3)
            vm.selectTab(TransferTab.FAILED)
            val failed = awaitItem()
            assertEquals(2, failed.visible.size)
            assertEquals(TransferTab.FAILED, failed.tab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tab_counts_expose_badges() = runTest {
        every { manager.observeTransfers() } returns flowOf(
            listOf(
                task("u1", TransferType.Upload, TransferStatus.Uploading),
                task("u2", TransferType.Upload, TransferStatus.Failed),
                task("d1", TransferType.Download, TransferStatus.Success),
            )
        )
        val vm = TransferViewModel(manager, networkMonitor)

        // Wait for the first real emission that carries the seeded tasks
        val s = vm.state.first { it.allCount == 3 }
        assertEquals(3, s.allCount)
        assertEquals(2, s.uploadCount)
        assertEquals(1, s.downloadCount)
        assertEquals(1, s.failedCount)
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

        val s = vm.state.first { it.allCount == 3 }
        assertTrue(s.summary, s.summary.contains("1 进行中"))
        assertTrue(s.summary, s.summary.contains("1 失败"))
        assertTrue(s.summary, s.summary.contains("1 完成"))
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
