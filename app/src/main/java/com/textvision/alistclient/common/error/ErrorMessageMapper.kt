package com.textvision.alistclient.common.error

object ErrorMessageMapper {
    fun toUserMessage(error: AppError): String = when (error) {
        AppError.NetworkUnavailable -> "网络不可用，请检查 WiFi 或移动数据"
        AppError.ServerUnreachable -> "无法连接服务器，请检查地址和网络"
        AppError.NotAlistServer -> "该地址不是 Alist 服务"
        AppError.Unauthorized -> "登录已失效，请重新登录"
        AppError.PermissionDenied -> "没有访问权限"
        AppError.NotFound -> "文件不存在"
        AppError.Conflict -> "操作冲突，请重试"
        AppError.Timeout -> "请求超时，请重试"
        AppError.SSLError -> "TLS 握手失败，服务器证书可能不受信任"
        AppError.CertificateUntrusted -> "服务器证书不受信任（过期/自签/主机名不匹配）"
        AppError.Cancelled -> ""
        is AppError.OperationFailed -> error.message
        is AppError.Unknown -> "出错了，请重试"
    }

    fun toActionLabel(error: AppError): String? = when (error) {
        AppError.NetworkUnavailable,
        AppError.ServerUnreachable,
        AppError.Timeout,
        AppError.NotFound,
        AppError.Conflict -> "重试"
        AppError.Unauthorized -> "重新登录"
        else -> null
    }

    fun isRetryable(error: AppError): Boolean = when (error) {
        AppError.NetworkUnavailable,
        AppError.ServerUnreachable,
        AppError.Timeout,
        AppError.NotFound,
        AppError.Conflict -> true
        else -> false
    }
}
