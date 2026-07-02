package com.textvision.alistclient.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.home.dto.HomeData

interface HomeRepositoryContract {
    suspend fun loadDashboard(): ApiResult<HomeData>
}