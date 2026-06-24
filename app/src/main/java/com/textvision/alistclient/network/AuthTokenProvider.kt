package com.textvision.alistclient.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenProvider @Inject constructor() {
    @Volatile private var token: String? = null

    fun getToken(): String? = token
    fun setToken(value: String) { token = value }
    fun clearToken() { token = null }
}
