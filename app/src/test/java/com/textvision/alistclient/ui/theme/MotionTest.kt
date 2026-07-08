package com.textvision.alistclient.ui.theme

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTest {

    @Test
    fun spring_specs_are_not_null() {
        assertNotNull(CloudMotion.SpringFast)
        assertNotNull(CloudMotion.SpringMedium)
        assertNotNull(CloudMotion.SpringSlow)
    }

    @Test
    fun tween_durations_and_easing_are_defined() {
        assertEquals(120, CloudMotion.TweenShort)
        assertEquals(240, CloudMotion.TweenMedium)
        assertEquals(400, CloudMotion.TweenLong)
        assertNotNull(CloudMotion.Easing)
    }
}
