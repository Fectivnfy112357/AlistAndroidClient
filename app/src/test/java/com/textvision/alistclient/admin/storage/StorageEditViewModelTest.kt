package com.textvision.alistclient.admin.storage

import app.cash.turbine.test
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
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
class StorageEditViewModelTest {
    private val auth: AuthRepository = mockk(relaxed = true)
    private val transfer: TransferManager = mockk(relaxed = true)
    private val preview: PreviewFileStore = mockk(relaxed = true)
    private val storage: StorageRepositoryContract = mockk(relaxed = true)
    private val session: SessionManager = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadPopulatesFormWithDriverItems() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work", addition = "{\"root_folder_path\":\"/data\"}")))
        )
        coEvery { storage.listDrivers(any()) } returns AdminResult.Ok(
            listOf(DriverInfo(name = "Local", label = "本地存储", common = emptyList(), additional = emptyList()))
        )
        val vm = StorageEditViewModel(auth, transfer, preview, storage, session)
        vm.load(1)
        vm.uiState.test {
            var s = awaitItem()
            assertTrue(s is StorageEditUiState.Form)
            s = s as StorageEditUiState.Form
            assertEquals("/local", s.storage.mountPath)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun saveCallsUpdate() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.listDrivers(any()) } returns AdminResult.Ok(emptyList())
        coEvery { storage.update(any(), any()) } returns AdminResult.Ok(Unit)
        val vm = StorageEditViewModel(auth, transfer, preview, storage, session)
        vm.load(1)
        vm.save()
        coVerify { storage.update(any(), match { it.id == 1L && it.mountPath == "/local" }) }
    }
}
