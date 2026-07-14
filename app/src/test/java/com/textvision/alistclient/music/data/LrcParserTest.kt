package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.model.LrcLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parses_singleTimeTagLine() {
        val lrc = "[00:01.50]Hello"
        assertEquals(listOf(LrcLine(1_500, "Hello")), LrcParser.parse(lrc))
    }

    @Test
    fun parses_minuteSecondOnly_noMillis() {
        val lrc = "[01:30]Line A"
        assertEquals(listOf(LrcLine(90_000, "Line A")), LrcParser.parse(lrc))
    }

    @Test
    fun parses_multipleTimeTags_oneLine() {
        val lrc = "[00:01.00][00:30.00]chorus"
        val actual = LrcParser.parse(lrc)
        assertEquals(listOf(LrcLine(1_000, "chorus"), LrcLine(30_000, "chorus")), actual)
    }

    @Test
    fun ignoresMetadataTags() {
        val lrc = """
            [ti:Title]
            [ar:Artist]
            [al:Album]
            [00:01.00]First
        """.trimIndent()
        assertEquals(listOf(LrcLine(1_000, "First")), LrcParser.parse(lrc))
    }

    @Test
    fun sortsByTime() {
        val lrc = """
            [00:30.00]Late
            [00:05.00]Early
        """.trimIndent()
        assertEquals(listOf(LrcLine(5_000, "Early"), LrcLine(30_000, "Late")), LrcParser.parse(lrc))
    }

    @Test
    fun emptyInput_returnsEmpty() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n\n   ").isEmpty())
    }

    @Test
    fun lineWithoutAnyTag_isSkipped() {
        val lrc = """
            no tag here

            [00:01.00]valid
        """.trimIndent()
        assertEquals(listOf(LrcLine(1_000, "valid")), LrcParser.parse(lrc))
    }

    @Test
    fun handlesThreeDigitMillis() {
        val lrc = "[00:01.123]Tick"
        assertEquals(listOf(LrcLine(1_123, "Tick")), LrcParser.parse(lrc))
    }

    @Test
    fun handlesZeroBoundary() {
        val lrc = "[00:00.00]Start"
        assertEquals(listOf(LrcLine(0, "Start")), LrcParser.parse(lrc))
    }
}
