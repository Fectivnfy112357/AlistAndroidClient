package com.textvision.alistclient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ServerUrlNormalizerTest {
    @Test fun addsHttpWhenSchemeMissing() {
        assertEquals("http://textvision.top:5244/", ServerUrlNormalizer.normalize("textvision.top:5244").getOrThrow())
    }

    @Test fun preservesHttpsAndAddsTrailingSlash() {
        assertEquals("https://example.com/alist/", ServerUrlNormalizer.normalize(" https://example.com/alist ").getOrThrow())
    }

    @Test fun rejectsBlankAndNonHttpSchemes() {
        assertTrue(ServerUrlNormalizer.normalize("   ").isFailure)
        assertTrue(ServerUrlNormalizer.normalize("ftp://example.com").isFailure)
    }

    @Test fun detectsHttpForWarning() {
        assertTrue(ServerUrlNormalizer.isHttp("http://example.com"))
        assertFalse(ServerUrlNormalizer.isHttp("https://example.com"))
        assertFalse(ServerUrlNormalizer.isHttp("example.com"))
    }
}
