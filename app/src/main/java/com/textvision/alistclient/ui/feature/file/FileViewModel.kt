package com.textvision.alistclient.ui.feature.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.FILE_WARM_TTL_MS
import com.textvision.alistclient.file.FileRepository
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FileViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val transferManager: TransferManager,
    private val networkMonitor: NetworkMonitorContract,
) : ViewModel() {

    private val _state = MutableStateFlow(FileUiState())

    val state: StateFlow<FileUiState> = combine(
        _state,
        networkMonitor.isOnline,
    ) { ui, online ->
        ui.copy(isOnline = online)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FileUiState(),
    )

    init {
        // Continuously observe the warmer's pre-fetched listings so a late
        // cache hit (splash gate still in flight when user first opens the
        // tab) still short-circuits the lazy load. We filter on the current
        // path so we don't poison the state with a pre-fetch for "/" when
        // the user is browsing "/docs".
        fileRepository.warmCache
            .onEach { cache ->
                val currentPath = _state.value.path.ifBlank { "/" }
                fileRepository.loadIfCached(currentPath, FILE_WARM_TTL_MS)?.let { items ->
                    applyCached(items, currentPath)
                }
            }
            .launchIn(viewModelScope)
    }

    // P0: load generation + dedicated Job. Earlier, `load` launched a new
    // coroutine on every resume and let late responses overwrite newer state
    // (e.g. tapping the refresh button while the prior request was still in
    // flight produced visible flicker when the older payload landed last). We
    // now cancel the prior job and tag the response so a late return is
    // discarded.
    private var loadJob: Job? = null
    private var loadGeneration: Int = 0

    // P0 (perf #40): debounce search input. Each keystroke previously fired
    // an immediate `_state.update { copy(query = ...) }` which re-ran the
    // `visibleFiles` filter and recomposed `FileListContent`. Typing "abc"
    // produced 3 filters; typing a Chinese phrase via the IME produced 8-12.
    // We now hold the in-flight query on a Job and only commit it 150ms after
    // the user stops typing, matching the typical IME commit interval.
    private var searchJob: Job? = null

    fun onIntent(intent: FileIntent) {
        when (intent) {
            is FileIntent.Load -> load(intent.path)
            is FileIntent.Search -> scheduleSearch(intent.query)
            is FileIntent.Upload -> transferManager.enqueueUpload(intent.uri, _state.value.path)
            is FileIntent.DownloadOne -> downloadOne(intent.path)
            is FileIntent.MultiSelectToggle -> toggleSelect(intent.path)
            is FileIntent.MultiSelectSet -> _state.update {
                it.copy(
                    selection = intent.paths,
                    isMultiSelectMode = intent.paths.isNotEmpty(),
                )
            }
            FileIntent.MultiSelectClear -> _state.update {
                it.copy(selection = emptySet(), isMultiSelectMode = false)
            }
            is FileIntent.MultiSelectDelete -> deleteSelected(intent.paths)
            is FileIntent.MultiSelectDownload -> downloadSelected(intent.paths)
        }
    }

    /**
     * Resume-friendly entry point. Skips the network round-trip when we
     * already have a successful cached listing for [path]; otherwise falls
     * through to a fresh load. Called from [FileScreen]'s `LaunchedEffect`,
     * which previously issued an unconditional Load on every resume (including
     * bottom-tab returns and Compose back-navigation) — paying a full server
     * request + UI refresh for a directory whose contents had not changed.
     *
     * When [path] differs from the last-loaded path we **also** reset the UI
     * to a blank loading state immediately. Without this, the SharedAxisX
     * navigation transition would show the *previous* directory's file list
     * for the full duration of the network round-trip (the ViewModel is
     * ViewModelStore-scoped, so navigating `/ → /我的照片` reuses the same
     * instance and the stale `state.files` remains visible until the new
     * listing lands). Clearing the list here lets the screen show the
     * "加载中…" placeholder during the animation, and the subsequent `load`
     * / `applyCached` populates it with the new directory's contents.
     */
    fun ensureLoaded(path: String) {
        val current = _state.value
        if (current.lastLoadedForPath == path && !current.isLoading) return

        // Path changed (or first entry / warm-cache miss): reset the visible
        // state so the navigation transition doesn't show the prior directory.
        if (current.lastLoadedForPath != path) {
            _state.update {
                it.copy(
                    path = path,
                    files = emptyList(),
                    isLoading = true,
                    error = null,
                )
            }
        }

        // App-startup warmer may have already pre-fetched this directory;
        // honour the cache before issuing a fresh network request. The TTL
        // is short (60s) — a stale hit just falls through to load(path).
        val cached = fileRepository.loadIfCached(path, FILE_WARM_TTL_MS)
        if (cached != null) {
            applyCached(cached, path)
            return
        }
        load(path)
    }

    private fun applyCached(cached: List<FileItem>, path: String) {
        _state.update {
            it.copy(
                path = path,
                files = cached,
                isLoading = false,
                error = null,
                selection = emptySet(),
                isMultiSelectMode = false,
                lastLoadedForPath = path,
            )
        }
    }

    private fun load(path: String) {
        loadJob?.cancel()
        val generation = ++loadGeneration
        _state.update { it.copy(path = path, isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = fileRepository.list(path)) {
                is ApiResult.Success -> if (generation == loadGeneration) {
                    _state.update {
                        it.copy(
                            path = path,
                            files = result.data,
                            isLoading = false,
                            error = null,
                            selection = emptySet(),
                            isMultiSelectMode = false,
                            lastLoadedForPath = path,
                        )
                    }
                }
                is ApiResult.Failure -> if (generation == loadGeneration) {
                    _state.update {
                        it.copy(isLoading = false, error = result.message.ifBlank { "加载失败 (${result.code})" })
                    }
                }
                is ApiResult.NetworkError -> if (generation == loadGeneration) {
                    _state.update {
                        it.copy(isLoading = false, error = result.cause.message ?: "网络错误")
                    }
                }
            }
        }
    }

    /**
     * Debounce search input by 150ms. Cancels the prior pending update so a
     * fast typist only commits one state change per pause, instead of one per
     * keystroke. On cancellation we still flush the latest value so the user
     * doesn't see a stale query on screen.
     */
    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(query = query) }
        }
    }

    private fun toggleSelect(path: String) {
        _state.update { current ->
            val newSelection = if (path in current.selection) current.selection - path else current.selection + path
            current.copy(
                selection = newSelection,
                isMultiSelectMode = newSelection.isNotEmpty(),
            )
        }
    }

    private fun deleteSelected(paths: List<String>) {
        if (paths.isEmpty()) return
        viewModelScope.launch {
            when (fileRepository.delete(paths)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(selection = emptySet(), isMultiSelectMode = false) }
                    // Mark the cache stale so the upcoming load actually fires;
                    // otherwise `ensureLoaded` would short-circuit on success.
                    _state.update { it.copy(lastLoadedForPath = null) }
                    load(_state.value.path)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = "删除失败 (${paths.size} 项)")
                }
                is ApiResult.NetworkError -> _state.update {
                    it.copy(error = "网络错误，删除失败")
                }
            }
        }
    }

    private fun downloadOne(path: String) {
        val file = _state.value.files.firstOrNull { it.path == path }
        if (file?.isDir == true) return
        val name = file?.name ?: path.substringAfterLast('/')
        transferManager.enqueueDownload(path, name)
    }

    private fun downloadSelected(paths: List<String>) {
        if (paths.isEmpty()) return
        val byPath: Map<String, FileItem> = _state.value.files.associateBy { it.path }
        paths.forEach { p ->
            val name = byPath[p]?.name ?: p.substringAfterLast('/')
            if (byPath[p]?.isDir != true) {
                transferManager.enqueueDownload(p, name)
            }
        }
        _state.update { it.copy(selection = emptySet(), isMultiSelectMode = false) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}
