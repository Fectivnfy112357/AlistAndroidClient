package com.textvision.alistclient.common.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SafeCrashHandlerTest {

    @Test fun sanitizeStackTraceReplacesBearerToken() {
        val handler = SafeCrashHandler(RuntimeEnvironment.getApplication(), null)
        val ex = RuntimeException("Authorization: Bearer abc123.def-456 in header")
        val sanitized = handler.sanitizeStackTrace(ex)
        assertTrue(sanitized.contains("Bearer ***"))
        assertTrue(!sanitized.contains("abc123.def-456"))
    }

    @Test fun sanitizeStackTraceReplacesPasswordEquals() {
        val handler = SafeCrashHandler(RuntimeEnvironment.getApplication(), null)
        val ex = RuntimeException("login failed: password=secret123 user=admin")
        val sanitized = handler.sanitizeStackTrace(ex)
        assertTrue(sanitized.contains("password=***"))
        assertTrue(!sanitized.contains("secret123"))
    }

    @Test fun sanitizeStackTraceReplacesPasswordColon() {
        val handler = SafeCrashHandler(RuntimeEnvironment.getApplication(), null)
        val ex = RuntimeException("body: {\"username\":\"u\",\"password\":\"myP@ss\"}")
        val sanitized = handler.sanitizeStackTrace(ex)
        assertTrue(sanitized.contains("password=***"))
        // The full secret (including the @ character) must be scrubbed, not just the word prefix.
        assertTrue(!sanitized.contains("myP@ss"))
        assertTrue(!sanitized.contains("@ss"))
    }

    @Test fun sanitizeStackTraceReplacesTokenEquals() {
        val handler = SafeCrashHandler(RuntimeEnvironment.getApplication(), null)
        val ex = RuntimeException("header: token=eyJhbGc.payload.sig extra")
        val sanitized = handler.sanitizeStackTrace(ex)
        assertTrue(sanitized.contains("token=***"))
        assertTrue(!sanitized.contains("eyJhbGc"))
    }

    @Test fun sanitizeStackTraceReplacesTokenColon() {
        val handler = SafeCrashHandler(RuntimeEnvironment.getApplication(), null)
        val ex = RuntimeException("config: token: 'abc.def-ghi'")
        val sanitized = handler.sanitizeStackTrace(ex)
        assertTrue(sanitized.contains("token=***"))
        assertTrue(!sanitized.contains("abc.def-ghi"))
    }

    @Test fun sanitizeStackTraceLeavesRegularTextUntouched() {
        val handler = SafeCrashHandler(RuntimeEnvironment.getApplication(), null)
        val ex = RuntimeException("just a normal error message with no secrets")
        val sanitized = handler.sanitizeStackTrace(ex)
        assertTrue(sanitized.contains("just a normal error message with no secrets"))
    }

    @Test fun retainsNewestTenFiles() {
        val context = RuntimeEnvironment.getApplication()
        val crashDir = File(context.filesDir, "crash_logs").apply { mkdirs() }
        // Clear any leftovers from previous tests.
        crashDir.listFiles()?.forEach { it.delete() }
        // Write 12 files with distinct, ascending timestamps so sort order is deterministic.
        val base = 1_700_000_000_000L
        for (i in 0 until 12) {
            File(crashDir, "crash-test-${base + i}.log").writeText("crash $i")
        }
        // Invoke the real retention helper that production code uses.
        SafeCrashHandler.enforceRetention(crashDir, SafeCrashHandler.MAX_CRASH_LOGS)

        val remaining = crashDir.listFiles().orEmpty()
        assertEquals(10, remaining.size)
        // The oldest two (indices 0 and 1) should be gone; the newest (index 11) should remain.
        assertTrue(remaining.none { it.name.contains("${base + 0}.log") })
        assertTrue(remaining.none { it.name.contains("${base + 1}.log") })
        assertTrue(remaining.any { it.name.contains("${base + 11}.log") })
    }

    @Test fun retainsAllFilesWhenTenOrFewer() {
        val context = RuntimeEnvironment.getApplication()
        val crashDir = File(context.filesDir, "crash_logs").apply { mkdirs() }
        crashDir.listFiles()?.forEach { it.delete() }
        val base = 1_700_000_000_000L
        for (i in 0 until 5) {
            File(crashDir, "crash-test-${base + i}.log").writeText("crash $i")
        }
        SafeCrashHandler.enforceRetention(crashDir, SafeCrashHandler.MAX_CRASH_LOGS)
        assertEquals(5, crashDir.listFiles().orEmpty().size)
    }
}
