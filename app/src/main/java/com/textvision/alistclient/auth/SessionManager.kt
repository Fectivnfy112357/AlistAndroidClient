package com.textvision.alistclient.auth

import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val credentialStore: CredentialStore,
    private val tokenProvider: AuthTokenProvider,
) {
    fun loadSavedSession(): SavedSession? {
        val serverUrl = credentialStore.readString(KEY_SERVER_URL) ?: return null
        val username = credentialStore.readString(KEY_USERNAME) ?: return null
        val password = credentialStore.readString(KEY_PASSWORD) ?: return null
        val token = credentialStore.readString(KEY_TOKEN) ?: return null
        tokenProvider.setToken(token)
        return SavedSession(serverUrl, username, password, token)
    }

    fun saveSession(session: SavedSession) {
        credentialStore.saveString(KEY_SERVER_URL, session.serverUrl)
        credentialStore.saveString(KEY_USERNAME, session.username)
        credentialStore.saveString(KEY_PASSWORD, session.password)
        credentialStore.saveString(KEY_TOKEN, session.token)
        tokenProvider.setToken(session.token)
    }

    fun clearToken() {
        credentialStore.remove(KEY_TOKEN)
        tokenProvider.clearToken()
    }

    fun clearSession() {
        credentialStore.clearAll()
        tokenProvider.clearToken()
    }

    companion object {
        const val KEY_SERVER_URL = "serverUrl"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_TOKEN = "token"
    }
}
