package com.textvision.alistclient.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.error.ErrorMapper
import com.textvision.alistclient.common.error.ErrorMessageMapper
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.util.ServerUrlNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AuthRepositoryContract {
    suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession>
}

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val showHttpWarning: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryContract,
) : ViewModel() {
    constructor(authRepository: AuthRepositoryContract, dispatcher: CoroutineDispatcher) : this(authRepository) {
        this.dispatcher = dispatcher
    }

    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value, showHttpWarning = ServerUrlNormalizer.isHttp(value), errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit = {}) {
        val current = _uiState.value
        val normalized = ServerUrlNormalizer.normalize(current.serverUrl).getOrElse { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "服务器地址不合法") }
            return
        }
        if (current.username.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "用户名和密码不能为空") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = withContext(dispatcher) { authRepository.login(normalized, current.username.trim(), current.password) }) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Failure -> {
                    val error = ErrorMapper.mapAlistFailure(result.code, result.message)
                    val message = if (result.code == 401 || result.code == 400) "用户名或密码错误" else ErrorMessageMapper.toUserMessage(error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
                is ApiResult.NetworkError -> {
                    val error = ErrorMapper.mapThrowable(result.cause)
                    _uiState.update { it.copy(isLoading = false, errorMessage = ErrorMessageMapper.toUserMessage(error)) }
                }
            }
        }
    }
}
