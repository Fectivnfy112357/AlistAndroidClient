package com.textvision.alistclient.debug

import android.annotation.SuppressLint
import android.os.Build
import android.os.Trace
import android.util.Log
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi

/**
 * Debug-only [CompositionTracer] that forwards each composable event as an
 * `android.os.Trace` section. The sections show up as
 * `tracing_mark_write: B|<pid>|<info>` markers in System Trace (atrace) and
 * survive the same `Tools -> Profile -> System Trace` workflow already used
 * by `tools/perf/Capture-SystemTrace.sh`.
 *
 * Why this exists:
 *   `androidx.compose.runtime:runtime-tracing` ships an auto-init
 *   `CompositionTracingInitializer` that hooks Perfetto SDK tracing. Perfetto
 *   SDK tracing emits to a SEPARATE stream from atrace, and on this device
 *   Perfetto cannot be enabled (`/data/misc/perfetto-configs/trace_config.pbtxt`
 *   is missing — see `docs/testing/ui-performance-baseline-2026-07-15.md`).
 *   We need function-level hook data, but we don't have Perfetto. So we wire
 *   a tracer ourselves that talks to `android.os.Trace`, which IS captured
 *   by atrace on any device.
 *
 * # Usage in atrace output
 * After installing the dev build, run:
 * ```
 *   adb -s <serial> shell atrace --async_start -t 14 -c -b 16384 \
 *       -a com.textvision.alistclient sched gfx view input freq idle res am
 * ```
 * Each Compose function call inside our app's TGID appears as a B/E pair
 * with the function name as the section label:
 *
 *   tracing_mark_write: B|27960|HomeScreen
 *   tracing_mark_write: B|27960|DashboardList
 *   tracing_mark_write: E|27960|DashboardList
 *   tracing_mark_write: E|27960|HomeScreen
 *
 * Together with `dumpsys gfxinfo com.textvision.alistclient` (the existing
 * `tools/perf/Measure-HomeScroll-ColdWarm.sh` measure), these markers tell
 * us which composable functions spent the most main-thread time during the
 * first switch-to-Home + scroll window.
 *
 * # Scope
 * This is OPT-IN: only the debug build installs the tracer; the release
 * build (`release` flavor / BuildConfig.DEBUG == false) has a no-op
 * install path. Until `androidx.compose.runtime:runtime-tracing` is added
 * to `app/build.gradle.kts`, this is the only Compose-function tracing
 * path that works on this device.
 */
internal object ComposeCompositionTracer {

    @Volatile
    private var installed = false

    /**
     * `android.os.Trace.beginSection` rejects names longer than 127 chars with
     * `IllegalArgumentException("sectionName is too long")` — and that check only
     * runs while the app trace tag is enabled. Compose's `info` for nested
     * lambda composables (e.g. the `subcompose` lambdas inside `Scaffold`)
     * comfortably exceeds it, so the very first app-level trace of a debug build
     * killed the process. Truncation keeps the head of the name, which is the
     * composable's class name and the part worth reading.
     */
    internal fun sectionName(info: String): String {
        if (info.isEmpty()) return ANONYMOUS_SECTION
        return if (info.length <= MAX_SECTION_NAME_LEN) info else info.substring(0, MAX_SECTION_NAME_LEN)
    }

    private const val MAX_SECTION_NAME_LEN = 120
    private const val ANONYMOUS_SECTION = "(anonymous)"

    @SuppressLint("UnclosedTrace")
    @OptIn(InternalComposeTracingApi::class)
    private class AtraceCompositionTracer : CompositionTracer {
        private val logged = java.util.concurrent.atomic.AtomicBoolean(false)
        override fun traceEventStart(
            key: Int,
            dirty1: Int,
            dirty2: Int,
            info: String,
        ) {
            // On API 29+ Trace is callable from any thread; our tracer is
            // always invoked on the UI thread but the assertion isn't strict.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (info.isNotEmpty() && logged.compareAndSet(false, true)) {
                    Log.d(
                        "ComposeTracer",
                        "first traceEventStart: info='$info' key=$key dirty=$dirty1/$dirty2",
                    )
                }
                Trace.beginSection(sectionName(info))
            } else {
                if (logged.compareAndSet(false, true)) {
                    Log.d(
                        "ComposeTracer",
                        "first traceEventStart: API < 29; no-op (info='$info')",
                    )
                }
            }
        }

        override fun traceEventEnd() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Trace.endSection()
            }
        }

        // UnclosedTrace is suppressed on the class above: lint cannot see that
        // the Compose runtime pairs every traceEventStart() with exactly one
        // traceEventEnd() for the same composable, and the two calls therefore
        // live in different methods by design.

        override fun isTraceInProgress(): Boolean {
            // Only emit events while an app-level trace is actually being
            // recorded (`atrace -a <pkg>` sets TRACE_TAG_APP = ours). Returning
            // `true` unconditionally — as this used to — made the runtime run
            // the begin/end pair for every composable invocation even with no
            // trace running, which is pure overhead inside the very frame
            // timings this tracer exists to measure.
            //
            // `Trace.isEnabled()` is a JNI call and this is consulted once per
            // composable invocation, so memoise it briefly. A 50 ms staleness
            // window only trims the first frames after `atrace` starts.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
            val now = System.nanoTime()
            if (now - enabledCheckedAtNanos > ENABLED_CHECK_TTL_NANOS) {
                enabledCache = Trace.isEnabled()
                enabledCheckedAtNanos = now
            }
            return enabledCache
        }

        @Volatile
        private var enabledCache = false

        @Volatile
        private var enabledCheckedAtNanos = 0L
    }

    private const val ENABLED_CHECK_TTL_NANOS = 50_000_000L

    /**
     * Install the atrace-backed tracer into the Compose runtime.
     * Idempotent: subsequent calls are no-ops.
     *
     * Must run after `androidx.compose.runtime.Composer.setTracer` becomes
     * safe to call — in practice, calling this in `Application.onCreate`
     * works because the runtime holds the tracer as a static and any
     * composable will read it on first frame.
     */
    @OptIn(InternalComposeTracingApi::class)
    fun installIfNeeded() {
        if (installed) return
        android.util.Log.d(
            "ComposeTracer",
            "installIfNeeded called; constructing AtraceCompositionTracer",
        )
        Composer.setTracer(AtraceCompositionTracer())
        android.util.Log.d(
            "ComposeTracer",
            "Composer.setTracer returned; installed=true",
        )
        installed = true
    }
}
