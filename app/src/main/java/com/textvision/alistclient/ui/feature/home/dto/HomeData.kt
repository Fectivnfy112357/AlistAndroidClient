package com.textvision.alistclient.ui.feature.home.dto

import com.textvision.alistclient.network.dto.StorageInfo

// P3 follow-up: do NOT mark HomeData / StorageData as @Immutable. They have
// computed properties (`get()`) for `storages`, `isGuest`, `total`, etc., and
// the same observed regression (30-60 ms jank spikes during continuous
// scrolling) appeared when these were @Immutable — Compose's stronger
// stability inference for the @Immutable case interacted badly with the
// getters running on every recomposition read.
//
// The plain `data class` form keeps the original (working) stability path:
// unstable by default, so callers decide per-field whether to wrap with
// `remember(...)`.
data class HomeData(
    val publicSection: SectionResult<PublicData>,
    val storageSection: SectionResult<StorageData>,
    val serverStatsSection: SectionResult<ServerStatsData>,
    val sessionSection: SectionResult<SessionData>,
    val taskSection: SectionResult<TaskData>,
) {
    /** Storage-only helper for legacy UI. Empty when storageSection is Failed. */
    val storages: List<StorageInfo>
        get() = (storageSection as? SectionResult.Ok)?.data?.storages ?: emptyList()

    val isGuest: Boolean
        get() = storageSection is SectionResult.Failed
}

data class StorageData(val storages: List<StorageInfo>) {
    val total: Int get() = storages.size
    val working: Int get() = storages.count { it.status == "work" }
    val abnormal: Int get() = total - working
}
