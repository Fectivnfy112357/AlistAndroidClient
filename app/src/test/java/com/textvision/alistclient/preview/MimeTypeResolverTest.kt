package com.textvision.alistclient.preview

import android.webkit.MimeTypeMap
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowMimeTypeMap

@RunWith(RobolectricTestRunner::class)
class MimeTypeResolverTest {
    @Before fun seedMimeTypeMap() {
        // Robolectric's default MimeTypeMap is empty; register the standard
        // mappings our resolver exercises so the test exercises real behavior.
        val shadow = Shadow.extract(MimeTypeMap.getSingleton()) as ShadowMimeTypeMap
        shadow.clearMappings()
        shadow.addExtensionMimeTypeMapping("jpg", "image/jpeg")
        shadow.addExtensionMimeTypeMapping("jpeg", "image/jpeg")
        shadow.addExtensionMimeTypeMapping("txt", "text/plain")
        shadow.addExtensionMimeTypeMapping("text", "text/plain")
    }

    @Test fun infersKnownTypesAndFallsBack() {
        assertEquals("image/jpeg", MimeTypeResolver.infer("cat.jpg"))
        assertEquals("text/plain", MimeTypeResolver.infer("note.txt"))
        assertEquals("application/octet-stream", MimeTypeResolver.infer("file.unknownext"))
        assertEquals("application/octet-stream", MimeTypeResolver.infer("file"))
    }
}
