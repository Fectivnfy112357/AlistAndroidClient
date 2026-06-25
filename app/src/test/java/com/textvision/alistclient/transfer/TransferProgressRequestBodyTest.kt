package com.textvision.alistclient.transfer

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferProgressRequestBodyTest {
    @Test
    fun writeToReportsCumulativeBytesAndTotalLength() {
        val body = "hello world".toRequestBody("text/plain".toMediaType())
        val events = mutableListOf<Pair<Long, Long>>()
        val progressBody = TransferProgressRequestBody(body) { bytesDone, totalBytes ->
            events += bytesDone to totalBytes
        }

        val sink = Buffer()
        progressBody.writeTo(sink)

        assertEquals("hello world", sink.readUtf8())
        assertEquals(listOf(11L to 11L), events)
    }
}
