package com.textvision.alistclient.navigation

import com.textvision.alistclient.file.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test fun previewRouteRoundTripsRemotePreviewMetadata() {
        val route = AppRoute.Preview.create(
            name = "a b.png",
            path = "/d/a b.png",
            type = FileType.Image,
            downloadUrl = "https://example.test/d/a%20b.png?sign=abc&x=1",
            size = 1234L,
        )

        val prefix = "preview/"
        val encoded = route.removePrefix(prefix)
        val args = AppRoute.Preview.decode(encoded)

        assertEquals("a b.png", args.name)
        assertEquals("/d/a b.png", args.path)
        assertEquals(FileType.Image, args.type)
        assertEquals("https://example.test/d/a%20b.png?sign=abc&x=1", args.downloadUrl)
        assertEquals(1234L, args.size)
    }
}
