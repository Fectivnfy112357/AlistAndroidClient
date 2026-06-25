package com.textvision.alistclient.transfer

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

class TransferProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var written = 0L
        val forwarding = object : ForwardingSink(sink) {
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                onProgress(written, total)
            }
        }
        val buffered = forwarding.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
    }
}
