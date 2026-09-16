package com.textvision.alistclient.ui.feature.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.ui.feature.home.dto.HomeData
import kotlinx.coroutines.flow.StateFlow

interface HomeRepositoryContract {
    suspend fun loadDashboard(): ApiResult<HomeData>
    suspend fun retrySection(data: HomeData, key: SectionKey): HomeData

    // ── App-startup warm-up (see AppStartupWarmer) ────────────────────────
    //
    // The dashboard is the most expensive first-frame on Home tab. When the
    // user opens the app already logged in we pre-fetch the same payload on
    // a background coroutine and stash it in [warmCache]. The HomeViewModel
    // then reads [loadIfCached] instead of re-issuing the four network calls.
    // If the warm-up is stale (older than [maxAgeMs]) or failed, the cache
    // is treated as a miss and the VM falls back to [loadDashboard].

    /** Latest warm-up result, or null when nothing has been pre-fetched yet. */
    val warmCache: StateFlow<HomeData?>

    /** Trigger a background pre-fetch of the dashboard. Idempotent. */
    suspend fun warmUpDashboard()

    /** Returns the cached payload if it landed within the last [maxAgeMs] ms. */
    fun loadIfCached(maxAgeMs: Long): HomeData?
}