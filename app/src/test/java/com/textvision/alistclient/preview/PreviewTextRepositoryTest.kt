package com.textvision.alistclient.preview

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreviewTextRepositoryTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    @Test fun fetchReturnsResponseText() = runTest {
        server.enqueue(MockResponse().setBody("hello preview"))
        val repo = PreviewTextRepository(OkHttpClient())

        val result = repo.fetch(server.url("/file.txt").toString())

        assertEquals("hello preview", result.getOrThrow())
    }

    @Test fun fetchFailsOnHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))
        val repo = PreviewTextRepository(OkHttpClient())

        val result = repo.fetch(server.url("/missing.txt").toString())

        assertTrue(result.isFailure)
    }
}
