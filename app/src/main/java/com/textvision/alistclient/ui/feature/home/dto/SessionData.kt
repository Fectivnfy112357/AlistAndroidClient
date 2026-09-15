package com.textvision.alistclient.ui.feature.home.dto

import androidx.compose.runtime.Immutable

@Immutable
data class SessionData(
    val totalCount: Int,
    val activeCount: Int,
)
