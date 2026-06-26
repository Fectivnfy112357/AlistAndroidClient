package com.textvision.alistclient.transfer

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
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

    @Test
    fun writeToCoalescesFrequentProgressCallbacksAndStillReportsCompletion() {
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = 512L * 1024L
            override fun writeTo(sink: BufferedSink) {
                repeat(512) {
                    sink.write(ByteArray(1024))
                }
            }
        }
        val events = mutableListOf<Pair<Long, Long>>()
        val progressBody = TransferProgressRequestBody(body) { bytesDone, totalBytes ->
            events += bytesDone to totalBytes
        }

        progressBody.writeTo(Buffer())

        assertEquals(listOf(512L * 1024L to 512L * 1024L), events.takeLast(1))
        assertEquals(8, events.size)
    }
}
