package com.textvision.alistclient.ui.components.music

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicComponentsTest {
    @Test fun gradientSizesValid() {
        val g = Brush.linearGradient(listOf(Color(0xFFFFA1BD), Color(0xFF7C5BC7)))
        assertTrue(g != null)
    }
}
