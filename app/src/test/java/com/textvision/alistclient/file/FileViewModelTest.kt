package com.textvision.alistclient.file

import app.cash.turbine.test
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.file.model.FileUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileViewModelTest {
    private fun item(name: String, dir: Boolean = false, size: Long = 1) = FileItem(name, "/$name", dir, size, null, null, if (dir) FileType.Folder else FileType.Other, null, null)

    private inner class FakeRepo : FileRepositoryContract {
        var listResult: ApiResult<List<FileItem>> = ApiResult.Success(listOf(item("b.txt"), item("docs", true), item("a.txt")))
        override suspend fun list(path: String) = listResult
        override suspend fun search(path: String, keyword: String) = ApiResult.Success(listOf(item("match.txt")))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun foldersSortFirstByDefault() = runTest {
        val vm = FileViewModel(FakeRepo(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value as FileUiState.Success
        assertEquals(listOf("docs", "a.txt", "b.txt"), state.items.map { it.name })
    }

    @Test fun searchDebouncesAndShowsResult() = runTest {
        val vm = FileViewModel(FakeRepo(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        vm.updateSearchQuery("match")
        testScheduler.advanceTimeBy(299)
        assertEquals("match", vm.searchQuery.value)
        testScheduler.advanceTimeBy(1)
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value as FileUiState.Success
        assertEquals(listOf("match.txt"), state.items.map { it.name })
    }
}