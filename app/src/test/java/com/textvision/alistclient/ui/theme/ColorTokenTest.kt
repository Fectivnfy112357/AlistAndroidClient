package com.textvision.alistclient.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokenTest {
    @Test fun brandPrimaryIsSkyBlue() {
        // Sky Blue #6FB6FF
        assertEquals(Color(0xFF6FB6FF), Brand500)
    }
    @Test fun candyMint() {
        assertEquals(Color(0xFF9BE3C8), CandyMint)
    }
    @Test fun ink() {
        assertEquals(Color(0xFF1F3A5F), Ink)
    }
    @Test fun stateErrorDesaturated() {
        // Not pure red
        assertEquals(Color(0xFFF49AA1), StateError)
    }
}
