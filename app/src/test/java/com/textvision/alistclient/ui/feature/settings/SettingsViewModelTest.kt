package com.textvision.alistclient.ui.feature.settings

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
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.ThemeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val themeRepo: ThemeRepository = mockk(relaxed = true) {
        coEvery { darkMode } returns MutableStateFlow(DarkMode.SYSTEM)
        coEvery { setDarkMode(any()) } returns Unit
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
        themeRepository = themeRepo,
    )

    @Test fun setDarkModePersists() = runTest {
        val viewModel = vm()
        viewModel.setDarkMode(DarkMode.DARK)
        coVerify { themeRepo.setDarkMode(DarkMode.DARK) }
    }

    @Test fun toggleStorageCallsRepo() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.update(any(), any()) } returns AdminResult.Ok(Unit)
        val viewModel = vm()
        viewModel.loadAdminData()
        viewModel.uiState.test {
            val s = awaitItem()
            val initialEnabled = s.storages[0].status == "work"
            viewModel.toggleStorage(id = 1, enabled = !initialEnabled)
            coVerify { storage.update(any(), match { it.id == 1L && it.disabled == initialEnabled }) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun loadAdminDataPopulatesStorages() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(
                content = listOf(
                    StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work"),
                    StorageInfo(id = 2, mountPath = "/onedrive", driver = "Onedrive", status = "disabled"),
                )
            )
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(emptyList())
        val viewModel = vm()
        viewModel.loadAdminData()

        val state = viewModel.uiState.value
        assertEquals(2, state.storages.size)
        assertEquals(1L, state.storages[0].id)
        assertEquals(null, state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test fun loadAdminDataFailureSurfacesError() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.ServerError(code = 500, message = "boom")
        coEvery { settings.list(any()) } returns AdminResult.Ok(emptyList())
        val viewModel = vm()
        viewModel.loadAdminData()

        val state = viewModel.uiState.value
        assertTrue(state.storages.isEmpty())
        assertTrue(state.errorMessage.orEmpty(), state.errorMessage?.contains("ServerError") == true)
        assertEquals(false, state.isLoading)
    }

    @Test fun toggleStorageFailureSurfacesError() = runTest {
        // list keeps returning the failure, so the reload triggered by
        // toggleStorage leaves the surfaced error in place.
        coEvery { storage.list(any()) } returns AdminResult.ServerError(code = 500)
        coEvery { storage.update(any(), any()) } returns AdminResult.ServerError(code = 500)
        val viewModel = vm()
        // seed a storage so toggleStorage finds a matching entry
        coEvery { storage.list(any()) } returnsMany listOf(
            AdminResult.Ok(StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))),
            AdminResult.ServerError(code = 500),
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(emptyList())
        viewModel.loadAdminData()
        assertEquals(1, viewModel.uiState.value.storages.size)

        viewModel.toggleStorage(id = 1, enabled = false)

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage.orEmpty(), state.errorMessage?.contains("ServerError") == true)
    }
}
