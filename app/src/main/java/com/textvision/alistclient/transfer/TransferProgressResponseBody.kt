package com.textvision.alistclient.transfer

import okhttp3.ResponseBody
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

class TransferProgressResponseBody(
    private val delegate: ResponseBody,
    private val minProgressBytes: Long = 64L * 1024L,
    private val onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
) : ResponseBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()
    override fun source(): BufferedSource {
        val total = contentLength()
        var readTotal = 0L
        var lastReported = 0L
        fun report(force: Boolean = false) {
            if ((force && readTotal != lastReported) || readTotal - lastReported >= minProgressBytes) {
                onProgress(readTotal, total)
                lastReported = readTotal
            }
        }
        return object : ForwardingSource(delegate.source()) {
            override fun read(sink: okio.Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read > 0) {
                    readTotal += read
                    report()
                } else if (read == -1L) {
                    report(force = true)
                }
                return read
            }
        }.buffer()
    }
}
