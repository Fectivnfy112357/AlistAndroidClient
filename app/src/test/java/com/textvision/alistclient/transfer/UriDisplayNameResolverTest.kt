package com.textvision.alistclient.transfer

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriDisplayNameResolverTest {
    @Test fun fallbackUsesLastPathSegmentBeforeDefaultName() {
        val uri = Uri.parse("content://provider/tree/photo.jpg")
        assertEquals("photo.jpg", UriDisplayNameResolver.fallbackName(uri, 123L))
    }

    @Test fun fallbackUsesTimestampWhenNoSegment() {
        val uri = Uri.parse("content://provider")
        assertEquals("upload-123", UriDisplayNameResolver.fallbackName(uri, 123L))
    }
}
