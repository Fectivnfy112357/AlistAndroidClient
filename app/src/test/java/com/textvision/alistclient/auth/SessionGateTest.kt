package com.textvision.alistclient.auth

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionGateTest {
    @Test
    fun unauthorized_event_clears_session_and_signals_nav() = runTest(UnconfinedTestDispatcher()) {
        val bus = SessionEventBus()
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val gate = SessionGate(bus, authRepository, backgroundScope)
        val navSignals = mutableListOf<Unit>()
        backgroundScope.launch { gate.navEvent.collect { navSignals.add(it) } }

        bus.emit(SessionEvent.Unauthorized)

        verify { authRepository.logout() }
        assertEquals(1, navSignals.size)
    }

    @Test
    fun network_down_event_sets_isNetworkDown() = runTest(UnconfinedTestDispatcher()) {
        val bus = SessionEventBus()
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val gate = SessionGate(bus, authRepository, backgroundScope)

        assertFalse(gate.isNetworkDown.value)
        bus.emit(SessionEvent.NetworkDown)

        assertTrue(gate.isNetworkDown.value)
        verify(exactly = 0) { authRepository.logout() }
    }
}
