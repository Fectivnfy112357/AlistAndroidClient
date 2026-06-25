package com.textvision.alistclient.network

import okhttp3.Request

object SkipAuthRetry {
    const val HEADER = "X-Skip-Auth-Retry"

    private val marker = Marker::class.java

    fun shouldSkip(request: Request): Boolean = request.tag(marker) != null

    fun mark(requestBuilder: Request.Builder): Request.Builder =
        requestBuilder.tag(marker, Marker)

    private object Marker
}
