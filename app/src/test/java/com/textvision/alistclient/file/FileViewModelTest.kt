package com.textvision.alistclient.file

import android.net.Uri
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private fun item(name: String, dir: Boolean = false, size: Long = 1) =
        FileItem(name, "/$name", dir, size, null, null, if (dir) FileType.Folder else FileType.Other, null, null)

    private inner class FakeRepo : FileRepositoryContract {
        var listCalls = 0
        var listResult: ApiResult<List<FileItem>> = ApiResult.Success(
            listOf(item("b.txt"), item("docs", true), item("a.txt"))
        )
        override suspend fun list(path: String): ApiResult<List<FileItem>> {
            listCalls++
            return listResult
        }
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

    private fun newManager(): TransferManager = mockk(relaxed = true)

    @Test fun foldersSortFirstByDefault() = runTest {
        val vm = FileViewModel(FakeRepo(), newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value as FileUiState.Success
        assertEquals(listOf("docs", "a.txt", "b.txt"), state.items.map { it.name })
    }

    @Test fun loadIfNeededDoesNotReloadWhenContentAlreadyExistsForPath() = runTest {
        val repo = FakeRepo()
        val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()

        vm.loadIfNeeded("/")
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.listCalls)
        assertEquals(listOf("docs", "a.txt", "b.txt"), (vm.uiState.value as FileUiState.Success).items.map { it.name })
    }

    @Test fun searchDebouncesAndShowsResult() = runTest {
        val vm = FileViewModel(FakeRepo(), newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        vm.updateSearchQuery("match")
        testScheduler.advanceTimeBy(299)
        assertEquals("match", vm.searchQuery.value)
        testScheduler.advanceTimeBy(1)
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value as FileUiState.Success
        assertEquals(listOf("match.txt"), state.items.map { it.name })
    }

    @Test fun enqueueDownloadIsForwardedToTransferManager() = runTest {
        val manager = newManager()
        every { manager.enqueueDownload(any(), any()) } returns "dl-1"
        val vm = FileViewModel(FakeRepo(), manager, StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()

        vm.enqueueDownload(item("a.txt", dir = false))

        verify(exactly = 1) { manager.enqueueDownload("/a.txt", "a.txt") }
    }

    @Test fun enqueueDownloadIgnoresDirectories() = runTest {
        val manager = newManager()
        val vm = FileViewModel(FakeRepo(), manager, StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()

        vm.enqueueDownload(item("docs", dir = true))

        verify(exactly = 0) { manager.enqueueDownload(any(), any()) }
    }

    @Test fun enqueueUploadIsForwardedToTransferManagerWithCurrentPath() = runTest {
        val manager = newManager()
        every { manager.enqueueUpload(any(), any()) } returns "ul-1"
        val vm = FileViewModel(FakeRepo(), manager, StandardTestDispatcher(testScheduler))
        vm.load("/docs")
        testScheduler.advanceUntilIdle()

        val uri = mockk<Uri>(relaxed = true)
        vm.enqueueUpload(uri)

        verify(exactly = 1) { manager.enqueueUpload(uri, "/docs") }
    }
}
