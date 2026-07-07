package com.textvision.alistclient.admin.settings

import app.cash.turbine.test
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.network.dto.ConfigItem
import com.textvision.alistclient.network.dto.SettingItem
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
class AdminSiteSettingsViewModelTest {
    private val auth: AuthRepository = mockk(relaxed = true)
    private val transfer: TransferManager = mockk(relaxed = true)
    private val preview: PreviewFileStore = mockk(relaxed = true)
    private val settings: SettingsRepositoryContract = mockk(relaxed = true)
    private val session: SessionManager = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadPopulatesGroups() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(listOf(
            SettingGroup("site", listOf(SettingItem(
                key = "site_title", value = "My Alist", group = "site",
                formItems = listOf(ConfigItem(name = "site_title", label = "标题", type = "string"))
            )))
        ))
        val vm = AdminSiteSettingsViewModel(auth, transfer, preview, settings, session)
        vm.load()
        vm.uiState.test {
            var s = awaitItem()
            assertTrue(s is AdminSiteSettingsUiState.Form)
            s = s as AdminSiteSettingsUiState.Form
            assertEquals(1, s.groups.size)
            assertEquals("site", s.groups[0].key)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun saveSendsAllPatches() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(listOf(
            SettingGroup("site", listOf(SettingItem(
                key = "site_title", value = "old", group = "site",
                formItems = listOf(ConfigItem(name = "site_title", label = "标题", type = "string"))
            )))
        ))
        coEvery { settings.save(any(), any()) } returns AdminResult.Ok(Unit)
        val vm = AdminSiteSettingsViewModel(auth, transfer, preview, settings, session)
        vm.load()
        vm.updateField("site_title", "new")
        vm.save()
        coVerify { settings.save(any(), match { patches -> patches.any { it.first == "site_title" && it.second == "new" } }) }
    }
}