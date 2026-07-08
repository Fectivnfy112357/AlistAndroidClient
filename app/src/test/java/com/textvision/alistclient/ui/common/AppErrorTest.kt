package com.textvision.alistclient.ui.common

import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AppErrorTest {

    @Test
    fun ioexception_converts_to_network() {
        val throwable = IOException("connect failed")
        val error = throwable.toAppError()
        assertEquals(AppError.Network("connect failed"), error)
    }

    @Test
    fun unknown_throwable_converts_to_unknown() {
        val throwable = IllegalStateException("boom")
        val error = throwable.toAppError()
        assertEquals(AppError.Unknown("boom"), error)
    }

    @Test
    fun http_401_converts_to_unauthorized() {
        val body = okhttp3.ResponseBody.Companion.create("text/plain".toMediaType(), "Unauthorized")
        val rawResponse = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("http://test/").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(body)
            .build()
        val response = Response.error<Any>(body, rawResponse)
        val httpException = HttpException(response)
        val error = httpException.toAppError()
        assertEquals(AppError.Unauthorized("Unauthorized"), error)
    }
}
