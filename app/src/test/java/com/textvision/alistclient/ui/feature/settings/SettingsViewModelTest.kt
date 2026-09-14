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

    private fun vm(ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Unconfined) =
        SettingsViewModel(
            authRepository = authRepo,
            transferManager = transfer,
            previewFileStore = preview,
            storageRepository = storage,
            settingsRepository = settings,
            sessionManager = session,
            musicRootStore = mockk(relaxed = true),
            musicCache = mockk(relaxed = true) {
                io.mockk.every { sizeBytes } returns 0L
            },
            ioDispatcher = ioDispatcher,
        )

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

    @Test fun musicCacheSize_isReadFromIoDispatcher_notMain() = runTest {
        // Single-threaded dispatcher lets us assert that the cache walk ran
        // there and not on the main test dispatcher.
        var calledOnIo = false
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val cache = mockk<com.textvision.alistclient.music.playback.MusicCache>(relaxed = true)
        io.mockk.every { cache.sizeBytes } answers {
            calledOnIo = true
            42L
        }

        val viewModel = SettingsViewModel(
            authRepository = authRepo,
            transferManager = transfer,
            previewFileStore = preview,
            storageRepository = storage,
            settingsRepository = settings,
            sessionManager = session,
            musicRootStore = mockk(relaxed = true),
            musicCache = cache,
            ioDispatcher = testDispatcher,
        )

        viewModel.musicCacheSize.test {
            // Initial value published by the StateFlow before the IO walk
            // completes is 0L; the IO result then publishes 42L.
            assertEquals(0L, awaitItem())
            // Drive the IO dispatcher.
            testScheduler.advanceUntilIdle()
            val after = awaitItem()
            assertEquals(42L, after)
            assertTrue("sizeBytes should be invoked on the IO dispatcher", calledOnIo)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun clearMusicCache_resetsSize_toZero() = runTest {
        val cache = mockk<com.textvision.alistclient.music.playback.MusicCache>(relaxed = true)
        io.mockk.every { cache.sizeBytes } returnsMany listOf(1234L, 0L)
        coEvery { cache.clear() } returns Unit

        val viewModel = SettingsViewModel(
            authRepository = authRepo,
            transferManager = transfer,
            previewFileStore = preview,
            storageRepository = storage,
            settingsRepository = settings,
            sessionManager = session,
            musicRootStore = mockk(relaxed = true),
            musicCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // init block reads 1234L
        assertEquals(1234L, viewModel.musicCacheSize.value)
        viewModel.onClearMusicCache()
        // After clear() the second sizeBytes read returns 0L.
        assertEquals(0L, viewModel.musicCacheSize.value)
        coVerify(exactly = 1) { cache.clear() }
    }
}
