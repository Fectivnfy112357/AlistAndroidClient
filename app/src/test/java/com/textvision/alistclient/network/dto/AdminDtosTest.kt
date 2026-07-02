package com.textvision.alistclient.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminDtosTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun adminInfoMapsFlatKeys() {
        val raw = """
            {"version":"v3.25.0","build_date":"2025-09-01","start_time":"2026-07-01T00:00:00Z",
             "used_bytes":1234,"total_bytes":5678}
        """.trimIndent()
        val info = json.decodeFromString<AdminInfo>(raw)
        assertEquals("v3.25.0", info.version)
        assertEquals("2025-09-01", info.buildDate)
        assertEquals("2026-07-01T00:00:00Z", info.startTime)
        assertEquals(1234L, info.usedBytes)
        assertEquals(5678L, info.totalBytes)
    }

    @Test fun storageInfoMapsSnakeCaseKeys() {
        val raw = """
            {"id":1,"mount_path":"/local","driver":"Local","status":"work",
             "used_bytes":100,"total_bytes":200}
        """.trimIndent()
        val info = json.decodeFromString<StorageInfo>(raw)
        assertEquals(1L, info.id)
        assertEquals("/local", info.mountPath)
        assertEquals("Local", info.driver)
        assertEquals("work", info.status)
        assertEquals(100L, info.usedBytes)
        assertEquals(200L, info.totalBytes)
    }

    @Test fun storageInfoToleratesMissingOptionalFields() {
        val raw = """{"mount_path":"/x"}"""
        val info = json.decodeFromString<StorageInfo>(raw)
        assertEquals("/x", info.mountPath)
        assertEquals("", info.driver)
        assertEquals(null, info.status)
        assertEquals(0L, info.usedBytes)
    }

    @Test fun publicSettingsMapsKnownKeys() {
        val raw = """{"title":"My Alist","logo":"/logo.svg","version":"v3.25.0"}"""
        val s = json.decodeFromString<PublicSettings>(raw)
        assertEquals("My Alist", s.title)
        assertEquals("/logo.svg", s.logo)
        assertEquals("v3.25.0", s.version)
    }
}