package com.textvision.alistclient.ui.feature.home

import com.textvision.alistclient.ui.feature.home.dto.HomeData

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
