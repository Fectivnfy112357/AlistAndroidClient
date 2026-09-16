package com.textvision.alistclient.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.ui.feature.home.dto.HomeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepositoryContract,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    networkMonitor: NetworkMonitorContract,
) : ViewModel() {
    constructor(
        repository: HomeRepositoryContract,
        dispatcher: CoroutineDispatcher,
    ) : this(repository, dispatcher, StubNetworkMonitor())

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private var loadJob: Job? = null
    private var hasLoadedInitial = false

    init {
        // Continuously observe the warmer's pre-fetched payload — if the
        // splash gate hasn't completed when the user first opens the tab,
        // a late cache hit still short-circuits the lazy load. We take(1)
        // so a stale emission after the first successful load doesn't
        // overwrite user-visible state (e.g. a refresh in progress).
        repository.warmCache
            .filterNotNull()
            .take(1)
            .onEach { cached -> applyCached(cached) }
            .launchIn(viewModelScope)
    }

    fun loadIfNeeded() {
        if (hasLoadedInitial) return
        // App-startup warmer may have already pre-fetched the dashboard; if
        // so, skip the network round-trip and present the cached snapshot
        // immediately. The cache TTL is short (60s) — a stale hit just
        // falls through to the live load below.
        val cached = repository.loadIfCached(HOME_WARM_TTL_MS)
        if (cached != null) {
            applyCached(cached)
            return
        }
        load()
    }

    private fun applyCached(cached: HomeData) {
        if (hasLoadedInitial) return
        hasLoadedInitial = true
        _uiState.value = HomeUiState.Success(cached)
    }

    fun refresh() {
        hasLoadedInitial = false
        load(isRefresh = true)
    }

    fun retrySection(key: SectionKey) {
        val current = _uiState.value as? HomeUiState.Success ?: return
        viewModelScope.launch(dispatcher) {
            val updated = repository.retrySection(current.data, key)
            _uiState.value = HomeUiState.Success(updated)
        }
    }

    private fun load(isRefresh: Boolean = false) {
        loadJob?.cancel()
        if (isRefresh) _isRefreshing.value = true
        loadJob = viewModelScope.launch(dispatcher) {
            if (!isRefresh) _uiState.value = HomeUiState.Loading
            when (val result = repository.loadDashboard()) {
                is ApiResult.Success -> {
                    hasLoadedInitial = true
                    _uiState.value = HomeUiState.Success(result.data)
                }
                is ApiResult.Failure -> _uiState.value = HomeUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = HomeUiState.Error(result.cause.message ?: "网络错误")
            }
            if (isRefresh) _isRefreshing.value = false
        }
    }
}

private class StubNetworkMonitor : NetworkMonitorContract {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}
