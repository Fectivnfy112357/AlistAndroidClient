package com.textvision.alistclient.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionEventBusTest {
    @Test
    fun emit_delivers_event_to_collectors() = runTest {
        val bus = SessionEventBus()
        bus.events.test {
            bus.emit(SessionEvent.Unauthorized)
            assertEquals(SessionEvent.Unauthorized, awaitItem())
            bus.emit(SessionEvent.NetworkDown)
            assertEquals(SessionEvent.NetworkDown, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
