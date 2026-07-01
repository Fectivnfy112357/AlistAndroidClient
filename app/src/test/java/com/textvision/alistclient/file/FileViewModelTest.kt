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
import kotlinx.coroutines.CompletableDeferred
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
        var deletedPaths = emptyList<String>()
        var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var listResult: ApiResult<List<FileItem>> = ApiResult.Success(
            listOf(item("b.txt"), item("docs", true), item("a.txt"))
        )
        override suspend fun list(path: String): ApiResult<List<FileItem>> {
            listCalls++
            return listResult
        }
        override suspend fun search(path: String, keyword: String) = ApiResult.Success(listOf(item("match.txt")))
        override suspend fun delete(paths: List<String>): ApiResult<Unit> {
            deletedPaths = paths
            return deleteResult
        }
    }

    private inner class PausedRepo : FileRepositoryContract {
        var listCalls = 0
        private val releaseSecondLoad = CompletableDeferred<Unit>()
        override suspend fun list(path: String): ApiResult<List<FileItem>> {
            listCalls++
            if (listCalls > 1) releaseSecondLoad.await()
            return ApiResult.Success(listOf(item("loaded-${path.trim('/')}.txt")))
        }
        override suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>> = ApiResult.Success(emptyList())
        override suspend fun delete(paths: List<String>): ApiResult<Unit> = ApiResult.Success(Unit)
        fun release() { releaseSecondLoad.complete(Unit) }
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
        testScheduler.runCurrent()

        vm.loadIfNeeded("/")
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.listCalls)
        assertEquals(listOf("docs", "a.txt", "b.txt"), (vm.uiState.value as FileUiState.Success).items.map { it.name })
    }

    @Test fun loadKeepsExistingContentVisibleWhileLoadingAnotherPath() = runTest {
        val repo = PausedRepo()
        val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.runCurrent()

        vm.load("/docs")
        testScheduler.runCurrent()

        val state = vm.uiState.value as FileUiState.Success
        assertEquals("/", state.path)
        assertEquals(listOf("loaded-.txt"), state.items.map { it.name })

        repo.release()
        testScheduler.advanceUntilIdle()
        val loadedState = vm.uiState.value as FileUiState.Success
        assertEquals("/docs", loadedState.path)
        assertEquals(listOf("loaded-docs.txt"), loadedState.items.map { it.name })
    }

    @Test fun navigateBackFromNestedDirectoryLoadsParentInsteadOfExiting() = runTest {
        val repo = FakeRepo()
        val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()
        vm.load("/docs/sub")
        testScheduler.advanceUntilIdle()

        assertEquals(true, vm.canNavigateUp)
        assertEquals(true, vm.navigateUp())
        testScheduler.advanceUntilIdle()

        assertEquals("/docs", (vm.uiState.value as FileUiState.Success).path)
    }

    @Test fun navigateBackAtRootIsNotHandledByFileViewModel() = runTest {
        val vm = FileViewModel(FakeRepo(), newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()

        assertEquals(false, vm.canNavigateUp)
        assertEquals(false, vm.navigateUp())
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

    @Test fun deleteSuccessCallsRepositoryAndRefreshesCurrentDirectory() = runTest {
        val repo = FakeRepo()
        val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()
        repo.listResult = ApiResult.Success(listOf(item("remaining.txt")))

        vm.delete(item("a.txt"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("/a.txt"), repo.deletedPaths)
        assertEquals(2, repo.listCalls)
        assertEquals(listOf("remaining.txt"), (vm.uiState.value as FileUiState.Success).items.map { it.name })
    }

    @Test fun deleteFailureKeepsCurrentListVisible() = runTest {
        val repo = FakeRepo().apply { deleteResult = ApiResult.Failure(500, "remove failed") }
        val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()

        vm.delete(item("a.txt"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("/a.txt"), repo.deletedPaths)
        assertEquals(1, repo.listCalls)
        assertEquals(listOf("docs", "a.txt", "b.txt"), (vm.uiState.value as FileUiState.Success).items.map { it.name })
    }
}
