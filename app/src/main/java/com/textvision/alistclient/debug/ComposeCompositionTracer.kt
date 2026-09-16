package com.textvision.alistclient.debug

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
                Trace.beginSection(info)
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

        override fun isTraceInProgress(): Boolean {
            // atrace records events whenever the system is collecting; we
            // could query `Trace.isTagEnabled`, but that returns whether
            // OUR trace tag is active, not whether ATRACE is. Always
            // returning true makes the runtime emit every event; the cost
            // is one beginSection / endSection per composable invocation,
            // which is exactly what we want.
            return true
        }
    }

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
