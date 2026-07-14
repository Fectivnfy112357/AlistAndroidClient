package com.textvision.alistclient.music.data

sealed interface MusicIndexState {
    data object NotIndexed : MusicIndexState
    data class Scanning(val artistsDone: Int, val songsFound: Int) : MusicIndexState
    data object Ready : MusicIndexState
    data class Failed(val message: String) : MusicIndexState
}
