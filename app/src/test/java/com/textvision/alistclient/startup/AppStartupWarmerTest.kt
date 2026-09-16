package com.textvision.alistclient.startup

import android.content.Context
import com.textvision.alistclient.auth.AuthRepositoryContract
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.file.FileRepositoryContract
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.playback.MusicCache
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.ui.feature.home.HomeRepositoryContract
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AppStartupWarmer]. Uses MockK to stand in for every
 * downstream collaborator and a [StandardTestDispatcher] so the warmer's
 * fire-and-forget coroutines can be drained deterministically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupWarmerTest {

    private val savedSession = SavedSession(
        serverUrl = "http://example.test",
        username = "user",
        password = "p",
        token = "t",
    )

    /**
     * Build the warmer with the given test dispatcher as its background
     * scheduler. We pass an application scope bound to the same dispatcher
     * so the warmer's supervisor job lives on the test scheduler — that's
     * what lets [advanceUntilIdle] drain the fan-out coroutines.
     */
    private fun buildWarmer(
        dispatcher: TestDispatcher,
        authRepo: AuthRepositoryContract,
        homeRepo: HomeRepositoryContract,
        fileRepo: FileRepositoryContract,
        musicIndexRepo: MusicIndexRepository,
        playback: PlaybackController,
        musicCache: MusicCache,
        transfer: TransferManager,
    ): AppStartupWarmer = AppStartupWarmer(
        context = mockk<Context>(relaxed = true),
        authRepository = authRepo,
        homeRepository = homeRepo,
        fileRepository = fileRepo,
        musicIndexRepository = musicIndexRepo,
        playbackController = playback,
        musicCache = musicCache,
        transferManager = transfer,
        ioDispatcher = dispatcher,
        applicationScope = CoroutineScope(dispatcher + SupervisorJob()),
    )

    private fun relaxedStubs(
        authRepo: AuthRepositoryContract,
        homeRepo: HomeRepositoryContract,
        fileRepo: FileRepositoryContract,
        musicIndexRepo: MusicIndexRepository,
        playback: PlaybackController,
        musicCache: MusicCache,
        transfer: TransferManager,
        loggedIn: Boolean,
    ) {
        every { authRepo.loadSavedSession() } returns if (loggedIn) savedSession else null
        coEvery { homeRepo.warmUpDashboard() } returns Unit
        coEvery { fileRepo.warmUp(any()) } returns Unit
        coEvery { musicIndexRepo.ensureIndexed() } returns Unit
        every { playback.warmUp(any()) } returns Unit
        every { musicCache.sizeBytes } returns 0L
        every { transfer.initialize() } returns Unit
    }

    private fun freshStubs(loggedIn: Boolean): TestStubs {
        val authRepo = mockk<AuthRepositoryContract>()
        val homeRepo = mockk<HomeRepositoryContract>()
        val fileRepo = mockk<FileRepositoryContract>()
        val musicIndexRepo = mockk<MusicIndexRepository>()
        val playback = mockk<PlaybackController>()
        val musicCache = mockk<MusicCache>()
        val transfer = mockk<TransferManager>()
        relaxedStubs(authRepo, homeRepo, fileRepo, musicIndexRepo, playback, musicCache, transfer, loggedIn)
        return TestStubs(authRepo, homeRepo, fileRepo, musicIndexRepo, playback, musicCache, transfer)
    }

    private data class TestStubs(
        val authRepo: AuthRepositoryContract,
        val homeRepo: HomeRepositoryContract,
        val fileRepo: FileRepositoryContract,
        val musicIndexRepo: MusicIndexRepository,
        val playback: PlaybackController,
        val musicCache: MusicCache,
        val transfer: TransferManager,
    )

    @Test
    fun warmUpAllIsNoOpWhenLoggedOut() = runTest {
        val stubs = freshStubs(loggedIn = false)
        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        warmer.warmUpAll()
        advanceUntilIdle()

        coVerify(exactly = 0) { stubs.homeRepo.warmUpDashboard() }
        coVerify(exactly = 0) { stubs.fileRepo.warmUp(any()) }
        coVerify(exactly = 0) { stubs.musicIndexRepo.ensureIndexed() }
        verify(exactly = 0) { stubs.playback.warmUp(any()) }
        verify(exactly = 0) { stubs.transfer.initialize() }
    }

    @Test
    fun warmUpAllFansOutToAllTabsWhenLoggedIn() = runTest {
        val stubs = freshStubs(loggedIn = true)
        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        warmer.warmUpAll()
        advanceUntilIdle()

        coVerify(exactly = 1) { stubs.homeRepo.warmUpDashboard() }
        coVerify(exactly = 1) { stubs.fileRepo.warmUp("/") }
        coVerify(exactly = 1) { stubs.musicIndexRepo.ensureIndexed() }
        verify(exactly = 1) { stubs.playback.warmUp(any()) }
        verify(exactly = 1) { stubs.transfer.initialize() }
        verify(exactly = 1) { stubs.musicCache.sizeBytes }
    }

    @Test
    fun warmUpAllIsIdempotent() = runTest {
        val stubs = freshStubs(loggedIn = true)
        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        warmer.warmUpAll()
        warmer.warmUpAll()
        warmer.warmUpAll()
        advanceUntilIdle()

        // Only one fan-out despite three warmUpAll calls.
        coVerify(exactly = 1) { stubs.homeRepo.warmUpDashboard() }
        coVerify(exactly = 1) { stubs.fileRepo.warmUp("/") }
        coVerify(exactly = 1) { stubs.musicIndexRepo.ensureIndexed() }
    }

    @Test
    fun warmUpAllContinuesWhenHomeTabFails() = runTest {
        val stubs = freshStubs(loggedIn = true)
        // Override home to throw — every other tab must still be warmed.
        coEvery { stubs.homeRepo.warmUpDashboard() } throws RuntimeException("network down")

        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        warmer.warmUpAll()
        advanceUntilIdle()

        coVerify(exactly = 1) { stubs.fileRepo.warmUp("/") }
        coVerify(exactly = 1) { stubs.musicIndexRepo.ensureIndexed() }
        verify(exactly = 1) { stubs.playback.warmUp(any()) }
    }

    @Test
    fun warmUpAllSuspendsUntilAllStepsComplete() = runTest {
        val stubs = freshStubs(loggedIn = true)
        // home takes "a while" — verifier checks warmUpAll is still in flight
        // when the call returns control to the gate.
        coEvery { stubs.homeRepo.warmUpDashboard() } coAnswers {
            kotlinx.coroutines.delay(50)
            Unit
        }

        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        val job = kotlinx.coroutines.GlobalScope.launch(StandardTestDispatcher(testScheduler)) {
            warmer.warmUpAll()
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { stubs.homeRepo.warmUpDashboard() }
        coVerify(exactly = 1) { stubs.fileRepo.warmUp("/") }
        coVerify(exactly = 1) { stubs.musicIndexRepo.ensureIndexed() }
        verify(exactly = 1) { stubs.playback.warmUp(any()) }
        verify(exactly = 1) { stubs.transfer.initialize() }
        assertTrue("warmer.warmUpAll() should complete", job.isCompleted)
    }

    @Test
    fun progressFlowReportsAllStepsAndDoneFlag() = runTest {
        val stubs = freshStubs(loggedIn = true)
        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        // Before warmUpAll is called: progress is the default empty state.
        assertEquals(false, warmer.progress.value.done)

        warmer.warmUpAll()
        advanceUntilIdle()

        val finalProgress = warmer.progress.value
        assertEquals(6, finalProgress.totalSteps)
        assertEquals(6, finalProgress.completedSteps)
        assertTrue("progress.done should flip after warmUpAll returns", finalProgress.done)
    }

    @Test
    fun progressFlowIsZeroStepsWhenLoggedOut() = runTest {
        val stubs = freshStubs(loggedIn = false)
        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        warmer.warmUpAll()
        advanceUntilIdle()

        val finalProgress = warmer.progress.value
        assertEquals(0, finalProgress.totalSteps)
        assertTrue("done flag must still flip when no work to do", finalProgress.done)
        assertEquals(1f, finalProgress.fraction, 0f)
    }

    @Test
    fun warmUpAllReturnsSuccessEvenWhenStepsFail() = runTest {
        val stubs = freshStubs(loggedIn = true)
        coEvery { stubs.homeRepo.warmUpDashboard() } throws RuntimeException("network down")
        coEvery { stubs.fileRepo.warmUp(any()) } throws RuntimeException("network down")
        coEvery { stubs.musicIndexRepo.ensureIndexed() } throws RuntimeException("network down")

        val warmer = buildWarmer(
            StandardTestDispatcher(testScheduler),
            stubs.authRepo, stubs.homeRepo, stubs.fileRepo,
            stubs.musicIndexRepo, stubs.playback, stubs.musicCache, stubs.transfer,
        )

        val result = warmer.warmUpAll()
        advanceUntilIdle()

        // Each failed step still counts toward completion, so progress.done
        // flips and the splash gate can move on.
        assertTrue("warmUpAll must succeed even when every step fails", result.isSuccess)
        assertTrue(warmer.progress.value.done)
    }
}