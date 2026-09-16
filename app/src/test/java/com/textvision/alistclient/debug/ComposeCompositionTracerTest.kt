package com.textvision.alistclient.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the `IllegalArgumentException: sectionName is too long`
 * crash (2026-09-16).
 *
 * `android.os.Trace.beginSection` rejects names longer than 127 chars, and that
 * check only runs while an app-level trace is enabled. The debug
 * [ComposeCompositionTracer] forwards Compose's `info` string straight into
 * `beginSection`, and Compose's `info` for nested lambda composables (the
 * `subcompose` lambdas inside `Scaffold`) exceeds that limit. Result: the app
 * died on the first `atrace -a com.textvision.alistclient` capture — exactly the
 * tool the tracer exists to feed.
 */
class ComposeCompositionTracerTest {

    @Test
    fun shortNameIsPassedThroughUnchanged() {
        assertEquals("C(HomeScreen)", ComposeCompositionTracer.sectionName("C(HomeScreen)"))
    }

    @Test
    fun emptyNameFallsBackToPlaceholder() {
        // beginSection("") is legal but produces an unreadable marker.
        assertEquals("(anonymous)", ComposeCompositionTracer.sectionName(""))
    }

    @Test
    fun nameAtTheLimitIsNotTruncated() {
        val exactly = "x".repeat(120)
        assertEquals(exactly, ComposeCompositionTracer.sectionName(exactly))
    }

    @Test
    fun longNameIsTruncatedBelowTheAtraceLimit() {
        // Shape of the real offending name: a fully qualified nested lambda.
        val info = "C(" +
            "com.textvision.alistclient.ui.feature.home.HomeScreenKt\$HomeScreen\$1\$1\$1\$1".repeat(3) +
            ")"
        assertTrue("fixture must exceed the atrace limit", info.length > 127)

        val result = ComposeCompositionTracer.sectionName(info)

        assertTrue(
            "section name must stay under atrace's 127 char limit, was ${result.length}",
            result.length <= 127,
        )
        assertEquals("truncation must keep the head of the name", info.take(result.length), result)
    }
}
