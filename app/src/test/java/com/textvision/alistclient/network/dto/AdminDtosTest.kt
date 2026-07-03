package com.textvision.alistclient.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminDtosTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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

    @Test fun userListParses() {
        val raw = """{"code":200,"message":"success","data":{"content":[{"id":1,"username":"admin","disabled":false,"role":"2"}],"total":1}}"""
        val resp = json.decodeFromString<AlistResponse<UserList>>(raw)
        assertEquals(200, resp.code)
        assertEquals(1, resp.data!!.content.size)
        assertEquals("admin", resp.data.content[0].username)
        assertEquals("2", resp.data.content[0].role)
    }

    @Test fun roleListParses() {
        val raw = """{"code":200,"message":"success","data":{"content":[{"id":2,"name":"admin","description":"管理员","default":false}],"total":1}}"""
        val resp = json.decodeFromString<AlistResponse<RoleList>>(raw)
        assertEquals("admin", resp.data!!.content[0].name)
    }

    @Test fun sessionInfoParses() {
        val raw = """{"code":200,"message":"success","data":[{"session_id":"abc","user_id":1,"last_active":1700000000,"status":0,"ua":"Mozilla/5.0","ip":"127.0.0.1"}]}"""
        val resp = json.decodeFromString<AlistResponse<List<SessionInfo>>>(raw)
        assertEquals(1, resp.data!!.size)
        assertEquals(0, resp.data[0].status)
        assertEquals("127.0.0.1", resp.data[0].ip)
    }

    @Test fun taskInfoParses() {
        val raw = """{"code":200,"message":"success","data":[{"id":"t1","name":"upload","state":"running","status":"uploading","progress":42.5,"total_bytes":1024,"error":""}]}"""
        val resp = json.decodeFromString<AlistResponse<List<TaskInfo>>>(raw)
        assertEquals("t1", resp.data!![0].id)
        assertEquals(42.5, resp.data[0].progress, 0.001)
    }

    @Test fun sessionInfoIgnoresExtraFields() {
        val raw = """{"code":200,"message":"success","data":[{"session_id":"x","user_id":2,"last_active":1,"status":1,"ua":"u","ip":"1.1.1.1","extra":"ignored"}]}"""
        val resp = json.decodeFromString<AlistResponse<List<SessionInfo>>>(raw)
        assertTrue(resp.data!!.isNotEmpty())
    }
}
