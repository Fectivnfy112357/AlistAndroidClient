package com.textvision.alistclient.music

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicLibraryRootStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = MusicLibraryRootStore(context)

    @After
    fun tearDown() = runTest {
        store.setRoot(MusicLibraryRootStore.DEFAULT_ROOT)
    }

    @Test
    fun defaultValue_whenNoPrefSaved() = runTest {
        val actual = store.rootPath.first()
        assertEquals(MusicLibraryRootStore.DEFAULT_ROOT, actual)
    }

    @Test
    fun setRoot_persistsAcrossReads() = runTest {
        store.setRoot("/custom/music")
        assertEquals("/custom/music", store.rootPath.first())
    }
}
