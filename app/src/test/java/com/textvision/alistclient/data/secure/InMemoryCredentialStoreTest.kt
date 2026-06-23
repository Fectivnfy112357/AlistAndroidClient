package com.textvision.alistclient.data.secure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryCredentialStoreTest {
    private class InMemoryCredentialStore : CredentialStore {
        private val values = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { values[key] = value }
        override fun readString(key: String): String? = values[key]
        override fun remove(key: String) { values.remove(key) }
        override fun clearAll() { values.clear() }
    }

    @Test
    fun saveReadRemoveAndClearWork() {
        val store = InMemoryCredentialStore()
        store.saveString("token", "abc")
        assertEquals("abc", store.readString("token"))

        store.remove("token")
        assertNull(store.readString("token"))

        store.saveString("serverUrl", "http://example.test/")
        store.saveString("username", "admin")
        store.clearAll()
        assertNull(store.readString("serverUrl"))
        assertNull(store.readString("username"))
    }
}
