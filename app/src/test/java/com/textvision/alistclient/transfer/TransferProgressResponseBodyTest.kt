package com.textvision.alistclient.transfer

import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferProgressResponseBodyTest {
    @Test
    fun sourceReportsCumulativeBytesAndTotalLength() {
        val body = "hello world".toResponseBody()
        val events = mutableListOf<Pair<Long, Long>>()
        val progressBody = TransferProgressResponseBody(body) { bytesDone, totalBytes ->
            events += bytesDone to totalBytes
        }

        val sink = Buffer()
        val source = progressBody.source()
        while (source.read(sink, 4L) != -1L) {
            // Read all bytes.
        }

        assertEquals("hello world", sink.readUtf8())
        assertEquals(11L to 11L, events.last())
    }
}
