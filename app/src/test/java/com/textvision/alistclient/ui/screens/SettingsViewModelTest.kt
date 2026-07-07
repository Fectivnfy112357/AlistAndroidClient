package com.textvision.alistclient.ui.screens

import app.cash.turbine.test
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.settings.SettingsRepositoryContract
import com.textvision.alistclient.admin.storage.StorageRepositoryContract
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SettingsViewModelTest {
    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val transfer: TransferManager = mockk(relaxed = true)
    private val preview: PreviewFileStore = mockk(relaxed = true)
    private val storage: StorageRepositoryContract = mockk(relaxed = true)
    private val settings: SettingsRepositoryContract = mockk(relaxed = true)
    private val session: SessionManager = mockk(relaxed = true) {
        coEvery { loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://test", username = "u", password = "p", token = "t"
        )
    }

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SettingsViewModel(
        authRepository = authRepo,
        transferManager = transfer,
        previewFileStore = preview,
        storageRepository = storage,
        settingsRepository = settings,
        sessionManager = session,
    )

    @Test fun loadAdminDataPopulatesUiStateOnSuccess() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(emptyList())
        val viewModel = vm()
        viewModel.uiState.test {
            // initial loading
            var s = awaitItem()
            viewModel.loadAdminData()
            s = awaitItem()
            assertTrue(s.storages.isNotEmpty())
            assertEquals("/local", s.storages[0].mountPath)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun toggleStorageSuccessUpdatesItem() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.update(any(), any()) } returns AdminResult.Ok(Unit)
        val viewModel = vm()
        viewModel.loadAdminData()
        viewModel.uiState.test {
            var s = awaitItem()
            val initialEnabled = s.storages[0].status == "work"
            viewModel.toggleStorage(id = 1, enabled = !initialEnabled)
            coVerify { storage.update(any(), match { it.id == 1L && it.enabled == !initialEnabled }) }
            s = awaitItem()
            val expectedStatus = if (!initialEnabled) "work" else "disabled"
            assertEquals(expectedStatus, s.storages[0].status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun toggleStorageFailureRollsBack() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.update(any(), any()) } returns AdminResult.ServerError(500)
        val viewModel = vm()
        viewModel.loadAdminData()
        viewModel.uiState.test {
            var s = awaitItem()
            val initialStatus = s.storages[0].status
            val initialEnabled = initialStatus == "work"
            viewModel.toggleStorage(id = 1, enabled = !initialEnabled)
            s = awaitItem() // optimistic update
            s = awaitItem() // rollback
            assertEquals(initialStatus, s.storages[0].status)
            assertTrue(s.errorMessage != null)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
