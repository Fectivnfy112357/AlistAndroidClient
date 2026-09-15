package com.textvision.alistclient.ui.feature.home.dto

import androidx.compose.runtime.Immutable

@Immutable
data class ServerStatsData(
    val userCount: Int,
    val roleCount: Int,
    val disabledUserCount: Int,
)
