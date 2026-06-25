package com.textvision.alistclient

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlistClientApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
