package com.textvision.alistclient.common.error

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class ErrorMapperTest {
    @Test fun mapsCommonThrowables() {
        assertEquals(AppError.ServerUnreachable, ErrorMapper.mapThrowable(ConnectException()))
        assertEquals(AppError.Timeout, ErrorMapper.mapThrowable(SocketTimeoutException()))
        assertEquals(AppError.CertificateUntrusted, ErrorMapper.mapThrowable(SSLHandshakeException("bad cert")))
        assertEquals(AppError.Cancelled, ErrorMapper.mapThrowable(CancellationException("cancelled")))
    }

    @Test fun mapsAlistFailures() {
        assertEquals(AppError.Unauthorized, ErrorMapper.mapAlistFailure(401, "unauthorized"))
        assertEquals(AppError.PermissionDenied, ErrorMapper.mapAlistFailure(403, "forbidden"))
        assertEquals(AppError.NotFound, ErrorMapper.mapAlistFailure(404, "not found"))
        assertEquals(AppError.Conflict, ErrorMapper.mapAlistFailure(409, "conflict"))
        assertTrue(ErrorMapper.mapAlistFailure(500, "boom") is AppError.OperationFailed)
    }
}
