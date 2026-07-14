package com.textvision.alistclient.ui.feature.music.dto

import com.textvision.alistclient.music.data.MusicIndexState

sealed interface UiIndexState {
    data object NotIndexed : UiIndexState
    data class Scanning(val artistsDone: Int, val songsFound: Int) : UiIndexState
    data object Ready : UiIndexState
    data class Failed(val message: String) : UiIndexState

    companion object {
        fun fromDomain(s: MusicIndexState): UiIndexState = when (s) {
            MusicIndexState.NotIndexed -> NotIndexed
            is MusicIndexState.Scanning -> Scanning(s.artistsDone, s.songsFound)
            MusicIndexState.Ready -> Ready
            is MusicIndexState.Failed -> Failed(s.message)
        }
    }
}
