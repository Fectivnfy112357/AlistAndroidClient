package com.textvision.alistclient.admin

sealed interface AdminResult<out T> {
    data class Ok<T>(val data: T?) : AdminResult<T>
    data object Unauthorized : AdminResult<Nothing>
    data class ServerError(val code: Int, val message: String? = null) : AdminResult<Nothing>
    data object Network : AdminResult<Nothing>
}
