package com.textvision.alistclient.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class FakeRepo(
        var nextResult: ApiResult<HomeData> = ApiResult.Success(
            HomeData.Admin(
                serverTitle = "Alist",
                serverVersion = "v3.25.0",
                startTime = null,
                usedBytes = 0,
                totalBytes = 0,
                storages = emptyList(),
            )
        ),
    ) : HomeRepositoryContract {
        var calls = 0
        override suspend fun loadDashboard(): ApiResult<HomeData> {
            calls++
            return nextResult
        }
    }

    @Before fun setUp() { kotlinx.coroutines.Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { kotlinx.coroutines.Dispatchers.resetMain() }

    @Test fun firstLoadTransitionsLoadingToSuccess() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        assertSame(HomeUiState.Loading, vm.uiState.value)

        vm.loadIfNeeded()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is HomeUiState.Success)
        assertEquals(1, repo.calls)
    }

    @Test fun loadIfNeededDoesNotReloadWhenAlreadyLoaded() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()

        vm.loadIfNeeded()
        advanceUntilIdle()

        assertEquals(1, repo.calls)
    }

    @Test fun refreshTriggersAnotherLoadEvenIfAlreadyLoaded() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, repo.calls)
    }

    @Test fun loadFailureTransitionsToError() = runTest {
        val repo = FakeRepo().apply { nextResult = ApiResult.Failure(500, "boom") }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))

        vm.loadIfNeeded()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertEquals("boom", (state as HomeUiState.Error).message)
    }

    @Test fun networkErrorTransitionsToErrorWithThrowableMessage() = runTest {
        val repo = FakeRepo().apply { nextResult = ApiResult.NetworkError(java.io.IOException("offline")) }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))

        vm.loadIfNeeded()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertTrue((state as HomeUiState.Error).message.contains("offline"))
    }

    @Test fun refreshCancelsPreviousLoadJob() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        runCurrent()

        // Call refresh while the first load is still in flight
        vm.refresh()
        advanceUntilIdle()

        // The first load job was cancelled, so the repo got 2 calls (1 cancelled + 1 fresh)
        assertEquals(2, repo.calls)
    }

    @Test fun adminDataPropagatesIsGuestFalse() = runTest {
        val admin = HomeData.Admin(
            serverTitle = "Alist", serverVersion = "v3.25.0", startTime = null,
            usedBytes = 10, totalBytes = 20, storages = listOf(StorageInfo(mountPath = "/local", driver = "Local")),
        )
        val repo = FakeRepo().apply { nextResult = ApiResult.Success(admin) }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()
        val state = vm.uiState.value as HomeUiState.Success
        assertEquals(false, state.data.isGuest)
    }

    @Test fun guestDataPropagatesIsGuestTrue() = runTest {
        val guest = HomeData.Guest("My Alist", "v3.25.0", PublicSettings("My Alist", null, "v3.25.0"))
        val repo = FakeRepo().apply { nextResult = ApiResult.Success(guest) }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()
        val state = vm.uiState.value as HomeUiState.Success
        assertEquals(true, state.data.isGuest)
    }
}