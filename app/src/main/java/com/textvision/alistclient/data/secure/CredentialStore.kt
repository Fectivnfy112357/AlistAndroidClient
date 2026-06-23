package com.textvision.alistclient.data.secure

interface CredentialStore {
    fun saveString(key: String, value: String)
    fun readString(key: String): String?
    fun remove(key: String)
    fun clearAll()
}
