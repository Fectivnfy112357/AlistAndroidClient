package com.textvision.alistclient.di

import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class NetworkModuleTest {
    @Test
    fun okHttpLoggingDoesNotBufferLargeTransferBodies() {
        val client = NetworkModule.provideOkHttpClient(AuthInterceptor(AuthTokenProvider(), com.textvision.alistclient.auth.SessionEventBus()))
        val logging = client.interceptors.filterIsInstance<HttpLoggingInterceptor>().single()

        assertNotEquals(HttpLoggingInterceptor.Level.BODY, logging.level)
    }

    @Test
    fun okHttpAllowsLongRunningLargeFileTransfers() {
        val client = NetworkModule.provideOkHttpClient(AuthInterceptor(AuthTokenProvider(), com.textvision.alistclient.auth.SessionEventBus()))

        assertEquals(TimeUnit.SECONDS.toMillis(15).toInt(), client.connectTimeoutMillis)
        assertEquals(0, client.writeTimeoutMillis)
        assertEquals(TimeUnit.MINUTES.toMillis(10).toInt(), client.readTimeoutMillis)
    }
}
