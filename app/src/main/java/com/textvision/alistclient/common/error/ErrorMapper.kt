package com.textvision.alistclient.common.error

import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

object ErrorMapper {
    fun mapThrowable(t: Throwable): AppError = when (t) {
        is CancellationException -> AppError.Cancelled
        is SSLHandshakeException -> AppError.CertificateUntrusted
        is SSLProtocolException -> AppError.SSLError
        is SSLException -> AppError.SSLError
        is UnknownHostException -> AppError.ServerUnreachable
        is ConnectException -> AppError.ServerUnreachable
        is SocketTimeoutException -> AppError.Timeout
        is IOException -> AppError.NetworkUnavailable
        else -> AppError.Unknown(t)
    }

    fun mapAlistFailure(code: Int, serverMessage: String?): AppError = when (code) {
        401 -> AppError.Unauthorized
        403 -> AppError.PermissionDenied
        404 -> AppError.NotFound
        409 -> AppError.Conflict
        in 500..599 -> AppError.OperationFailed(
            serverMessage?.takeIf { it.isNotBlank() } ?: "服务器错误 ($code)，请稍后重试"
        )
        else -> AppError.OperationFailed(
            serverMessage?.takeIf { it.isNotBlank() } ?: "操作失败 ($code)"
        )
    }
}
