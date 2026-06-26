package com.textvision.alistclient.di

import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun okHttpLoggingDoesNotBufferLargeTransferBodies() {
        val client = NetworkModule.provideOkHttpClient(AuthInterceptor(AuthTokenProvider()))
        val logging = client.interceptors.filterIsInstance<HttpLoggingInterceptor>().single()

        assertNotEquals(HttpLoggingInterceptor.Level.BODY, logging.level)
    }
}
