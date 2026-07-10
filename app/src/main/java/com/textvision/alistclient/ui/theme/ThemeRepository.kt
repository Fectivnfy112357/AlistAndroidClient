package com.textvision.alistclient.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DarkMode enum is declared in Theme.kt (single source of truth).

internal val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")
internal val DARK_MODE_KEY = stringPreferencesKey("dark_mode_override")

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val darkMode: Flow<DarkMode> = context.themeDataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY]?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() } ?: DarkMode.SYSTEM
    }

    suspend fun setDarkMode(mode: DarkMode) {
        context.themeDataStore.edit { it[DARK_MODE_KEY] = mode.name }
    }
}
