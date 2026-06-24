package com.textvision.alistclient.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInterceptorTest {
    @Test
    fun skipAuthRetryHeaderIsRemovedBeforeProceedButPreservedAsInternalMarker() {
        val tokenProvider = AuthTokenProvider()
        val interceptor = AuthInterceptor(tokenProvider)
        val initialRequest = Request.Builder()
            .url("https://example.test/upload")
            .header(SkipAuthRetry.HEADER, "true")
            .build()
        val chain = CapturingChain(initialRequest)

        interceptor.intercept(chain)

        val proceededRequest = requireNotNull(chain.proceededRequest)
        assertNull(proceededRequest.header(SkipAuthRetry.HEADER))
        assertTrue(SkipAuthRetry.shouldSkip(proceededRequest))
    }

    private class CapturingChain(
        private val initialRequest: Request,
    ) : Interceptor.Chain {
        var proceededRequest: Request? = null
            private set

        override fun request(): Request = initialRequest

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        override fun connection() = null
        override fun call() = throw UnsupportedOperationException("call is not used by this test")
        override fun connectTimeoutMillis(): Int = 0
        override fun readTimeoutMillis(): Int = 0
        override fun writeTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    }
}
