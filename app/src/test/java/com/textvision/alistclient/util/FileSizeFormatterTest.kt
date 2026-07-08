package com.textvision.alistclient.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun `zero bytes returns B`() = assertEquals("0 B", FileSizeFormatter.humanize(0))

    @Test
    fun `512 bytes returns B`() = assertEquals("512 B", FileSizeFormatter.humanize(512))

    @Test
    fun `2 KB returns 2_0 KB`() = assertEquals("2.0 KB", FileSizeFormatter.humanize(2048))

    @Test
    fun `5 MB returns 5_0 MB`() = assertEquals("5.0 MB", FileSizeFormatter.humanize(5L * 1024 * 1024))

    @Test
    fun `10 GB returns 10_00 GB`() = assertEquals("10.00 GB", FileSizeFormatter.humanize(10L * 1024 * 1024 * 1024))
}
