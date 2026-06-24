package com.textvision.alistclient.util

import java.net.URI

object ServerUrlNormalizer {
    fun normalize(input: String): Result<String> = runCatching {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "服务器地址不能为空" }
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val uri = URI(withScheme)
        require(uri.scheme == "http" || uri.scheme == "https") { "仅支持 HTTP 或 HTTPS" }
        require(!uri.host.isNullOrBlank()) { "服务器地址不合法" }
        if (withScheme.endsWith('/')) withScheme else "$withScheme/"
    }

    fun isHttp(input: String): Boolean = input.trim().startsWith("http://", ignoreCase = true)
}
