package com.textvision.alistclient.auth

import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
) {
    suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession> = try {
        val response = api.login(LoginRequest(username, password))
        if (response.code == 200 && response.data?.token?.isNotBlank() == true) {
            val session = SavedSession(serverUrl, username, password, response.data.token)
            sessionManager.saveSession(session)
            ApiResult.Success(session)
        } else {
            ApiResult.Failure(response.code, response.message)
        }
    } catch (t: Throwable) {
        ApiResult.NetworkError(t)
    }

    fun loadSavedSession(): SavedSession? = sessionManager.loadSavedSession()

    fun logout() {
        sessionManager.clearSession()
    }
}
