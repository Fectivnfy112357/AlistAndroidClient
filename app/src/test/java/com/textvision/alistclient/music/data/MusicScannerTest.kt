package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.MusicScanner.Parsed
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MusicScannerTest {

    private fun parse(name: String): Parsed? = MusicScanner(mockk(relaxed = true), kotlinx.coroutines.Dispatchers.Unconfined)
        .parseFileName(name)

    @Test
    fun parses_simpleName() {
        val p = parse("01-周杰伦-晴天.mp3")
        assertNotNull(p)
        assertEquals("01", p!!.trackNo)
        assertEquals(1, p.trackNoInt)
        assertEquals("晴天", p.title)
    }

    @Test
    fun parses_artistWithSpaces() {
        val p = parse("02-Jay Chou-晴天.flac")
        assertEquals("02", p!!.trackNo)
        assertEquals("晴天", p.title)
    }

    @Test
    fun parses_nonNumericTrackNo() {
        val p = parse("disc1-周杰伦-Intro.m4a")
        assertEquals("disc1", p!!.trackNo)
        assertNull(p.trackNoInt)
    }

    @Test
    fun rejects_onlyOneSegment() {
        assertNull(parse("周杰伦.mp3"))
    }

    @Test
    fun rejects_onlyTwoSegments() {
        assertNull(parse("01-周杰伦.mp3"))
    }

    @Test
    fun rejects_emptyTitle() {
        assertNull(parse("01-周杰伦-.mp3"))
    }

    @Test
    fun handlesTitleWithDashes() {
        val p = parse("05-Artist-Hello-World-Today.mp3")
        assertEquals("05", p!!.trackNo)
        assertEquals("Hello-World-Today", p.title)
    }

    @Test
    fun noExtension_stillParses() {
        val p = parse("01-A-T")
        assertEquals("T", p!!.title)
    }
}
