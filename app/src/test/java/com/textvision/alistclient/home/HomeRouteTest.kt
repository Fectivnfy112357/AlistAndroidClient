package com.textvision.alistclient.home

import com.textvision.alistclient.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class HomeRouteTest {
    @Test fun filesRouteEncodesPathWithSlashes() {
        assertEquals("files?path=%2Flocal", AppRoute.Files.create("/local"))
    }

    @Test fun filesRouteDefaultsToRoot() {
        assertEquals("files?path=%2F", AppRoute.Files.create())
    }

    @Test fun filesRouteRoundtripsEncodedPath() {
        val route = AppRoute.Files.create("/my/nested/folder")
        val decoded = URLDecoder.decode(route.substringAfter("path="), StandardCharsets.UTF_8.name())
        assertEquals("/my/nested/folder", decoded)
    }

    @Test fun homeRouteIsHome() {
        assertEquals("home", AppRoute.Home.route)
    }
}