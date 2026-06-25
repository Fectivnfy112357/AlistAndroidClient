package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.TransferNotificationController
import com.textvision.alistclient.ui.theme.AlistClientTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var transferManager: TransferManager
    @Inject lateinit var notificationController: TransferNotificationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationController.ensureChannels()
        transferManager.initialize()
        setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = false)
            }
        }
    }
}
