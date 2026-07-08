package com.textvision.alistclient.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class AppErrorTest {

    @Test
    fun ioexception_converts_to_network() {
        val throwable = IOException("connect failed")
        val error = throwable.toAppError()
        assertEquals(AppError.Network("connect failed"), error)
    }

    @Test
    fun unknown_throwable_converts_to_unknown() {
        val throwable = IllegalStateException("boom")
        val error = throwable.toAppError()
        assertEquals(AppError.Unknown("boom"), error)
    }

}
