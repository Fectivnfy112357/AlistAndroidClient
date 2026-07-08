package com.textvision.alistclient.ui.common

import androidx.compose.runtime.Composable
import retrofit2.HttpException
import java.io.IOException

sealed interface AppError {
    data class Network(val cause: String?) : AppError
    data class Server(val code: Int, val message: String) : AppError
    data class Unauthorized(val reason: String) : AppError
    data class Unknown(val message: String) : AppError
}

fun Throwable.toAppError(): AppError = when (this) {
    is IOException -> AppError.Network(message)
    is HttpException -> if (code() in 401..403) AppError.Unauthorized(message()) else AppError.Server(code(), message())
    is AppError -> this
    else -> AppError.Unknown(message ?: this::class.simpleName.orEmpty())
}

@Composable
fun AppError.userMessage(): String = when (this) {
    is AppError.Network -> "网络连接失败：${cause ?: "请检查网络"}"
    is AppError.Server -> "服务错误 $code：$message"
    is AppError.Unauthorized -> "请重新登录"
    is AppError.Unknown -> message.ifBlank { "未知错误" }
}
