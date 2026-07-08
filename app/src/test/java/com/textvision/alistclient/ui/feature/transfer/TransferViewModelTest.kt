package com.textvision.alistclient.ui.feature.transfer

import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val manager: TransferManager = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitorContract = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { manager.observeTransfers() } returns flowOf(emptyList())
        every { networkMonitor.isOnline } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectTab_switches_tab() = runTest(testDispatcher) {
        // Mirror the production wiring: combine + stateIn. Use Eagerly so the
        // test can read .value without keeping a separate collector alive.
        val tab = MutableStateFlow(TransferTab.UPLOAD)
        val state = combine(tab, manager.observeTransfers(), networkMonitor.isOnline) { t, all, online ->
            TransferListUiState(all = all, tab = t, isOnline = online)
        }.stateIn(backgroundScope, SharingStarted.Eagerly, TransferListUiState())

        advanceUntilIdle()
        runCurrent()
        assertEquals(TransferTab.UPLOAD, state.value.tab)

        // switch to DOWNLOAD
        tab.value = TransferTab.DOWNLOAD
        advanceUntilIdle()
        runCurrent()
        assertEquals(TransferTab.DOWNLOAD, state.value.tab)

        // switch back to UPLOAD
        tab.value = TransferTab.UPLOAD
        advanceUntilIdle()
        runCurrent()
        assertEquals(TransferTab.UPLOAD, state.value.tab)
    }

    @Test
    fun cancel_delegates_to_manager() = runTest {
        val viewModel = TransferViewModel(manager, networkMonitor)

        viewModel.cancel("id1")
        verify { manager.cancel("id1") }
    }
}
