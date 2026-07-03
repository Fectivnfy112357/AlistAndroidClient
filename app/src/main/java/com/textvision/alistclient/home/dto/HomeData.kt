package com.textvision.alistclient.home.dto

import com.textvision.alistclient.network.dto.StorageInfo

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
