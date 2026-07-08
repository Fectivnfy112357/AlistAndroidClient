package com.textvision.alistclient.admin

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionEvent
import com.textvision.alistclient.auth.SessionEventBus
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val sessionEventBus: SessionEventBus,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun <T : Any> runAdmin(
        base: String,
        call: suspend () -> AlistResponse<T>,
    ): AdminResult<T> = withContext(dispatcher) {
        val first = safeCall(call)
        if (first is AdminResult.Ok) return@withContext first
        if (first is AdminResult.Unauthorized) {
            val refreshed = refreshAndRetry(base)
            if (refreshed) {
                val retried = safeCall(call)
                if (retried is AdminResult.Unauthorized) sessionEventBus.emit(SessionEvent.Unauthorized)
                return@withContext retried
            } else {
                sessionEventBus.emit(SessionEvent.Unauthorized)
            }
        }
        first
    }

    private suspend fun refreshAndRetry(base: String): Boolean {
        val session = sessionManager.loadSavedSession() ?: return false
        val username = session.username ?: return false
        val password = session.password ?: return false
        return when (val r = authRepository.login(base, username, password)) {
            is ApiResult.Success -> true
            else -> false
        }
    }

    private suspend fun <T : Any> safeCall(call: suspend () -> AlistResponse<T>): AdminResult<T> {
        return try {
            val resp = call()
            when {
                resp.code == 200 -> AdminResult.Ok(resp.data)
                resp.code == 401 || resp.code == 403 -> AdminResult.Unauthorized
                else -> AdminResult.ServerError(resp.code, resp.message)
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: HttpException) {
            val code = t.code()
            if (code == 401 || code == 403) AdminResult.Unauthorized else AdminResult.ServerError(code, t.message)
        } catch (t: Throwable) {
            AdminResult.Network
        }
    }
}
