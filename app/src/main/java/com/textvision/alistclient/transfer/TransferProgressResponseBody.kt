package com.textvision.alistclient.transfer

import okhttp3.ResponseBody
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

class TransferProgressResponseBody(
    private val delegate: ResponseBody,
    private val onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
) : ResponseBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()
    override fun source(): BufferedSource {
        val total = contentLength()
        var readTotal = 0L
        return object : ForwardingSource(delegate.source()) {
            override fun read(sink: okio.Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read > 0) {
                    readTotal += read
                    onProgress(readTotal, total)
                }
                return read
            }
        }.buffer()
    }
}
