package com.textvision.alistclient.music

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.musicDataStore by preferencesDataStore("music_prefs")

@Singleton
class MusicLibraryRootStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("library_root")

    val rootPath: Flow<String> = context.musicDataStore.data
        .map { prefs -> prefs[key]?.takeIf { it.isNotBlank() } ?: DEFAULT_ROOT }

    suspend fun setRoot(path: String) {
        val normalized = path.trim().ifEmpty { DEFAULT_ROOT }
        context.musicDataStore.edit { prefs ->
            prefs[key] = normalized
        }
    }

    companion object {
        const val DEFAULT_ROOT = "/我的音乐"
    }
}
