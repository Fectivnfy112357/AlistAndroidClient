package com.textvision.alistclient.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        val request = chain.request()
        val skipAuth = request.header(SkipAuthRetry.HEADER) != null || SkipAuthRetry.shouldSkip(request)
        val builder = request.newBuilder()
        if (skipAuth) {
            SkipAuthRetry.mark(builder)
        }
        builder.removeHeader(SkipAuthRetry.HEADER)
        if (!skipAuth && !token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
