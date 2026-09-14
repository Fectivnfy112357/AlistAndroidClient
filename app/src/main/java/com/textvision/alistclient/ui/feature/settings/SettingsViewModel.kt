package com.textvision.alistclient.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.settings.SettingsRepositoryContract
import com.textvision.alistclient.admin.storage.StorageRepositoryContract
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.music.MusicLibraryRootStore
import com.textvision.alistclient.music.playback.MusicCache
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import com.textvision.alistclient.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val storages: List<StorageInfo> = emptyList(),
    val quickSettings: List<SettingItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transferManager: TransferManager,
    private val previewFileStore: PreviewFileStore,
    private val storageRepository: StorageRepositoryContract,
    private val settingsRepository: SettingsRepositoryContract,
    private val sessionManager: SessionManager,
    private val musicRootStore: MusicLibraryRootStore,
    private val musicCache: MusicCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun loadAdminData() {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val storageResult = storageRepository.list(base)
            val settingsResult = settingsRepository.list(base)
            val storages = (storageResult as? AdminResult.Ok)?.data?.content.orEmpty()
            val allItems = (settingsResult as? AdminResult.Ok)?.data.orEmpty().flatMap { it.items }
            val quick = allItems.filter { it.key in QUICK_KEYS }
            _uiState.update {
                it.copy(
                    storages = storages,
                    quickSettings = quick,
                    isLoading = false,
                    errorMessage = failureMessage(storageResult, settingsResult),
                )
            }
        }
    }

    fun toggleStorage(id: Long, enabled: Boolean) {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        val current = _uiState.value.storages.firstOrNull { it.id == id } ?: return
        val previous = current
        _uiState.update { state ->
            state.copy(storages = state.storages.map { if (it.id == id) it.copy(disabled = !enabled, status = if (enabled) "work" else "disabled") else it })
        }
        viewModelScope.launch {
            val patch = StoragePatch(
                id = id,
                mountPath = current.mountPath,
                driver = current.driver,
                disabled = !enabled,
                addition = current.addition ?: "{}",
            )
            val r = storageRepository.update(base, patch)
            _uiState.update { it.copy(errorMessage = failureMessage(r)) }
            loadAdminData()
        }
    }

    fun saveQuickSetting(key: String, value: String) {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            when (val r = settingsRepository.save(base, listOf(key to value))) {
                is AdminResult.Ok -> {
                    _uiState.update { state ->
                        state.copy(
                            quickSettings = state.quickSettings.map { if (it.key == key) it.copy(value = value) else it },
                            errorMessage = null,
                        )
                    }
                }
                else -> _uiState.update { it.copy(errorMessage = failureMessage(r)) }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout() {
        transferManager.clearAllTasks()
        authRepository.logout()
        // Off the main thread: previewDir can hold many MB of files, and
        // listFiles() + deleteRecursively() block the main thread if called
        // synchronously. Logout itself is fire-and-forget; the user has already
        // confirmed they want to leave.
        viewModelScope.launch(ioDispatcher) {
            previewFileStore.clearPreviewFiles()
        }
        _loggedOut.value = true
    }

    /**
     * P0 fix: run the preview-dir cleanup off the main thread. The previous
     * synchronous call did `previewDir().listFiles()` (one syscall per cached
     * preview) followed by `deleteRecursively()` (one syscall per nested file
     * inside), which blocked the UI thread for hundreds of ms on first
     * Settings open. Now we hop to [ioDispatcher] and return -1 as a sentinel
     * while the actual count is collected asynchronously.
     */
    fun clearPreviewFiles() {
        viewModelScope.launch(ioDispatcher) {
            previewFileStore.clearPreviewFiles()
        }
    }

    val musicRoot: StateFlow<String> = musicRootStore.rootPath.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicLibraryRootStore.DEFAULT_ROOT,
    )

    private val _musicCacheSize = MutableStateFlow(0L)
    val musicCacheSize: StateFlow<Long> = _musicCacheSize

    init {
        // P0: walk the music cache directory off the main dispatcher. The cache
        // lives under filesDir/music_cache and can hold hundreds of MB; the
        // recursive walk used to block the main thread on first Settings open
        // and was visible as a long-tail frame in `settings-scroll`.
        viewModelScope.launch {
            val size = withContext(ioDispatcher) { musicCache.sizeBytes }
            _musicCacheSize.value = size
        }
    }

    fun onMusicRootChange(path: String) = viewModelScope.launch {
        musicRootStore.setRoot(path)
    }

    fun onClearMusicCache() = viewModelScope.launch {
        // P0: clear() rebuilds the SimpleCache instance (acquires DB locks,
        // recreates the index). Keep it off the main dispatcher; the follow-up
        // `sizeBytes` walk is on the same dispatcher.
        withContext(ioDispatcher) {
            musicCache.clear()
            _musicCacheSize.value = musicCache.sizeBytes
        }
    }

    companion object {
        val QUICK_KEYS = setOf("site_title", "logo", "login_background", "announcement")

        private fun failureMessage(vararg results: AdminResult<*>): String? = results
            .firstOrNull { it !is AdminResult.Ok && it !is AdminResult.Unauthorized }
            ?.let { "加载失败：${it.javaClass.simpleName}" }
    }
}
