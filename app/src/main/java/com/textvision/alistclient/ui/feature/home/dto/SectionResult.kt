package com.textvision.alistclient.ui.feature.home.dto

import androidx.compose.runtime.Immutable

// P3 follow-up: do NOT mark `sealed interface SectionResult` or `data class Ok<T>`
// as @Immutable. Two earlier attempts caused 30-60 ms jank spikes during fast
// continuous scrolling:
//
//  1. The interface itself — Compose treats @Immutable on sealed types as a
//     trust contract; when one of the cases is generic (`Ok<T>`) the
//     stability inference can take a more aggressive path that misses an
//     actual recomposition trigger.
//  2. `Ok<T>` specifically — `T` is unbounded; nothing in this file can
//     guarantee `T` is itself Immutable, so the claim is unsound.
//
// We keep @Immutable on the two leaf singletons (Loading, Failed) where the
// claim is trivially true.
sealed interface SectionResult<out T> {
    data class Ok<T>(val data: T) : SectionResult<T>

    @Immutable
    data object Loading : SectionResult<Nothing>

    @Immutable
    data class Failed(val cause: SectionFailure) : SectionResult<Nothing>
}

sealed interface SectionFailure {
    data object Network : SectionFailure
    data object Unauthorized : SectionFailure
    data class Server(val code: Int) : SectionFailure
}
