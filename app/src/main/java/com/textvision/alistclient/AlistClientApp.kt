package com.textvision.alistclient

import android.app.Application
import com.textvision.alistclient.common.crash.SafeCrashHandler
import com.textvision.alistclient.debug.ComposeCompositionTracer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlistClientApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(
            SafeCrashHandler(this, Thread.getDefaultUncaughtExceptionHandler())
        )
        // Debug-only install of the CompositionTracer that emits composable
        // function names to `android.os.Trace`, which is captured by the
        // existing `tools/perf/Capture-SystemTrace.sh` workflow. In the
        // release build, BuildConfig.DEBUG is false and the call is a no-op,
        // so this has no impact on shipped binaries.
        if (BuildConfig.DEBUG) {
            ComposeCompositionTracer.installIfNeeded()
        }
    }
}
