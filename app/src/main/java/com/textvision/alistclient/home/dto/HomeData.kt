package com.textvision.alistclient.home.dto

import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageInfo
import kotlinx.datetime.Instant

/**
 * All values `HomeScreen` needs, regardless of which combination of
 * endpoints succeeded. `isGuest` is true when at least one admin call
 * returned 401/403 and we fell back to public/settings.
 */
sealed interface HomeData {
    val serverTitle: String
    val serverVersion: String?
    val isGuest: Boolean

    data class Admin(
        override val serverTitle: String,
        override val serverVersion: String?,
        val startTime: Instant?,
        val usedBytes: Long,
        val totalBytes: Long,
        val storages: List<StorageInfo>,
        override val isGuest: Boolean = false,
    ) : HomeData

    data class Guest(
        override val serverTitle: String,
        override val serverVersion: String?,
        val publicSettings: PublicSettings,
    ) : HomeData {
        override val isGuest: Boolean = true
    }
}

sealed interface HomeFailure {
    /** public/settings also failed; show Error UiState with retry */
    data class Unreachable(val message: String) : HomeFailure
}