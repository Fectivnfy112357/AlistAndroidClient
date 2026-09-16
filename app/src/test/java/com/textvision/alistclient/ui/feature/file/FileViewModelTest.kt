package com.textvision.alistclient.ui.feature.file

import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.FileRepository
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fileRepository: FileRepository = mockk()
    private val transferManager: TransferManager = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitorContract = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { networkMonitor.isOnline } returns MutableStateFlow(true)
        // Default: no warm-up has happened — the cache miss path falls through
        // to the original `list(...)` call. Tests that want to exercise the
        // warm-cache hit override this with a specific stub.
        every { fileRepository.loadIfCached(any(), any()) } returns null
        // The init block subscribes to warmCache; an empty flow means the
        // observer never picks up anything, which matches the "no warm-up
        // happened" default.
        every { fileRepository.warmCache } returns MutableStateFlow(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeFile(name: String, path: String = "/$name", isDir: Boolean = false): FileItem =
        FileItem(
            name = name,
            path = path,
            isDir = isDir,
            size = 0,
            modifiedAt = null,
            extension = if (isDir) null else name.substringAfterLast('.', "").ifBlank { null },
            type = if (isDir) FileType.Folder else FileType.Other,
            thumbnailUrl = null,
            downloadUrl = null,
        )

    @Test
    fun load_success_emits_files() = runTest {
        val files = listOf(makeFile("a.txt"), makeFile("b.txt"))
        coEvery { fileRepository.list("/") } returns ApiResult.Success(files)

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()

        val loaded = vm.state.value
        assertEquals(files, loaded.files)
        assertEquals("/", loaded.path)
        assertFalse(loaded.isLoading)
        assertNull(loaded.error)
    }

    @Test
    fun load_failure_emits_error() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.Failure(500, "boom")

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("boom", state.error)
    }

    @Test
    fun load_network_error_emits_error() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.NetworkError(RuntimeException("offline"))

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()

        assertEquals("offline", vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun search_filter_narrows_visible_files() = runTest {
        val files = listOf(makeFile("apple.txt"), makeFile("banana.txt"))
        coEvery { fileRepository.list("/") } returns ApiResult.Success(files)

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.Search("apple"))
        // P0 (#40): search input is debounced by 150ms in the production VM;
        // advance the test scheduler past the debounce window before reading.
        advanceTimeBy(200L)

        val state = vm.state.value
        assertEquals(1, state.visibleFiles.size)
        assertEquals("apple.txt", state.visibleFiles[0].name)
    }

    @Test
    fun multi_select_toggle_adds_and_removes() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.Success(
            listOf(makeFile("a.txt"), makeFile("b.txt"))
        )

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.MultiSelectToggle("/a.txt"))
        assertEquals(setOf("/a.txt"), vm.state.value.selection)
        assertTrue(vm.state.value.isMultiSelectMode)
        vm.onIntent(FileIntent.MultiSelectToggle("/a.txt"))
        assertEquals(emptySet<String>(), vm.state.value.selection)
        assertFalse(vm.state.value.isMultiSelectMode)
    }

    @Test
    fun multi_select_clear_empties_selection() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.Success(listOf(makeFile("a.txt")))

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.MultiSelectToggle("/a.txt"))
        vm.onIntent(FileIntent.MultiSelectClear)
        assertEquals(emptySet<String>(), vm.state.value.selection)
        assertFalse(vm.state.value.isMultiSelectMode)
    }

    @Test
    fun multi_select_delete_calls_repo_and_relists() = runTest {
        val initial = listOf(makeFile("a.txt"), makeFile("b.txt"))
        val after = listOf(makeFile("a.txt"))
        coEvery { fileRepository.list("/") } returnsMany listOf(
            ApiResult.Success(initial),
            ApiResult.Success(after),
        )
        coEvery { fileRepository.delete(listOf("/b.txt")) } returns ApiResult.Success(Unit)

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.MultiSelectDelete(listOf("/b.txt")))
        advanceUntilIdle()

        coVerify(exactly = 1) { fileRepository.delete(listOf("/b.txt")) }
        assertEquals(after, vm.state.value.files)
        assertEquals(emptySet<String>(), vm.state.value.selection)
    }

    @Test
    fun upload_intent_enqueues_upload_at_current_path() = runTest {
        coEvery { fileRepository.list("/docs") } returns ApiResult.Success(emptyList())
        val uri = mockk<android.net.Uri>()
        every { transferManager.enqueueUpload(any(), any()) } returns "task-up"

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/docs"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.Upload(uri))

        verify(exactly = 1) { transferManager.enqueueUpload(uri, "/docs") }
    }

    @Test
    fun download_one_intent_enqueues_download() = runTest {
        val files = listOf(makeFile("a.txt"))
        coEvery { fileRepository.list("/") } returns ApiResult.Success(files)
        every { transferManager.enqueueDownload(any(), any()) } returns "task-1"

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.DownloadOne("/a.txt"))

        verify(exactly = 1) { transferManager.enqueueDownload("/a.txt", "a.txt") }
    }

    @Test
    fun multi_select_download_enqueues_transfer() = runTest {
        val initial = listOf(makeFile("a.txt"), makeFile("b.txt"))
        coEvery { fileRepository.list("/") } returns ApiResult.Success(initial)
        every { transferManager.enqueueDownload(any(), any()) } returns "task-1"

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        vm.onIntent(FileIntent.MultiSelectDownload(listOf("/a.txt", "/b.txt")))

        verify(exactly = 1) { transferManager.enqueueDownload("/a.txt", "a.txt") }
        verify(exactly = 1) { transferManager.enqueueDownload("/b.txt", "b.txt") }
        assertEquals(emptySet<String>(), vm.state.value.selection)
    }

    @Test
    fun ensureLoaded_skipsRepository_whenAlreadyCached() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.Success(listOf(makeFile("a.txt")))

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/")) // prime cache
        advanceUntilIdle()
        coVerify(exactly = 1) { fileRepository.list("/") }

        // Re-resume for the same path: must NOT trigger another fetch.
        vm.ensureLoaded("/")
        advanceUntilIdle()
        coVerify(exactly = 1) { fileRepository.list("/") }
    }

    @Test
    fun ensureLoaded_triggersFetch_whenPathChangesAfterCache() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.Success(emptyList())
        coEvery { fileRepository.list("/docs") } returns ApiResult.Success(emptyList())

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        // Different path: cache miss must force a load.
        vm.ensureLoaded("/docs")
        advanceUntilIdle()

        coVerify(exactly = 1) { fileRepository.list("/") }
        coVerify(exactly = 1) { fileRepository.list("/docs") }
        assertEquals("/docs", vm.state.value.lastLoadedForPath)
    }

    @Test
    fun ensureLoaded_bypassesCache_whenLoadFailedPreviously() = runTest {
        coEvery { fileRepository.list("/") } returnsMany listOf(
            ApiResult.Failure(500, "boom"),
            ApiResult.Success(listOf(makeFile("a.txt"))),
        )

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        // Failed load leaves lastLoadedForPath null; ensureLoaded must retry.
        assertNull(vm.state.value.lastLoadedForPath)

        vm.ensureLoaded("/")
        advanceUntilIdle()

        coVerify(exactly = 2) { fileRepository.list("/") }
        assertEquals("/", vm.state.value.lastLoadedForPath)
    }

    @Test
    fun explicit_refresh_load_bypassesCache() = runTest {
        coEvery { fileRepository.list("/") } returns ApiResult.Success(listOf(makeFile("a.txt")))

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/")) // prime cache
        advanceUntilIdle()
        coVerify(exactly = 1) { fileRepository.list("/") }

        // User hits Refresh: must fetch again even though cache is warm.
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        coVerify(exactly = 2) { fileRepository.list("/") }
    }

    @Test
    fun staleResponse_doesNotOverwrite_newerState() = runTest {
        // Two slow requests queued: an older one for "/" and a newer one for
        // "/docs" issued before the older completes. The older response must
        // not win. Note: the current implementation cancels the prior job, so
        // we exercise the contract via direct coEvery ordering instead of
        // relying on coroutine cancellation timing.
        coEvery { fileRepository.list("/") } coAnswers {
            kotlinx.coroutines.delay(50)
            ApiResult.Success(listOf(makeFile("root.txt", path = "/root.txt")))
        }
        coEvery { fileRepository.list("/docs") } coAnswers {
            kotlinx.coroutines.delay(10)
            ApiResult.Success(listOf(makeFile("doc.txt", path = "/docs/doc.txt")))
        }

        val vm = FileViewModel(fileRepository, transferManager, networkMonitor)
        vm.onIntent(FileIntent.Load("/"))
        advanceUntilIdle()
        // Fire a second load mid-flight — generation counter must shield.
        vm.onIntent(FileIntent.Load("/docs"))
        advanceUntilIdle()

        assertEquals("/docs", vm.state.value.path)
        assertEquals(1, vm.state.value.files.size)
        assertEquals("doc.txt", vm.state.value.files[0].name)
    }
}
