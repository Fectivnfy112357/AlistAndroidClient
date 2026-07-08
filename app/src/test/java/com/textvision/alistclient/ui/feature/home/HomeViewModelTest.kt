package com.textvision.alistclient.ui.feature.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.ui.feature.home.dto.HomeData
import com.textvision.alistclient.ui.feature.home.dto.PublicData
import com.textvision.alistclient.ui.feature.home.dto.SectionResult
import com.textvision.alistclient.ui.feature.home.dto.ServerStatsData
import com.textvision.alistclient.ui.feature.home.dto.SessionData
import com.textvision.alistclient.ui.feature.home.dto.StorageData
import com.textvision.alistclient.ui.feature.home.dto.TaskData
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

private fun emptyHomeData(): HomeData = HomeData(
    publicSection = SectionResult.Ok(PublicData("Test", "v1", null, null, null, null, false)),
    storageSection = SectionResult.Ok(StorageData(emptyList())),
    serverStatsSection = SectionResult.Ok(ServerStatsData(0, 0, 0)),
    sessionSection = SectionResult.Ok(SessionData(0, 0)),
    taskSection = SectionResult.Ok(TaskData(0, 0, emptyList(), emptyList())),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class FakeRepo(
        var nextResult: ApiResult<HomeData> = ApiResult.Success(emptyHomeData()),
        var retryBehavior: (HomeData, SectionKey) -> HomeData = { d, _ -> d },
        var retryCalls: Int = 0,
        var loadDelayMs: Long = 0,
    ) : HomeRepositoryContract {
        var loadCalls = 0
        override suspend fun loadDashboard(): ApiResult<HomeData> {
            loadCalls++
            if (loadDelayMs > 0) kotlinx.coroutines.delay(loadDelayMs)
            return nextResult
        }
        override suspend fun retrySection(data: HomeData, key: SectionKey): HomeData {
            retryCalls++
            return retryBehavior(data, key)
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
        assertEquals(1, repo.loadCalls)
    }

    @Test fun loadIfNeededDoesNotReloadWhenAlreadyLoaded() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        vm.loadIfNeeded(); advanceUntilIdle()
        assertEquals(1, repo.loadCalls)
    }

    @Test fun refreshTriggersAnotherLoad() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        vm.refresh(); advanceUntilIdle()
        assertEquals(2, repo.loadCalls)
    }

    @Test fun refreshFlipsIsRefreshingTrueThenFalse() = runTest {
        val repo = FakeRepo(loadDelayMs = 1000L)
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        assertEquals(false, vm.isRefreshing.value)
        vm.refresh()
        // Refresh is now in flight; load uses a 1s delay so isRefreshing stays true.
        assertEquals(true, vm.isRefreshing.value)
        advanceUntilIdle()
        assertEquals(false, vm.isRefreshing.value)
    }

    @Test fun loadFailureTransitionsToError() = runTest {
        val repo = FakeRepo().apply { nextResult = ApiResult.Failure(500, "boom") }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        assertTrue(vm.uiState.value is HomeUiState.Error)
        assertEquals("boom", (vm.uiState.value as HomeUiState.Error).message)
    }

    @Test fun retrySectionUpdatesUiStateForThatKeyOnly() = runTest {
        val initial = emptyHomeData()
        val updatedStorage = initial.copy(
            storageSection = SectionResult.Ok(StorageData(listOf(
                com.textvision.alistclient.network.dto.StorageInfo(mountPath = "/x", driver = "Local", status = "work")
            ))),
        )
        val repo = FakeRepo().apply {
            nextResult = ApiResult.Success(initial)
            retryBehavior = { data, key -> if (key == SectionKey.Storage) updatedStorage else data }
        }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()

        vm.retrySection(SectionKey.Storage)
        advanceUntilIdle()

        val state = vm.uiState.value as HomeUiState.Success
        val storage = state.data.storageSection as SectionResult.Ok
        assertEquals(1, storage.data.storages.size)
        assertEquals("/x", storage.data.storages.first().mountPath)
        // public unchanged
        assertTrue(state.data.publicSection is SectionResult.Ok)
        assertEquals(1, repo.retryCalls)
    }

    @Test fun retrySectionNoopWhenStateIsNotSuccess() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        // before any load
        vm.retrySection(SectionKey.Storage)
        advanceUntilIdle()
        assertEquals(0, repo.retryCalls)
        assertSame(HomeUiState.Loading, vm.uiState.value)
    }
}
