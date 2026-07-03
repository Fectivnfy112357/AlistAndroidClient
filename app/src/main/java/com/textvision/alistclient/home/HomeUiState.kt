package com.textvision.alistclient.home

import com.textvision.alistclient.home.dto.HomeData

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
