package com.textvision.alistclient.startup

import android.content.Context
import com.textvision.alistclient.auth.AuthRepositoryContract
import com.textvision.alistclient.debug.TraceMarkers
import com.textvision.alistclient.di.ApplicationScope
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.file.FileRepositoryContract
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.playback.MusicCache
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.ui.feature.home.HomeRepositoryContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-startup warmer. When the app launches into an authenticated
 * session, this kicks off the heavy first-time work for every bottom-tab
 * ViewModel **before** the user is allowed to see the main UI (see
 * [com.textvision.alistclient.MainActivity]'s splash gate):
 *
 * - Home dashboard: pre-fetched into `HomeRepositoryContract.warmCache`
 *   so [com.textvision.alistclient.ui.feature.home.HomeViewModel] can serve
 *   the cached snapshot without re-issuing the four network calls.
 * - Files root listing: pre-fetched into `FileRepositoryContract.warmCache`
 *   so [com.textvision.alistclient.file.FileViewModel] can render "/"
 *   without an extra round-trip.
 * - Music index: `MusicIndexRepository.ensureIndexed` runs early so the
 *   first Music tab open finds Room already warm.
 * - Playback Service: `PlaybackController.warmUp` connects to the
 *   MediaSession in the background so the first play tap avoids the
 *   200-500 ms Service cold-start.
 * - Music cache size: `MusicCache.sizeBytes` walks the cache off the
 *   IO dispatcher so the Settings tab already knows the byte count.
 * - Transfer manager: `TransferManager.initialize` marks stale active tasks
 *   interrupted.
 *
 * Each tab's completion is published via [progress] so the splash can show
 * the user something readable (e.g. "已预热 4 / 6"). Failures in one tab do
 * not abort the others — every coroutine has its own `runCatching`. The
 * warmer is a **no-op** when there is no saved session (Login screen is up
 * and there's nothing to warm against). Idempotent across `MainActivity`
 * recreations via [started].
 */
@Singleton
class AppStartupWarmer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepositoryContract,
    private val homeRepository: HomeRepositoryContract,
    private val fileRepository: FileRepositoryContract,
    private val musicIndexRepository: MusicIndexRepository,
    private val playbackController: PlaybackController,
    private val musicCache: MusicCache,
    private val transferManager: TransferManager,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(
        SupervisorJob(applicationScope.coroutineContext[Job]) + ioDispatcher,
    )

    /** Live progress observed by the splash gate. */
    private val _progress = MutableStateFlow(WarmupProgress())
    val progress: StateFlow<WarmupProgress> = _progress.asStateFlow()

    /**
     * Suspend until all warm-up tasks finish (or fail). Idempotent: the
     * second call returns immediately. Returns [Result.success] when the
     * warmer was either fully run or already completed; returns
     * [Result.success] with empty progress when there is no session.
     *
     * The splash gate is responsible for imposing its own timeout
     * (`withTimeoutOrNull`); this function does not time out on its own.
     */
    suspend fun warmUpAll(): Result<Unit> {
        // alist: temporary jank instrumentation, see TraceMarkers.
        val warmCookie = TraceMarkers.begin("warm:all")
        try {
            return warmUpAllTracked()
        } finally {
            TraceMarkers.end("warm:all", warmCookie)
        }
    }

    private suspend fun warmUpAllTracked(): Result<Unit> {
        if (!started.compareAndSet(false, true)) {
            // Already running or finished. Wait for it.
            awaitInFlight()
            return Result.success(Unit)
        }
        if (authRepository.loadSavedSession() == null) {
            // No session → Login screen is up. Nothing to warm.
            _progress.value = WarmupProgress(totalSteps = 0, completedSteps = 0, done = true)
            return Result.success(Unit)
        }

        val steps = listOf<WarmupStep>(
            WarmupStep.HOME,
            WarmupStep.FILES,
            WarmupStep.MUSIC_INDEX,
            WarmupStep.PLAYBACK,
            WarmupStep.MUSIC_CACHE,
            WarmupStep.TRANSFER,
        )
        _progress.value = WarmupProgress(totalSteps = steps.size, completedSteps = 0, done = false)

        coroutineScope {
            val deferreds: List<Deferred<WarmupStep>> = steps.map { step ->
                scope.async {
                    try {
                        runStep(step)
                        step
                    } catch (t: Throwable) {
                        // One failed step must not poison the gate. We
                        // still mark it complete so the progress counter
                        // advances; the splash moves on.
                        step
                    } finally {
                        _progress.update { current ->
                            current.copy(completedSteps = current.completedSteps + 1)
                        }
                    }
                }
            }
            deferreds.awaitAll()
        }

        _progress.update { it.copy(done = true) }
        return Result.success(Unit)
    }

    private suspend fun runStep(step: WarmupStep) {
        // alist: temporary jank instrumentation, see TraceMarkers.
        val cookie = TraceMarkers.begin("warm:" + step.name)
        try {
            when (step) {
                WarmupStep.HOME -> homeRepository.warmUpDashboard()
                WarmupStep.FILES -> fileRepository.warmUp(ROOT_PATH)
                WarmupStep.MUSIC_INDEX -> musicIndexRepository.ensureIndexed()
                WarmupStep.PLAYBACK -> playbackController.warmUp(context)
                WarmupStep.MUSIC_CACHE -> musicCache.sizeBytes
                WarmupStep.TRANSFER -> transferManager.initialize()
            }
        } finally {
            TraceMarkers.end("warm:" + step.name, cookie)
        }
    }

    /**
     * Block until any in-flight warm-up finishes. Returns immediately if
     * the warmer is already idle. Used by the idempotency fast-path.
     */
    private suspend fun awaitInFlight() {
        while (!_progress.value.done) {
            kotlinx.coroutines.delay(16)
        }
    }

    private fun MutableStateFlow<WarmupProgress>.update(
        transform: (WarmupProgress) -> WarmupProgress,
    ) {
        value = transform(value)
    }

    private companion object {
        const val ROOT_PATH = "/"
    }
}

/** One concrete warm-up task tracked by [AppStartupWarmer]. */
enum class WarmupStep(val label: String) {
    HOME("首页"),
    FILES("文件"),
    MUSIC_INDEX("音乐库索引"),
    PLAYBACK("播放服务"),
    MUSIC_CACHE("音乐缓存"),
    TRANSFER("传输任务"),
}

/**
 * Snapshot of the warmer's progress, observed by the splash gate.
 *
 * @param totalSteps total number of [WarmupStep]s the warmer intends to run
 *   (0 when no session is present — splash should skip).
 * @param completedSteps number of steps that have finished (success or fail).
 * @param done true once [AppStartupWarmer.warmUpAll] has returned.
 */
data class WarmupProgress(
    val totalSteps: Int = 0,
    val completedSteps: Int = 0,
    val done: Boolean = false,
) {
    val fraction: Float
        get() = if (totalSteps == 0) 1f else completedSteps.toFloat() / totalSteps
}