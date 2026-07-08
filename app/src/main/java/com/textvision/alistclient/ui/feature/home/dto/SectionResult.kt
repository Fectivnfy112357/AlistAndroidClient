package com.textvision.alistclient.ui.feature.home.dto

sealed interface SectionResult<out T> {
    data class Ok<T>(val data: T) : SectionResult<T>
    data object Loading : SectionResult<Nothing>
    data class Failed(val cause: SectionFailure) : SectionResult<Nothing>
}

sealed interface SectionFailure {
    data object Network : SectionFailure
    data object Unauthorized : SectionFailure
    data class Server(val code: Int) : SectionFailure
}
