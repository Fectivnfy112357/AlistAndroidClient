package com.textvision.alistclient

import android.app.Application
import com.textvision.alistclient.common.crash.SafeCrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlistClientApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(
            SafeCrashHandler(this, Thread.getDefaultUncaughtExceptionHandler())
        )
    }
}
