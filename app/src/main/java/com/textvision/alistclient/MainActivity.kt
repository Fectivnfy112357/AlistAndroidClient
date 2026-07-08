package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.TransferNotificationController
import com.textvision.alistclient.ui.theme.AlistClientTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("MainActivity must provide a SnackbarHostState via CompositionLocalProvider")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var transferManager: TransferManager
    @Inject lateinit var notificationController: TransferNotificationController
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationController.ensureChannels()
        transferManager.initialize()
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                AlistClientTheme {
                    AppNavHost(startAuthenticated = authRepository.loadSavedSession() != null, snackbarHostState = snackbarHostState)
                }
            }
        }
    }
}
