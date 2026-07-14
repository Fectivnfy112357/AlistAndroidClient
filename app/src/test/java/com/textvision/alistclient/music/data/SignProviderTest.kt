package com.textvision.alistclient.music.data

import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistFsGetData
import com.textvision.alistclient.network.dto.AlistResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class SignProviderTest {

    private val api: AlistApi = mockk()
    private val provider = SignProvider(api, Dispatchers.Unconfined)

    @Test
    fun returnsCachedSign_onSecondCall() = runTest {
        coEvery { api.fsGet(any(), any()) } returns
            AlistResponse(code = 200, message = "ok", data = AlistFsGetData(name = "x.mp3", sign = "abc"))
        val first = provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/")
        val second = provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/")
        assertEquals("abc", first)
        assertEquals("abc", second)
    }

    @Test
    fun returnsNull_whenServerFails() = runTest {
        coEvery { api.fsGet(any(), any()) } returns
            AlistResponse(code = 500, message = "fail", data = null)
        assertNull(provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
    }

    @Test
    fun returnsNull_onNetworkThrow() = runTest {
        coEvery { api.fsGet(any(), any()) } throws IOException("network")
        assertNull(provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
    }

    @Test
    fun invalidate_forcesRefetch() = runTest {
        coEvery { api.fsGet(any(), any()) } returnsMany listOf(
            AlistResponse(200, "ok", AlistFsGetData(name = "x.mp3", sign = "first")),
            AlistResponse(200, "ok", AlistFsGetData(name = "x.mp3", sign = "second")),
        )
        assertEquals("first", provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
        provider.invalidate("/x.mp3")
        assertEquals("second", provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
    }
}
