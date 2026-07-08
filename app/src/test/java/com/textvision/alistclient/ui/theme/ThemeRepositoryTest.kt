package com.textvision.alistclient.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemeRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear DataStore before each test to avoid pollution from earlier tests
        // sharing the same Application context (and thus the same DataStore file).
        runBlocking {
            context.themeDataStore.edit { it.clear() }
        }
    }

    @Test
    fun default_is_system() = runTest {
        val repo = ThemeRepository(context)
        assertEquals(DarkMode.SYSTEM, repo.darkMode.first())
    }

    @Test
    fun set_then_read_returns_same() = runTest {
        val repo = ThemeRepository(context)
        repo.setDarkMode(DarkMode.DARK)
        assertEquals(DarkMode.DARK, repo.darkMode.first())
    }
}
