package com.textvision.alistclient.common.error

sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object ServerUnreachable : AppError
    data object NotAlistServer : AppError
    data object Unauthorized : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object Conflict : AppError
    data object Timeout : AppError
    data object SSLError : AppError
    data object CertificateUntrusted : AppError
    data object Cancelled : AppError
    data class OperationFailed(val message: String) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
