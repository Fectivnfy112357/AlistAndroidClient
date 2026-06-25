package com.textvision.alistclient

import android.app.Application
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AlistClientApp : Application() {
    @Inject lateinit var transferManager: TransferManager

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startupScope.launch { transferManager.markInterruptedOnStartup() }
    }
}
