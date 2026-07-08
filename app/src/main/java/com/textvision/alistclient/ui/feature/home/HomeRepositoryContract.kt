package com.textvision.alistclient.ui.feature.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.ui.feature.home.dto.HomeData

interface HomeRepositoryContract {
    suspend fun loadDashboard(): ApiResult<HomeData>
    suspend fun retrySection(data: HomeData, key: SectionKey): HomeData
}