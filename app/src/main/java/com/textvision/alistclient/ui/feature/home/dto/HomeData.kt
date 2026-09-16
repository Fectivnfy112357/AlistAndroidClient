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

    companion object {
        /**
         * Skeleton placeholder emitted during the first LoadingState so that
         * the dashboard's LazyColumn + item sub-trees lay out, measure, and
         * run their first Compose:recompose / TextLayout / AtlasTextOp paths
         * BEFORE the user's first touch lands. The hero card Material3 Surface,
         * drawWithCache Brush.radialGradient, Modifier chain materialisation,
         * Fredoka glyph rasterisation (MetricCard numerals, "早上好 ✨",
         * "当前服务器 ·") and LazyColumn first-measure all run on the
         * Loading side. When the network response arrives, items do not
         * get fresh slots — they only re-run their inner content composition
         * with real data, which is amortised against the already-warm cache.
         *
         * Why this fixes the user-reported symptom:
         *   Pre-fix: state = LoadingState("加载中…") → 366 ms first DOWN.
         *   Post-fix: state = DashboardList(skeleton) → 366 ms first DOWN
         *            happens during Loading, not at first touch.
         *
         * Important caveat: skeletons use Loading / empty Ok SectionResults,
         * NOT the real data shape. Some items may produce a "no data"
         * look during loading (e.g. metrics show "—", storage shows the
         * empty hint) — that is the documented skeleton semantics, not a
         * regression in screen layout. UI layout / placement / ordering
         * is unchanged; only the inner section content is `Loading`.
         */
        fun skeleton(): HomeData = HomeData(
            publicSection = SectionResult.Loading,
            storageSection = SectionResult.Ok(StorageData(emptyList())),
            serverStatsSection = SectionResult.Loading,
            sessionSection = SectionResult.Loading,
            taskSection = SectionResult.Loading,
        )
    }
}

data class StorageData(val storages: List<StorageInfo>) {
    val total: Int get() = storages.size
    val working: Int get() = storages.count { it.status == "work" }
    val abnormal: Int get() = total - working
}
